# ===== Stage 1: Build JAR bằng Gradle =====
FROM gradle:8.5-jdk17-alpine AS build

WORKDIR /home/gradle/project

# Copy file config Gradle trước để cache dependency
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
# Copy source code
COPY src src

# Build jar (Spring Boot dùng task bootJar)
RUN ./gradlew clean bootJar --no-daemon

# ===== Stage 2: Runtime image gọn nhẹ =====
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy jar từ stage build sang
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

# Port app (nhớ server.port=7000 trong application.properties)
EXPOSE 7000

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
