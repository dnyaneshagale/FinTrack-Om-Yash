FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/target/*.jar app.jar
USER 10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
