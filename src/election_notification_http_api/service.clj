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
            [election-notification-http-api.election-notification-works :as en]))

(def ping
  (interceptor
   {:enter
    (fn [ctx]
      (assoc ctx :response (ring-resp/response "OK")))}))

(defn rabbit-error->http-status
  [rabbit-error]
  (case (:type rabbit-error)
    :semantic 400
    :validation 400
    :server 500
    :timeout 504
    500))

(defn rabbit-result->http-status
  [rabbit-result]
  ;; TODO: Figure out what we get back on reads w/o subscriptions so we can return 404
  (case (:status rabbit-result)
    :error (rabbit-error->http-status (:error rabbit-result))
    500))

(def response-timeout 10000)

(def create-subscription
  (interceptor
   {:enter
    (fn [ctx]
      (let [user-id (-> ctx
                        (get-in [:request :path-params :user-id])
                        java.util.UUID/fromString)
            medium (keyword (get-in ctx [:request :path-params :medium]))
            response-chan (en/create-subscription {:user-id user-id
                                                   :mediums #{medium}})]
        (log/debug :in :create-subscription :enter
                   "Creating subscription for user-id"
                   user-id "and medium" medium)
        (go
          (let [result (alt! (timeout response-timeout) {:status :error
                                                         :error {:type :timeout}}
                             response-chan ([v] v))]
            (if (= (:status result) :ok)
              (let [subscription (:subscription result)]
                (assoc ctx :response
                       (ring-resp/response subscription)))
              (let [http-status (rabbit-result->http-status result)]
                (assoc ctx :response
                       (-> result
                           ring-resp/response
                           (ring-resp/status http-status)))))))))}))

(def read-subscription
  (interceptor
   {:enter
    (fn [ctx]
      (let [user-id (-> ctx
                        (get-in [:request :path-params :user-id])
                        java.util.UUID/fromString)
            response-chan (en/read-subscription {:user-id user-id})]
        (log/debug :in :read-subscription :enter
                   "Reading subscription for user-id"
                   user-id)
        (go
          (let [result (alt! (timeout response-timeout) {:status :error
                                                         :error {:type :timeout}}
                             response-chan ([v] v))]
            (if (= (:status result) :ok)
              (let [subscription (:subscription result)]
                (assoc ctx :response
                       (ring-resp/response subscription)))
              (let [http-status (rabbit-result->http-status result)]
                (assoc ctx :response
                       (-> result
                           ring-resp/response
                           (ring-resp/status http-status)))))))))}))

(def delete-subscription
  (interceptor
   {:enter
    (fn [ctx]
      (let [user-id (-> ctx
                        (get-in [:request :path-params :user-id])
                        java.util.UUID/fromString)
            medium (keyword (get-in ctx [:request :path-params :medium]))
            response-chan (en/delete-subscription {:user-id user-id
                                                   :mediums #{medium}})]
        (log/debug :in :delete-subscription :enter
                   "Deleting subscription for user-id"
                   user-id "and medium" medium)
        (go
          (let [result (alt! (timeout response-timeout) {:status :error
                                                         :error {:type :timeout}}
                             response-chan ([v] v))]
            (if (= (:status result) :ok)
              (let [subscription (:subscription result)]
                (assoc ctx :response
                       (ring-resp/response subscription)))
              (let [http-status (rabbit-result->http-status result)]
                (assoc ctx :response
                       (-> result
                           ring-resp/response
                           (ring-resp/status http-status)))))))))}))

(defroutes routes
  [[["/"
     ^:interceptors [(body-params)
                     (negotiate-response-content-type ["application/edn"
                                                       "application/transit+json"
                                                       "application/transit+msgpack"
                                                       "application/json"
                                                       "text/plain"])]
     ["/ping" {:get [:ping ping]}]
     ["/subscriptions/:user-id" {:get [:read-subscription read-subscription]}
      ["/:medium" {:put [:create-subscription create-subscription]
                   :delete [:delete-subscription delete-subscription]}]]]]])

(defn service []
  {::env :prod
   ::bootstrap/routes routes
   ::bootstrap/router :linear-search
   ::bootstrap/resource-path "/public"
   ::bootstrap/host (config [:server :hostname])
   ::bootstrap/type :immutant
   ::bootstrap/port (config [:server :port])})
