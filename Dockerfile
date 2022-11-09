FROM sbtscala/scala-sbt:openjdk-8u342_1.7.3_2.13.10

WORKDIR /opt/test-runner

ENV CONFIG_PATH '.meta'

RUN apt-get update
RUN apt-get install --yes jq

COPY project/ project/
COPY src/ src/
COPY build.sbt build.sbt

RUN sbt assembly

COPY . .

ENTRYPOINT ["/opt/test-runner/bin/run.sh"]
