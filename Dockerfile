# A "light" image: sbt is installed, nothing is warmed. Every warmed variant
# names a Scala version in its tag and none of them names 3.9.0 yet, but sbt
# resolves `scalaVersion` from build.sbt either way - the warmed compiler would
# go unused, and the tag would be one more thing to re-pin at the next bump.
FROM sbtscala/scala-sbt:eclipse-temurin-25.0.4.1_1_2.x@sha256:78d2f6f588422161f6b8fcc975964da29fd8c0878a5bbb2466e6063158c79d8a AS builder

WORKDIR /build

COPY project/ project/
COPY src/ src/
COPY build.sbt build.sbt

# `test` shares the sbt launch with `assembly`, so running the unit tests here
# costs a couple of seconds and no image can be built with a red test suite.
RUN sbt test assembly

FROM eclipse-temurin:27_35-jdk-alpine@sha256:8b32b7f44f8752d9aaa0a58d2fb8d679aa719083f8f0c2d9bd90eb35f2fc149d AS runner

# The compiler every solution is built with. build.sbt pins the same version for
# the runner's own jar, and the two are read by different tools, so they are
# spelled out separately - but nothing else in the image repeats either of them.
ARG SCALA_VERSION=3.9.0

WORKDIR /opt/test-runner

RUN apk add --no-cache bash jq wget coreutils diffutils sed 
ADD --unpack=true https://github.com/scala/scala3/releases/download/${SCALA_VERSION}/scala3-${SCALA_VERSION}.tar.gz /opt/
RUN ln -s "/opt/scala3-${SCALA_VERSION}" /opt/scala

ENV PATH="/opt/scala/bin:${PATH}"

# Named after neither the Scala version nor its own: one jar under one name the
# scripts keep across a version bump. Were the glob to match anything but the
# assembly jar, the build would stop here rather than ship a wrong classpath.
COPY --from=builder /build/target/scala-*/TestRunner-assembly-*.jar ./target/test-runner.jar
COPY bin/ bin/

# Dump the class data sharing archives bin/run.sh starts its JVMs with. This
# does a throwaway run to collect them, so it has to come last: an archive is
# tied to the timestamp of every jar it was dumped against, and a later step
# that rewrote the assembly jar would leave the JVM quietly ignoring the
# archive. bin/warmup.sh checks its own work, so such a mistake fails the build.
RUN bin/warmup.sh

ENTRYPOINT ["/opt/test-runner/bin/run.sh"]
