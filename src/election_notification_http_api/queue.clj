(ns election-notification-http-api.queue
  (:require [clojure.tools.logging :as log]
            [langohr.core :as rmq]
            [kehaar.core :as k]
            [kehaar.wire-up :as wire-up]
            [kehaar.rabbitmq]
            [election-notification-http-api.channels :as channels]
            [election-notification-http-api.handlers :as handlers]
            [turbovote.resource-config :refer [config]]))

(defn initialize []
  (let [max-retries 5
        rabbit-config (config [:rabbitmq :connection])
        connection (kehaar.rabbitmq/connect-with-retries rabbit-config max-retries)]
    (let [incoming-events []
          incoming-services [(wire-up/incoming-service
                              connection
                              "election-notification-http-api.ok"
                              (config [:rabbitmq :queues "election-notification-http-api.ok"])
                              channels/ok-requests
                              channels/ok-responses)]
          external-services [(wire-up/external-service
                              connection
                              ""
                              "election-notification-works.subscription.create"
                              (config [:rabbitmq :queues "election-notification-works.subscription.create"])
                              40000
                              channels/create-subscriptions)

                             (wire-up/external-service
                              connection
                              ""
                              "election-notification-works.subscription.read"
                              (config [:rabbitmq :queues "election-notification-works.subscription.read"])
                              40000
                              channels/read-subscriptions)

                             (wire-up/external-service
                              connection
                              ""
                              "election-notification-works.subscription.delete"
                              (config [:rabbitmq :queues "election-notification-works.subscription.delete"])
                              40000
                              channels/delete-subscriptions)

                             (wire-up/external-service
                              connection
                              ""
                              "election-notification-works.transactional.send"
                              (config [:rabbitmq :queues "election-notification-works.transactional.send"])
                              40000
                              channels/send-transactional)

                             (wire-up/external-service
                              connection
                              ""
                              "election-notification-works.turbovote-signup.create"
                              (config [:rabbitmq :queues "election-notification-works.turbovote-signup.create"])
                              40000
                              channels/create-turbovote-signup)

                             (wire-up/external-service
                              connection
                              ""
                              "election-notification-works.turbovote-signup.delete"
                              (config [:rabbitmq :queues "election-notification-works.turbovote-signup.delete"])
                              40000
                              channels/delete-turbovote-signup)]
          outgoing-events []]

      (wire-up/start-responder! channels/ok-requests
                                channels/ok-responses
                                handlers/ok)

      {:connections [connection]
       :channels (vec (concat
                       incoming-events
                       incoming-services
                       external-services
                       outgoing-events))})))

(defn close-resources! [resources]
  (doseq [resource resources]
    (when-not (rmq/closed? resource) (rmq/close resource))))

(defn close-all! [{:keys [connections channels]}]
  (close-resources! channels)
  (close-resources! connections))
