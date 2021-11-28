#!/usr/bin/env bash
set -x -e
mvn package -DskipTests
mvn install:install-file -Dfile=target/EventFramework-0.0.1.jar -DgroupId=com.github.glfrazier -DartifactId=EventFramework -Dversion=0.0.1 -Dpackaging=jar -DlocalRepositoryPath=/home/pi/mvn-repo

