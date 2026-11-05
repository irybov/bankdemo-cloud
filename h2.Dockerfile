FROM bellsoft/liberica-runtime-container:jre-25-slim-glibc
ENV H2_VERSION=2.5.252
RUN wget -O /opt/h2.jar https://repo1.maven.org/maven2/com/h2database/h2/${H2_VERSION}/h2-${H2_VERSION}.jar
EXPOSE 8082 9092
VOLUME /opt/h2-data
ENTRYPOINT ["java", "-cp", "/opt/h2.jar", "org.h2.tools.Server", \
            "-web", "-webAllowOthers", "-webPort", "8082", \
            "-tcp", "-tcpAllowOthers", "-tcpPort", "9092", \
            "-baseDir", "/opt/h2-data", "-ifNotExists"]