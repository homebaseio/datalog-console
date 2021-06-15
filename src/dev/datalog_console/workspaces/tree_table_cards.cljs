(ns datalog-console.workspaces.tree-table-cards
  {:no-doc true}
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as r]
            [datascript.core :as d]
            [datalog-console.components.entity :as c.entity]
            [datalog-console.components.tree-table :as c.tree-table]))

(def conn
  (let [conn (d/create-conn
              {:name {:db/unique :db.unique/identity}
               :person/parents {:db/valueType :db.type/ref
                                :db/cardinality :db.cardinality/many}
               :person/friends {:db/valueType :db.type/ref
                                :db/cardinality :db.cardinality/many}
               :employer/person {:db/valueType :db.type/ref
                                 :db/cardinality :db.cardinality/one}})]
    (d/transact! conn [{:db/id -1
                        :name "A"
                        :description "I'm baby truffaut craft beer cold-pressed sartorial listicle aesthetic normcore edison bulb XOXO seitan celiac. Blog irony succulents synth shabby chic. Health goth hexagon semiotics wolf jean shorts narwhal, intelligentsia hell of hot chicken. La croix pour-over coloring book wayfarers austin, asymmetrical whatever messenger bag four dollar toast activated charcoal vice banjo vegan portland. Twee selfies chillwave normcore 3 wolf moon af coloring book flexitarian plaid poke mlkshk pitchfork bicycle rights. Franzen gluten-free lyft snackwave meditation man braid, tousled shabby chic banjo. Selfies tote bag chicharrones, hammock fam umami woke typewriter poutine fanny pack artisan copper mug."
                        :employer/person -2
                        :item/size 2
                        :item/age 3
                        :item/with-a-very-very-very-long-name 3
                        :item/thingy 4}
                       {:db/id -2
                        :name "B"
                        :person/parents [-1 -3]}
                       {:db/id -3
                        :name "C"}
                       {:db/id -4
                        :name "D"
                        :person/parents [-1 -3]}
                       {:db/id -5
                        :name "E"
                        :person/friends [-1]}
                       {:db/id -6
                        :name "F"
                        :employer/person -1}])
    conn))

(ws/defcard tree-table-card
  {::wsm/align {:flex 1}
   ::wsm/node-props {:style {:padding 0}}}
  (ct.react/react-card
   (r/as-element
    [:div {:class "w-full h-full overflow-auto pb-5"}
     [c.tree-table/tree-table
      {:caption (str "entity " (select-keys (d/entity @conn 1) [:db/id]))
       :head-row ["Attribute", "Value"]
       :rows (c.entity/entity->rows (d/entity @conn 1))
       :expandable-row? c.entity/expandable-row?
       :expand-row c.entity/expand-row
       :render-col c.entity/render-col}]])))


