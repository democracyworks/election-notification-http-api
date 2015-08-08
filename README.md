# election-notification-http-api

HTTP API gateway for election notifications. Only handles subscriptions for now.

## Configuration

### New config

New configuration on this app should be done in four steps:

1. Add a configuration to the `resources/config.edn` file, using
   `#resource-config/env` tagged literals for environment variables.
2. Add the env var to the `election-notification-http-api@.service.template` docker run
   command, pulling in the value from Consul.
   `--env FAKE_ENV_VAR=$(curl -s http://${COREOS_PRIVATE_IPV4}:8500/v1/kv/election-notification-http-api/fake/env/var?raw)`
3. Set up the value in Consul.
4. Add them to the README.md (this file) in the Running with
   docker-compose section as well as documented in the existing config
   section.

Keys in Consul should be appropriately namespaced, preferably under election-notification-http-api.

## Usage

PUTs to `/subscriptions/[user-id]/[email|sms]` will idempotently create
subscriptions for that user via that medium. It returns status 200 if
successful.

DELETEs to `/subscriptions/[user-id]/[email|sms]` will idempotently delete
subscriptions for that user via that medium. It returns status 200 if
successful.

GETs to `/subscriptions/[user-id]` will return the subscription entity for that
user as returned by election-notification-works but encoded the way it was
requested in the Accept header (if supported). If no subscription entity exists
for that user-id, it returns status 404.

## Running

### With docker-compose

Build it:

```
> docker-compose build
```

Run it:

```
> docker-compose up
```

### Running in CoreOS

There is a election-notification-http-api@.service.template file provided in the repo. Look
it over and make any desired customizations before deploying. The
DOCKER_REPO, IMAGE_TAG, and CONTAINER values will all be set by the
build script.

The `script/build` and `script/deploy` scripts are designed to
automate building and deploying to CoreOS.

1. Run `script/build`.
1. Note the resulting image name and push it if needed.
1. Set your FLEETCTL_TUNNEL env var to a node of the CoreOS cluster
   you want to deploy to.
1. Make sure rabbitmq service is running.
1. Run `script/deploy`.

## License

Copyright © 2015 Democracy Works, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
