(ns datalog-console.components.entity
  (:require [datascript.core :as d]
            [reagent.core :as r]))

(defonce conn
  (let [conn (d/create-conn
              {:parent {:db/valueType :db.type/ref
                        :db/cardinality :db.cardinality/one}})]
    (d/transact! conn [{:db/id -1
                        :name "A"
                        :item/size 2
                        :item/age 3
                        :item/thingy 4}
                       {:db/id -2
                        :name "B"
                        :parent -1}])
    conn))


(defn entity-tree-av [[a v]]
  [:div (pr-str a) (pr-str v)])

(defn entity-tree [entity]
  ;; (let [reverse-lookups (d/q conn
  ;;                            '[:find e?
  ;;                              :where []])])
  ;; (fn [])
  [:div
   (for [[a :as av] (seq entity)]
     ^{:key (pr-str a)} [entity-tree-av av])])

(defn entity []
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
       [entity-tree @entity]])))
