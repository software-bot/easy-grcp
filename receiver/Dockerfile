FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} receiver.jar
ENTRYPOINT ["java","-jar","/app/receiver.jar"]