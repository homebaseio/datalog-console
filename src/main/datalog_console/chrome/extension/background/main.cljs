(ns datalog-console.chrome.extension.background.main
  {:no-doc true}
  (:require [goog.object :as gobj]
            [cljs.reader]
            [datalog-console.lib.messaging :as msg]))

(defonce port-conns (atom {:tools {}
                           :remote {}}))

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

(js/chrome.runtime.onConnect.addListener
 (fn [port]
   (let [tab-id (atom (gobj/getValueByKeys port "sender" "tab" "id"))]
     (msg/create-conn {:to port
                       :send-fn (fn [{:keys [to msg]}]
                                  (.postMessage to (clj->js {(str ::msg/msg) (pr-str msg)})))
                       :tab-id tab-id
                       :routes {:datalog-console.client/init! (fn [conn _msg]
                                                                (swap! port-conns assoc-in [:tools @(:tab-id @conn)] (:to @conn)))
                                :datalog-console.remote/db-detected (fn [conn _msg]
                                                                      (set-icon-and-popup @(:tab-id @conn)))
                                :* (fn [conn msg]
                                     (let [env-context (if (gobj/getValueByKeys (:to @conn) "sender" "tab" "id") :tools :remote)
                                           to (get-in @port-conns [env-context @(:tab-id @conn)])]
                                       (.postMessage to (clj->js {(str ::msg/msg) (pr-str msg)}))))}
                       :receive-fn (fn [cb _conn]
                                     (when-let [tab-id (gobj/getValueByKeys port "sender" "tab" "id")]
                                       (swap! port-conns assoc-in [:remote tab-id] port))
                                     (let [listener (fn [message _port]
                                                      (when-let [msg-tab-id (gobj/get message "tab-id")]
                                                        (reset! tab-id msg-tab-id))
                                                      (cb (cljs.reader/read-string (gobj/get message (str ::msg/msg)))))]
                                       (.addListener (gobj/get port "onMessage") listener)
                                       (.addListener (gobj/get port "onDisconnect")
                                                     (fn [port]
                                                       (when-let [msg (gobj/get port "onMessage")]
                                                         (.removeListener msg listener))
                                                       ;; conn-context may not work with other connection types: eg native application, cross-extension connections
                                                       (let [conn-context (if (gobj/getValueByKeys port "sender" "tab" "id")
                                                                            :remote
                                                                            :tools)]
                                                         (when-let [port-key (->> (conn-context @port-conns)
                                                                                  (keep (fn [[k v]] (when (= v port) k)))
                                                                                  (first))]
                                                           (swap! port-conns update conn-context dissoc port-key)))))))}))))

