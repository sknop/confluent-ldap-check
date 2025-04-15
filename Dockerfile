
FROM maven:3.8.3-openjdk-17 AS build
COPY src /home/app/src
COPY docker-entrypoint.sh /home/app
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package
RUN chmod +x /home/app/docker-entrypoint.sh

ENTRYPOINT [ "/home/app/docker-entrypoint.sh" ]
