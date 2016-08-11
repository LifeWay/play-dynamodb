package com.lifeway.play.dynamo

import play.api.libs.json._

import scala.collection.GenTraversable
import scala.util.Try

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
      * Note that this returns a Try[JsObject], because if a non dynamoDB json object is passed to it (which we can't
      * prevent), or if a currently unsupported DynamoDB type was in the object (i.e. Binary or BinarySet), then we
      * would throw a match exception.
      */
    def fromDynamoJson: Try[JsObject] = {
      def objectConversion(i: JsObject): JsObject =
        JsObject(i.fields.flatMap {
          case (k, JsObject(wrappedObj)) =>
            wrappedObj.map {
              case ("S", JsString(v))     => (k, JsString(v))
              case ("NULL", _)            => (k, JsBoolean(true))
              case ("BOOL", JsBoolean(v)) => (k, JsBoolean(v))
              case ("N", JsString(v))     => (k, JsNumber(BigDecimal(v)))
              case ("L", v: JsArray)      => (k, arrayConversion(v))
              case ("M", m: JsObject)     => (k, objectConversion(m))
              case ("SS", v: JsArray)     => (k, v)
              case ("NS", v: JsArray)     => (k, JsArray(v.value.map(x => JsNumber(BigDecimal(x.as[JsString].value)))))
            }
        })

      def arrayConversion(i: JsArray): JsValue =
        JsArray(i.value.flatMap {
          case x: JsNumber => GenTraversable(x)
          case x: JsString => GenTraversable(x)
          case x: JsObject =>
            x.fields.map {
              case ("S", v: JsString)     => v
              case ("NULL", _)            => JsNull
              case ("BOOL", v: JsBoolean) => v
              case ("N", JsString(v))     => JsNumber(BigDecimal(v))
              case ("L", v: JsArray)      => arrayConversion(v)
              case ("M", m: JsObject)     => objectConversion(m)
              case ("SS", v: JsArray)     => v
              case ("NS", v: JsArray)     => JsArray(v.value.map(x => JsNumber(BigDecimal(x.as[JsString].value))))
            }
        })

      Try(objectConversion(input))
    }
  }
}
