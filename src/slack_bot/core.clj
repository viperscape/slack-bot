(ns slack-bot.core
  (:require [cheshire.core :refer :all]
            [clj-http.client :as client]
            [irclj.core :as irc]
            [kuroshio.chan :refer :all])
  (:gen-class))

(def core-stream (new-stream))
(def debug-chan (new-chan core-stream))

(defn send-handlers [m]
  (broadcast! core-stream m))


(def slackers (atom {}))

(defn save-slackers []
  (spit "slackers.txt" (generate-string @slackers)))

(defn load-slackers []
  (reset! slackers (parse-string (slurp "slackers.txt"))))

(load-slackers) ;;load me up, make sure at the least the file exists

(defn register-slacker [n t]
  (swap! slackers assoc n t)
  (save-slackers))


(defn post-slack [c t]
  (client/post c
               {:body (generate-string {:text t})
                :content-type :json
                :accept :json}))

(defn check-slack-tokens [msg]
  (doseq [n @slackers]
    (if (.contains (:text msg) (first n))
      (post-slack (second n) (str (:nick msg) "/" (:target msg) " - " (:text msg))))))

;;

(defn priv-msg [irc args]
  (send-handlers {:msg [irc args]}))
(defn notify-msg [irc args]
  (send-handlers {:notify-msg [irc args]}))




(defn quit-bot [conn] 
  (send-handlers {::quit true})
  (irc/kill conn))

(defn start-handler [h]
  (let [_ (send-handlers {::quit true}) ;;kill any previous handlers
          ch (new-chan core-stream)]
      (future
        (loop []
          (let [t (take! ch)]
            (if-let [r (:msg t)]
              (when-let [e (:error (try (h (first r) (second r))
                                        (catch Exception e {:error e})))]
                (send! debug-chan e)))
            (if (::quit t) 
              (send! debug-chan {::quitting ch})
              (recur)))))
      ch))

(defn handler [conn msg]
  (when (or (.contains (:text msg) "slack-bot")
            (.contains (:target msg) "slack-bot"))
    (if (.contains (:text msg) "?")
      (irc/reply conn msg "https://github.com/viperscape/slack-bot/"))
    (if (and (.contains (:text msg) "!")
             (= "slack-bot" (:nick msg)))
      (quit-bot conn))
    (if (.contains (:text msg) "#")
      (let [t (second (clojure.string/split (:text msg) #"\#"))]
        (register-slacker (:nick msg) t))))
  (check-slack-tokens msg))


(defn -main [& args]
  (let [conn (irc/connect "irc.freenode.net" 6667 
                          (str "slack-bot" (rand-int 100))
                          :callbacks {:privmsg priv-msg
                                      :notice notify-msg})]
    (doseq [n args]
      (irc/join conn (str "#" n)))
    (start-handler handler)
    conn))
