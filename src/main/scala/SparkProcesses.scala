import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import java.io.{File, PrintWriter}

object SparkProcesses {

    def getSparkSession: SparkSession = {
        val spark = SparkSession.builder()
                .appName("Spark Processes")
                .master("local[*]")
                .getOrCreate()
        spark
    }

    def readCsv(path: String, spark: SparkSession): DataFrame = {
        val df = spark.read
                .option("header", "false")
                .option("inferSchema", "true")
                .csv(path)
        df
    }

    def performAggregation(df: DataFrame): DataFrame = {
        val df1 = df.groupBy(col("_c0")).agg(sum(col("_c3")).alias("total_sum")).select(col("_c0").alias("server_name"), col("total_sum"))
        df1
    }

    def printDataFrame(df: DataFrame): Unit = {
        df.show()
    }

    def getListOfSubDirectories(directoryName: String): Array[String] = {
        new File(directoryName)
            .listFiles
            .filter(_.isDirectory)
            .filter(dir => !dir.getName.contains("_spark_metadata"))
            .map(_.getPath)
    }

    def processFolder(path: String, spark: SparkSession): Unit = {
        val df = readCsv(path, spark)
        val df1 = performAggregation(df)
        println(path)
        printDataFrame(df1)
    }

    def main(args: Array[String]): Unit = {
        val spark = getSparkSession

        val baseDirectory = "/Users/srinadh/Documents/csv_files" 
        val subFolders = getListOfSubDirectories(baseDirectory)

        for(subFolder <- subFolders) {
            processFolder(subFolder+"/*", spark)
        }
    }
}