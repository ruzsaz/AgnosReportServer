FROM openjdk:17-slim
ARG src
WORKDIR /
RUN mkdir /usr/local/agnos/ /usr/local/agnos/Products
ENV AGNOS_HOME=/usr/local/agnos/Products
ADD ${src} report-server.jar
EXPOSE 9091
CMD java -jar -Dspring.profiles.active=prod report-server.jar