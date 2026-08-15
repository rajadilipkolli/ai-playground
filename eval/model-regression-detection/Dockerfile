FROM eclipse-temurin:25-jdk-alpine as build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN ./mvnw install -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
VOLUME /tmp
WORKDIR /app
COPY --from=build --chown=appuser:appgroup /workspace/app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE="pipeline"
ENV OLLAMA_BASE_URL="http://ollama:11434"
ENV DB_URL="jdbc:postgresql://db:5432/evaldb"
ENV DB_USER="postgres"

USER appuser

ENTRYPOINT ["java","-jar","/app/app.jar"]
