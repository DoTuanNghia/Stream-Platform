FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

# Cài ffmpeg (FREE)
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java","-jar","app.jar"]
