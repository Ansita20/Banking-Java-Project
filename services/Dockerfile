# syntax=docker/dockerfile:1.7
# Builds one service of the reactor: docker build --build-arg SERVICE=account-service .
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -q -B -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:17-jre
ARG SERVICE
RUN groupadd --system bank && useradd --system --gid bank bank
WORKDIR /app
COPY --from=build /src/${SERVICE}/target/${SERVICE}-1.0.0.jar app.jar
USER bank
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
