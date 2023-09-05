FROM openjdk:17-slim
ARG src
WORKDIR /
RUN mkdir /usr/local/agnos/ /usr/local/agnos/Meta
ENV AGNOS_META_DIR=/usr/local/agnos/Meta
ENV AGNOS_XSD_URI=/usr/local/agnos/report.xsd
ADD ${src} report-server.jar
EXPOSE 9091
CMD java -jar -Dspring.profiles.active=prod report-server.jar