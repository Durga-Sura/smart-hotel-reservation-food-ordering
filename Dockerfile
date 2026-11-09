FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
COPY dist dist
RUN mvn -B package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/table-thyme-1.0.0.jar app.jar
RUN useradd --system --uid 10001 app
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
