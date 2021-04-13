FROM mozilla/sbt:8u232_1.4.5

WORKDIR /opt/test-runner

COPY project/ project/
COPY src/ src/
COPY build.sbt build.sbt
RUN sbt compile -Dsbt.rootdir=true;

COPY . .

ENTRYPOINT ["/opt/test-runner/bin/run.sh"]
