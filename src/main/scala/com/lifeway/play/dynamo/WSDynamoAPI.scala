package com.lifeway.play.dynamo

import play.api.libs.json.JsObject
import play.api.libs.ws.WSResponse

import scala.concurrent.{ExecutionContext, Future}

trait WSDynamoAPI {
  def putItem(data: JsObject)(implicit ec: ExecutionContext): Future[WSResponse]

  def getItem(data: JsObject)(implicit ec: ExecutionContext): Future[WSResponse]

  def updateItem(data: JsObject)(implicit ec: ExecutionContext): Future[WSResponse]

  def deleteItem(data: JsObject)(implicit ec: ExecutionContext): Future[WSResponse]

  def queryItem(data: JsObject)(implicit ec: ExecutionContext): Future[WSResponse]
}
