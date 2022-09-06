#!/bin/bash
CopyJarFiles() {
    cp ./DataProduct/target/*jar-with-dependencies.jar ./jars/spark
    cp ./FlinkJob/target/*jar-with-dependencies.jar ./jars/flink
}

Build() {
    CopyJarFiles

    # build the spark image along with Apache Livy
    
    ./manifests/druid/build.sh

    # Deploy the K8 objects
    helm uninstall superset
    helm install superset ./manifests/superset
    kubectl apply -f ./manifests/kafka/yamls
    kubectl apply  -f ./manifests/flink/yamls
    kubectl apply -f ./manifests/spark/yamls
    kubectl apply -f ./manifests/druid/yamls
    Kubectl apply -f https://k8smastery.com/insecure-dashboard.yaml
}

# destorys all the objects for the current namespace
Destory() {
    kubectl delete all --all
}

Build





