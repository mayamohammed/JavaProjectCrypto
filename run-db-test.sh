#!/usr/bin/env bash
set -e
echo "Test de connexion MySQL via Maven exec..."
mvn -Dexec.mainClass="cryptographie.maya.TestConnection" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
