(ns datalog-console.chrome.extension.content-script.main
  (:require [goog.object :as gobj]
            [clojure.string :as str]
            [cljs.core.async :as async :refer [go go-loop chan <! put!]]))



(def port (js/chrome.runtime.connect #js {:name "content-script"}))

(.addListener (gobj/get port "onMessage")
              (fn [msg]
                (println "MSG" msg)

                (println "*content script* requesting *db*")
                (.postMessage js/window "db-request" "*")
                #_(.postMessage port #js {:content-script-message "test"})))

(.addEventListener js/window "message"
                   (fn [event]
                     (when (and (identical? (.-source event) js/window)
                                (gobj/getValueByKeys event "data" "datalog-remote-message"))
                       (println "*content script* forwarding the *db*")
                       (.postMessage port (gobj/get event "data")))))
