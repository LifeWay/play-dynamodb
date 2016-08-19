[![Build Status](https://travis-ci.org/lifeway/play-dynamodb.svg?branch=master)](https://travis-ci.org/lifeway/play-dynamodb)

# Play DynamoDB

Provides support for using DynamoDB directly via the low-level REST API without using the blocking nature of the AWS SDK for API calls.

## Features
* Non-blocking, async by using the Play WS library for all requests directly to the low-level DynamoDB rest API
* Auto retries failed requests with return codes of 500 or 400 level AWS errors that are allowed to be retried with a jittered back-off. This is similar to what the AWS Java SDK provides you out of the box. The max number of retries is configurable.
* Takes care of DynamoDB Json via one of two mechanisms desribed further below.

## Dealing with DynamoDB Json
One of the primary benefits you get from using the AWS Java SDK, or building a more native scala library ontop of the SDK is the ability to work with the SDK's mappers for dealing with Dynamo's Json.

We were unable to figure out how to use only the mapper of the SDK apart from actually making API calls, so we had to provide a handle DynamoDB json without putting to much pain on the developer.

There are two ways in which you may use this library to handle DynamoDB Json.
1. Implement your own Reads / Writes for your types, using the reads / writes converters supplied by this library.
2. Use Play's built in Reads / Writes to get your types to Json first, then pass that Json through a DynamoJsonConverter which will transform ANY JsObject into a Dynamo JSON Object. The same process can occur for reading Dynamo JSON Object, which can transform Dynamo JSON back to "normal" JSON that can be parsed by Play's built in JSON reads.

In general, it is easiest to use method #2. Method #1 works, but is not recommended for more complex types and causes you to write more lines of code. However, method #1 may be appealing to you if you are trying to keep your storage in Dynamo as precise as possible. Method #1 allows you to store native DynamoDB types like String Sets and Number Sets in the most efficient way in DynamoDB.

To see an example of #1, check out [this test](src/test/scala/com/lifeway/play/dynamo/CaseClassDynamoReadsWritesSpec.scala)
To see an example of #2, check out [this test](src/test/scala/com/lifeway/play/dynamo/JsonToFromDynamoSpec.scala)


## Installing (SBT)
`"com.lifeway" %% "play-dynamodb" % "0.1`
Additionally, your application must provide the following additional dependencies: 
* Play 2.5
* Play WS 2.5
* aws-java-sdk-core version 1.11.+ (for example: `"com.amazonaws" % "aws-java-sdk-core" % "1.11.27"`

  `NOTE: We only require the AWS CORE part of the SDK so we can better handle getting the AWS creds via a Credentials Provider giving you the freedom to provide creds in many different ways - no other part of the AWS library is used.`
* `"net.kaliber" %% "play-s3" % "8.0.0"`

  We use this library by Kaliber (https://github.com/Kaliber/play-s3) to handle the version 4 signing process for us. Unfortunately, Kaliber has chosen not to seperate the AWS version 4 signing process for Play WS from their S3 plugin. For now, that means you have to pull down this S3 library as well to use our DynamoDB library.
* `"org.scalactic" %% "scalactic" % "3.0.0"`

## Using in your project

Currently, we only support compile time DI as of the 0.1 release. To use, add `with WSDynamoDBComponents` to your Application's Components. You will be required to implement a `def awsCredProvider` method that provides an AWS credentials provider. See below for an example of how to do that. You will now have a `wsDynamoAPI` value available to use in your components.

## Example
The following example assumes you have a DynamoDB table created called `demo.person` and that table has a primary parition key `personId` of type String.

This example is using method #2 from above.

```scala
package controllers

import com.lifeway.play.dynamo.DynamoJsonConverters._
import com.lifeway.play.dynamo.WSDynamoAPI
import org.scalactic._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsObject, Json}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

class TryIt(wsDynamo: WSDynamoAPI) extends Controller {

  case class Songs(songName: String, artist: String)

  object Songs {
    implicit val reads = Json.reads[Songs]
    implicit val writes = Json.writes[Songs]
  }

  case class Person(personId: String, emailAddr: String, favoriteSongs: Seq[Songs])

  object Person {
    implicit val reads = Json.reads[Person]
    implicit val writes = Json.writes[Person]
  }

  def putPerson() = Action.async(parse.json) { implicit request =>
    request.body
      .validate[Person]
      .fold(
        errors => Future.successful(BadRequest(JsError.toJson(errors))),
        person => {
          val req = Json.obj(
            "TableName" -> s"demo.person",
            "ReturnConsumedCapacity" -> "NONE",
            "Item" -> Json.toJson(person).as[JsObject].toDynamoJson
          )

          wsDynamo.putItem(req).map { resp =>
            if (resp.status == 200)
              Ok("Successfully saved person")
            else {
              InternalServerError("There was a problem saving the person..")
            }
          }
        }
      )
  }

  def getPerson(personId: String) = Action.async { implicit request =>
    val req = Json.obj(
      "TableName"              -> s"demo.person",
      "Key"                    -> Json.obj("personId" -> Json.obj("S" -> personId)),
      "ReturnConsumedCapacity" -> "NONE",
      "ConsistentRead"         -> false
    )

    wsDynamo.getItem(req).map { resp =>
      if (resp.status == 200) {
        itemReader[Person](resp.json) match {
          case Good(Some(p)) => Ok(Json.toJson(p))
          case Good(None)    => NotFound("Person not found by that ID")
          case Bad(errors)   => InternalServerError(errors.toSeq.toString)
        }
      } else {
        InternalServerError("Unable to communicate successfully with DynamoDB")
      }
    }
  }

  def itemReader[T](i: JsValue)(implicit rds: Reads[T]): Option[T] Or Every[ErrorMessage] = {
    (i \ "Item")
      .asOpt[JsObject]
      .fold[Or[Option[T], Every[ErrorMessage]]](Good(Option.empty[T]))(_.dynamoReads[T](rds).map(x => Some(x)))
  }
}

```

The important parts of the example are `Json.toJson(person).as[JsObject].toDynamoJson` which is able to convert a JsObject created by the standard play writes method into DynamoDB formatted JSON and the cooresponding `record.fromDynamoJson` which takes a JsObject that is DynamoJson and turns it back into Json. 

Please note however that the `fromDynamoJson` returns a type of `JsObject Or Every[ErrorMessage]`. Errors should be rare, but may occur if:
1.) Dynamo ever adds new types and you used a different library to create the original record. (Currently, we don't support Binary or Binary Set types either, but this shouldn't be a problem if you always are reading objects that were written using the `toDynamoJson` call)
2.) You pass a non-dynamoDB json object to this method. Clearly, you should expect failure here as the JSON you passed cannot be parsed as Dynamo formatted json.


### License

This software is licensed under the Apache 2 license, quoted below.

Copyright (C) 2016 LifeWay Christian Resources. (https://www.lifeway.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.