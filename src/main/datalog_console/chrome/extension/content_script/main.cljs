(ns datalog-console.chrome.extension.content-script.main
  (:require [goog.object :as gobj]))



(def port (js/chrome.runtime.connect #js {:name "content-script"}))

;; send message to background that datalog db is registered
;; TODO: Actually check if a database exists. 
;; Need to consider if it makes sense to prefetch the database when the application has registered.
;; This would create overhead when people are not using the devtool.
(when (js/document.documentElement.getAttribute "__datalog-inspect-remote-installed__")
  (.postMessage port #js {:datalog-db-detected true}))

;; forward devtool message to window
(.addListener (gobj/get port "onMessage")
              (fn [msg]
                (when (gobj/get msg "devtool-message")
                  (.postMessage js/window msg "*"))))

;; forward message to background
(.addEventListener js/window "message"
                   (fn [event]
                     (when (and (identical? (.-source event) js/window)
                                (gobj/getValueByKeys event "data" "datalog-remote-message"))
                       (.postMessage port (gobj/get event "data")))))
