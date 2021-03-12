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
  (some? false #_(try
           (:db/id (first v))
           (catch js/Error e false))))




(defn entity-tree-av [[a v]]
  [:div {:class "flex justify w-full hover:bg-gray-200"}
   [:span {:class "w-1/2 pl-2"} (str a)]
   [:span {:class "w-1/2 pl-4"} (str v)]])


(defn make-keyword-reverse-ref [kw]
  (keyword (str (namespace kw) "/_" (name kw))))




(defn entity-tree [entity]
  [:div {:class (str "mt-4")}
   (when-let [db-id (:db/id entity)]
     (let [reverse-lookups (d/q '[:find ?e ?ref
                                  :in $ [?ref ...] ?ref-id
                                  :where [?e ?ref ?ref-id]]
                                @conn
                                (for [[attr props] (:schema @conn)
                                      :when (= :db.type/ref (:db/valueType props))]
                                  attr)
                                db-id)
           group-reverse-lookups (group-by second reverse-lookups)
           counted-reverse-refs (reduce-kv (fn [vect k v]
                                             (conj vect [(make-keyword-reverse-ref  k)
                                                         (set (map (fn [[eid]] (d/entity @conn eid)) v))
                                                         true
                                                         #_(str "[ " (count v) " items ]")]))
                                           []
                                           group-reverse-lookups)
           entity-attrs (reduce conj (vec (seq entity)) counted-reverse-refs)]
       [:div
        [:div
         [:div {:class "flex justify w-full font-bold"}
          [:span {:class "w-1/2 pl-2"} "Attribute"]
          [:span {:class "w-1/2 pl-4"} "Value"]]
         [entity-tree-av [:db/id db-id]]]
        (for [[a :as av] entity-attrs #_(seq entity)]
          ^{:key (pr-str a)} [entity-tree-av av])]))])


(defn entity []
  (let [lookup (r/atom "")
        entity (r/atom nil)]
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
