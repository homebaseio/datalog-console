(ns datalog-console.chrome.extension.content-script.main
  (:require [goog.object :as gobj]))



(def port (js/chrome.runtime.connect #js {:name "content-script"}))

(.addListener (gobj/get port "onMessage")
              (fn [msg]
                (when (gobj/get msg "devtool-message")
                  (.postMessage js/window msg "*"))))

(.addEventListener js/window "message"
                   (fn [event]
                     (when (and (identical? (.-source event) js/window)
                                (gobj/getValueByKeys event "data" "datalog-remote-message"))
                       (println "*content script* forwarding the *db*")
                       (.postMessage port (gobj/get event "data")))))
