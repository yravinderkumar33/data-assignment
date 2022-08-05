package org.example

import org.apache.spark.sql.SparkSession
import org.example.App.{sparkConfig}

object Small {

  val kafkaConfig = Map(("kafka.bootstrap.servers" -> sys.env("KAFKA_URL")), ("topic" -> "events"))

  def main(args: Array[String]): Unit = {
    val sparkSession: SparkSession = SparkScala.getSparkSession(appName = "assignment", config = sparkConfig);
    val df = sparkSession.range(1, 1000);
    println(kafkaConfig);
    SparkScala.writeDf(df.selectExpr("to_json(struct(*)) as value"), "kafka", kafkaConfig);
  }

}
