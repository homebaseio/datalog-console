(ns datalog-console.lib.messaging
  (:require [clojure.core.async :as async :refer [>! <! go chan]]
            [goog.object :as gobj]
            [cljs.reader]))




(def active-connections (atom {}))
(def received-msgs (atom #{}))


(defn enqueue [conn {:keys [id] :as msg}]
  (when conn
    (swap! conn assoc-in [:msgs id] (merge msg {:tries 0}))
    (go (>! (:send-queue @conn) msg))))

  (defn send [{:keys [conn type data]}]
    (let [ts (js/Date.now)
          ;; TODO: use nano-id
          id (rand)]
      (enqueue conn {:id id
                     :type type
                     :data data
                     :timestamp ts})))

(defn add-to-failed [msg]
  (fn [messenger]
    (-> messenger
        (update :msgs dissoc (:id msg))
        (update :failed-list butlast)
        (update :failed-list conj msg))))

(defn handle-retry [conn id]
  ;; TODO: refactor this
  (when-let [msg (get-in @conn [:msgs id])]
    (when-not (= (:type msg) ::ack)
      (go (<! (async/timeout (* (:base-retry-timeout @conn) (:tries msg))))
          (when-let [msg (get-in @conn [:msgs id])]
            (if (<= 3 (:tries msg))
              (swap! conn (add-to-failed msg))
              (when-let [msg (get-in @conn [:msgs id])]
                (swap! conn update-in [:msgs id :tries] inc)
                ((:send-fn @conn) {:tab-id (:tab-id @conn)
                                   :to (:to @conn)
                                   :conn conn
                                   :msg msg})
                (handle-retry conn id))))))))

(defn handle-send [conn]
  (async/go-loop []
    (let [msg (<! (:send-queue @conn))]
      (swap! conn update-in [:msgs (:id msg) :tries] inc)
      ((:send-fn @conn) {:tab-id (:tab-id @conn)
                         :to (:to @conn)
                         :conn conn
                         :msg msg})
      (handle-retry conn (:id msg)))
    (recur)))

(defn forward [forward-to-conn]
  (fn [conn msg]
    (send {:conn forward-to-conn
           :from conn
           :type (:type msg)
           :data (:data msg)})))

(defn route [conn msg]
  (let [{:keys [routes]} @conn]
    (if-let [route-fn (get routes (:type msg))]
      (route-fn conn msg)
      (when-let [route-fn (:* routes)]
        (route-fn conn msg)))))

(defn handle-listen [conn]
  ((:receive-fn @conn)
   (fn [msg]
     (if (= ::ack (:type msg))
       (swap! conn update :msgs dissoc (:id (:data msg)))
       (when-not (contains? @received-msgs (:id msg))

         (route conn msg)
         (send {:conn conn
                :type ::ack
                :data {:id (:id msg)}})
         (swap! received-msgs conj (:id msg))
         (go
           (<! (async/timeout (:base-received-timeout @conn)))
           (swap! received-msgs disj (:id msg))))))
   conn))

(defn create-conn [{:keys [to from tab-id send-fn receive-fn base-retry-timeout base-received-timeout routes]}]
  (let [conn (atom {:msgs {}
                    :id (rand)
                    :routes (or routes {})
                    :send-queue (chan 1000)
                    :base-retry-timeout (or base-retry-timeout 1000)
                    :base-received-timeout (or base-received-timeout 0) ; This is a convenience for testing. Need to rethink.
                    :failed-list (take 20 (repeat nil))
                    :send-fn send-fn
                    :receive-fn receive-fn
                    :to to
                    :from from
                    :tab-id tab-id})]
    (swap! active-connections assoc (:id @conn) conn)
    (handle-send conn)
    (when (:receive-fn @conn)
      (handle-listen conn))
    conn))