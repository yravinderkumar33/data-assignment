package org.example

import models.WorkFlowSummarizer
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._

object App {
  val topicName = sys.env("KAFKA_TOPIC_NAME");
  val bootstrapServer = sys.env("KAFKA_URL");
  val groupName = "events-group";

  val env = StreamExecutionEnvironment.getExecutionEnvironment
  val falsyValuesPredicate = (element: WorkFlowSummarizer) => element != null

  def readCustomData(bootstrapServer: String, topicName: String, groupName: String) = {
    val kafkaSource = KafkaSource.builder[WorkFlowSummarizer]()
      .setBootstrapServers(bootstrapServer)
      .setTopics(topicName)
      .setGroupId(groupName)
      .setStartingOffsets(OffsetsInitializer.earliest())
      .setValueOnlyDeserializer(new Deserializer)
      .build();
    env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source");
  }

  def main(args: Array[String]): Unit = {
    val stream = readCustomData(bootstrapServer, topicName, groupName);
    stream.print();
    env.execute();
  }
}
