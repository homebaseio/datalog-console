(ns datalog-console.messaging-test
  (:require [cljs.test :as t :refer [is deftest testing]]
            [clojure.core.async :as async :refer [>! <! go chan]]
            [datalog-console.lib.messaging :as msg]
            [cljs.reader]))

(defn take-with-timout! [c t]
  (go
    (first (async/alts! [c (async/timeout t)] :priority true))))

(deftest enqueue-message
  (let [send-chan (chan 10)
        conn (msg/create-conn {:send-fn (fn [msg] (go (>! send-chan msg)))})]

    (testing "enqueue a message"
      (msg/send {:conn conn
                 :type :test})
      (is (= (count (:msgs @conn)) 1)))))

(deftest acknowledge-message
  (t/async done
           (go
             (let [send-chan (chan 10)
                   listen-chan (chan 10)
                   ack-message-counter (atom 0)
                   send-conn (msg/create-conn {:send-fn (fn [msg]
                                                          (go (>! send-chan msg)))
                                               :receive-fn (fn [cb]
                                                             (async/go-loop []
                                                               (when-let [msg (<! (take-with-timout! listen-chan 20))]
                                                                 (swap! ack-message-counter inc)
                                                                 (cb msg))
                                                               (recur)))})
                   _receive-conn (msg/create-conn {:send-fn (fn [msg] (go (>! listen-chan msg)))
                                                   :receive-fn (fn [cb] (async/go-loop []
                                                                          (when-let [msg (<! (take-with-timout! send-chan 50))]
                                                                            (cb msg))
                                                                          (recur)))})]

               (testing "send 1 message"
                 (msg/send {:conn send-conn
                            :type :test-message
                            :data {}})
                 (<! (async/timeout 250))
                 (is (= 1 (count (:msgs @send-conn))))
                 (is (= 1 @ack-message-counter))))

             (done))))

(deftest retry-messages
  (t/async done
           (go
             (let [send-chan (chan 10)
                   send-conn (msg/create-conn {:base-retry-timeout 2
                                               :send-fn (fn [msg]
                                                          (go (>! send-chan msg)))
                                               :receive-fn (fn [_cb]
                                                             nil)})
                   _receive-conn (msg/create-conn {:send-fn (fn [_msg] (go nil))
                                                   :base-received-timeout 100
                                                   :receive-fn (fn [cb] (async/go-loop []
                                                                          (when-let [msg (<! (take-with-timout! send-chan 1000))]
                                                                            (cb msg))
                                                                          (recur)))})]

               (msg/send {:conn send-conn
                          :type :test-message
                          :data {}})
               (<! (async/timeout 100))
               (is (= 0 (count (:msgs @send-conn))))
               (is (= 1 (count (filter some? (:failed-list @send-conn)))))

               (testing "track unique messages received"
                 (is (= (count @msg/received-msgs) 1))
                 (<! (async/timeout 300))
                 (is (= (count @msg/received-msgs) 0))))

             (done))))

