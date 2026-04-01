FROM eclipse-temurin:21-jdk-jammy
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} sender.jar
ENTRYPOINT ["java","-jar","/sender.jar"]