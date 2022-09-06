#Workflow Summarizer on Sunbird Telemetry Data
## Technologies Used
- Scala 3
- Apache Spark
- Apache Flink
- Apache Kafka
- Apache Livy for Spark Job Submission
- Kubernetes with Docker as Container Runtime
- Apache Druid
- Apache Zookeeper
- Apache Maven

## How to Run :- 
- Build the Fat Jar for the spark job (`mvn package`)
- Build the Fat Jar for the flink job (`mvn package`)
- ./build.sh to Deploy the cluster

##Workflow Diagram


![Alt text](assets/images/flow.png?raw=true "Title")


## Submitting the Spark job

Using Rest API exposed by Apache Ivy

API Doc Reference - `https://livy.incubator.apache.org/docs/latest/rest-api.html`

```
curl --location --request POST 'localhost:30190/batches' \
--header 'Content-Type: application/json' \
--data-raw '{
    "file": "http://192.168.1.16:8080/DataProduct-1.0-SNAPSHOT-jar-with-dependencies.jar",
    "className": "org.example.App",
    "args": [
        "/opt/spark-data/2022-01-01-14-1641034589056.json.gz"
    ],
    "numExecutors": 1,
    "conf": {
        "spark.jars.packages": "org.apache.spark:spark-sql-kafka-0-10_2.12:3.0.2",
        "spark.driver.host": "10.1.6.157",
        "spark.submit.deployMode": "client",
        "spark.driver.bindAddress": "0.0.0.0"
    }
}'
```
or manually

```
spark-submit --class `<class-name>` --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.0.2 `<jar-path>` `<file-path>` `<num-partitions optional>`
```

Example - 

```
spark-submit --class org.example.App --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.0.2 /opt/spark-jars/DataProduct-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/spark-data/2022-01-01-14-1641034589056.json.gz
```

Submitting Ingestion spec to the Druid
```
curl -X POST -H 'Content-Type: application/json' -d @/Users/ravinderkumar/workspace/sanketika/assignment/Data-pipeline/assets/ingestionSpec/telemetry.json http://localhost:30101/druid/indexer/v1/supervisor
```


Druid Connection string
```
druid://router:8888/druid/v2/sql
```