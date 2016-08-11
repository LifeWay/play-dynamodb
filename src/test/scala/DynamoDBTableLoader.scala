import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.model._

object DynamoDBTableLoader {

  val awsCredProvider = new AWSCredentialsProvider {
    override def refresh(): Unit = ()

    override def getCredentials: AWSCredentials = new BasicAWSCredentials("accessKey", "secretKey")
  }

  val dynamoDBClient = new AmazonDynamoDBClient(awsCredProvider.getCredentials)
  dynamoDBClient.setEndpoint("http://localhost:8000")
  val dynamoDB = new DynamoDB(dynamoDBClient)
  dynamoDB.shutdown()


  //Helper functions
  private val keySchemaType = (name: String, keyType: KeyType) => new KeySchemaElement().withAttributeName(name).withKeyType(keyType)
  private val keySchema = (name: String) => keySchemaType(name, KeyType.HASH)
  private val attributeDefType = (name: String, attrType: ScalarAttributeType) => new AttributeDefinition().withAttributeName(name).withAttributeType(attrType)
  private val attributeDef = (name: String) => attributeDefType(name, ScalarAttributeType.S)
  private val provisionedThroughput = (throughput: Long) => new ProvisionedThroughput().withReadCapacityUnits(throughput).withWriteCapacityUnits(throughput)
  private val globalSecondaryIndex = (indexName: String, keySchema: Seq[KeySchemaElement], throughput: ProvisionedThroughput) =>
    new GlobalSecondaryIndex()
      .withIndexName(indexName)
      .withKeySchema(keySchema: _*)
      .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
      .withProvisionedThroughput(throughput)

  def initTestTable() = {
    new CreateTableRequest()
        .withTableName("test_table")
        .withKeySchema(keySchema("my_key"))

  }
}
