FROM amazoncorretto:11-alpine as base
COPY --from=digitalpatterns/jdk:7 /usr/bin/run.sh /usr/bin/run.sh
RUN set -eux; \
  apk update; \
  apk add --no-cache \
  openssl \
  shadow \
  less \
  curl; \
  rm -rf /var/cache/apk/* ; \
  groupadd -r java -g 1000 && useradd -u 1000 -m --no-log-init -r -g java java ; \
  mkdir /app /etc/keystore  ; \
  chown -R java:java /home/java /app /etc/keystore ; \
  ln -s /usr/lib/jvm/default-jvm /usr/lib/jvm/java ; \
  chmod +x /usr/bin/run.sh
ADD . /app/
WORKDIR /app

From base as build
USER root
RUN ./gradlew clean build -x test ; \
  find . -name \*.out.log -exec rm '{}' \; || echo "skipping removal" ; \
  find /usr/lib/jvm/ -name src.zip -delete || echo "skipping src.zip removal" ; \
  find /app/ -name classAnalysis.bin -delete || echo "skipping classAnalysis.bin removal" ; \
  rm -rf ~/.gradle/caches/ kube /root/.gradle .git*

FROM build as engine
EXPOSE 8080
USER 1000
# ENTRYPOINT exec /usr/bin/run.sh java $JAVA_OPTS -jar /app/dist/libs/workflow-engine.jar
ENTRYPOINT exec /usr/bin/run.sh
