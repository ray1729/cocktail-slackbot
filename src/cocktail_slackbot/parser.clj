(ns cocktail-slackbot.parser
  (:require [clojure.string :as str]))

(defn parse-plus-one
  [text]
  (when-let [[_ plus-or-minus digits] (re-find #"^([+-])\s*(\d+)$" text)]
    (when-let [n (try (Long/parseLong digits) (catch Exception _))]
      (if (= plus-or-minus "+")
        {:type :plus-one :delta n}
        {:type :plus-one :delta (- n)}))))

(defn parse-status
  [text]
  (when (= (str/upper-case text) "STATUS")
    {:type :show-status}))

(defn parse-volunteer
  [text]
  (when-let [[_ cocktail] (re-find #"(?i)I\s+will\s+make\s+(.+)" text)]
    {:type :volunteer :making cocktail}))

(def parse-message (some-fn parse-plus-one
                            parse-volunteer
                            parse-status))
