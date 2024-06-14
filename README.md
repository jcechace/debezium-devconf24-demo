# Debezium Knative demo
This repository contains a demo, originally presented at [DevConf 2024](https://www.devconf.info/cz/),
showcasing the use of Debezium Server as Knative source.

## Problem Domain
The demo showcases the use of [Debezium Server](https://debezium.io/documentation/reference/stable/operations/debezium-server.html) to capture changes to cryptocurrency data from database of legacy application,
and streams them to knative broker as [CloudEvents](https://cloudevents.io/). A knative service then stores cryptocurrency prices
from these events as time-series data in Redis.

![Diagram](img/diagram.png)
## Prerequisities

1. Obtain Kubernetes Cluster with [Installed Knative (eventing, serving)](https://knative.dev/docs/install/)
2. Create namespaces
```shell
kubectl create -f k8s/0001_namespaces.yml
```
3. Install Debezium Operator

```shell
helm repo add debezium https://charts.debezium.io
helm install debezium-operator debezium/debezium-operator --version 2.7.0-beta2 -n crypto-demo
```

4. Install Grafana operator

```shell
helm install grafana-operator oci://ghcr.io/grafana/helm-charts/grafana-operator --version v5.9.2 -n crypto-infra
```

## Deployment
To deploy the entire demo, use `deploy.sh` which will walk you through the entire setup.
Alternatively you can follow the steps in the consecutive sections.

## Deploying the Legacy Crypto Application
For demonstration purposes the "legacy" application will be deployed in its
own Kubernetes namespaces.

```shell
kubectl -n crypto-legacy create -f k8s/crypto-legacy/0001_postgres.yml
kubectl -n crypto-legacy create -f k8s/crypto-legacy/0002_crypto-app.yml
```

## Deploy Monitoring Infrastructure and Redis

```shell
kubectl -n crypto-infra create -f k8s/crypto-infra
kubectl -n crypto-infra create -f k8s/crypto-infra/grafana
```

## Deploying the CDC-Based Solution
Finally we are ready to deploy the cornerstone of our solution.


### 1. Knative Broker
We will need a Knative broker, as a destion for our events.

```shell
kubectl -n crypto-demo create -f k8s/crypto-demo/0001_broker.yml
```


### 2. Debezium CDC component
To capture database changes we will deployed the CDC component via the CR bellow.
This `DebeziumServer` resources describes [Debezium Server](https://debezium.io/documentation/reference/stable/operations/debezium-server.html) instance with PostgreSQL source
and HTTP sink destination. It also declares CloudEvents as output format and exposes metrics
via [Prometheus JMX exporter](https://github.com/prometheus/jmx_exporter)
```yaml
apiVersion: debezium.io/v1alpha1
kind: DebeziumServer
metadata:
  name: debezium-crypto-cdc
spec:
  quarkus:
    config:
      log.console.json: false
      kubernetes-config.enabled: true
      kubernetes-config.secrets: postgresql-credentials
  runtime:
    metrics:
      jmxExporter:
        enabled: true
  sink:
    type: http
  format:
    value:
      type: cloudevents
      config:
        json.schemas.enable: false
  source:
    class: io.debezium.connector.postgresql.PostgresConnector
    offset:
      memory: {}
    schemaHistory:
      memory: {}
    config:
      database.hostname: postgresql.crypto-legacy.svc.cluster.local
      database.port: 5432
      database.user: ${POSTGRES_USER}
      database.password: ${POSTGRES_PASSWORD}
      database.dbname: ${POSTGRES_DB}
      table.include.list: public.coincap
      topic.prefix: crypto
      plugin.name: pgoutput
      decimal.handling.mode: string
```
Additionally to integrate with the Cloud Native broker we need to bind our Debezium Server
instance to the broker via `SinkBinding` resource.

```yaml
apiVersion: sources.knative.dev/v1
kind: SinkBinding
metadata:
  name: debezium-crypto-cdc-binding
spec:
  subject:
    apiVersion: apps/v1
    kind: Deployment
    selector:
      matchLabels:
        debezium.io/component: DebeziumServer
        debezium.io/instance: debezium-crypto-cdc
  sink:
    ref:
      apiVersion: eventing.knative.dev/v1
      kind: Broker
      name: debezium-sample-broker
```

Both of these component can be deployed by running the following command

```shell
# Deploy the CDC component
kubectl -n crypto-demo create -f k8s/crypto-demo/0002_crypto-cdc.yml
```

### 3. Crypto Collector Service
To process the CloudEvent describing databse changes we will also deploy a simple
[Knative Service](https://knative.dev/docs/serving/services/) which will transform the
change into a time-series record and stores it in Redis.

```shell
kubectl -n crypto-demo create -f k8s/crypto-demo/0003_crypto-collector.yml
```

When everything starts a time-series data for each cryptocurrency coin will
be persisted in our Redis.

### 4. Visualising Cryptocurrency prices
To visualise the price changes over time we will use a simple Grafana dashboard.

```shell
kubectl -n crypto-infra create -f k8s/crypto-demo/dashboards/0002_dashboard-redis.yaml
```

To access these dashboards you can expose the Grafana instance to your localhost like this:
```
kubectl -n crypto-infra  port-forward services/grafana-service 3000:3000
```

Then simply visit `http://localhost:3000/` and log in using `root` as username  and `secret` as password.

### 5. Monitoring the CDC Process
Our DebeziumServer instance exposes metrics to Prometheus. To viusalise this metrics
we can deploy another Grafana dashboard for the CDC process.

```shell
kubectl -n crypto-infra create -f k8s/crypto-demo/dashboards/0001_dashboard-cdc.yaml
```
The dashboard will be available in the same Grafana instance as our cryptocurrency data.
