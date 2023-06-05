# Logging Sample Configuration File

Below is a sample logging configuration file. This file can be tweaked to control how your
homeserver will output logs. A restart of the server is generally required to apply any
changes made to this file. The value of the `log_config` option in your homeserver
config should be the path to this file.

Note that a default logging configuration (shown below) is created automatically alongside
the homeserver config when following the [installation instructions](../../setup/installation.md).
It should be named `<SERVERNAME>.log.config` by default.

```yaml
{{#include ../../sample_log_config.yaml}}
```
