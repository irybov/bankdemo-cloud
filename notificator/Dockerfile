FROM bellsoft/liberica-runtime-container:jre-25-slim-glibc
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
# ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["java", "-jar", "app.jar"]