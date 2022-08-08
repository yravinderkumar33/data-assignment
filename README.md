# Deployment Steps - 
- Build the Fat Jar for the spark job (`mvn package`)
- Build the Fat Jar for the flink job (`mvn package`)
- ./build.sh to Deploy the cluster

Commands


![Alt text](assets/images/flow.png?raw=true "Title")


## Submitting the Spark job

Using Rest API exposed by Apache Ivy

API Doc Reference - `https://livy.incubator.apache.org/docs/latest/rest-api.html`

```
curl --location --request POST 'localhost:30190/batches' \
--header 'Content-Type: application/json' \
--data-raw '{

    "file": "http://192.168.1.25:8080/DataProduct-1.0-SNAPSHOT-jar-with-dependencies.jar",
    "className": "org.example.App",
    "args": ["/opt/spark-data/2022-01-01-14-1641034589056.json.gz"],
    "conf": {
        "spark.jars.packages": "org.apache.spark:spark-sql-kafka-0-10_2.12:3.0.2"
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