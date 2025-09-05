# --- build ---
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY . .
RUN mvn -q -DskipTests package

# --- runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*SNAPSHOT.jar /app/app.jar
ENV SERVER_PORT=8081
EXPOSE 8081
ENTRYPOINT ["sh","-c","java -jar /app/app.jar --server.port=${SERVER_PORT} --spring.profiles.active=docker-compose"]
