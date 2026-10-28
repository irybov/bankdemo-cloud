FROM bellsoft/liberica-runtime-container:jre-25-slim-glibc
WORKDIR /app
#RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY target/*.jar app.jar
#COPY src/main/resources/application.properties application.properties
COPY src/main/resources/configs ./configs/
EXPOSE 8888
ENTRYPOINT ["java", "-Dspring.cloud.config.server.native.searchLocations=${SPRING_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS}", "-jar", "app.jar"]
#CMD ["java", "-jar", "app.jar"]