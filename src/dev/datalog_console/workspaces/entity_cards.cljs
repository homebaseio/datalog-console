(ns datalog-console.workspaces.entity-cards
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as reagent]
            [datalog-console.components.entity :as c.entity]))

(ws/defcard entity-card
  (ct.react/react-card
   (reagent/as-element [c.entity/entity])))


