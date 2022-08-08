#!/bin/bash

CopyJarFiles() {
    cp ./DataProduct/target/*jar-with-dependencies.jar ./jars/spark
    cp ./flink-job-2/target/*jar-with-dependencies.jar ./jars/flink
}

Build() {
    CopyJarFiles
    # Deploy the K8 objects 
    kubectl apply  -f ./k8/flink/yamls
    kubectl apply -f ./k8/spark/yamls
    kubectl apply -f ./k8/kafka/yamls
}

# destorys all the objects for the current namespace
Destory() {
    # for local dev
    kubectl delete all --all
}

Build





