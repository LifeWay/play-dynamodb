package com.lifeway.play.dynamo

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.functional.syntax._
import play.api.libs.json._

class DynamoJsonSpec extends WordSpec with MustMatchers {
  def readTest[T](r: => Reads[T], json: JsValue, testVal: T): Unit = {
    val read = r.reads(json)
    read.isSuccess mustEqual true
    read.get mustEqual testVal
  }

  def writeTest[T](w: => Writes[T], json: JsValue, testVal: T): Unit = {
    w.writes(testVal) mustEqual json
  }

  case class TestObject(someKey: String, someFlag: Boolean)

  object TestObject {

    import DynamoJson._

    implicit val reads: Reads[TestObject] = (
      (JsPath \ "someKey").read[String] and
        (JsPath \ "someFlag").read[Boolean]
      )(TestObject.apply _)

    implicit val writes: Writes[TestObject] = (
      (JsPath \ "someKey").write[String] and
        (JsPath \ "someFlag").write[Boolean]
      )(unlift(TestObject.unapply))
  }

  "A Byte type" should {
    val testJson = Json.parse("""
        |{ "N": "1" }
      """.stripMargin)
    val testVal: Byte = 1

    "read from Dynamo JSON" in readTest(DynamoJson.byteReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.byteWrites, testJson, testVal)
  }

  "A Short type" should {
    val testJson = Json.parse("""
        |{ "N": "1024" }
      """.stripMargin)
    val testVal: Short = 1024

    "read from Dynamo JSON" in readTest(DynamoJson.shortReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.shortWrites, testJson, testVal)
  }

  "A Int type" should {
    val testJson = Json.parse("""
        |{ "N": "60000" }
      """.stripMargin)
    val testVal: Int = 60000

    "read from Dynamo JSON" in readTest(DynamoJson.intReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.intWrites, testJson, testVal)
  }

  "A Long type" should {
    val testJson = Json.parse("""
        |{ "N": "6442450941" }
      """.stripMargin)
    val testVal: Long = 6442450941l

    "read from Dynamo JSON" in readTest(DynamoJson.longReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.longWrites, testJson, testVal)
  }

  "A Float type" should {
    val testJson = Json.parse("""
        |{ "N": "12.35" }
      """.stripMargin)
    val testVal: Float = 12.35f

    "read from Dynamo JSON" in readTest(DynamoJson.floatReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.floatWrites, testJson, testVal)
  }

  "A Double type" should {
    val testJson = Json.parse("""
        |{ "N": "6.44245094135E9" }
      """.stripMargin)
    val testVal: Double = 6442450941.35d

    "read from Dynamo JSON" in readTest(DynamoJson.doubleReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.doubleWrites, testJson, testVal)
  }

  "A BigDecimal type" should {
    val testJson = Json.parse("""
        |{ "N": "6442450941.35" }
      """.stripMargin)
    val testVal: BigDecimal = BigDecimal("6442450941.35")

    "read from Dynamo JSON" in readTest(DynamoJson.bigDecimalReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.bigDecimalWrites, testJson, testVal)
  }

  "A String type" should {
    val testJson = Json.parse("""
        |{ "S": "This is a String." }
      """.stripMargin)
    val testVal: String = "This is a String."

    "read from Dynamo JSON" in readTest(DynamoJson.stringReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.stringWrites, testJson, testVal)
  }

  "A Boolean type" should {
    val testJson = Json.parse("""
        |{ "BOOL": true }
      """.stripMargin)
    val testVal: Boolean = true

    "read from Dynamo JSON" in readTest(DynamoJson.booleanReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.booleanWrites, testJson, testVal)
  }

  "A Set[Byte] type" should {
    val testJson = Json.parse("""
        |{
        |  "NS": [
        |    "1",
        |    "2"
        |  ]
        |}
      """.stripMargin)

    val testVal: Set[Byte] = Set(1, 2)

    "read from Dynamo JSON" in readTest(DynamoJson.byteSetReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.byteSetWrites, testJson, testVal)
  }

  "A Set[Short] type" should {
    val testJson = Json.parse("""
        |{
        |  "NS": [
        |    "1024",
        |    "2048"
        |  ]
        |}
      """.stripMargin)

    val testVal: Set[Short] = Set(1024, 2048)

    "read from Dynamo JSON" in readTest(DynamoJson.shortSetReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.shortSetWrites, testJson, testVal)
  }

