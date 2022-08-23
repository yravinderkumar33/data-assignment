mkdir /private/tmp/data
mkdir /private/tmp/data/broker_var
mkdir /private/tmp/data/coordinator_var
mkdir /private/tmp/data/druid_shared
mkdir /private/tmp/data/historical_var
mkdir /private/tmp/data/metadata_data
mkdir /private/tmp/data/middle_var
mkdir /private/tmp/data/router_var

kubectl create configmap druid-config --from-env-file=environment