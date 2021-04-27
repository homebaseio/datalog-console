(ns datalog-console.chrome.extension.background.main
  (:require [goog.object :as gobj]))


(defonce tools-conns* (atom {}))
(defonce remote-conns* (atom {}))


(defn handle-devtool-message [devtool-port message _port]
  (let [tab-id (gobj/get message "tab-id")]
    (cond
      (= "init" (gobj/get message "name"))
      (swap! tools-conns* assoc tab-id devtool-port)

      ;; send message to content-script
      (gobj/getValueByKeys message "devtool-message")
      (.postMessage (get @remote-conns* tab-id) message))))


(defn set-icon-and-popup [tab-id]
  (js/chrome.browserAction.setIcon
   #js {:tabId tab-id
        :path  #js {"16"  "images/active/icon-16.png"
                    "32"  "images/active/icon-16.png"
                    "48"  "images/active/icon-16.png"
                    "128" "images/active/icon-16.png"}})
  (js/chrome.browserAction.setPopup
   #js {:tabId tab-id
        :popup "popups/enabled.html"}))


(defn handle-remote-message [_remote-port message port]
  (let [tab-id (gobj/getValueByKeys port "sender" "tab" "id")]
    (cond
      ; send message to devtool
      (gobj/getValueByKeys message "datalog-remote-message")
      (.postMessage (get @tools-conns* tab-id) message)

      ; set icon and popup
      (gobj/getValueByKeys message "datalog-db-detected")
      (set-icon-and-popup tab-id))))

(js/chrome.runtime.onConnect.addListener
 (fn [port]
   (case (gobj/get port "name")

     "content-script"
     (let [listener (partial handle-remote-message port)
           tab-id   (gobj/getValueByKeys port "sender" "tab" "id")]

       (swap! remote-conns* assoc tab-id port)

       (.addListener (gobj/get port "onMessage") listener)
       #_(.addListener (gobj/get port "onDisconnet")
                       (fn [port]
                         (.removeListener  (gobj/get port "onMessage") listener))))

     "devtool"
     (let [listener (partial handle-devtool-message port)]

       (.addListener (gobj/get port "onMessage") listener)
       #_(.addListener (gobj/get port "onDisconnet")
                       (fn [port]
                         (.removeListener  (gobj/get port "onMessage") listener))))


     (js/console.log "Ignoring connection:" (gobj/get port "name")))))