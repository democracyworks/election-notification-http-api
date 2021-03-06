FROM quay.io/democracyworks/clojure-yourkit:lein-2.7.1

RUN mkdir -p /usr/src/election-notification-http-api
WORKDIR /usr/src/election-notification-http-api

COPY project.clj /usr/src/election-notification-http-api/

ARG env=production

RUN lein with-profile $env deps

COPY . /usr/src/election-notification-http-api

RUN lein with-profiles $env,test test
RUN lein with-profile $env uberjar

CMD java ${JVM_OPTS:--XX:+UseG1GC} \
    -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap \
    -XX:MaxRAMFraction=1 \
    -javaagent:resources/jars/com.newrelic.agent.java/newrelic-agent.jar \
    $YOURKIT_AGENT_OPTION \
    -jar target/election-notification-http-api.jar
