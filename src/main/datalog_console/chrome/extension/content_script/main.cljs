(ns datalog-console.chrome.extension.content-script.main
  (:require [goog.object :as gobj]))



(def port (js/chrome.runtime.connect #js {:name "content-script"}))

(when (js/document.documentElement.getAttribute "__datalog-inspect-remote-installed__")
  (.postMessage port #js {:datalog-db-detected true}))

(.addListener (gobj/get port "onMessage")
              (fn [msg]
                (when (gobj/get msg "devtool-message")
                  (.postMessage js/window msg "*"))))

(.addEventListener js/window "message"
                   (fn [event]
                     (when (and (identical? (.-source event) js/window)
                                (gobj/getValueByKeys event "data" "datalog-remote-message"))
                       (.postMessage port (gobj/get event "data")))))
