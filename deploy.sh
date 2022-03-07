#!/usr/bin/env bash

./gradlew -Dorg.gradle.java.home=$JAVA_17_HOME jar

if [ $? -ne 0 ] ; then
  exit 1;
fi

$JAVA_17_HOME/bin/java -jar build/libs/InsideBot.jar
