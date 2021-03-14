(ns datalog-console.workspaces.entity-cards
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as reagent]
            [datalog-console.components.entity :as c.entity]))

(ws/defcard entity-card
  {::wsm/align {:flex 1}
   ::wsm/node-props {:style {:overflow "hidden" :padding 0}}}
  (ct.react/react-card
   (reagent/as-element [c.entity/entity])))


