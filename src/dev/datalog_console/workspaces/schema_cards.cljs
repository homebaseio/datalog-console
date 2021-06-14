(ns datalog-console.workspaces.schema-cards
  {:no-doc true}
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as reagent]
            [datalog-console.components.schema :as c.schema]
            [datascript.core :as d]))

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
                        :description 1
                        :person/parents [-1 -3]}
                       {:db/id -5
                        :name 1
                        :person/friends [-1]}
                       {:db/id -6
                        :name (js/Date.)
                        :employer/person -1}])
    conn))

(ws/defcard schema-card
  {::wsm/align {:flex 1}
   ::wsm/node-props {:style {:padding 0}}}
  (ct.react/react-card
   (reagent/as-element [c.schema/schema conn])))




