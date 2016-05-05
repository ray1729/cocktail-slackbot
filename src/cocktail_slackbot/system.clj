(ns cocktail-slackbot.system
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cocktail-slackbot.slack :refer [new-rtm-client]]
            [cocktail-slackbot.waiter :refer [new-cocktail-waiter]]
            [com.stuartsierra.component :as component]
            [manifold.stream :as stream])
  (:import java.io.PushbackReader))

(defn read-config
  []
  (with-open [r (PushbackReader. (io/reader (io/resource "config.edn")))]
    (edn/read r)))

(defn system
  []
  "Returns a new instance of the application."
  (let [config (read-config)]
    (component/system-map
     :config            config
     :incoming-events   (stream/stream 8)
     :outgoing-messages (stream/stream 8)
     :rtm-client        (component/using
                         (new-rtm-client (:slack config))
                         [:incoming-events :outgoing-messages])
     :cocktail-waiter   (component/using
                         (new-cocktail-waiter (:waiter config))
                         [:incoming-events :outgoing-messages]))))

(defn start
  [system]
  "Starts the system."
  (component/start system))

(defn stop
  [system]
  "Stops the system."
  (component/stop system))
