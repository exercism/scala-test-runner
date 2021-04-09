# FROM gradle:jdk-alpine

### Taken from: https://github.com/mozilla/docker-sbt

ARG OPENJDK_TAG=8u232
FROM openjdk:${OPENJDK_TAG}

ARG SBT_VERSION=1.4.5

# Install sbt
RUN \
  mkdir /working/ && \
  cd /working/ && \
  curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  cd && \
  rm -r /working/ && \
  sbt sbtVersion

USER root

# Install sbt
# RUN apk add --no-cache -X http://dl-cdn.alpinelinux.org/alpine/edge/testing sbt

# COPY --chown=gradle:gradle . .
#RUN sbt -Dsbt.ivy.home=.ivy2 -Dsbt.global.base=.sbt/1.0 -Dsbt.boot.directory=.sbt/boot test
RUN mkdir -p /opt/scala-test-runner
RUN mkdir -p /opt/scala-test-runner/lib

#RUN cp /home/gradle/target/scala-2.12/*.jar /opt/scala-test-runner/lib
COPY bin/run.sh /opt/scala-test-runner/bin/
# RUN sbt compile
#RUN cp -r .ivy2 /opt/scala-test-runner
#RUN cp -r .sbt /opt/scala-test-runner
# COPY dependencies/apache-ivy-2.5.0-bin.tar.gz /opt/scala-test-runner
# COPY dependencies/sbt-1.5.0.tgz /opt/scala-test-runner
# RUN tar -xvf /opt/scala-test-runner/apache-ivy-2.5.0-bin.tar.gz -C /opt/scala-test-runner
# RUN tar -xvf /opt/scala-test-runner/sbt-1.5.0.tgz -C /opt/scala-test-runner

# RUN chown -R gradle:gradle /opt/scala-test-runner

WORKDIR /opt/scala-test-runner

ENTRYPOINT ["/opt/scala-test-runner/bin/run.sh"]
