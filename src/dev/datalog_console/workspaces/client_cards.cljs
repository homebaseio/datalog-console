(ns datalog-console.workspaces.client-cards
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as r]
            [datalog-console.client :as client]
            [datalog-console.workspaces.entity-cards :refer [conn]]))

(reset! client/rconn conn)


(ws/defcard client-card
  {::wsm/align {:flex 1}
   ::wsm/node-props {:style {:overflow "hidden" :padding 0}}}
  (ct.react/react-card
   (r/as-element [client/root])))