package org.example

import com.google.gson.{Gson, JsonSyntaxException}
import models.WorkFlowSummarizer
import org.apache.flink.api.common.serialization.DeserializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
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
