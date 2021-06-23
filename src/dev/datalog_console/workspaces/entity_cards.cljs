(ns datalog-console.workspaces.entity-cards
  {:no-doc true}
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as r]
            [datalog-console.workspaces.workspace-db-conn :refer [conn]]
            [datalog-console.components.entities :as c.entities]
            [datalog-console.components.entity :as c.entity]))

(def entity-lookup-ratom (r/atom ""))

(ws/defcard entity-card
  {::wsm/align {:flex 1}
   ::wsm/node-props {:style {:padding 0}}}
  (ct.react/react-card
   (r/as-element [c.entity/entity conn entity-lookup-ratom])))

(ws/defcard entities
  {::wsm/align {:flex 1}
   ::wsm/node-props {:style {:padding 0}}}
  (ct.react/react-card
   (r/as-element [c.entities/entities conn entity-lookup-ratom])))

