# Build stage
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Set timezone
ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]