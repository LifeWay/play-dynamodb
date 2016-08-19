package com.lifeway.play.dynamo

import org.scalactic._
import play.api.libs.json._

import scala.collection.GenTraversable

object DynamoJsonConverters {

  /**
    * Extend JsObject type so that we can easily convert from Json <---> DynamoDBJson
    *
    * @param input
    */
  implicit class Converters(input: JsObject) {

    /**
      * Given a JsObject that you wish to store in DynamoDB, this will turn that JsObject into a DynamoDB Json value.
      * This method is not stack-safe as it recursively walks through your input Json and transforms the values into
      * their Dynamo forms.
      *
      * One easy way to use this function is to use Play's standard Json writes method to create a standard JSON object
      */
    def toDynamoJson: JsObject = {
      def objectConversion(i: JsObject): JsObject =
        JsObject(i.fields.map {
          case (k, JsString(v))  => (k, DynamoJson.stringWrites.writes(v))
          case (k, JsNull)       => (k, DynamoJson.nullWrites.writes(null))
          case (k, JsBoolean(v)) => (k, DynamoJson.booleanWrites.writes(v))
          case (k, JsNumber(v))  => (k, Json.obj("N" -> JsString(v.toString)))
          case (k, v: JsArray)   => (k, arrayConversion(v))
          case (k, o: JsObject)  => (k, Json.obj("M" -> objectConversion(o)))
        })

      def arrayConversion(i: JsArray): JsValue =
        JsObject(Seq("L" -> JsArray(i.value.map {
          case JsString(v)  => DynamoJson.stringWrites.writes(v)
          case JsNull       => DynamoJson.nullWrites.writes(null)
          case JsBoolean(v) => DynamoJson.booleanWrites.writes(v)
          case JsNumber(v)  => Json.obj("N" -> JsString(v.toString))
          case v: JsArray   => arrayConversion(v)
          case o: JsObject  => Json.obj("M" -> objectConversion(o))
        })))

      objectConversion(input)
    }

    /**
      * Given a JsObject that was stored in DynamoDB, this will turn that Dynamo json into a standard JsObject will all of
      * the dynamo types removed. Typically, you would use this to transform the Dynamo response back to a JsValue type
      * that you could then pass to Play's standard Json writes method.
      *
      * Note that this returns a JsObject Or Every[ErrorMessage], because if a non dynamoDB json object is passed to it
      * (which we can't prevent), or if a currently unsupported DynamoDB type was in the object (i.e. Binary or
      * BinarySet), then we can't successfully process the request. Rather, we return the errors to the user of all
      * of the fields that we couldn't convert.
      */
    def fromDynamoJson: JsObject Or Every[ErrorMessage] = {

      def objectConversion(i: JsObject): JsObject Or Every[ErrorMessage] = {
        val temp: Seq[(String, JsValue) Or Every[ErrorMessage]] = i.fields.flatMap {
          case (k, JsObject(wrappedObj)) =>
            wrappedObj.map {
              case ("S", JsString(v))     => Good(k -> JsString(v))
              case ("NULL", _)            => Good(k, JsBoolean(true))
              case ("BOOL", JsBoolean(v)) => Good((k, JsBoolean(v)))
              case ("N", JsString(v))     => Good((k, JsNumber(BigDecimal(v))))
              case ("L", v: JsArray) =>
                for {
                  conv <- arrayConversion(v)
                } yield (k, conv)
              case ("M", m: JsObject) =>
                for {
                  conv <- objectConversion(m)
                } yield (k, conv)
              case ("SS", v: JsArray) => Good((k, v))
              case ("NS", v: JsArray) =>
                Good((k, JsArray(v.value.map(x => JsNumber(BigDecimal(x.as[JsString].value))))))
              case (t, _) => Bad(One(s"The field `$t` under field `$k` is not a valid / supported DynamoDB type"))
            }
          case (k, _) => Seq(Bad(One(s"The value for field `$k` is not a valid DynamoDB type.")))
        }

        val (goodSeq, badSeq) = temp.partition(_.isGood)
        if (badSeq.isEmpty) Good(JsObject(goodSeq.map(_.get)))
        else Bad(Every.from(badSeq.flatMap(_.swap.get.map(x => x))).get)
      }

      def arrayConversion(i: JsArray): JsValue Or Every[ErrorMessage] = {
        val temp: Seq[JsValue Or Every[ErrorMessage]] = i.value.flatMap {
          case x: JsNumber => GenTraversable(Good(x))
          case x: JsString => GenTraversable(Good(x))
          case x: JsObject =>
            x.fields.map {
              case ("S", v: JsString)     => Good(v)
              case ("NULL", _)            => Good(JsNull)
              case ("BOOL", v: JsBoolean) => Good(v)
              case ("N", JsString(v))     => Good(JsNumber(BigDecimal(v)))
              case ("L", v: JsArray)      => arrayConversion(v)
              case ("M", m: JsObject)     => objectConversion(m)
              case ("SS", v: JsArray)     => Good(v)
              case ("NS", v: JsArray)     => Good(JsArray(v.value.map(x => JsNumber(BigDecimal(x.as[JsString].value)))))
              case (t, _)                 => Bad(One(s"`$t` is not a valid / supported DynamoDB type in an array"))
            }
          case x: JsValue => Seq(Bad(One(s"`$x` is not a valid / supported DynamoDB type in a DynamoDB array")))
        }

        val (goodSeq, badSeq) = temp.partition(_.isGood)
        if (badSeq.isEmpty) Good(JsArray(goodSeq.map(_.get)))
        else Bad(Every.from(badSeq.flatMap(_.swap.get.map(x => x))).get)
      }

      objectConversion(input)
    }

    /**
      * Uses the fromDynamoJson conversion to convert from a DynamoDB Json into the given type T using the provided
      * implicit reads for type T.
      *
      * @param rds
      * @tparam T
      * @return
      */
    def dynamoReads[T](implicit rds: Reads[T]): T Or Every[ErrorMessage] = {
      input.fromDynamoJson.flatMap(
          _.validate[T].fold(
              errors => Bad(Every.from(errors.map(_.toString)).get),
              success => Good(success)
          ))
    }
  }
}
