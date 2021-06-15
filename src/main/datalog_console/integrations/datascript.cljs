(ns datalog-console.integrations.datascript
  (:require [goog.object :as gobj]
            [cljs.reader]
            [datascript.core :as d]))

(defn transact-from-remote! [conn transact-str]
  (try
    (d/transact conn (cljs.reader/read-string transact-str))
    :success
    (catch js/Error e {:error (goog.object/get e "message")})))


(defn enable! 
  "Takes a [datascript](https://github.com/tonsky/datascript) database connection atom. Adds message handlers for a remote datalog-console process to communicate with. E.g. the datalog-console browser [extension](https://chrome.google.com/webstore/detail/datalog-console/cfgbajnnabfanfdkhpdhndegpmepnlmb?hl=en)."
  [{:keys [conn]}]
  (js/document.documentElement.setAttribute "__datalog-console-remote-installed__" true)
  (.addEventListener js/window "message"
                     (fn [event]
                       (when-let [devtool-message (gobj/getValueByKeys event "data" ":datalog-console.client/devtool-message")]
                         (let [msg-type (:type (cljs.reader/read-string devtool-message))]
                           (case msg-type

                             :datalog-console.client/request-whole-database-as-string
                             (.postMessage js/window #js {":datalog-console.remote/remote-message" (pr-str @conn)} "*")

                             :datalog-console.client/transact!
                             (let [transact-result (transact-from-remote! conn (:data (cljs.reader/read-string devtool-message)))]
                               (.postMessage js/window #js {":datalog-console.remote/remote-message" (pr-str transact-result)} "*"))

                             nil))))))