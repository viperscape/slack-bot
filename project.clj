(defproject slack-bot "0.1.0-SNAPSHOT"
  :description "slack.com irc notifications"
  :url "http://github.com/viperscape/slack-bot/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.3.1"]
                 [clj-http "1.0.0"]
                 [irclj "0.5.0-alpha4"]
                 [kuroshio "0.2.3-SNAPSHOT"]]
  :main ^:skip-aot slack-bot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
