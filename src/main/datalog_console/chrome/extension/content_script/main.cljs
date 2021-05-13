(ns datalog-console.chrome.extension.content-script.main
  (:require [goog.object :as gobj]))



(def port (js/chrome.runtime.connect #js {:name ":datalog-console.remote/content-script-port"}))

;; send message to background that datalog db is registered
;; TODO: Actually check if a database exists. 
;; Need to consider if it makes sense to prefetch the database when the application has registered.
;; This would create overhead when people are not using the devtool.
(when (js/document.documentElement.getAttribute "__datalog-console-remote-installed__")
  (.postMessage port #js {":datalog-console.remote/db-detected" true}))

;; forward devtool message to window
(.addListener (gobj/get port "onMessage")
              (fn [msg]
                (when (gobj/get msg ":datalog-console.client/devtool-message")
                  (.postMessage js/window msg "*"))))

;; forward message to background
(.addEventListener js/window "message"
                   (fn [event]
                     (when (and (identical? (.-source event) js/window)
                                (gobj/getValueByKeys event "data" ":datalog-console.remote/remote-message"))
                       (.postMessage port (gobj/get event "data")))))
