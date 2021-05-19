(ns datalog-console.chrome.extension.content-script.main
  (:require [goog.object :as gobj]))

(def port (js/chrome.runtime.connect #js {:name ":datalog-console.remote/content-script-port"}))

(defn supports-datalog-console? []
  (js/document.documentElement.getAttribute "__datalog-console-remote-installed__"))

(defn detect-db! []
  (when (supports-datalog-console?)
    (.postMessage port #js {":datalog-console.remote/db-detected" true})))

(defn init-detector! 
  "Attempts to detect if the datalog console is supported in the current tab multiple times before giving up."
  []
  (detect-db!)
  (js/setTimeout detect-db! 1000)
  (js/setTimeout detect-db! 3000)
  (js/setTimeout detect-db! 10000))

(init-detector!)

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
