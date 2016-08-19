package com.lifeway.play.dynamo

import akka.actor.ActorSystem
import com.amazonaws.auth.AWSCredentialsProvider
import play.api.Configuration
import play.api.libs.ws.WSClient

/**
  * WSDynamoAPI components for compile time injection
  */
trait WSDynamoDBComponents {
  def configuration: Configuration
  def awsCredProvider: AWSCredentialsProvider
  def wsClient: WSClient
  def actorSystem: ActorSystem

  lazy val wsDynamoAPI: WSDynamoAPI =
    new DefaultWSDynamoAPI(awsCredProvider, wsClient, configuration.underlying, actorSystem)
}
