package org.example

import org.apache.spark.sql.Row

object UDF {

   val getInteractEventsCount = (eidToCount: Seq[Row]) => {
    (eidToCount.find(row => row(0) == "INTERACT")) match {
      case Some(row) => row(1).asInstanceOf[Long]
      case None => 0L
    }
  }

}
