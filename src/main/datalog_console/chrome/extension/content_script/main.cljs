(ns datalog-console.chrome.extension.content-script.main
  (:require [goog.object :as gobj]
            [clojure.string :as str]
            [cljs.core.async :as async :refer [go go-loop chan <! put!]]))



(def port (js/chrome.runtime.connect #js {:name "content-script"}))

(.addListener (gobj/get port "onMessage")
              (fn [msg]
                (println "MSG" msg)

                ;; (println "requesting db")
                (.postMessage js/window "db-request" "*")
                (.postMessage port #js {:content-script-message "test"})))

(.addEventListener js/window "message"
                   (fn [event]
                    ;;  (println "content-script")
                     (when (and (identical? (.-source event) js/window)
                                (gobj/getValueByKeys event "data" "datalog-remote-message"))
                       (.postMessage port (gobj/get event "data")))
                     (case (gobj/get event "data")

                       "db-forward"
                       (do
                         (js/console.log "db-forward from workspace" event))

                       (js/console.log "Ignoring (content-script)" (gobj/get event "data")))))
