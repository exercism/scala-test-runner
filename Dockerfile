FROM sbtscala/scala-sbt:eclipse-temurin-jammy-22_36_1.10.1_3.4.2

WORKDIR /opt/test-runner

RUN apt-get update
RUN apt-get install --yes jq

COPY project/ project/
COPY src/ src/
COPY build.sbt build.sbt

# `test` shares the sbt launch with `assembly`, so running the unit tests here
# costs a couple of seconds and no image can be built with a red test suite.
RUN sbt test assembly

COPY . .

ENTRYPOINT ["/opt/test-runner/bin/run.sh"]