  "A Set[Int] type" should {
    val testJson = Json.parse("""
        |{
        |  "NS": [
        |    "60000",
        |    "120000"
        |  ]
        |}
      """.stripMargin)

    val testVal: Set[Int] = Set(60000, 120000)

    "read from Dynamo JSON" in readTest(DynamoJson.intSetReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.intSetWrites, testJson, testVal)
  }

  "A Set[Long] type" should {
    val testJson = Json.parse("""
        |{
        |  "NS": [
        |    "6442450941",
        |    "12884901882"
        |  ]
        |}
      """.stripMargin)

    val testVal: Set[Long] = Set(6442450941l, 12884901882l)

    "read from Dynamo JSON" in readTest(DynamoJson.longSetReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.longSetWrites, testJson, testVal)
  }

  "A Set[Float] type" should {
    val testJson = Json.parse("""
        |{
        |  "NS": [
        |    "12.35",
        |    "12305.98"
        |  ]
        |}
      """.stripMargin)

    val testVal: Set[Float] = Set(12.35f, 12305.98f)

    "read from Dynamo JSON" in readTest(DynamoJson.floatSetReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.floatSetWrites, testJson, testVal)
  }

  "A Set[Double] type" should {

    val testJson = Json.parse("""
        |{
        |  "NS": [
        |    "6.44245094135E9",
        |    "12305.98"
        |  ]
        |}
      """.stripMargin)

    val testVal: Set[Double] = Set(6442450941.35d, 12305.98d)

    "read from Dynamo JSON" in readTest(DynamoJson.doubleSetReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.doubleSetWrites, testJson, testVal)
  }

  "A Set[BigDecimal] type" should {
    val testJson = Json.parse("""
        |{
        |   "NS":[
        |     "6442450941.35",
        |     "7642450941232.35"
        |   ]
        |}
      """.stripMargin)
    val testVal: Set[BigDecimal] = Set(BigDecimal("6442450941.35"), BigDecimal("7642450941232.35"))

    "read from Dynamo JSON" in readTest(DynamoJson.bigDecimalSetReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.bigDecimalSetWrites, testJson, testVal)
  }

  "A Set[String] type" should {
    val testJson = Json.parse("""
        |{
        |  "SS": [
        |    "0A5EF04F-B7F8-4014-8665-5230D4B25D0D",
        |    "7E02F28A-F2F9-4C40-AF53-E6B41EB049D"
        |  ]
        |}
      """.stripMargin)

    val testVal = Set("0A5EF04F-B7F8-4014-8665-5230D4B25D0D", "7E02F28A-F2F9-4C40-AF53-E6B41EB049D")

    "read from Dynamo JSON" in readTest(DynamoJson.stringSetReads, testJson, testVal)

    "write to Dynamo JSON" in writeTest(DynamoJson.stringSetWrites, testJson, testVal)
  }

  "A Set[T] type" should {
    val testJson = Json.parse(
        """
        |{
        |  "L": [
        |    {"S": "item1"},
        |    {"S": "item2"}
        |  ]
        |}
      """.stripMargin
    )

    val testVal = Set("item1", "item2")

    "read from DynamoJson" in readTest(DynamoJson.setReads[String](DynamoJson.stringReads), testJson, testVal)

    "write to DynamoJson" in writeTest(DynamoJson.setWrites[String](DynamoJson.stringWrites), testJson, testVal)
  }

  "A Seq[T] type" should {
    val testJson = Json.parse(
        """
        |{
        |  "L": [
        |    {"S": "item1"},
        |    {"S": "item2"}
        |  ]
        |}
      """.stripMargin
    )

    val testVal = Seq("item1", "item2")

    "read from DynamoJson" in readTest(DynamoJson.seqReads[String](DynamoJson.stringReads), testJson, testVal)

    "write to DynamoJson" in writeTest(DynamoJson.seqWrites[String](DynamoJson.stringWrites), testJson, testVal)
  }

  "A Object[T] type" should {
    val testJson = Json.parse(
        """
        |{
        |  "M": {
        |    "someKey": {"S": "value1"},
        |    "someFlag": {"BOOL": true}
        |  }
        |}
      """.stripMargin
    )

    val testVal = TestObject("value1", someFlag = true)

    "read from DynamoJson" in readTest(DynamoJson.objReads[TestObject], testJson, testVal)

    "write to DynamoJson" in writeTest(DynamoJson.objWrites[TestObject], testJson, testVal)
  }
}
