FROM eclipse-temurin:25-jdk-alpine as build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN ./mvnw install -DskipTests

FROM eclipse-temurin:25-jre-alpine
VOLUME /tmp
WORKDIR /app
COPY --from=build /workspace/app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE="pipeline"
ENV OLLAMA_BASE_URL="http://localhost:11434"
ENV DB_URL="jdbc:postgresql://localhost:5432/evaldb"
ENV DB_USER="postgres"
ENV DB_PASSWORD="password"

ENTRYPOINT ["java","-jar","/app/app.jar"]
