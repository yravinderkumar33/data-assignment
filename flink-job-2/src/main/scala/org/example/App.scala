package org.example

import com.google.gson.{Gson, JsonSyntaxException}
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.DeserializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala.function.AllWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import org.apache.flink.streaming.api.scala._

class Deserializer extends DeserializationSchema[WorkFlowSummarizer] {

  override def deserialize(message: Array[Byte]): WorkFlowSummarizer = {
    val string = new String(message);
    val gson = new Gson;
    try {
      gson.fromJson(string, classOf[WorkFlowSummarizer]);
    } catch {
      case e: JsonSyntaxException => null.asInstanceOf[WorkFlowSummarizer]
    }
  }

  override def isEndOfStream(nextElement: WorkFlowSummarizer): Boolean = false;
  override def getProducedType: TypeInformation[WorkFlowSummarizer] = implicitly[TypeInformation[WorkFlowSummarizer]];
}

class CountByWindowAll extends AllWindowFunction[WorkFlowSummarizer, WorkFlowSummarizer, TimeWindow] {
  override def apply(window: TimeWindow, input: Iterable[WorkFlowSummarizer], out: Collector[WorkFlowSummarizer]): Unit = {
    input.foreach(element => {
      out.collect(element);
    });
  }
}

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
