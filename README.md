# dynatrace-jenkins-plugin
Export Jenkins runtime signals to Dynatrace for performance, availability and usage analysis

# Setup
## Download from Dynatrace
- Login to Jenkins and navigate to `Manage Jenkins` -> `Plugins` -> `Advanced Settings`
- There are two ways the HPI file can be installed:
  - Download the HPI file from the Dynatrace hub and upload it under the `Deploy Plugin` secion of the advanced settings
  - Copy the URL to the HPI file on the Dynatrace hub and paste the URL 
- Click on the `Deploy` button and if needed restart Jenkins
## Compile from source
- Ensure you have maven installed and clone this repository
- Build the Jenkins plugin into an HPI file by running `mvn package` in the plugin directory
- Login to Jenkins and navigate to `Manage Jenkins` -> `Plugins` -> `Advanced Settings`
- Upload the HPI file under the `Deploy Plugin` secion of the advanced settings
- Click on the `Deploy` button and if needed restart Jenkins

# Authors
Moviri. See https://github.com/Moviri/dynatrace-jenkins-plugin for source of the source.

