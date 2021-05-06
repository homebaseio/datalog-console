(ns datalog-console.chrome.extension.background.main
  (:require [goog.object :as gobj]))


(defonce tools-conns* (atom {}))
(defonce remote-conns* (atom {}))

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

(defn handle-devtool-message [devtool-port message _port]
  (let [tab-id (gobj/get message "tab-id")]
    (cond
      (= ":datalog-console.client/init" (gobj/get message "name"))
      (swap! tools-conns* assoc tab-id devtool-port)

      ;; send message to content-script
      (gobj/getValueByKeys message ":datalog-console.client/devtool-message")
      (.postMessage (get @remote-conns* tab-id) message))))


(defn handle-remote-message [_remote-port message port]
  (let [tab-id (gobj/getValueByKeys port "sender" "tab" "id")]
    (cond
      ; send message to devtool
      (gobj/getValueByKeys message ":datalog-console.remote/remote-message")
      (.postMessage (get @tools-conns* tab-id) message)

      ; set icon and popup
      (gobj/getValueByKeys message ":datalog-console.remote/db-detected")
      (set-icon-and-popup tab-id))))

(js/chrome.runtime.onConnect.addListener
 (fn [port]
   (let [remove-listener (fn [port listener]
                           (when-let [msg (gobj/get port "onMessage")]
                             (.removeListener msg listener)))]
     (case (gobj/get port "name")

       ":datalog-console.remote/content-script-port"
       (let [listener (partial handle-remote-message port)
             tab-id   (gobj/getValueByKeys port "sender" "tab" "id")]

         (swap! remote-conns* assoc tab-id port)

         (.addListener (gobj/get port "onMessage") listener)
         (.addListener (gobj/get port "onDisconnect")
                       (fn [port]
                         (remove-listener port listener)
                         (swap! remote-conns* dissoc tab-id))))

       ":datalog-console.client/devtool-port"
       (let [listener (partial handle-devtool-message port)]
         
         (.addListener (gobj/get port "onMessage") listener)
         (.addListener (gobj/get port "onDisconnect")
                       (fn [port]
                         (remove-listener port listener)
                         (when-let [port-key (->> @tools-conns*
                                                  (keep (fn [[k v]] (when (= v port) k)))
                                                  (first))]
                           (swap! tools-conns* dissoc port-key)))))


       nil))))
