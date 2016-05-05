(ns cocktail-slackbot.slack
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.stream :as stream]
            [manifold.deferred :as deferred]
            [manifold.time :as time]
            [cheshire.core :as json]))

(defn format-message
  "Slack requires certain characters to be escaped, and @channel or @user
  to be wrapped in angle brackets."
  [text]
  (-> text
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "&" "&amp;")
      (str/replace #"@(\S+)" "<@$1>")))

(defn parse-json
  [s]
  (json/parse-string s true))

(defn web-api
  "Make a call to the Slack web API."
  ([client method]
   (web-api client method {}))
  ([{:keys [api-url token] :as client} method args]
   (let [url (str api-url "/" method)
         res @(deferred/chain
               (http/get url {:query-params (assoc args :token token)})
               :body
               bs/to-string
               parse-json)]
     (if (:ok res)
       res
       (throw (ex-info (str method " failed") res))))))

(defn create-websocket
  [ws-url]
  (let [websocket @(http/websocket-client ws-url)
        src       (stream/map parse-json websocket)
        sink      (stream/stream)]
    (stream/connect-via sink (fn [msg] (stream/put! websocket (json/generate-string msg))) websocket)
    (stream/splice sink src)))

(defn rtm-start
  [api-url token]
  (let [rtm-response (web-api {:api-url api-url :token token} "rtm.start")
        websocket    (create-websocket (:url rtm-response))]
    [rtm-response websocket]))

(defprotocol IRTMClient
  (connect [this])
  (disconnect [this])
  (reconnect [this])
  (shutdown [this])
  (send-ping [this])
  (send-message [this channel text]))

(defrecord RTMClient [api-url token event-id websocket cancel-ping shutdown? incoming-events outgoing-messages]
  component/Lifecycle

  (start [this]
    (log/info "Starting RTMClient")
    (reset! shutdown? false)
    (connect this)
    this)

  (stop [this]
    (log/info "Stopping RTMClient")
    (reset! shutdown? true)
    (disconnect this)
    this)

  IRTMClient

  (connect [this]
    (log/debug "Connecting...")
    (let [[init-state ws] (rtm-start api-url token)
          ping (time/every (time/seconds 10) #(send-ping this))]
      (stream/put! incoming-events {:type :init-state :init-state init-state})
      (stream/connect ws incoming-events {:downstream? false})
      (stream/consume (fn [[channel text]] (send-message this channel text))
                      outgoing-messages)
      (stream/on-closed ws #(reconnect this))
      (reset! websocket ws)
      (reset! cancel-ping ping)))

  (disconnect [this]
    (log/debug "Disconnecting...")
    (when-let [old-cancel-ping @cancel-ping]
      (reset! cancel-ping nil)
      (old-cancel-ping))
    (when-let [old-websocket @websocket]
      (reset! websocket nil)
      (stream/close! old-websocket)))

  (reconnect [this]
    (when-not @shutdown?
      (disconnect this)
      (connect this)))

  (send-ping [this]
    (when-let [websocket @websocket]
      @(stream/put! websocket {:type "ping"
                               :id (swap! event-id inc)})))

  (send-message [this channel text]
    (when-let [websocket @websocket]
      @(stream/put! websocket {:type "message"
                               :id (swap! event-id inc)
                               :channel channel
                               :text (format-message text)}))))

(defn new-rtm-client
  [{:keys [api-url token]}]
  (map->RTMClient {:api-url api-url
                   :token token
                   :event-id (atom 0)
                   :websocket (atom nil)
                   :cancel-ping (atom nil)
                   :shutdown? (atom false)}))
