FROM adoptopenjdk/openjdk11:latest

RUN apt-get update && \
    apt-get install -y sqlite3

COPY pleo-antaeus-app/build/libs/pleo-antaeus-app-*-all.jar /anteus/app.jar
WORKDIR /anteus

EXPOSE 7001
# When the container starts: build, test and run the app.
CMD ["java", "-jar", "app.jar"]
