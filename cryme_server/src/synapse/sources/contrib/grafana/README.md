# Using the Synapse Grafana dashboard

0. Set up Prometheus and Grafana. Out of scope for this readme. Useful documentation about using Grafana with Prometheus: http://docs.grafana.org/features/datasources/prometheus/
1. Have your Prometheus scrape your Synapse. https://matrix-org.github.io/synapse/latest/metrics-howto.html
2. Import dashboard into Grafana. Download `synapse.json`. Import it to Grafana and select the correct Prometheus datasource. http://docs.grafana.org/reference/export_import/
3. Set up required recording rules. [contrib/prometheus](../prometheus)
