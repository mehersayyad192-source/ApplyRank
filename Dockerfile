# Build:  docker build -t applyrank-backend .      Run:  docker run -p 8080:8080 applyrank-backend
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
# run as a normal user, not root (a hacked app then has far fewer rights)
RUN groupadd -r app && useradd -r -g app app && mkdir -p /app/uploads /app/data && chown -R app:app /app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
