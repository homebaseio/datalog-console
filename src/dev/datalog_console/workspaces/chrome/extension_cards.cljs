(ns datalog-console.workspaces.chrome.extension-cards
  (:require [nubank.workspaces.core :as ws]
            [goog.object :as gobj]
            [cljs.reader]
            [datascript.core :as d]
            [nubank.workspaces.card-types.react :as ct.react]
            [datalog-console.workspaces.workspace-db-conn :refer [conn]]))

(defn transact-from-remote! [conn transact-str]
  (try
    (d/transact conn (cljs.reader/read-string transact-str))
    {:success (pr-str @conn)}
    (catch js/Error e {:error (goog.object/get e "message")})))

(defn enable-remote-database-inspection []
  ;; TODO: consider passing in a map to allow passing in extra information without creating breaking changes.
  ;; One example could be passing in an atom containing a register of the databases.
  ;; We could watch this atom and based on databases being registered or de-registered post a message of the database existance.
  (js/document.documentElement.setAttribute "__datalog-console-remote-installed__" true)
  (.addEventListener js/window "message"
                     (fn [event]
                       (when-let [devtool-message (gobj/getValueByKeys event "data" ":datalog-console.client/devtool-message")]
                         (let [msg-type (:type (cljs.reader/read-string devtool-message))]
                           (case msg-type

                             :datalog-console.client/request-whole-database-as-string
                             (.postMessage js/window #js {":datalog-console.remote/remote-message" (pr-str {:success (pr-str @conn)})} "*")

                             :datalog-console.client/transact!
                             (let [transact-result (transact-from-remote! conn (:data (cljs.reader/read-string devtool-message)))]
                               (.postMessage js/window #js {":datalog-console.remote/remote-message" (pr-str transact-result)} "*"))
                             nil))))))


(enable-remote-database-inspection)


(defn element [name props & children]
  (apply js/React.createElement name (clj->js props) children))


(ws/defcard chrome-extension-card
  (ct.react/react-card
   (element "div" {}
            (element "div" {:className "font-black"} "Install the chrome extension and the open datalog panel. It should connect to the running datascript DB in this card."))))