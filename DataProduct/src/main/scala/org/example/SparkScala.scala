package org.example

import org.apache.spark.sql.{DataFrame, SparkSession}

object SparkScala {

  def getSparkSession(appName: String, config: Map[String, String]): SparkSession = {
    val sparkBuilder = SparkSession.builder().appName(appName)

    config.foreach(conf => {
      val (key, value) = conf;
      sparkBuilder.config(key, value);
    })

    sparkBuilder.getOrCreate()
  }

  def registerUDFS(sparkSession: SparkSession): Unit = {
    sparkSession.udf.register("interactEventsCount", UDF.getInteractEventsCount);
  }

  // read a source as df
  def readFile(spark: SparkSession, format: String, options: Map[String, String]): DataFrame = {
    spark
      .read
      .format(format)
      .options(options)
      .load()
  }

  // write a df as per the given options and format
  def writeDf(df: DataFrame, format: String, options: Map[String, String]): Unit = {
    df
      .write
      .format(format)
      .options(options)
      .save()
  }

}
