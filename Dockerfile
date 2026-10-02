# Build locally with Java 17: mvn clean package
# Supply the resulting target/backend-0.0.1-SNAPSHOT.jar in the build context.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /data/private && chown -R 10001:10001 /app /data
COPY --chown=10001:10001 target/backend-0.0.1-SNAPSHOT.jar /app/backend.jar
USER 10001:10001
ENV APP_FILES_ROOT=/data/private
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]
