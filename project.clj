(defproject election-notification-http-api "0.1.0-SNAPSHOT"
  :description "FIXME: HTTP API gateway for ..."
  :url "https://github.com/democracyworks/election-notification-http-api"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [turbovote.resource-config "0.2.1"]
                 [com.novemberain/langohr "3.7.0"]
                 [prismatic/schema "1.1.5"]
                 [ch.qos.logback/logback-classic "1.2.3"]

                 ;; core.async has to come before pedestal or kehaar.wire-up will
                 ;; not compile. Something to do with the try-catch in
                 ;; kehaar.core/go-handler.
                 [org.clojure/core.async "0.3.442"]
                 [democracyworks/kehaar "0.10.1"]

                 [io.pedestal/pedestal.service "0.5.2"
                  :exclusions [org.clojure/tools.reader
                               com.cognitect/transit-clj]]
                 [io.pedestal/pedestal.service-tools "0.5.2"]
                 [democracyworks/pedestal-toolbox "0.7.0"
                  :exclusions [io.pedestal/pedestal.service]]

                 ;; this has to go before pedestal.immutant
                 ;; until this is fixed:
                 ;; https://github.com/pedestal/pedestal/issues/33
                 [org.immutant/web "2.1.6"
                  :exclusions [org.clojure/tools.reader]]
                 [io.pedestal/pedestal.immutant "0.5.2"]
                 [org.immutant/core "2.1.6"
                  :exclusions [org.clojure/tools.reader]]
                 [democracyworks/bifrost "0.1.5"]]
  :plugins [[lein-immutant "2.1.0"]
            [com.carouselapps/jar-copier "0.3.1"]]
  :java-agents [[com.newrelic.agent.java/newrelic-agent "3.35.1"]]
  :jar-copier {:java-agents true
               :destination "resources/jars"}
  :prep-tasks ["javac" "compile" "jar-copier"]
  :main ^:skip-aot election-notification-http-api.server
  :uberjar-name "election-notification-http-api.jar"
  :profiles {:uberjar {:aot :all}
             :dev {:resource-paths ["dev-resources"]}
             :test {:dependencies [[clj-http "3.5.0"]]
                    :jvm-opts ["-Dlog-level=OFF"]}})
