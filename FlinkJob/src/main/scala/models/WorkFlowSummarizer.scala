package models

case class Context(granularity: String, pdata: Pdata)

case class Dimensions(sid: String, did: String, channel: String, `type`: String, pdata: Pdata1);

case class Edata(eks: Eks)

case class Eks(env_summary: Array[EnvSummary], events_summary: Array[EventsSummary], interact_events_count: Long, start_time: Long, end_time: Long, time_diff: Long)

case class EnvSummary(env: String, count: Long, time_spent: Long);

case class EventsSummary(id: String, count: Long)

case class Object(id: String, rollup: Rollup, `type`: String)

case class Pdata(id: String, mod: String, ver: String)

case class Pdata1(id: String, ver: String)

case class Rollup(l1: String)

case class WorkFlowSummarizer(did: String, sid: String, eid: String, ets: String, `object`: Object, context: Context, dimensions: Dimensions, edata: Edata);