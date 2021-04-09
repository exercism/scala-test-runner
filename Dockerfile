# FROM gradle:jdk-alpine

### Taken from: https://github.com/mozilla/docker-sbt

FROM mozilla/sbt:8u232_1.4.5

USER root

RUN mkdir -p /opt/scala-test-runner
RUN mkdir -p /opt/scala-test-runner/lib

COPY bin/run.sh /opt/scala-test-runner/bin/
RUN sbt compile -Dsbt.rootdir=true;

WORKDIR /opt/scala-test-runner

ENTRYPOINT ["/opt/scala-test-runner/bin/run.sh"]
