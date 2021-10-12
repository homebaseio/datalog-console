(ns datalog-console.workspaces.console-cards
  {:no-doc true}
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as r]
            [datalog-console.console :as console]
            [datalog-console.workspaces.workspace-db-conn :refer [conn]]))

(reset! console/r-db-conn conn)

(ws/defcard console-card
  {::wsm/align {:flex 1}
   ::wsm/node-props {:style {:overflow "hidden" :padding 0}}}
  (ct.react/react-card
   (r/as-element [console/root])))