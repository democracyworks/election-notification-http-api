(ns election-notification-http-api.channels
  (:require [clojure.core.async :as async]))


(defonce ok-requests (async/chan))
(defonce ok-responses (async/chan))

(defn close-all! []
  (doseq [c [ok-requests ok-responses]]
    (async/close! c)))
