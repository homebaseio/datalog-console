(ns datalog-console.integrations.datascript
  (:require [goog.object :as gobj]
            [cljs.reader]
            [datascript.core :as d]
            [datalog-console.lib.version :as dc]
            [datalog-console.lib.messaging :as msg]))

(defn transact-from-devtool! [db-conn transact-str]
  (try
    (d/transact db-conn (cljs.reader/read-string transact-str))
    {:success true}
    (catch js/Error e {:error (goog.object/get e "message")})))

(defn enable! 
  "Takes a [datascript](https://github.com/tonsky/datascript) database connection atom. Adds message handlers for a remote datalog-console process to communicate with. E.g. the datalog-console browser [extension](https://chrome.google.com/webstore/detail/datalog-console/cfgbajnnabfanfdkhpdhndegpmepnlmb?hl=en)."
  [{db-conn :conn}]
  (try
    (js/document.documentElement.setAttribute "__datalog-console-remote-installed__" true)
    (msg/create-conn {:to js/window
                      :routes {:datalog-console.client/request-whole-database-as-string
                               (fn [msg-conn _msg]
                                 (msg/send {:conn msg-conn
                                            :type :datalog-console.remote/db-as-string
                                            :data (pr-str @db-conn)}))

                               :datalog-console.client/transact!
                               (fn [msg-conn msg] (let [transact-result (transact-from-devtool! db-conn (:data msg))]
                                                    (msg/send {:conn msg-conn
                                                               :type :datalog-console.client.response/transact!
                                                               :data transact-result})))

                               :datalog-console.client/request-integration-version
                               (fn [msg-conn _msg]
                                 (msg/send {:conn msg-conn
                                            :type :datalog-console.remote/version
                                            :data dc/version}))}
                      :send-fn (fn [{:keys [to conn msg]}]
                                 (.postMessage to (clj->js {(str ::msg/msg) (pr-str msg)
                                                            :conn-id (:id @conn)})))
                      :receive-fn (fn [cb msg-conn]
                                    (.addEventListener (:to @msg-conn) "message"
                                                       (fn [event]
                                                         (when (and (identical? (.-source event) js/window)
                                                                    (not= (:id @msg-conn) (gobj/getValueByKeys event "data" "conn-id")))
                                                           (when-let [raw-msg (gobj/getValueByKeys event "data" (str ::msg/msg))]
                                                             (cb (cljs.reader/read-string raw-msg)))))))})
    (catch js/Error _e nil)))



