FROM eclipse-temurin:21-jdk-jammy
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} mongo-api.jar
ENTRYPOINT ["java","-jar","/mongo-api.jar"]