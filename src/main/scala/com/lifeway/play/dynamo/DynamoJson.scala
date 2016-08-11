package com.lifeway.play.dynamo

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsObject, _}

object DynamoJson {
  def typeReader[A](f: (JsObject => JsResult[A])) = new Reads[A] {
    def reads(json: JsValue): JsResult[A] = json match {
      case obj: JsObject => f(obj)
      case _             => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsobject"))))
    }
  }

  //I is input type, O is output type
  private def reads[I, O](typeChar: String, f: I => O)(implicit rds: Reads[I]) =
    typeReader[O](x => (x \ typeChar).validate[I](rds).map(f))

  private def writes[I](typeChar: String, f: I => JsValue): Writes[I] = new Writes[I] {
    override def writes(input: I): JsValue = Json.obj(typeChar -> f(input))
  }

  private def numReads[T](f: String => T) = reads[String, T]("N", f)(Reads.StringReads)

  private def numWrites[T](f: T => String) = writes[T]("N", x => JsString(f(x)))

  private def internalSetReads[T](typeChar: String, f: String => T) =
    reads[JsArray, Set[T]](typeChar, _.value.map(x => f(x.as[JsString].value)).toSet)

  private def internalSetWrites[T](typeChar: String, f: T => String) =
    writes[Set[T]](typeChar, x => JsArray(x.toSeq.map(x => JsString(f(x)))))

  val nullWrites: Writes[Null] = writes("NULL", x => JsBoolean(true))

  implicit val byteReads: Reads[Byte] = numReads[Byte](_.toByte)
  implicit val byteWrites: Writes[Byte] = numWrites[Byte](_.toString)

  implicit val shortReads: Reads[Short] = numReads[Short](_.toShort)
  implicit val shortWrites: Writes[Short] = numWrites[Short](_.toString)

  implicit val intReads: Reads[Int] = numReads[Int](_.toInt)
  implicit val intWrites: Writes[Int] = numWrites[Int](_.toString)

  implicit val longReads: Reads[Long] = numReads[Long](_.toLong)
  implicit val longWrites: Writes[Long] = numWrites[Long](_.toString)

  implicit val floatReads: Reads[Float] = numReads[Float](_.toFloat)
  implicit val floatWrites: Writes[Float] = numWrites[Float](_.toString)

  implicit val doubleReads: Reads[Double] = numReads[Double](_.toDouble)
  implicit val doubleWrites: Writes[Double] = numWrites[Double](_.toString)

  implicit val bigDecimalReads: Reads[BigDecimal] = numReads[BigDecimal](x => BigDecimal(x))
  implicit val bigDecimalWrites: Writes[BigDecimal] = numWrites[BigDecimal](_.toString)

  implicit val stringReads: Reads[String] = reads[String, String]("S", x => x)(Reads.StringReads)
  implicit val stringWrites: Writes[String] = writes[String]("S", x => JsString(x))

  implicit val booleanReads: Reads[Boolean] = reads[Boolean, Boolean]("BOOL", x => x)(Reads.BooleanReads)
  implicit val booleanWrites: Writes[Boolean] = writes[Boolean]("BOOL", x => JsBoolean(x))

  implicit val byteSetReads: Reads[Set[Byte]] = internalSetReads[Byte]("NS", x => x.toByte)
  implicit val byteSetWrites: Writes[Set[Byte]] = internalSetWrites[Byte]("NS", _.toString)

  implicit val shortSetReads: Reads[Set[Short]] = internalSetReads[Short]("NS", _.toShort)
  implicit val shortSetWrites: Writes[Set[Short]] = internalSetWrites[Short]("NS", _.toString)

  implicit val intSetReads: Reads[Set[Int]] = internalSetReads[Int]("NS", _.toInt)
  implicit val intSetWrites: Writes[Set[Int]] = internalSetWrites[Int]("NS", _.toString)

  implicit val longSetReads: Reads[Set[Long]] = internalSetReads[Long]("NS", _.toLong)
  implicit val longSetWrites: Writes[Set[Long]] = internalSetWrites[Long]("NS", _.toString)

  implicit val floatSetReads: Reads[Set[Float]] = internalSetReads[Float]("NS", _.toFloat)
  implicit val floatSetWrites: Writes[Set[Float]] = internalSetWrites[Float]("NS", _.toString)

  implicit val doubleSetReads: Reads[Set[Double]] = internalSetReads[Double]("NS", _.toDouble)
  implicit val doubleSetWrites: Writes[Set[Double]] = internalSetWrites[Double]("NS", _.toString)

  implicit val bigDecimalSetReads: Reads[Set[BigDecimal]] = internalSetReads[BigDecimal]("NS", x => BigDecimal(x))
  implicit val bigDecimalSetWrites: Writes[Set[BigDecimal]] = internalSetWrites[BigDecimal]("NS", _.toString)

  implicit val stringSetReads: Reads[Set[String]] = internalSetReads[String]("SS", x => x)
  implicit val stringSetWrites: Writes[Set[String]] = internalSetWrites[String]("SS", x => x)

  def setReads[T](implicit rds: Reads[T]): Reads[Set[T]] =
    typeReader[Set[T]](x =>
          (x \ "L").validate[JsArray].flatMap { i =>
        val rx = i.value.map(y => rds.reads(y))
        if (rx.exists(t => t.isError))
          JsError("One of the values in the array did not parse into given type for setReads[T]")
        else JsSuccess(rx.map(_.get).toSet)
    })

  def setWrites[T](implicit wts: Writes[T]): Writes[Set[T]] =
    writes[Set[T]]("L", x => JsArray(x.toSeq.map(y => wts.writes(y))))

  def seqReads[T](implicit rds: Reads[T]): Reads[Seq[T]] =
    typeReader[Seq[T]](x =>
          (x \ "L").validate[JsArray].flatMap { i =>
        val rx = i.value.map(y => rds.reads(y))
        if (rx.exists(t => t.isError))
          JsError("One of the values in the array did not parse into given type for seqReads[T]")
        else JsSuccess(rx.map(_.get))
    })

  def seqWrites[T](implicit wts: Writes[T]): Writes[Seq[T]] =
    writes[Seq[T]]("L", x => JsArray(x.map(y => wts.writes(y))))

  /**
    * The object type will have a Json Object underneath an M value. The contents of this object
    * will need to be read with another reader (i.e. a reader from a case class) which itself uses the dynamo reader.
    *
    * The best way to use this function is when building your reads & writers, you should import all of DynamoJson with
    * DynamoJson._ this will import all of the implicit reads & writes which will cause this reader, when used to use
    * other Dynamo Readers instead of the default ones provided by Play. For example if you had:
    * ...[Reads](DynamoJson.objReads[YourType]) then, if YourType had provided it's own reader for its members, which
    * may be dynamoStrings - this will automatically pick those up instead of using the default string readers for JSON.
    */
  def objReads[T](implicit rds: Reads[T]): Reads[T] =
    typeReader[T](x => (x \ "M").validate[JsObject].flatMap(x => rds.reads(x)))

  def objWrites[T](implicit wts: Writes[T]): Writes[T] =
    writes[T]("M", x => wts.writes(x))
}
