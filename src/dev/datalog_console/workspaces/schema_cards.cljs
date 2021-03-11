(ns datalog-console.workspaces.schema-cards
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as reagent]
            [datalog-console.components.schema :as c.schema]))

(ws/defcard schema-card
  (ct.react/react-card
   (reagent/as-element [c.schema/schema])))


