FROM quay.io/democracyworks/didor:latest

RUN mkdir -p /usr/src/election-notification-http-api
WORKDIR /usr/src/election-notification-http-api

COPY project.clj /usr/src/election-notification-http-api/

RUN lein deps

COPY . /usr/src/election-notification-http-api

RUN lein test
RUN lein immutant war --name election-notification-http-api --destination target --nrepl-port=13456 --nrepl-start --nrepl-host=0.0.0.0
