(ns election-notification-http-api.service-test
  (:require [election-notification-http-api.server :as server]
            [election-notification-http-api.service :as service]
            [election-notification-http-api.channels :as channels]
            [clj-http.client :as http]
            [clojure.edn :as edn]
            [cognitect.transit :as transit]
            [clojure.test :refer :all]
            [clojure.string :as str]
            [clojure.core.async :as async]
            [bifrost.core :as bifrost])
  (:import [java.io ByteArrayInputStream]))

(def test-server-port 56303)

(defn start-test-server [run-tests]
  (server/start-http-server {:io.pedestal.http/port test-server-port})
  (run-tests))

(use-fixtures :once start-test-server)

(def root-url (str "http://localhost:" test-server-port))

(deftest ping-test
  (testing "ping responds with 'OK'"
    (let [response (http/get (str root-url "/ping")
                             {:headers {:accept "text/plain"}})]
      (is (= 200 (:status response)))
      (is (= "OK" (:body response))))))

(deftest create-subscription-test
  (testing "PUT to /subscriptions/:user-id/email puts appropriate create message
            on create-subscriptions channel"
    (let [fake-user-id (java.util.UUID/randomUUID)
          http-response-ch (async/thread
                             (http/put (str/join "/" [root-url
                                                      "subscriptions"
                                                      fake-user-id
                                                      "email"])
                                       {:headers {:accept "application/edn"}}))
          [response-ch message] (async/alt!! channels/create-subscriptions ([v] v)
                                             (async/timeout 1000) [nil ::timeout])]
      (assert (not= message ::timeout))
      (async/>!! response-ch {:status :ok
                              :subscription {:user-id fake-user-id
                                             :mediums #{:email}}})
      (let [http-response (async/alt!! http-response-ch ([v] v)
                                       (async/timeout 1000) ::timeout)]
        (assert (not= http-response ::timeout))
        (is (= fake-user-id (:user-id message)))
        (is (= #{:email} (:mediums message)))
        (is (= 200 (:status http-response)))
        (is (= {:user-id fake-user-id, :mediums #{:email}}
               (-> http-response :body edn/read-string))))))
  (testing "PUT to /subscriptions/:user-id/email can respond with Transit"
    (let [fake-user-id (java.util.UUID/randomUUID)
          http-response-ch (async/thread
                             (http/put (str/join "/" [root-url
                                                      "subscriptions"
                                                      fake-user-id
                                                      "email"])
                                       {:headers {:accept "application/transit+json"}}))
          [response-ch message] (async/alt!! channels/create-subscriptions ([v] v)
                                             (async/timeout 1000) [nil ::timeout])]
      (assert (not= message ::timeout))
      (async/>!! response-ch {:status :ok
                              :subscription {:user-id fake-user-id
                                             :mediums #{:email}}})
      (let [http-response (async/alt!! http-response-ch ([v] v)
                                       (async/timeout 1000) ::timeout)
            transit-in (ByteArrayInputStream. (-> http-response
                                                  :body
                                                  (.getBytes "UTF-8")))
            transit-reader (transit/reader transit-in :json)
            create-data (transit/read transit-reader)]
        (assert (not= http-response ::timeout))
        (is (= fake-user-id (:user-id message)))
        (is (= #{:email} (:mediums message)))
        (is (= 200 (:status http-response)))
        (is (= {:user-id fake-user-id, :mediums #{:email}} create-data)))))
  (testing "error from backend service results in HTTP server error response"
    (let [fake-user-id (java.util.UUID/randomUUID)
          http-response-ch (async/thread
                             (http/put (str/join "/" [root-url
                                                      "subscriptions"
                                                      fake-user-id
                                                      "email"])
                                       {:headers {:accept "application/edn"}
                                        :throw-exceptions false}))
          [response-ch message] (async/alt!! channels/create-subscriptions ([v] v)
                                             (async/timeout 1000) [nil ::timeout])]
      (assert (not= message ::timeout))
      (async/>!! response-ch {:status :error
                              :error {:type :server}})
      (let [http-response (async/alt!! http-response-ch ([v] v)
                                       (async/timeout 1000) ::timeout)]
        (assert (not= http-response ::timeout))
        (is (= 500 (:status http-response))))))
  (testing "no response from backend service results in HTTP gateway timeout error response"
    (with-redefs [bifrost/*response-timeout* 500]
      (let [fake-user-id (java.util.UUID/randomUUID)
            http-response-ch (async/thread
                               (http/put (str/join "/" [root-url
                                                        "subscriptions"
                                                        fake-user-id
                                                        "email"])
                                         {:headers {:accept "application/edn"}
                                          :throw-exceptions false}))
            [response-ch message] (async/alt!! channels/create-subscriptions ([v] v)
                                               (async/timeout 1000) [nil ::timeout])]
        (assert (not= message ::timeout))
        (let [http-response (async/alt!! http-response-ch ([v] v)
                                         (async/timeout 1000) ::timeout)]
          (assert (not= http-response ::timeout))
          (is (= 504 (:status http-response))))))))

(deftest delete-subscription-test
  (testing "DELETE to /subscriptions/:user-id/sms puts appropriate delete message
            on delete-subscriptions channel"
    (let [fake-user-id (java.util.UUID/randomUUID)
          http-response-ch (async/thread
                             (http/delete (str/join "/" [root-url
                                                         "subscriptions"
                                                         fake-user-id
                                                         "sms"])
                                          {:headers {:accept "application/edn"}}))
          [response-ch message] (async/alt!! channels/delete-subscriptions ([v] v)
                                             (async/timeout 1000) [nil ::timeout])]
      (assert (not= message ::timeout))
      (async/>!! response-ch {:status :ok
                              :subscription {:user-id fake-user-id
                                             :mediums #{:sms}}})
      (let [http-response (async/alt!! http-response-ch ([v] v)
                                       (async/timeout 1000) ::timeout)]
        (assert (not= http-response ::timeout))
        (is (= fake-user-id (:user-id message)))
        (is (= #{:sms} (:mediums message)))
        (is (= 200 (:status http-response)))
        (is (= {:user-id fake-user-id, :mediums #{:sms}}
               (-> http-response :body edn/read-string))))))
  (testing "DELETE to /subscriptions/:user-id/email can respond with Transit"
    (let [fake-user-id (java.util.UUID/randomUUID)
          http-response-ch (async/thread
                             (http/delete (str/join "/" [root-url
                                                         "subscriptions"
                                                         fake-user-id
                                                         "email"])
                                          {:headers {:accept "application/transit+json"}}))
          [response-ch message] (async/alt!! channels/delete-subscriptions ([v] v)
                                             (async/timeout 1000) [nil ::timeout])]
      (assert (not= message ::timeout))
      (async/>!! response-ch {:status :ok
                              :subscription {:user-id fake-user-id
                                             :mediums #{:email}}})
      (let [http-response (async/alt!! http-response-ch ([v] v)
                                       (async/timeout 1000) ::timeout)
            transit-in (ByteArrayInputStream. (-> http-response
                                                  :body
                                                  (.getBytes "UTF-8")))
            transit-reader (transit/reader transit-in :json)
            delete-data (transit/read transit-reader)]
        (assert (not= http-response ::timeout))
        (is (= fake-user-id (:user-id message)))
        (is (= #{:email} (:mediums message)))
        (is (= 200 (:status http-response)))
        (is (= {:user-id fake-user-id, :mediums #{:email}} delete-data)))))
  (testing "error from backend service results in HTTP server error response"
    (let [fake-user-id (java.util.UUID/randomUUID)
          http-response-ch (async/thread
                             (http/delete (str/join "/" [root-url
                                                         "subscriptions"
                                                         fake-user-id
                                                         "email"])
                                          {:headers {:accept "application/edn"}
                                           :throw-exceptions false}))
          [response-ch message] (async/alt!! channels/delete-subscriptions ([v] v)
                                             (async/timeout 1000) [nil ::timeout])]
      (assert (not= message ::timeout))
      (async/>!! response-ch {:status :error
                              :error {:type :server}})
      (let [http-response (async/alt!! http-response-ch ([v] v)
                                       (async/timeout 1000) ::timeout)]
        (assert (not= http-response ::timeout))
        (is (= 500 (:status http-response))))))
  (testing "no response from backend service results in HTTP gateway timeout error response"
    (with-redefs [bifrost/*response-timeout* 500]
      (let [fake-user-id (java.util.UUID/randomUUID)
            http-response-ch (async/thread
                               (http/delete (str/join "/" [root-url
                                                           "subscriptions"
                                                           fake-user-id
                                                           "email"])
                                            {:headers {:accept "application/edn"}
                                             :throw-exceptions false}))
            [response-ch message] (async/alt!! channels/delete-subscriptions ([v] v)
                                               (async/timeout 1000) [nil ::timeout])]
        (assert (not= message ::timeout))
        (let [http-response (async/alt!! http-response-ch ([v] v)
                                         (async/timeout 1000) ::timeout)]
          (assert (not= http-response ::timeout))
          (is (= 504 (:status http-response))))))))

(deftest read-subscription-test
  (testing "GET to /subscriptions/:user-id puts appropriate read message
            on read-subscriptions channel"
    (let [fake-user-id (java.util.UUID/randomUUID)
          http-response-ch (async/thread
                             (http/get (str/join "/" [root-url
                                                      "subscriptions"
                                                      fake-user-id])
                                       {:headers {:accept "application/edn"}}))
          [response-ch message] (async/alt!! channels/read-subscriptions ([v] v)
                                             (async/timeout 1000) [nil ::timeout])]
      (assert (not= message ::timeout))
      (async/>!! response-ch {:status :ok
                              :subscription {:user-id fake-user-id
                                             :mediums #{:sms :email}}})
      (let [http-response (async/alt!! http-response-ch ([v] v)
                                       (async/timeout 1000) ::timeout)]
        (assert (not= http-response ::timeout))
        (is (= fake-user-id (:user-id message)))
        (is (= 200 (:status http-response)))
        (is (= {:user-id fake-user-id, :mediums #{:email :sms}}
               (-> http-response :body edn/read-string))))))
  (testing "GET to /subscriptions/:user-id can respond with Transit"
    (let [fake-user-id (java.util.UUID/randomUUID)
          http-response-ch (async/thread
                             (http/get (str/join "/" [root-url
                                                      "subscriptions"
                                                      fake-user-id])
                                       {:headers {:accept "application/transit+json"}}))
          [response-ch message] (async/alt!! channels/read-subscriptions ([v] v)
                                             (async/timeout 1000) [nil ::timeout])]
      (assert (not= message ::timeout))
      (async/>!! response-ch {:status :ok
                              :subscription {:user-id fake-user-id
                                             :mediums #{:email}}})
      (let [http-response (async/alt!! http-response-ch ([v] v)
                                       (async/timeout 1000) ::timeout)
            transit-in (ByteArrayInputStream. (-> http-response
                                                  :body
                                                  (.getBytes "UTF-8")))
            transit-reader (transit/reader transit-in :json)
            read-data (transit/read transit-reader)]
        (assert (not= http-response ::timeout))
        (is (= fake-user-id (:user-id message)))
        (is (= 200 (:status http-response)))
        (is (= {:user-id fake-user-id, :mediums #{:email}} read-data)))))
  (testing "error from backend service results in HTTP server error response"
    (let [fake-user-id (java.util.UUID/randomUUID)
          http-response-ch (async/thread
                             (http/get (str/join "/" [root-url
                                                      "subscriptions"
                                                      fake-user-id])
                                       {:headers {:accept "application/edn"}
                                        :throw-exceptions false}))
          [response-ch message] (async/alt!! channels/read-subscriptions ([v] v)
                                             (async/timeout 1000) [nil ::timeout])]
      (assert (not= message ::timeout))
      (async/>!! response-ch {:status :error
                              :error {:type :server}})
      (let [http-response (async/alt!! http-response-ch ([v] v)
                                       (async/timeout 1000) ::timeout)]
        (assert (not= http-response ::timeout))
        (is (= 500 (:status http-response))))))
  (testing "no response from backend service results in HTTP gateway timeout error response"
    (with-redefs [bifrost/*response-timeout* 500]
      (let [fake-user-id (java.util.UUID/randomUUID)
            http-response-ch (async/thread
                               (http/get (str/join "/" [root-url
                                                        "subscriptions"
                                                        fake-user-id])
                                         {:headers {:accept "application/edn"}
                                          :throw-exceptions false}))
            [response-ch message] (async/alt!! channels/read-subscriptions ([v] v)
                                               (async/timeout 1000) [nil ::timeout])]
        (assert (not= message ::timeout))
        (let [http-response (async/alt!! http-response-ch ([v] v)
                                         (async/timeout 1000) ::timeout)]
          (assert (not= http-response ::timeout))
          (is (= 504 (:status http-response))))))))
