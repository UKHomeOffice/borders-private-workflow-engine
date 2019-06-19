FROM digitalpatterns/jdk:1

ADD . /app/

WORKDIR /app

USER root

RUN ./gradlew clean build -x test

EXPOSE 8080

USER 1000

ENTRYPOINT java -cp /workflow-resources $JAVA_OPTS -jar /app/dist/libs/workflow-engine.jar

