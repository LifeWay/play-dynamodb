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


## Installing
Your application must provide the following libraries:
* Play 2.5
* Play WS 2.5
* aws-java-sdk-core version 1.11.+ 
`NOTE: We only require the AWS CORE part of the SDK so we can better handle getting the AWS creds via a Credentials Provider giving you the freedom to provide creds in many different ways - no other part of the AWS library is used.`
* "net.kaliber" %% "play-s3" % "8.0.0"
We use this fine library by Kaliber (https://github.com/Kaliber/play-s3) to handle the version 4 signing process for us. Unfortunately, Kaliber has chosen not to seperate the AWS version 4 signing process for Play WS from their S3 plugin. For now, that means you have to pull down this S3 library as well to use our DynamoDB library.


### License

This software is licensed under the Apache 2 license, quoted below.

Copyright (C) 2016 LifeWay Christian Resources. (https://www.lifeway.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.