#! /usr/bin/env bash

OPTS=`getopt -o e: --long enviornment:,skip-operators,skip-legacy,skip-infra,prepare-only,demo-only,delete-all,delete-demo,print -n 'parse-options' -- "$@"`
if [ $? != 0 ] ; then echo "Failed parsing options." >&2 ; exit 1 ; fi
eval set -- "$OPTS"

# Defaults
CREATE_NS=true
DEPLOY_OPEATORS=true
DEPLOY_LEGACY=true
DEPLOY_INFRA=true
DEPLOY_DEMO=true
DELETE_ALL=false
DELETE_DEMO=false;
IS_OCP=false
IS_K8S=true
CLIENT="kubectl"
PRINT=false

function set-client() {
    case "$1" in
        k8s )   CLIENT="kubectl";
                IS_OCP=false;
                IS_K8S=true;    shift ;;
        ocp )   CLIENT="oc";
                IS_OCP=true;
                IS_K8S=false;   shift ;;
    esac
}

function exe() {
    echo "\$ $@" ; "$@" ;
}

function set-print() {
  if ! command -v yq &> /dev/null
  then
      PRINT=false
  else
      PRINT=true
  fi
}

# Process script options
while true; do
  case "$1" in
    -e | --enviornment )        set-client $2;                      shift; shift ;;
    --skip-operators )          DEPLOY_OPERATORS=false;             shift ;;
    --skip-legacy )             DEPLOY_LEGACY=false;                shift ;;
    --skip-infra )              DEPLOY_INFRA=false;                 shift ;;
    --prepare-only )            DEPLOY_DEMO=false;                  shift ;;
    --demo-only)                CREATE_NS=false;
                                DEPLOY_OPERATORS=false;
                                DEPLOY_LEGACY=false;
                                DEPLOY_INFRA=false;                 shift ;;
    --delete-demo)              DELETE_DEMO=true;                   shift ;;
    --delete-all)               DELETE_ALL=true;                    shift ;;
    --print)                    set-print;                          shift ;;
    -- )                                                            shift; break ;;
    * )                                                             break ;;
  esac
done


echo "Using $CLIENT client"
echo ""

if $DELETE_ALL; then
    read -p ">> Press key to delete everything"
    exe $CLIENT delete --force -f k8s/0001_namespaces.yml
    exit 0
fi

if $DELETE_DEMO; then
    read -p ">> Press key to delete demo"
    exe $CLIENT -n crypto-infra delete -f k8s/crypto-demo/dashboards/
    exe $CLIENT -n crypto-demo delete -f k8s/crypto-demo/
    exe $CLIENT -n crypto-infra delete pods -l app=redis
    exe $CLIENT -n crypto-infra delete pods -l app=prometheus-server
    exe $CLIENT -n crypto-infra wait --for=condition=Available deployments/redis --timeout 60s
    exe $CLIENT -n crypto-infra wait --for=condition=Available deployments/prometheus-server --timeout 60s
    exit 0
fi

if $CREATE_NS; then
    read -p ">> Press key to create namespaces"
    exe $CLIENT apply -f k8s/0001_namespaces.yml
    echo ""
fi

if $DEPLOY_OPERATORS; then

    read -p ">> Press key to install Debezium Operator"
    exe helm repo add  --force-update debezium https://charts.debezium.io
    exe helm install debezium-operator debezium/debezium-operator --version 2.7.0-beta2 -n crypto-demo
    echo ""

    read -p ">> Press key to install Grafana Operator"
    if $IS_K8S; then
        exe helm install grafana-operator oci://ghcr.io/grafana/helm-charts/grafana-operator --version v5.9.2 -n crypto-infra
    fi

    if $IS_OCP; then
        exe $CLIENT apply -f k8s/crypto-infra/grafana/0001_operator.yml
    fi
    echo ""
fi

if $DEPLOY_LEGACY; then
    read -p ">> Press key to deploy Legacy Crypto Application"
    exe $CLIENT -n crypto-legacy create -f k8s/crypto-legacy/0001_postgres.yml
    exe $CLIENT -n crypto-legacy wait --for=condition=Available deployments/postgresql --timeout 60s
    exe $CLIENT -n crypto-legacy create -f k8s/crypto-legacy/0002_crypto-app.yml
    exe $CLIENT -n crypto-legacy wait --for=condition=Available deployments/debezium-crypto-app --timeout 60s
    echo ""
fi


if $DEPLOY_INFRA; then
    read -p ">> Press key to deploy Redis and Monitoring infrastructure"
    exe $CLIENT -n crypto-infra create -f k8s/crypto-infra
    exe $CLIENT -n crypto-infra create -f k8s/crypto-infra/grafana/0002_grafana.yml

    exe $CLIENT -n crypto-infra wait --for=condition=Available deployments/prometheus-server --timeout 60s
    exe $CLIENT -n crypto-infra wait --for=condition=Available deployments/redis --timeout 60s
    exe $CLIENT -n crypto-infra wait --for=jsonpath='{.status.stageStatus}'=success grafana/grafana --timeout 60s
    exe $CLIENT -n crypto-infra wait --for=condition=Available deployments/grafana-deployment --timeout 60s

    read -p ">> Press key to deploy Grafana data sources"
    exe $CLIENT -n crypto-infra create -f k8s/crypto-infra/grafana/0010_data-source-prometheus.yml
    exe $CLIENT -n crypto-infra create -f k8s/crypto-infra/grafana/0020_data-source-redis.yml
    echo ""
fi

if $DEPLOY_DEMO; then
    read -p ">> Press key to deploy Knative broker"
    exe $CLIENT -n crypto-demo create -f k8s/crypto-demo/0001_broker.yml
    echo ""

    if $PRINT; then
        read -p ">> Press key to preview CDC component"
        exe yq k8s/crypto-demo/0002_crypto-cdc.yml
        echo ""
    fi

    read -p ">> Press key to deploy CDC component"
    exe $CLIENT -n crypto-demo create -f k8s/crypto-demo/0002_crypto-cdc.yml
    echo ""

    read -p ">> Press key to deploy crypto-collector service"
    exe $CLIENT -n crypto-demo create -f k8s/crypto-demo/0003_crypto-collector.yml
    echo ""

    read -p ">> Press key to deploy Grafana Dashboards"
    exe $CLIENT -n crypto-infra create -f k8s/crypto-demo/dashboards/0001_dashboard-cdc.yaml
    exe $CLIENT -n crypto-infra create -f k8s/crypto-demo/dashboards/0002_dashboard-redis.yaml
fi
