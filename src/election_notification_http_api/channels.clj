(ns election-notification-http-api.channels
  (:require [clojure.core.async :as async]))

(defonce create-subscriptions (async/chan))
(defonce read-subscriptions (async/chan))
(defonce delete-subscriptions (async/chan))
(defonce send-transactional (async/chan))
(defonce create-turbovote-signup (async/chan))
(defonce delete-turbovote-signup (async/chan))

(defn close-all! []
  (doseq [c [create-subscriptions read-subscriptions
             delete-subscriptions send-transactional
             create-turbovote-signup delete-turbovote-signup]]
    (async/close! c)))
