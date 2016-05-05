(ns cocktail-slackbot.waiter
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [clj-time.core :as t]
            [medley.core :as medley]
            [manifold.stream :as stream]
            [manifold.time :as time]
            [cocktail-slackbot.parser :refer [parse-message]]))

(defn send-message
  [bot channel message]
  (stream/put! (:outgoing-messages bot) [channel message]))

(defn resolve-team-entity
  [{:keys [state]} entity-type keyfn value]
  (medley/find-first
   (fn [x] (= (keyfn x) value))
   (get-in @state [:team-state entity-type])))

(defn user-name
  [bot user-id]
  (when-let [user (resolve-team-entity bot :users :id user-id)]
    (:name user)))

(defn channel-id
  [bot channel-name]
  (when-let [channel (resolve-team-entity bot :channels :name channel-name)]
    (:id channel)))

(defn my-name
  [bot]
  (-> bot :state deref :team-state :self :name))

(defn my-id
  [bot]
  (-> bot :state deref :team-state :self :id))

(defmulti handle-message (fn [bot message] (:type message)))

(defmethod handle-message :plus-one
  [bot {:keys [channel user delta] :as message}]
  (log/info "plus-one" message))

(defmethod handle-message :show-status
  [bot message]
  (log/info "show-status" message))

(defmethod handle-message :volunteer
  [bot {:keys [channel user making] :as message}]
  (log/info "volunteer" message))

(defmethod handle-message :default
  [bot message]
  (log/error "No handler for message type" message))

(defmulti handle-event (fn [bot event] (:type event)))

(defmethod handle-event :default
  [bot event]
  (log/info "Unhandled event" event))

(defmethod handle-event :init-state
  [bot event]
  (log/info "Resetting team state")
  (swap! (:state bot) assoc :team-state (:init-state event)))

(defmethod handle-event "pong"
  [bot event]
  (log/info "Setting last pong time" event)
  (let [t (t/now)]
    (swap! (:state bot) assoc-in [:team-state :last-pong-time] t)))

(defmethod handle-event "hello"
  [bot event]
  (log/info "Received hello"))

(defn directed-at-me?
  [id text]
  (and text (.contains text (str "<@" id ">"))))

(defn strip-at-me
  [id text]
  (let [m (re-pattern (str "\\Q<@" id ">\\E\\s*:?"))]
    (str/replace text m "")))

(defmethod handle-event "message"
  [bot {:keys [channel user text] :as message}]
  (log/info "Received message" message)
  (let [me (my-id bot)]
    (when (directed-at-me? me text)
      (if-let [parsed (->> text
                          (strip-at-me me)
                          (str/trim)
                          (parse-message))]
        (handle-message bot (assoc parsed :user user :channel channel))
        (send-message bot channel "Sorry, I don't understand that.")))))

(defrecord CocktailWaiter [config state incoming-events outgoing-messages]
  component/Lifecycle

  (start [this]
    (stream/consume (partial handle-event this) incoming-events)
    this)

  (stop [this]
    this))

(defn new-cocktail-waiter
  [config]
  (map->CocktailWaiter (assoc config :state (atom {}))))
