FROM openjdk:8-alpine

COPY target/uberjar/datains.jar /datains/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/datains/app.jar"]
