(ns datalog-console.workspaces.transact-cards
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as reagent]
            [datalog-console.components.transact :as c.transact]
            [datalog-console.workspaces.chrome.extension-cards :refer [transact-from-remote!]]
            [datalog-console.workspaces.workspace-db-conn :refer [conn]]
            [datalog-console.client :as c.client]))

;; TODO: rethink how we want to handle the db state for cards

#_(defn fake-remote-transaction [conn transact-str]
  (c.client/process-remote-data
   (transact-from-remote! conn transact-str)))

#_(ws/defcard transact-card
  {::wsm/align {:flex 1}
   ::wsm/node-props {:style {:padding 0}}}
  (ct.react/react-card
   (reagent/as-element [c.transact/transact (partial fake-remote-transaction conn) @c.client/rerror])))