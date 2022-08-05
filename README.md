Commands

spark-submit --class org.example.App --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.0.2 /opt/spark-jars/DataProduct-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/spark-data/2022-01-01-14-1641034589056.json.gz


Steps
1 - DataProduct - build the FAT Jar and copy to jars/spark folder
2-  Flink-job - build the FAT Jar and copy to jars/flink folder

