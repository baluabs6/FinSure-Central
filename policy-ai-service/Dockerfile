FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY claims-service claims-service
COPY policy-ai-service policy-ai-service
RUN mvn -q -pl policy-ai-service -am -DskipTests package
FROM eclipse-temurin:21-jre
COPY --from=build /app/policy-ai-service/target/*.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
