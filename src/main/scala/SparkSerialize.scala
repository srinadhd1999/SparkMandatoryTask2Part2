import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.spark.sql.streaming.Trigger
import java.util.Base64
import metricMessage.MetricOuterClass.Metric

object SparkSerialize {

    def getSparkSession: SparkSession = {
        val spark = SparkSession.builder()
                .appName("KafkaProtobufJob")
                .master("local[*]")
                .getOrCreate()
        spark
    }

    def main(args: Array[String]): Unit = {
        val spark = getSparkSession

        import spark.implicits._

        val schema = new StructType()
                    .add("host", StringType)
                    .add("metricName", StringType)
                    .add("region", StringType)
                    .add("timestamp", StringType)
                    .add("value", IntegerType)

        val df = spark
                .readStream
                .format("kafka")
                .option("kafka.bootstrap.servers", "localhost:9092")
                .option("subscribe", "metric-topic")
                .option("startingOffsets", "earliest")
                .load()
                .selectExpr("CAST(value AS STRING) as json")

        val metrics = df.select(from_json($"json", schema).as("data")).select("data.*")

        val protobufData = metrics.map { row =>
        import metricMessage.MetricOuterClass.Metric

            val metric = Metric.newBuilder()
                    .setHost(row.getAs[String]("host"))
                    .setMetricName(row.getAs[String]("metricName"))
                    .setRegion(row.getAs[String]("region"))
                    .setTimestamp(row.getAs[String]("timestamp"))
                    .setValue(row.getAs[Int]("value"))
                    .build()
            Base64.getEncoder.encodeToString(metric.toByteArray)
        }

        val query = protobufData
                    .writeStream
                    .outputMode("append")
                    .format("kafka")
                    .option("kafka.bootstrap.servers", "localhost:9092")
                    .option("topic", "metric-proto")
                    .trigger(Trigger.ProcessingTime("10 seconds"))
                    .option("checkpointLocation", "/Users/srinadh/Documents/kafka_spark")
                    .start()

        query.awaitTermination()
    }
}