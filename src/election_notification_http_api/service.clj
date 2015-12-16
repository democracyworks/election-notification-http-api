(ns election-notification-http-api.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor :refer [interceptor]]
            [ring.util.response :as ring-resp]
            [turbovote.resource-config :refer [config]]
            [pedestal-toolbox.params :refer :all]
            [pedestal-toolbox.content-negotiation :refer :all]
            [kehaar.core :as k]
            [clojure.core.async :refer [go alt! timeout]]
            [clojure.tools.logging :as log]
            [bifrost.core :as bifrost]
            [bifrost.interceptors :as bifrost.i]
            [election-notification-http-api.channels :as channels]))

(def ping
  (interceptor
   {:enter
    (fn [ctx]
      (assoc ctx :response (ring-resp/response "OK")))}))

(defroutes routes
  [[["/"
     ^:interceptors [(body-params)
                     (negotiate-response-content-type ["application/edn"
                                                       "application/transit+json"
                                                       "application/transit+msgpack"
                                                       "application/json"
                                                       "text/plain"])]
     ["/ping" {:get [:ping ping]}]
     ["/subscriptions/:user-id" {:get [:read-subscription (bifrost/interceptor channels/read-subscriptions)]}
      ^:interceptors [(bifrost.i/update-in-request
                       [:path-params :user-id]
                       #(java.util.UUID/fromString %))
                      (bifrost.i/update-in-response
                       [:body :subscription]
                       [:body] identity)]
      ["/:medium" {:put [:create-subscription (bifrost/interceptor channels/create-subscriptions)]
                   :delete [:delete-subscription (bifrost/interceptor channels/delete-subscriptions)]}
       ^:interceptors [(bifrost.i/update-in-request
                        [:path-params :medium]
                        [:path-params :mediums]
                        (comp (partial conj #{}) keyword))]]]
     ["/transactional" {:post [:send-transactional (bifrost/interceptor channels/send-transactional)]}]]]])

(defn service []
  {::env :prod
   ::bootstrap/routes routes
   ::bootstrap/router :linear-search
   ::bootstrap/resource-path "/public"
   ::bootstrap/allowed-origins (if (= :all (config [:server :allowed-origins]))
                                 (constantly true)
                                 (config [:server :allowed-origins]))
   ::bootstrap/host (config [:server :hostname])
   ::bootstrap/type :immutant
   ::bootstrap/port (config [:server :port])})
