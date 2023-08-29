#!/bin/bash

cp ../../../../target/AgnosReportServer-2.0.jar ./AgnosReportServer.jar

docker build -t agnos-report-server:2.0 .

#rm ./AgnosReportServer.jar
