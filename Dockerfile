FROM gradle:jdk-alpine

ARG SBT_VERSION=1.2.8

USER root

# Install sbt
RUN apk add --no-cache -X http://dl-cdn.alpinelinux.org/alpine/edge/testing sbt

COPY --chown=gradle:gradle . .
#RUN sbt -Dsbt.ivy.home=.ivy2 -Dsbt.global.base=.sbt/1.0 -Dsbt.boot.directory=.sbt/boot test
RUN mkdir -p /opt/scala-test-runner
RUN mkdir -p /opt/scala-test-runner/lib

#RUN cp /home/gradle/target/scala-2.12/*.jar /opt/scala-test-runner/lib
COPY bin/run.sh /opt/scala-test-runner/bin/
#RUN cp -r .ivy2 /opt/scala-test-runner
#RUN cp -r .sbt /opt/scala-test-runner
COPY dependencies/ivy2.tar.gz /opt/scala-test-runner
COPY dependencies/sbt.tar.gz /opt/scala-test-runner
RUN tar -xvf /opt/scala-test-runner/ivy2.tar.gz -C /opt/scala-test-runner
RUN tar -xvf /opt/scala-test-runner/sbt.tar.gz -C /opt/scala-test-runner

RUN chown -R gradle:gradle /opt/scala-test-runner


WORKDIR /opt/scala-test-runner

ENTRYPOINT ["/opt/scala-test-runner/bin/run.sh"]
