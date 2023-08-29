FROM openjdk:17-slim
WORKDIR /
RUN mkdir /usr/local/agnos/ /usr/local/agnos/Products
ENV AGNOS_HOME=/usr/local/agnos/Products
#ENV AGNOS_MOLAP_SERVER_URI=http://172.21.0.1:7979/ams/
ENV AGNOS_MOLAP_SERVER_URI=http://192.168.1.6:7979/acs/
ENV SPRINGFRAMEWORK_LOGGING_LEVEL=DEBUG
ENV AGNOS_MOLAP_SERVER_LOGGING_LEVEL=DEBUG

ADD AgnosReportServer-2.0.jar report-server.jar
#CMD java -jar reporting-server.jar --agnos.molap.server.uri=http://172.21.0.1:7979/ams/ --logging.level.hu.mi.agnos.rs=DEBUG
CMD java -jar report-server.jar