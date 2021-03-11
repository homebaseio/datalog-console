(ns datalog-console.components.schema
  (:require [datascript.core :as d]
            [reagent.core :as r]))

(defonce conn
  (let [conn (d/create-conn
              {:parent {:db/valueType :db.type/ref
                        :db/cardinality :db.cardinality/one}})]
    (d/transact! conn [{:db/id -1
                        :name "A"
                        :age 1
                        :item/size 2
                        :item/age 3
                        :item/thingy 4}
                       {:db/id -2
                        :name "B"
                        :parent -1}])
    conn))


(defn schema-tree-av [[a v]]
  [:div (pr-str a) (pr-str v)])

(defn schema-tree-ns [[a-ns av-pairs-in-ns]]
  (if a-ns
    [:div ":" a-ns "/"
     [:div {:class "pl-3"}
      (for [[a :as av] (seq av-pairs-in-ns)]
        ^{:key (pr-str a)} [schema-tree-av av])]]
    [:div
     (for [[a :as av] av-pairs-in-ns]
       ^{:key (pr-str a)} [schema-tree-av av])]))

(defn agg-attrs-by-ns [entity]
  (reduce
   (fn [acc [a v]]
     (assoc-in acc [(namespace a) (name a)] v))
   {} entity))

(defn schema-tree [entity]
  [:div
   (for [[a-ns :as x] (agg-attrs-by-ns entity)]
     ^{:key (pr-str a-ns)} [schema-tree-ns x])])

;; TODO: replace all the entity stuff below with the real db schema
(defn schema []
  (let [lookup (r/atom "")
        entity (r/atom nil)]
    (fn []
      [:div
       [:label "Entity lookup"
        [:input {:type "text"
                 :placholder "Entity id or lookup..."
                 :class "border py-1 px-2 rounded"
                 :value @lookup
                 :on-change #(reset! lookup (.-value (.-target %)))
                 :on-key-down #(when (= "Enter" (.-key %))
                                 (reset! entity (d/entity @conn (cljs.reader/read-string @lookup))))}]]
       [schema-tree @entity]])))
