FROM mozilla/sbt:8u292_1.5.7

WORKDIR /opt/test-runner

RUN wget https://downloads.lightbend.com/scala/2.13.8/scala-2.13.8.tgz
RUN tar zxvf scala-2.13.8.tgz

COPY project/ project/
COPY src/ src/
COPY build.sbt build.sbt

RUN sbt assembly

COPY . .

ENTRYPOINT ["/opt/test-runner/bin/run.sh"]
