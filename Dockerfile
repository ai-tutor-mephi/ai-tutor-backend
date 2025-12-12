FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src

# Собрать, но не запускать тесты на этом этапе
RUN mvn clean package -Dmaven.test.skip=true

# ------------------ Stage для тестов ------------------
#FROM builder AS tester
#CMD ["mvn", "test", "-Dspring.profiles.active=test"]

# ------------------ Stage для прод-образа ------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
