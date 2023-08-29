FROM openjdk:17-slim
WORKDIR /
RUN mkdir /usr/local/agnos/ /usr/local/agnos/Products
ENV AGNOS_HOME=/usr/local/agnos/Products
ADD ./AgnosReportServer.jar report-server.jar
EXPOSE 9091
CMD java -jar report-server.jar