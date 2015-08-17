(ns election-notification-http-api.election-notification-works
  (:require [kehaar.wire-up :as wire-up]
            [election-notification-http-api.channels :as channels]))

(def create-subscription (wire-up/async->fn channels/create-subscriptions))
(def read-subscription (wire-up/async->fn channels/read-subscriptions))
(def delete-subscription (wire-up/async->fn channels/delete-subscriptions))
