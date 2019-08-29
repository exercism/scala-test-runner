FROM gradle:jdk-alpine

ARG SBT_VERSION=1.2.8

USER root

# Install sbt
RUN apk add --no-cache -X http://dl-cdn.alpinelinux.org/alpine/edge/testing sbt

COPY --chown=gradle:gradle . .
#RUN sbt assembly
RUN mkdir -p /opt/scala-test-runner
RUN mkdir -p /opt/scala-test-runner/lib

#RUN cp /home/gradle/target/scala-2.12/*.jar /opt/scala-test-runner/lib
COPY bin/run.sh /opt/scala-test-runner/bin/

RUN chown -R gradle:gradle /opt/scala-test-runner

WORKDIR /opt/scala-test-runner

ENTRYPOINT ["/opt/scala-test-runner/bin/run.sh"]
