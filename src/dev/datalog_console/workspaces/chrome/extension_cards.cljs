(ns datalog-console.workspaces.chrome.extension-cards
  (:require [nubank.workspaces.core :as ws]
            [cljs.reader]
            [nubank.workspaces.card-types.react :as ct.react]
            [datalog-console.workspaces.entity-cards :refer [conn]]
            [datalog-console.integrations.datascript :as integrations]))

(integrations/enable! {:conn conn})

(defn element [name props & children]
  (apply js/React.createElement name (clj->js props) children))

(ws/defcard chrome-extension-card
  (ct.react/react-card
   (element "div" {}
            (element "div" {:className "font-black"} "Install the chrome extension and the open datalog panel. It should connect to the running datascript DB in this card."))))