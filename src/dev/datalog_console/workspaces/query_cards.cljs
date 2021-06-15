(ns datalog-console.workspaces.query-cards
  {:no-doc true}
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as reagent]
            [datalog-console.components.query :as c.query]
            [datascript.core :as d]
            [datalog-console.workspaces.workspace-db-conn :refer [conn]]))


(ws/defcard query-card
  {::wsm/align {:flex 1}
   ::wsm/node-props {:style {:padding 0}}}
  (ct.react/react-card
   (reagent/as-element [c.query/query conn])))




