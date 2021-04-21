(ns datalog-console.chrome.extension.content-script.main
  (:require [goog.object :as gobj]
            [cljs.core.async :as async :refer [go go-loop chan <! put!]]))



(def port (js/chrome.runtime.connect #js {:name "content-script"}))

(.addListener (gobj/get port "onMessage")
              (fn [msg]
                (println "MSG" msg)
                (.postMessage js/window "pinging ya back" "*")
                (.postMessage port #js {:content-script-message "test"})))