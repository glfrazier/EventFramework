#!/usr/bin/env bash 
set -x -e
KERNEL=`uname -s`
ID=EventFramework
VER=0.0.1

if [[  $KERNEL == "Linux" ]]; then
  mvn package -DskipTests
  REPO=/home/pi/mvn-repo
else
  mvn -f pom.cyg.xml package -DskipTests
  REPO=../mvn-repo
fi

mvn install:install-file -Dfile=target/${ID}-${VER}.jar -DgroupId=com.github.glfrazier -DartifactId=${ID} -Dversion=${VER} -Dpackaging=jar -DlocalRepositoryPath=${REPO}

