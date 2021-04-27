(ns datalog-console.workspaces.chrome.extension-cards
  (:require [nubank.workspaces.core :as ws]
            [goog.object :as gobj]
            [cljs.reader]
            [nubank.workspaces.card-types.react :as ct.react]
            [datalog-console.workspaces.entity-cards :refer [conn]]))



(defn install []
  (js/document.documentElement.setAttribute "__datalog-inspect-remote-installed__" true))

(install)



(.addEventListener js/window "message"
                   (fn [event] 
                     (when-let [devtool-message (gobj/getValueByKeys event "data" "devtool-message")]
                       (let [msg-type (:type (cljs.reader/read-string devtool-message))]
                         
                         (case msg-type

                           :datalog-console.client/request-whole-database-as-string
                           (.postMessage js/window #js {:datalog-remote-message (pr-str @conn)} "*")

                           nil)))))


(defn element [name props & children]
  (apply js/React.createElement name (clj->js props) children))



(ws/defcard chrome-extension-card
  (ct.react/react-card
   (element "div" {}
            (element "div" {:className "font-black"} "Install the chrome extension and the open datalog panel. It should connect to the running datascript DB in this card."))))