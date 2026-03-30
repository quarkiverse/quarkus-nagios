# Quarkus Nagios

[![Build](https://github.com/quarkiverse/quarkus-unleash/workflows/Build/badge.svg?branch=main)](https://github.com/quarkiverse/quarkus-unleash/actions?query=workflow%3ABuild)
[![License](https://img.shields.io/github/license/quarkiverse/quarkus-unleash.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.nagios/quarkus-nagios?logo=apache-maven&style=flat-square)](https://central.sonatype.com/artifact/io.quarkiverse.nagios/quarkus-nagios-parent)

Nagios (https://www.nagios.org/) is a monitoring and alerting tool.

Nagios allows to add custom health checks via shell scripts. A simple way to monitor the uptime of a quarkus application is to curl the (SmallRye) health endpoints.

However, if you want to use more features of Nagios, such as specific error messages, performance graphs, and alert levels, you need to implement a custom script for each aspect you want to check. Scripts need to output their data in a specific format for Nagios to pick it up. Such a setup creates friction between Devs and Operation every time checks need to be modified.

This extension adds endpoints that report all Microprofile health checks in the Nagios format, so that in the Nagios server a small re-usable script around curl is enough to configure all checks.

Furthermore, this extension provides a custom implementation of the Microprofile HealthCheckResponse API, that allows to use more Nagios features:

 * 4 alert levels (ok, warning, unknown, critical)
 * export numerical results as performance data (allows Nagios to graph historic data)
 * re-usable check definitions with Nagios alert ranges

Documentation is available at https://docs.quarkiverse.io/quarkus-nagios/dev/index.html#
