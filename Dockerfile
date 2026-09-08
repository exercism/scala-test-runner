FROM sbtscala/scala-sbt:eclipse-temurin-jammy-22_36_1.10.1_3.4.2@sha256:40040a00d0eb6e3f293fcc41c7a4df6ffe301cfc60af203aab26ed7349b5cabc AS builder

WORKDIR /build

COPY project/ project/
COPY src/ src/
COPY build.sbt build.sbt

# `test` shares the sbt launch with `assembly`, so running the unit tests here
# costs a couple of seconds and no image can be built with a red test suite.
RUN sbt test assembly

FROM eclipse-temurin:22.0.2_9-jdk-alpine@sha256:f412633b75c929e68fd83d36a3abd9104c778161b987e5088eab645b0e5af3f6 AS runner

WORKDIR /opt/test-runner

RUN apk add --no-cache bash jq wget coreutils diffutils sed 
ADD --unpack=true https://github.com/lampepfl/dotty/releases/download/3.4.2/scala3-3.4.2.tar.gz /opt/
RUN ln -s /opt/scala3-3.4.2 /opt/scala

ENV PATH="/opt/scala/bin:${PATH}"

COPY --from=builder /build/target/scala-3.4.2/TestRunner-assembly-0.1.0-SNAPSHOT.jar ./target/scala-3.4.2/
COPY bin/ bin/

ENTRYPOINT ["/opt/test-runner/bin/run.sh"]
