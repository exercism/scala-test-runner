FROM sbtscala/scala-sbt:eclipse-temurin-alpine-22_36_1.10.1_3.4.2

WORKDIR /opt/test-runner

RUN apk add --no-cache jq

COPY project/ project/
COPY src/ src/
COPY build.sbt build.sbt

RUN sbt assembly

COPY . .

ENTRYPOINT ["/opt/test-runner/bin/run.sh"]
