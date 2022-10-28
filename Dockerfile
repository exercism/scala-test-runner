FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.2_2.13.10

WORKDIR /opt/test-runner

COPY project/ project/
COPY src/ src/
COPY build.sbt build.sbt

RUN sbt assembly

COPY . .

ENTRYPOINT ["/opt/test-runner/bin/run.sh"]
