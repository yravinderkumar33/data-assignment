#!/bin/bash
CopyJarFiles() {
    cp ./DataProduct/target/*jar-with-dependencies.jar ./jars/spark
    cp ./FlinkJob/target/*jar-with-dependencies.jar ./jars/flink
}

Build() {
    CopyJarFiles

    # build the spark image along with Apache Livy
    docker image build -t yravinderkumar33/livy ./manifests/spark/livy
    
    ./manifests/druid/build.sh

    # Deploy the K8 objects
    kubectl apply -f ./manifests/kafka/yamls
    kubectl apply  -f ./manifests/flink/yamls
    kubectl apply -f ./manifests/spark/yamls
    kubectl apply -f ./manifests/druid/yamls
}

# destorys all the objects for the current namespace
Destory() {
    kubectl delete all --all
}

Build





