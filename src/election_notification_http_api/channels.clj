(ns election-notification-http-api.channels
  (:require [clojure.core.async :as async]))


(defonce ok-requests (async/chan))
(defonce ok-responses (async/chan))
(defonce create-subscriptions (async/chan))
(defonce read-subscriptions (async/chan))
(defonce delete-subscriptions (async/chan))

(defn close-all! []
  (doseq [c [ok-requests ok-responses create-subscriptions read-subscriptions
             delete-subscriptions]]
    (async/close! c)))
