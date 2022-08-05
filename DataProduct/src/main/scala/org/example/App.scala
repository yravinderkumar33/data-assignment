package org.example

import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

object App {

  val sparkConfig: Map[String, String] = Map("spark.master" -> "local[*]");
  val readConfig = (path: String) => Map(("inferSchema" -> "true"), ("path" -> path), ("compression", "gzip"));
  val kafkaConfig = Map(("kafka.bootstrap.servers" -> sys.env("KAFKA_URL")), ("topic" -> "telemetry"));

  /*
  * Util functions start
  * */
  val isNotNull = (expression: String) => expr(expression) isNotNull;
  val filterPredicate = isNotNull("eid != 'LOG' or eid != 'AUDIT' or eid != 'SEARCH'") and isNotNull("context.did") and isNotNull("context.sid");
  val firstEventTimestamp = first("ets") as "start_time";
  val lastEventTimestamp = last("ets") as "end_time";
  val getTimeDiff = abs(expr("last(ets) - first(ets)")) as "time_diff";

  val envToCount = struct(expr("env"), expr("count(env) as count"), getTimeDiff as "time_spent") as "envToCount"
  val eidToCount = struct(expr("eid as id"), expr("count(*) as count")) as "eidToCount"

  val Dimension = struct(
    first("sid") as "sid",
    first("did") as "did",
    first("context.channel") as "channel",
    first("edata.type") as "type",
    first("edata.mode") as "mode",
    struct(
      first("context.pdata.id") as "id",
      first("context.pdata.ver") as "ver"
    ) as "pdata"
  ) as "dimensions";

  val WorkflowSummarizerPData = struct(
    lit("AnalyticsDataPipeline") as "id",
    lit("WorkflowSummarizer") as "mod",
    lit("1.0") as "ver"
  ) as "pdata";

  val Context = struct(lit("SESSION") as "granularity", WorkflowSummarizerPData);

  val EData = struct(
    struct(
      expr("env_summary"),
      expr("events_summary"),
      expr("interact_events_count"),
      expr("start_time"),
      expr("end_time"),
      expr("time_diff")
    ) as "eks"
  );

  /*
   * Util functions end
  * */

  // main function - Arguements - Folder Path
  def main(args: Array[String]): Unit = {
    val folderPath = args(0);

    if (folderPath == null) {
      println("Invalid Arguments.")
      return;
    }

    val sparkSession: SparkSession = SparkScala.getSparkSession(appName = "assignment", config = sparkConfig);
    SparkScala.registerUDFS(sparkSession);

    try {
      //read Df
      val telemetryDf = SparkScala.readFile(sparkSession, "json", options = readConfig(folderPath))
      //transformation
      val workFlowSummarizerDf = processTelemetryDf(telemetryDf);
      //action - write to kafka queque.
      SparkScala.writeDf(workFlowSummarizerDf.selectExpr("to_json(struct(*)) as value"), "kafka", kafkaConfig);
      // close spark session.
      sparkSession.close();
    } catch {
      case _ => sparkSession.close();
    }
  }

  def processTelemetryDf(telemetryDataFrame: DataFrame): DataFrame = {

    val flattenedDf = telemetryDataFrame
      .selectExpr("*", "context.env as env", "context.did as did", "context.sid as sid", "context.channel as channel")
      .filter(filterPredicate)
      .sort(col("ets"))

    // events Summary grouped by did and sid
    val eventsSummary = flattenedDf
      .groupBy(expr("did"), expr("sid"), expr("eid"))
      .agg(eidToCount)
      .groupBy("did", "sid")
      .agg(collect_list("eidToCount") as "events_summary")
      .withColumn("interact_events_count", expr("interactEventsCount(events_summary)"))
      .repartition(2)

    // env Summary grouped by did and sid
    val envSummaryDf = flattenedDf
      .groupBy(expr("did"), expr("sid"), expr("env"))
      .agg(envToCount)
      .groupBy("did", "sid")
      .agg(collect_list("envToCount") as "env_summary")
      .repartition(2)

    // details about the first event grouped by did and sid
    val firstEventDetailsDf = flattenedDf.
      groupBy("did", "sid")
      .agg(firstEventTimestamp, lastEventTimestamp, getTimeDiff, Dimension, first("object") as "object")
      .repartition(2)

    val columnsToJoin = Seq("did", "sid");

    // joining the previous columns
    val WorkFlowSummarizer = eventsSummary
      .join(envSummaryDf, columnsToJoin, "inner")
      .join(firstEventDetailsDf, columnsToJoin, "inner")
      .withColumn("edata", EData)
      .selectExpr("did", "sid", "edata", "dimensions")
      .withColumn("eid", lit("ME_WORKFLOW_SUMMARY"))
      .withColumn("ets", current_timestamp())
      .withColumn("context", Context)
      .repartition(2)

    WorkFlowSummarizer;
  }
}
