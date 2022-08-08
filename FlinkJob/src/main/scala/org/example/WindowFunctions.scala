package org.example

import models.WorkFlowSummarizer
import org.apache.flink.streaming.api.scala.function.AllWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

object WindowFunctions {

  class CountByWindowAll extends AllWindowFunction[WorkFlowSummarizer, WorkFlowSummarizer, TimeWindow] {
    override def apply(window: TimeWindow, input: Iterable[WorkFlowSummarizer], out: Collector[WorkFlowSummarizer]): Unit = {
      input.foreach(element => {
        out.collect(element);
      });
    }
  }

}
