(ns datalog-console.components.entity
  (:require [datascript.core :as d]
            [reagent.core :as r]))

(def conn
  (let [conn (d/create-conn
              {:person/parents {:db/valueType :db.type/ref
                                :db/cardinality :db.cardinality/many}
               :person/friends {:db/valueType :db.type/ref
                                :db/cardinality :db.cardinality/many}
               :employer/person {:db/valueType :db.type/ref
                                 :db/cardinality :db.cardinality/one}})]
    (d/transact! conn [{:db/id -1
                        :name "A"
                        :item/size 2
                        :item/age 3
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

(defn entity? [v]
  (try
    (not (nil? (:db/id v)))
    (catch js/Error e false)))

(defn entity-tree-av [[a v]]
  (let [entity? (entity? (if (set? v) (first v) v))]
    [:div {:class "flex w-full odd:bg-gray-100"}
     [:span
      {:title (str a)
       :class "w-1/2 pl-2 line-clamp-1"}
      (str a)]
     [:span
      {:title (str v)
       :class "w-1/2 pl-4 line-clamp-1"}
      (if (and entity? (set? v))
        (str "[" (count v) " item" (when (< 1 (count v)) "s") "]")
        (str v))]]))

(defn make-keyword-reverse-ref [kw]
  (keyword (str (namespace kw) "/_" (name kw))))

(defn reverse-refs [entity]
  (let [rev-ref-attrs-and-eids (d/q '[:find ?ref-attr ?e
                                          :in $ [?ref-attr ...] ?ref-id
                                          :where [?e ?ref-attr ?ref-id]]
                                        @conn
                                        (for [[attr props] (:schema @conn)
                                              :when (= :db.type/ref (:db/valueType props))]
                                          attr)
                                        (:db/id entity))
        rev-ref-eids-grouped-by-attr (group-by first rev-ref-attrs-and-eids)
        rev-ref-entities-grouped-by-attr (reduce-kv (fn [acc k v]
                                                          (conj acc [(make-keyword-reverse-ref  k)
                                                                     (set (map (fn [[_ eid]] (d/entity @conn eid)) v))]))
                                                        [] rev-ref-eids-grouped-by-attr)]
    rev-ref-entities-grouped-by-attr))

(defn entity-tree [entity]
  [:div {:class "mt-4"}
   [:div
    [:div {:class "flex justify w-full font-bold"}
     [:span {:class "w-1/2 pl-2"} "Attribute"]
     [:span {:class "w-1/2 pl-4"} "Value"]]
    [entity-tree-av [:db/id (:db/id entity)]]
    (for [[a :as av] (into
                      (vec entity)
                      (reverse-refs entity))]
      ^{:key (str a)} [entity-tree-av av])]])

(defn entity []
  (let [lookup (r/atom "")
    (fn []
      [:div {:class "w-full h-full"}
       [:label [:p {:class "font-bold"} "Entity lookup"]
        [:input {:type "text"
                 :placholder "Entity id or lookup..."
                 :class "border py-1 px-2 rounded"
                 :value @lookup
                 :on-change #(reset! lookup (.-value (.-target %)))
                 :on-key-down #(when (= "Enter" (.-key %))
                                 (reset! entity (d/entity @conn (cljs.reader/read-string @lookup))))}]]
       [entity-tree @entity]])))
