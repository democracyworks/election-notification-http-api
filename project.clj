(defproject election-notification-http-api "0.1.0-SNAPSHOT"
  :description "FIXME: HTTP API gateway for ..."
  :url "https://github.com/democracyworks/election-notification-http-api"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [turbovote.resource-config "0.2.0"]
                 [com.novemberain/langohr "3.6.1"]
                 [prismatic/schema "1.1.2"]
                 [ch.qos.logback/logback-classic "1.1.7"]

                 ;; core.async has to come before pedestal or kehaar.wire-up will
                 ;; not compile. Something to do with the try-catch in
                 ;; kehaar.core/go-handler.
                 [org.clojure/core.async "0.2.385"]
                 [democracyworks/kehaar "0.5.0"]

                 [io.pedestal/pedestal.service "0.4.1"
                  :exclusions [org.clojure/tools.reader
                               com.cognitect/transit-clj]]
                 [io.pedestal/pedestal.service-tools "0.4.1"]
                 [democracyworks/pedestal-toolbox "0.7.0"
                  :exclusions [io.pedestal/pedestal.service]]

                 ;; this has to go before pedestal.immutant
                 ;; until this is fixed:
                 ;; https://github.com/pedestal/pedestal/issues/33
                 [org.immutant/web "2.1.5"
                  :exclusions [org.clojure/tools.reader]]
                 [io.pedestal/pedestal.immutant "0.4.0"]
                 [org.immutant/core "2.1.5"
                  :exclusions [org.clojure/tools.reader]]
                 [democracyworks/bifrost "0.1.4"]]
  :plugins [[lein-immutant "2.1.0"]]
  :main ^:skip-aot election-notification-http-api.server
  :target-path "target/%s"
  :uberjar-name "election-notification-http-api.jar"
  :profiles {:uberjar {:aot :all}
             :dev {:resource-paths ["dev-resources"]}
             :test {:dependencies [[clj-http "2.0.0"]]
                    :jvm-opts ["-Dlog-level=INFO"]}})
