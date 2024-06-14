#!/bin/bash
set +x
read -p "Press key to create namespaces"
kubectl create -f k8s/0001_namespaces.yml

read -p "Press key to install Debezium Operator"
helm repo add debezium https://charts.debezium.io
helm install debezium-operator debezium/debezium-operator --version 2.7.0-beta1 -n crypto-demo

read -p "Press key to install Grafana Operator"
helm install grafana-operator oci://ghcr.io/grafana/helm-charts/grafana-operator --version v5.6.3 -n crypto-infra

read -p "Press key to deploy Legacy Crypto Application"
kubectl -n crypto-legacy create -f k8s/crypto-legacy/0001_postgres.yml
kubectl -n crypto-legacy create -f k8s/crypto-legacy/0002_crypto-app.yml

read -p "Press key to deploy Redis and Monitoring infrastructure"
kubectl -n crypto-infra create -f k8s/crypto-infra
kubectl -n crypto-infra create -f k8s/crypto-infra/grafana

read -p "Press key to deploy Knative components"
kubectl -n crypto-demo create -f k8s/crypto-demo/0001_broker.yml
kubectl -n crypto-demo create -f k8s/crypto-demo/0002_crypto-cdc.yml
kubectl -n crypto-demo create -f k8s/crypto-demo/0003_crypto-collector.yml

read -p "Press key to deploy Grafana Dashboards"
kubectl -n crypto-infra create -f k8s/crypto-demo/dashboards/0001_dashboard-cdc.yaml
kubectl -n crypto-infra create -f k8s/crypto-demo/dashboards/0002_dashboard-redis.yaml
