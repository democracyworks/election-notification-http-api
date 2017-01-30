FROM clojure:lein-2.7.1-alpine

RUN mkdir -p /usr/src/election-notification-http-api
WORKDIR /usr/src/election-notification-http-api

COPY project.clj /usr/src/election-notification-http-api/

ARG env=production

RUN lein with-profile $env deps

COPY . /usr/src/election-notification-http-api

RUN lein with-profiles $env,test test
RUN lein with-profile $env uberjar

CMD java ${JVM_OPTS:--XX:+UseG1GC} \
    -javaagent:resources/jars/com.newrelic.agent.java/newrelic-agent.jar \
    -jar target/election-notification-http-api.jar
