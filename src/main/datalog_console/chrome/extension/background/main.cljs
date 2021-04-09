(ns datalog-console.chrome.extension.background.main
  (:require [goog.object :as gobj]
            [cljs.core.async :as async :refer [go-loop chan <! put!]]))

(defonce remote-conns* (atom {}))
(defonce tools-conns* (atom {}))

(defn handle-devtool-message [devtool-port message _port]
  (cond
    (= "init" (gobj/get message "name"))
    (let [tab-id (gobj/get message "tab-id")]
      (swap! tools-conns* assoc tab-id devtool-port))

    (gobj/getValueByKeys message "datalog-console-devtool-message")
    (let [tab-id      (gobj/get message "tab-id")
          remote-port (get @remote-conns* tab-id)]
      (.postMessage remote-port message))))

(defn set-icon-and-popup [tab-id]
  (js/chrome.browserAction.setIcon
   #js {:tabId tab-id
        :path  #js {"16"  "icon-16.png"
                    "32"  "icon-32.png"
                    "48"  "icon-48.png"
                    "128" "icon-128.png"}})
  (js/chrome.browserAction.setPopup
   #js {:tabId tab-id
        :popup "popups/enabled.html"}))

(defn handle-remote-message [ch message port]
  (cond
    ; send message to devtool
    (gobj/getValueByKeys message "datalog-console-remote-message")
    (let [tab-id (gobj/getValueByKeys port "sender" "tab" "id")]
      (put! ch {:tab-id tab-id :message message})

      ; ack message received
      (when-let [id (gobj/getValueByKeys message "__datalog-console-msg-id")]
        (.postMessage port #js {:ack "ok" "__datalog-console-msg-id" id})))

    ; set icon and popup
    (gobj/getValueByKeys message "datalog-console-fulcro-detected")
    (let [tab-id (gobj/getValueByKeys port "sender" "tab" "id")]
      (set-icon-and-popup tab-id))))

(js/chrome.runtime.onConnect.addListener
 (fn [port]
   (case (gobj/get port "name")
     "datalog-console-remote"
     (let [background->devtool-chan (chan (async/sliding-buffer 50000))
           listener                 (partial handle-remote-message background->devtool-chan)
           tab-id                   (gobj/getValueByKeys port "sender" "tab" "id")]

       (swap! remote-conns* assoc tab-id port)

       (.addListener (gobj/get port "onMessage") listener)
       (.addListener (gobj/get port "onDisconnect")
                     (fn [port]
                       (.removeListener (gobj/get port "onMessage") listener)
                       (swap! remote-conns* dissoc tab-id)
                       (async/close! background->devtool-chan)))

       (go-loop []
         (when-let [{:keys [tab-id message] :as _data} (<! background->devtool-chan)]
            ; send message to devtool
           (if (contains? @tools-conns* tab-id)
             (do
               (.postMessage (get @tools-conns* tab-id) message)
               (recur))
             (recur)))))

     "datalog-console-devtool"
     (let [listener (partial handle-devtool-message port)]
       (.addListener (gobj/get port "onMessage") listener)
       (.addListener (gobj/get port "onDisconnect")
                     (fn [port]
                       (.removeListener (gobj/get port "onMessage") listener)
                       (if-let [port-key (->> @tools-conns*
                                              (keep (fn [[k v]] (when (= v port) k)))
                                              (first))]
                         (swap! tools-conns* dissoc port-key)))))

     (js/console.log "Ignoring connection" (gobj/get port "name")))))