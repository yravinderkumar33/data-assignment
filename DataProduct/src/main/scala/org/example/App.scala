package org.example

import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.util.{Failure, Success, Try}

object App {

  val sparkConfig: Map[String, String] = Map(
    "spark.master" -> "spark://spark-master-service:7077"
    );
  val readConfig = (path: String) => Map("inferSchema" -> "true", "path" -> path, "compression" -> "gzip");
  val kafkaConfig = Map("kafka.bootstrap.servers" -> sys.env("KAFKA_URL"), "topic" -> sys.env("KAFKA_TOPIC_NAME"));
  val defaultArgs = Map("numPartitions" -> 1);
  /*
  * Util functions start
  * */
  val isNotNull = (expression: String) => expr(expression) isNotNull;
  val filterPredicate = isNotNull("eid != 'LOG' or eid != 'AUDIT' or eid != 'SEARCH'") and isNotNull("context.did") and isNotNull("context.sid");
  val firstEventTimestamp = first("ets") as "start_time";
  val lastEventTimestamp = last("ets") as "end_time";
  val getTimeDiff = abs(expr("last(ets) - first(ets)")) as "time_diff";

  val envToCountExpr = struct(expr("env"), expr("count(env) as count"), getTimeDiff as "time_spent") as "envToCount"
  val eidToCountExpr = struct(expr("eid as id"), expr("count(*) as count")) as "eidToCount"

  val PDataExpr = struct(first("context.pdata.id") as "id", first("context.pdata.ver") as "ver") as "pdata";

  val DimensionExpr = struct(
    first("sid") as "sid",
    first("did") as "did",
    first("context.channel") as "channel",
    first("edata.type") as "type",
    first("edata.mode") as "mode",
    PDataExpr
  ) as "dimensions";

  val WorkflowSummarizerPDataExpr = struct(
    lit("AnalyticsDataPipeline") as "id",
    lit("WorkflowSummarizer") as "mod",
    lit("1.0") as "ver"
  ) as "pdata";

  val ContextExpr = struct(lit("SESSION") as "granularity", WorkflowSummarizerPDataExpr);

  val EksExpr = struct(
    expr("env_summary"),
    expr("events_summary"),
    expr("interact_events_count"),
    expr("start_time"),
    expr("end_time"),
    expr("time_diff")
  );

  val EDataExpr = struct(EksExpr as "eks");

  // main function - Arguements - Folder Path
  def main(args: Array[String]): Unit = {
    val folderPath = args(0);

    val numPartitions = Try(args(1)) match {
      case Success(value) => value.toInt
      case Failure(_) => defaultArgs.get("numPartitions").get
    }

    if (folderPath == null) {
      println("Invalid Arguments.")
      return;
    }

    val sparkSession: SparkSession = SparkScala.getSparkSession(appName = "assignment", config = sparkConfig);

    // register UDF functions with the spark session.
    SparkScala.registerUDFS(sparkSession);
    try {
      //read Df
      val telemetryDf = SparkScala.readFile(sparkSession, "json", options = readConfig(folderPath))
      //transformation
      val workFlowSummarizerDf = processTelemetryDf(telemetryDf, numPartitions);
      //action - write to kafka.
      SparkScala.writeDf(workFlowSummarizerDf.selectExpr("to_json(struct(*)) as value"), "kafka", kafkaConfig);
      // close spark session.
      sparkSession.close();
    } catch {
      case _ => sparkSession.close();
    }
  }

  def processTelemetryDf(telemetryDataFrame: DataFrame, numPartitions: Int): DataFrame = {

    val flattenedDf = telemetryDataFrame.selectExpr("*", "context.env as env", "context.did as did", "context.sid as sid", "context.channel as channel");
    val filteredDf = flattenedDf.filter(filterPredicate);
    val sortedByEtsDf = filteredDf.sort(col("ets")).cache();

    // events Summary grouped by did and sid
    val eventsSummary = sortedByEtsDf
      .groupBy(expr("did"), expr("sid"), expr("eid"))
      .agg(eidToCountExpr)
      .groupBy("did", "sid")
      .agg(collect_list("eidToCount") as "events_summary")
      .withColumn("interact_events_count", expr("interactEventsCount(events_summary)"))
      .repartition(numPartitions)

    // env Summary grouped by did and sid
    val envSummaryDf = sortedByEtsDf
      .groupBy(expr("did"), expr("sid"), expr("env"))
      .agg(envToCountExpr)
      .groupBy("did", "sid")
      .agg(collect_list("envToCount") as "env_summary")
      .repartition(numPartitions)

    // details about the first event grouped by did and sid
    val firstEventDetailsDf = sortedByEtsDf.
      groupBy("did", "sid")
      .agg(firstEventTimestamp, lastEventTimestamp, getTimeDiff, DimensionExpr, first("object") as "object")
      .repartition(numPartitions)

    // joining the previous columns
    val columnsToJoin = Seq("did", "sid");
    val WorkFlowSummarizer = eventsSummary
      .join(envSummaryDf, columnsToJoin, "inner")
      .join(firstEventDetailsDf, columnsToJoin, "inner")
      .withColumn("edata", EDataExpr)
      .selectExpr("did", "sid", "edata", "dimensions")
      .withColumn("eid", lit("ME_WORKFLOW_SUMMARY"))
      .withColumn("ets", current_timestamp())
      .withColumn("context", ContextExpr)
      .repartition(numPartitions)

    WorkFlowSummarizer;
  }
}
