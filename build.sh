#!/bin/bash

# Deploy the K8 objects 
kubectl apply  -f ./k8/flink/yamls
kubectl apply -f ./k8/spark/yamls
kubectl apply -f ./k8/kafka/yamls



