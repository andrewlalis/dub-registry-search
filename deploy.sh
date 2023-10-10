#!/usr/bin/env bash

# Simple script to deploy to d-package-search.andrewlalis.com

mvn clean package
jarfile=$(find target/d-package-search-*.jar)
echo "Built JAR: $jarfile"
ssh -f root@andrewlalis.com 'systemctl stop d-package-search && rm -rf /opt/d-package-search/package-index'
echo "Shut down d-package-search service."
scp $jarfile root@andrewlalis.com:/opt/d-package-search/d-package-search.jar
echo "Uploaded JAR."
scp -r package-index root@andrewlalis.com:/opt/d-package-search/package-index
echo "Uploaded package-index."
ssh -f root@andrewlalis.com 'systemctl start d-package-search'
echo "Started d-package-search service."
