FROM eclipse-temurin:21-jre

WORKDIR /application
COPY target/similar-products-1.0.0.jar application.jar

EXPOSE 5000
ENTRYPOINT ["java", "-jar", "application.jar"]
