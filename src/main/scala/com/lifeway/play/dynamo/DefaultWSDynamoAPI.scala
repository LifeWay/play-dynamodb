package com.lifeway.play.dynamo

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Scheduler}
import com.amazonaws.auth.{AWSCredentialsProvider, BasicSessionCredentials}
import com.typesafe.config.Config
import fly.play.aws.{Aws4Signer, AwsCredentials, AwsRequestHolder}
import play.api.libs.json.{JsObject, JsString}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultWSDynamoAPI @Inject()(credProvider: AWSCredentialsProvider, ws: WSClient, config: Config, as: ActorSystem)
    extends WSDynamoAPI {

  import DefaultWSDynamoAPI._

  val serviceUrl = config.getString("wsDynamoDB.url")
  val serviceRegion = config.getString("wsDynamoDB.serviceRegion")
  val serviceRetries = config.getInt("wsDynamoDB.serviceRetriesBeforeFail")

  private val serviceSigner = (wsReq: WSRequest, ec: ExecutionContext) =>
    retryableWSSigner(wsReq, credProvider, "dynamodb", serviceRegion, serviceRetries)(ec, as.scheduler)

  private val dynamoWSRequest =
    ws.url(serviceUrl).withHeaders("Content-Type" -> "application/x-amz-json-1.0").withMethod("POST")

  def putItem(data: JsObject)(implicit ec: ExecutionContext) =
    serviceSigner(dynamoWSRequest.withHeaders("x-amz-target" -> "DynamoDB_20120810.PutItem").withBody(data), ec)

  def getItem(data: JsObject)(implicit ec: ExecutionContext) =
    serviceSigner(dynamoWSRequest.withHeaders("x-amz-target" -> "DynamoDB_20120810.GetItem").withBody(data), ec)

  def updateItem(data: JsObject)(implicit ec: ExecutionContext) =
    serviceSigner(dynamoWSRequest.withHeaders("x-amz-target" -> "DynamoDB_20120810.UpdateItem").withBody(data), ec)

  def deleteItem(data: JsObject)(implicit ec: ExecutionContext) =
    serviceSigner(dynamoWSRequest.withHeaders("x-amz-target" -> "DynamoDB_20120810.DeleteItem").withBody(data), ec)

  def queryItem(data: JsObject)(implicit ec: ExecutionContext) =
    serviceSigner(dynamoWSRequest.withHeaders("x-amz-target" -> "DynamoDB_20120810.Query").withBody(data), ec)
}

object DefaultWSDynamoAPI {

  val awsPlayWSSigner = (wsReq: WSRequest, signer: Aws4Signer) => AwsRequestHolder(wsReq, signer).execute()
  //Must be a function so that instances of the class don't cache the credentials from the provider.
  val awsSigner = (credProvider: AWSCredentialsProvider, serviceName: String, serviceRegion: String) => {
    val credentials = credProvider.getCredentials
    credentials match {
      case cred: BasicSessionCredentials =>
        new Aws4Signer(AwsCredentials(cred.getAWSAccessKeyId, cred.getAWSSecretKey, Some(cred.getSessionToken)),
                       serviceName,
                       serviceRegion)
      case _ =>
        new Aws4Signer(AwsCredentials(credentials.getAWSAccessKeyId, credentials.getAWSSecretKey),
                       serviceName,
                       serviceRegion)
    }
  }

  def retryableWSSigner(
      wsReq: WSRequest,
      credProvider: AWSCredentialsProvider,
      serviceName: String,
      serviceRegion: String,
      maxRetries: Int = 10)(implicit executionContext: ExecutionContext, scheduler: Scheduler): Future[WSResponse] = {

    def loop(wsReq: WSRequest, retryCount: Int): Future[WSResponse] = {

      /**
        * Retries a request, adding a jittered delay before scheduling the retry with Akka.
        */
      def loopRetry() = {
        //Add jittered back-off with a 25ms minimum delay until retry
        val rnd = new scala.util.Random
        val newRetryCount = retryCount + 1
        //The first retry will be a random number between (25, 100)ms, second (25, 200)ms, third (25, 300)ms, etc.
        val scheduledSleep = 25 + rnd.nextInt(100 * newRetryCount)
        akka.pattern.after(scheduledSleep.millis, using = scheduler)(loop(wsReq, newRetryCount))
      }

      awsPlayWSSigner(wsReq, awsSigner(credProvider, serviceName, serviceRegion)).flatMap { response =>
        if (response.status == 400) {
          val errorType = (response.json \ "__type").as[JsString].value.split('#')(1)
          errorType match {
            case "LimitExceededException"                 => loopRetry()
            case "ProvisionedThroughputExceededException" => loopRetry()
            case "ThrottlingException"                    => loopRetry()
            case _                                        => Future.successful(response)
          }
        } else if (response.status < 500)
          Future.successful(response)
        else if (retryCount == maxRetries)
          Future.successful(response)
        else
          loopRetry()
      }
    }

    loop(wsReq, 0)
  }
}
