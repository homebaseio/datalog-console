(ns datalog-console.chrome.extension.background.main
  (:require [goog.object :as gobj]))


(println ::loaded)

(defonce tools-conns* (atom {}))
(defonce remote-conns* (atom {}))


(defn handle-devtool-message [devtool-port message _port]
  #_(js/console.log "handling devtool message:" (gobj/get message "name") message _port devtool-port)
  (cond
    (= "init" (gobj/get message "name"))
    (let [tab-id (gobj/get message "tab-id")]
      (swap! tools-conns* assoc tab-id devtool-port)
      (js/console.log #js {:name "init" :tab-id tab-id :message message}))

    (gobj/getValueByKeys message "devtool-message")
    (let [tab-id      (gobj/get message "tab-id")
          remote-port (get @remote-conns* tab-id)]
      (js/console.log #js {:name "devtool-message"
                           :remote-port remote-port
                           :tab-id tab-id 
                           :message message})
      (.postMessage remote-port message))))


(defn handle-remote-message [remote-port message port]

  (let [tab-id (gobj/getValueByKeys port "sender" "tab" "id")]
    (cond
    ; send message to devtool
      (gobj/getValueByKeys message "content-script-message")
      (.postMessage (get @tools-conns* tab-id) message)

      (gobj/getValueByKeys message "datalog-remote-message")
      (.postMessage (get @tools-conns* tab-id) message))))

(js/chrome.runtime.onConnect.addListener
 (fn [port]
   (case (gobj/get port "name")

     "content-script"
     (let [listener (partial handle-remote-message port)
           tab-id   (gobj/getValueByKeys port "sender" "tab" "id")]
       
       (println "content-script handling")

       (swap! remote-conns* assoc tab-id port)

       (.addListener (gobj/get port "onMessage") listener)
       #_(.addListener (gobj/get port "onDisconnet")
                       (fn [port]
                         (.removeListener  (gobj/get port "onMessage") listener))))

     "devtool"
     (let [listener (partial handle-devtool-message port)]
       #_(js/console.log "devtool port: " port)

       (.addListener (gobj/get port "onMessage") listener)
       #_(.addListener (gobj/get port "onDisconnet")
                       (fn [port]
                         (.removeListener  (gobj/get port "onMessage") listener))))


     (js/console.log "Ignoring connection:" (gobj/get port "name")))))