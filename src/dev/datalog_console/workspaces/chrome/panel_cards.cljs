(ns datalog-console.workspaces.chrome.panel-cards
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]
            [datalog-console.chrome.panel :as panel]
            [datalog-console.workspaces.entity-cards :refer [conn]]))

(defn element [name props & children]
  (apply js/React.createElement name (clj->js props) children))

(ws/defcard chrome-panel-card
  (do
    (panel/install! conn)
    (ct.react/react-card
     (element "div" {:className "font-black"} "Open the chrome console"))))
