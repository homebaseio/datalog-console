(ns datalog-console.workspaces.chrome.extension-cards
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]
            [datascript.core :as d]
            [datalog-console.workspaces.entity-cards :refer [conn]]))


(set! (.-datascriptDbConn js/window) conn)

; add lisener to reset rconn atom
#_(js/chrome.runtime.onConnect.addListener
 (fn [port] (js/console.log "main tab the port: " port)))

#_(js/chrome.runtime.onRequest.addListener
   (fn [port] (js/console.log "main tab the request: " port)))

(defn element [name props & children]
  (apply js/React.createElement name (clj->js props) children))

(ws/defcard chrome-extension-card
  (do
    (js/console.log "Install chrome extension and open datalog panel")
    ;; (js/chrome.runtime.connect #js {:name "datalog-console-remote"})
    (ct.react/react-card
     (element "div" {:className "font-black"} "Install the chrome extension and the open datalog panel. It should connect to the running datascript DB in this card."))))