FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/search-engine-0.0.1-SNAPSHOT.jar app.jar
# Data directory
RUN mkdir -p /app/data
ENV ATLAS_DATA_DIR=/app/data
# Expose port
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar", "--atlas.data.dir=${ATLAS_DATA_DIR}"]
