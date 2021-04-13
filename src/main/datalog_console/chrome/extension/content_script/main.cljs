(ns datalog-console.chrome.extension.content-script.main
  (:require [goog.object :as gobj]
            [cljs.core.async :as async :refer [go go-loop chan <! put!]]
            [fulcro.inspect.remote.transit :as encode]))


(defn init []
  (println "Content script init"))

(println ::loaded)



;; (defonce active-messages* (atom {}))



;; (defn ack-message [msg]
;;   (go
;;     (let [id  (gobj/get msg "__datalog-console-msg-id")]
;;       (if-let [res (some-> (get @active-messages* id) (<!))]
;;         (do
;;           (swap! active-messages* dissoc id)
;;           res)
;;         nil))))

;; (defn envelope-ack [data]
;;   (let [id   (str (random-uuid))]
;;     (gobj/set data "__datalog-console-msg-id" id)
;;     (swap! active-messages* assoc id (async/promise-chan))
;;     data))

;; (defn setup-new-port 
;;   "addListener is fired when a message is sent from either an extension process (by sendMessage) or a content script (by tabs.sendMessage)."
;;   []
;;   (let [port (js/chrome.runtime.connect #js {:name "datalog-console-remote"})]
;;     (.addListener (gobj/get port "onMessage")
;;                   (fn [msg]
;;                     (cond
;;                       (gobj/getValueByKeys msg "datalog-console-devtool-message")
;;                       (.postMessage js/window msg "*") ;; Q: Why is there a asterix here?

;;                       :else
;;                       (when-let [ch (some->> (gobj/getValueByKeys msg "__datalog-console-msg-id")
;;                                              (get @active-messages*))]
;;                         (put! ch msg)))))
;;     port))

;; (defn event-loop []
;;   (when (js/document.documentElement.getAttribute "__datalog-console-remote-installed__")
;;     (let [content-script->background-chan (chan (async/sliding-buffer 50000))
;;           port*                           (atom (setup-new-port))]

;;       ; set browser icon
;;       (.postMessage @port* #js {:datalog-console-db-detected true})

;;       ; clear inspector
;;       (put! content-script->background-chan
;;             (envelope-ack
;;              #js {:datalog-console-remote-message
;;                   (encode/write
;;                    {:type :fulcro.inspect.client/reset
;;                     :data {}})}))

;;       (.addEventListener js/window "message"
;;                          (fn [event]
;;                            (when (and (identical? (.-source event) js/window)
;;                                       (gobj/getValueByKeys event "data" "datalog-console-remote-message"))
;;                              (put! content-script->background-chan (envelope-ack (gobj/get event "data"))))))

;;       (.postMessage js/window #js {:datalog-console-start-consume true} "*")

;;       (js/console.log "Hello world!" port*)

;;       (go-loop []
;;         (when-let [data (<! content-script->background-chan)]
;;           ; keep trying to send
;;           (loop []
;;             (.postMessage @port* data)
;;             (let [timer (async/timeout 1000)
;;                   acker (ack-message data)
;;                   [_ c] (async/alts! [acker timer] :priority true)]
;;               ; restart the port in case of a timeout
;;               (when (= c timer)
;;                 (reset! port* (setup-new-port))
;;                 (recur))))
;;           (recur)))))

;;   :ready)

;; (defonce start (event-loop))