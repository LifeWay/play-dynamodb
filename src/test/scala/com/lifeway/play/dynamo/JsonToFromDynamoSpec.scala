package com.lifeway.play.dynamo

import com.lifeway.play.dynamo
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsObject, Json}

import scala.util.Try

class JsonToFromDynamoSpec extends WordSpec with MustMatchers {

  case class NestedObject(title: String, age: Byte)

  object NestedObject {
    implicit val reads = Json.reads[NestedObject]
    implicit val writes = Json.writes[NestedObject]
  }

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
  object TestSample {
    implicit val reads = Json.reads[TestSample]
    implicit val writes = Json.writes[TestSample]
  }

  "A Full Example with nested objects, sets, and sequences" should {
    val sampleJson = Json.parse("""
        |{
        |  "name": {
        |    "S": "Test User Id"
        |  },
        |  "enabled": {
        |    "BOOL": true
        |  },
        |  "age": {
        |    "N": "31"
        |  },
        |  "emails": {
        |    "L": [
        |      { "S": "user-test@mail.com" },
        |      { "S": "user-email@email.com" }
        |    ]
        |  },
        |  "contactTimes": {
        |    "L": [
        |      { "N": "6442450941" },
        |      { "N": "12884901882" }
        |    ]
        |  },
        |  "stringList": {
        |    "L": [
        |      { "S": "item-1-string" },
        |      { "S": "item-2-string" }
        |    ]
        |  },
        |  "numList": {
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

    "read from DynamoDB Json through direct Json Converters to standard case class readers" in {
      import DynamoJsonConverters.Converters

      val result: Try[TestSample] = sampleJson.as[JsObject].fromDynamoJson.map(_.as[TestSample])
      result.isSuccess mustEqual true
      result.get mustEqual sampleVal
    }

    "read from an invalid DynamoDB JSON through direct Json Converters should return an failed Try in the event of non-dynamo Json" in {
      import DynamoJsonConverters.Converters

      val json = Json.parse(
        """
          |{
          |   "key": "This is normal Json",
          |   "otherThing": true
          |}
        """.stripMargin)

      val result: Try[TestSample] = json.as[JsObject].fromDynamoJson.map(_.as[TestSample])
      result.isSuccess mustEqual false
    }

    "write to DynamoDB Json through direct Json converters that occur after the standard case class Json writers" in {
      import dynamo.DynamoJsonConverters.Converters

      Json.toJson(sampleVal).as[JsObject].toDynamoJson mustEqual sampleJson
    }
  }
}
