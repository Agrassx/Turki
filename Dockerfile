FROM gradle:8.10-jdk21 AS build

WORKDIR /app

COPY gradle gradle
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

COPY core core
COPY bot bot
COPY admin admin

RUN chmod +x gradlew
RUN ./gradlew :bot:jar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN mkdir -p /app/data

COPY --from=build /app/bot/build/libs/*.jar /app/bot.jar

EXPOSE 8080

ENV BOT_TOKEN=""
ENV DB_PATH="/app/data/turki.db"
ENV PORT="8080"

ENTRYPOINT ["java", "-jar", "/app/bot.jar"]
