(ns datalog-console.components.schema
  (:require [datascript.core :as d]
            [reagent.core :as r]))

(def conn
  (let [conn (d/create-conn
              {:parent {:db/valueType :db.type/ref
                        :db/cardinality :db.cardinality/one
                        :db/doc "the parent"}
               :child {:db/valueType :db.type/ref
                       :db/cardinality :db.cardinality/one
                       :db/doc "the child"}
               :employer/person {:db/valueType :db.type/ref
                                 :db/cardinality :db.cardinality/one
                                 :db/doc "the employer"}
               :employer/parent {:db/valueType :db.type/ref
                                 :db/cardinality :db.cardinality/one
                                 :db/doc "the parent employer"}
               :test.employer/person {:db/valueType :db.type/ref
                                         :db/cardinality :db.cardinality/one
                                         :db/doc "the test employer"}})]
    (d/transact! conn [{:db/id -1
                        :name "A"
                        :age 1
                        :item/size 2
                        :item/age 3
                        :item/thingy 4}
                       {:db/id -2
                        :name "B"
                        :parent -1}
                       {:db/id -2
                        :name/parent "C"
                        :parent -1
                        :item/age "20"
                        :item/thingy 1}])
    conn))

;; Utilities

(defn schema-vals [schema]
  (reduce-kv (fn [m k v]
               (assoc m (keyword (name k)) v))
             {}
             schema))


(defn value-type [v]
  (cond
    (string? v) "String"
    (number? v) "Number"
    :else "Unknown"))
  
  (defn agg-attrs-by-ns [entity]
    (reduce
     (fn [acc [a v]]
       (assoc-in acc [(namespace a) (name a)] v))
     {} entity))


(defn generate-schema-datastructure [on-write? schema]
  (let [convert-vals #(if on-write? (schema-vals %) (value-type %))]
    (->> (agg-attrs-by-ns schema)
         (map (fn [[k v]]
                (let [extract-vals #(reduce-kv (fn [m k v]
                                                 (assoc m k (convert-vals v))) {} %)]
                  (if k
                    {(str  k "/") (extract-vals v)}
                    (extract-vals v))))))))

  

;; Views

(defn schema-tree-av [[a v]]
  [:div
   [:div {:class "flex justify w-full hover:bg-gray-200"}
    [:span {:class "w-1/2 pl-2"} 
     (str (name a))]
    [:span {:class "w-1/2 pl-4"} 
     (str v )]]])


(defn schema-tree-ns [[a-ns av-pairs-in-ns] & nested?]
  (if (clojure.string/ends-with? a-ns "/")
    [:div
     [:p {:class "w-1/2 pl-2 w-full hover:bg-gray-200 mt-2 "} 
      (str ":" a-ns)]
     [:div {:class "pl-2"}
      (for [[a-ns :as x] av-pairs-in-ns]
        ^{:key (pr-str a-ns)} [schema-tree-ns x true])]]
    (if (map? av-pairs-in-ns)
      [:div
       (let [doc (:doc av-pairs-in-ns)
             av-pairs-in-ns (if doc (dissoc av-pairs-in-ns :doc) av-pairs-in-ns)]
         [:div {:class (str "flex justify w-full hover:bg-gray-200" (when (not nested?) " mt-2"))}
          [:span {:class "w-1/2 pl-2"}
           (if nested? (str a-ns) (str ":" a-ns))]
          [:span {:class "w-1/2 pl-4"}
           (str doc)]])
       [:div {:class "pl-3"}
        (for [[a :as av] (seq av-pairs-in-ns)]
          ^{:key (pr-str a)} [schema-tree-av av])]]
      [:div 
       [:div {:class "pl-3"}
        (for [[a :as av] (seq {a-ns av-pairs-in-ns})]
          ^{:key (pr-str a)} [schema-tree-av av])]])))


(defn schema-tree [schema]
  [:div
   (for [[a-ns :as x] schema]
     ^{:key (pr-str a-ns)} [schema-tree-ns x])])



(defn schema []
  [:div {:class "w-full h-full"}
   [:div {:class "flex justify w-full font-bold"}
    [:span {:class "w-1/2 pl-2"} "Attribute"]
    [:span {:class "w-1/2 pl-4"} "Value"]]
   [schema-tree (reduce merge (generate-schema-datastructure true (:schema @conn)))]

   [:div {:class "font-bold pt-4"} "On read schema"]
   [schema-tree (reduce merge (generate-schema-datastructure
                               false (->> (d/q '[:find ?attr ?v
                                                 :where [_ ?attr ?v]]
                                               @conn))))]])
