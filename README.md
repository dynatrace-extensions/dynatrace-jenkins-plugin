# dynatrace-jenkins-plugin

## Purpose

Export Jenkins runtime signals to Dynatrace for performance, availability and usage analysis and reporting.

See https://www.dynatrace.com/hub/detail/jenkins-infrastructure-performance/ for the companion Dynatrace extension and the use case description.

## Getting started

This plugin needs to be deployed on your Jenkins instance. There are two ways to obtain this plugin:

### Download from Dynatrace GitHub

- Login to Jenkins and navigate to `Manage Jenkins` -> `Plugins` -> `Advanced Settings`
- There are two ways the HPI file can be installed:
  - Download the HPI file from this repo at `dist/dynatrace-jenkins-plugin.hpi` and upload it under the `Deploy Plugin` section of the advanced settings
  - Copy the URL to the HPI file on the Dynatrace GitHub and paste the URL: https://github.com/dynatrace-extensions/dynatrace-jenkins-plugin/dist/dynatrace-jenkins-plugin.hpi
- Click on the `Deploy` button and if needed restart Jenkins

### Compile from source

- Ensure you have maven installed and clone this repository
- Build the Jenkins plugin into an HPI file by running `mvn package` in the plugin directory
- Login to Jenkins and navigate to `Manage Jenkins` -> `Plugins` -> `Advanced Settings`
- Upload the HPI file under the `Deploy Plugin` section of the advanced settings
- Click on the `Deploy` button and if needed restart Jenkins

## Authors

This plugin is a contribution of the Dynatrace partner [Moviri](https://www.dynatrace.com/hub/partners/detail/moviri/). Support for Dynatrace customers is provided by Dynatrace.
