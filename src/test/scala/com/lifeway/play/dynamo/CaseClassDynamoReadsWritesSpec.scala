package com.lifeway.play.dynamo

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.functional.syntax._
import play.api.libs.json._

class CaseClassDynamoReadsWritesSpec extends WordSpec with MustMatchers {

  case class TestSample(name: String,
                        enabled: Boolean = true,
                        age: Byte,
                        emails: Set[String],
                        contactTimes: Set[Long],
                        stringList: Seq[String],
                        numList: Seq[Int],
                        objectType: NestedObject,
                        nestedObjectSet: Set[NestedObject],
                        nestedObjectSeq: Seq[NestedObject])

  case class NestedObject(title: String, age: Byte)

  object NestedObject {

    import DynamoJson._

    implicit val reads: Reads[NestedObject] = (
        (JsPath \ "title").read[String] and
          (JsPath \ "age").read[Byte]
    )(NestedObject.apply _)

    implicit val writes: Writes[NestedObject] = (
        (JsPath \ "title").write[String] and
          (JsPath \ "age").write[Byte]
    )(unlift(NestedObject.unapply))
  }

  object TestSample {

    import DynamoJson._

    implicit val reads: Reads[TestSample] = (
        (JsPath \ "name").read[String] and
          (JsPath \ "enabled_fl").readNullable[Boolean].map(_.getOrElse(true)) and
          (JsPath \ "age").read[Byte] and
          (JsPath \ "emails").read[Set[String]] and
          (JsPath \ "contactTimes").read[Set[Long]] and
          (JsPath \ "stringList").read[Seq[String]](DynamoJson.seqReads[String]) and
          (JsPath \ "numberList").read[Seq[Int]](DynamoJson.seqReads[Int]) and
          (JsPath \ "objectType").read[NestedObject](DynamoJson.objReads[NestedObject]) and
          (JsPath \ "nestedObjectSet")
            .read[Set[NestedObject]](DynamoJson.setReads[NestedObject](DynamoJson.objReads[NestedObject])) and
          (JsPath \ "nestedObjectSeq")
            .read[Seq[NestedObject]](DynamoJson.seqReads[NestedObject](DynamoJson.objReads[NestedObject]))
    )(TestSample.apply _)

    implicit val writes: Writes[TestSample] = (
        (JsPath \ "name").write[String] and
          (JsPath \ "enabled_fl").write[Boolean] and
          (JsPath \ "age").write[Byte] and
          (JsPath \ "emails").write[Set[String]] and
          (JsPath \ "contactTimes").write[Set[Long]] and
          (JsPath \ "stringList").write[Seq[String]](DynamoJson.seqWrites[String]) and
          (JsPath \ "numberList").write[Seq[Int]](DynamoJson.seqWrites[Int]) and
          (JsPath \ "objectType").write[NestedObject](DynamoJson.objWrites[NestedObject]) and
          (JsPath \ "nestedObjectSet")
            .write[Set[NestedObject]](DynamoJson.setWrites[NestedObject](DynamoJson.objWrites[NestedObject])) and
          (JsPath \ "nestedObjectSeq")
            .write[Seq[NestedObject]](DynamoJson.seqWrites[NestedObject](DynamoJson.objWrites[NestedObject]))
    )(unlift(TestSample.unapply))
  }

  "A Full Example with nested objects, sets, and sequences" should {
    val sampleJson = Json.parse("""
        |{
        |  "name": {
        |    "S": "Test User Id"
        |  },
        |  "enabled_fl": {
        |    "BOOL": true
        |  },
        |  "age": {
        |    "N": "31"
        |  },
        |  "emails": {
        |    "SS": [
        |      "user-test@mail.com",
        |      "user-email@email.com"
        |    ]
        |  },
        |  "contactTimes": {
        |    "NS": [
        |      "6442450941",
        |      "12884901882"
        |    ]
        |  },
        |  "stringList": {
        |    "L": [
        |      { "S": "item-1-string" },
        |      { "S": "item-2-string" }
        |    ]
        |  },
        |  "numberList": {
        |    "L": [
        |       { "N": "12345"},
        |       { "N": "45678"}
        |    ]
        |  },
        |  "objectType": {
        |    "M": {
        |      "title": {
        |         "S": "title-val"
        |      },
        |      "age" : {
        |         "N": "31"
        |      }
        |    }
        |  },
        |  "nestedObjectSet": {
        |     "L": [
        |        {
        |            "M": {
        |              "title": {
        |                "S": "title-val-a"
        |              },
        |              "age": {
        |                "N": "31"
        |              }
        |            }
        |        },
        |        {
        |            "M": {
        |              "title": {
        |                "S": "title-val-b"
        |              },
        |              "age": {
        |                "N": "32"
        |              }
        |            }
        |        }
        |     ]
        |  },
        |  "nestedObjectSeq": {
        |     "L": [
        |        {
        |            "M": {
        |              "title": {
        |                "S": "title-val-c"
        |              },
        |              "age": {
        |                "N": "33"
        |              }
        |            }
        |        },
        |        {
        |            "M": {
        |              "title": {
        |                "S": "title-val-d"
        |              },
        |              "age": {
        |                "N": "34"
        |              }
        |            }
        |        }
        |     ]
        |  }
        |}
      """.stripMargin)

    val sampleVal = TestSample("Test User Id",
                               enabled = true,
                               31,
                               Set("user-test@mail.com", "user-email@email.com"),
                               Set(6442450941l, 12884901882l),
                               Seq("item-1-string", "item-2-string"),
                               Seq(12345, 45678),
                               NestedObject("title-val", 31),
                               Set(NestedObject("title-val-a", 31), NestedObject("title-val-b", 32)),
                               Seq(NestedObject("title-val-c", 33), NestedObject("title-val-d", 34)))

    "read from user defined case classes that have defined readers" in {
      sampleJson.as[TestSample] mustEqual sampleVal
    }

    "write from user defined case classes that have defined writers" in {
      TestSample.writes.writes(sampleVal) mustEqual sampleJson
    }
  }
}
