(defproject cocktail-slackbot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 [aleph "0.4.1-beta3"]
                 [com.stuartsierra/component "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [logback-bundle/core-bundle "0.1.0"]
                 [medley "0.7.0"]
                 [clj-time "0.11.0"]
                 [com.rpl/specter "0.9.1"]]
  :repl-options {:init-ns user}
  :main ^:skip-aot cocktail-slackbot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]]}})
