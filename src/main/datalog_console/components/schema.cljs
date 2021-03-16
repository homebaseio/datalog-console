(ns datalog-console.components.schema
  (:require [datascript.core :as d]
            [reagent.core :as r]))

(defonce conn
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
                       {:db/id -3
                        :name 3
                        :parent -1}
                       {:db/id -4
                        :name "D"
                        :person/parents [-1 -3]}
                       {:db/id -5
                        :name/parent "C"
                        :parent -1
                        :item/age "20"
                        :item/thingy 1}])
    conn))

(def conn2
  (d/create-conn
   {:country {:db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "the parent"}
    :city {:db/valueType :db.type/ref
           :db/cardinality :db.cardinality/one
           :db/doc "the child"}}))


(def db-list {:idb conn2 :mem conn})


;; Utilities

(defn schema-vals [schema]
  (reduce-kv (fn [m k v]
               (assoc m (keyword (name k)) v))
             {}
             schema))


(defn inferred-schema-types [v]
  (let [many? (coll? v)
        value #(cond
                 (string? %) :db.type/string
                 (number? %) :db.type/long
                 :else "Unknown")
        valueType (set (map value v))]
    {:valueType (first valueType)
     :cardinality (if many? :db.cardinality/many :db.cardinality/one)}))


(defn infer-schema-type [v]
  (let [many? (coll? v)
        value #(cond
                 (string? %) :db.type/string
                 (number? %) :db.type/long
                 :else "Unknown")
        valueType (if many?
                    (set (map value v)) ; We could check here to see if there is more than one type
                    (value  v))]
    {:valueType  #{valueType :inferred}
     :cardinality #{(if many? :db.cardinality/many :db.cardinality/one) :inferred}}))

(defn merge-schemas [infered-schema real-schema]
  (reduce-kv (fn [acc k v] (update acc k merge v)) infered-schema real-schema))


(defn agg-attrs-by-ns [entity]
  (reduce
   (fn [acc [a v]]
     (assoc-in acc [(namespace a) (name a)] v))
   {} entity))


(defn generate-schema-datastructure [on-write? schema]
  (let [convert-vals #(if on-write? (schema-vals %) (infer-schema-type %) #_(inferred-schema-types %))]
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
     (if (contains? v :inferred)
       [:<> (str (first (disj v :inferred))) [:span {:class "pl-2 text-gray-400"} "ℹ️"]]
       (str v))]]])


(defn schema-tree-ns [[a-ns av-pairs-in-ns] & nested?]
  (let [open? (r/atom false)]
    (fn []
      (if (clojure.string/ends-with? a-ns "/")
        [:div
         [:div {:class "flex"}
          [:button {:class "pr-1 focus:outline-none"
                    :on-click #(reset! open? (not @open?))}
           (if @open? "▼" "▶")]
          [:p {:class "w-1/2 pl-2 w-full hover:bg-gray-200 mt-2 "}
           (str ":" a-ns)]]
         (when @open?
           [:div {:class "pl-2"}
            (for [[a-ns :as x] av-pairs-in-ns]
              ^{:key (pr-str a-ns)} [schema-tree-ns x true])])]
        [:div
         (let [doc (:doc av-pairs-in-ns)
               av-pairs-in-ns (if doc (dissoc av-pairs-in-ns :doc) av-pairs-in-ns)]
           [:div {:class "flex"}
            [:button {:class "pr-1 focus:outline-none"
                      :on-click #(reset! open? (not @open?))}
             (if @open? "▼" "▶")]
            [:div {:class (str "flex justify w-full hover:bg-gray-200" (when (not nested?) " mt-2"))}
             [:span {:class "w-1/2 pl-2"}
              (if nested? (str a-ns) (str ":" a-ns))]
             [:span {:class "w-1/2 pl-4"}
              (str doc)]]])
         (when @open?
           [:div {:class "pl-3"}
            (for [[a :as av] (seq av-pairs-in-ns)]
              ^{:key (pr-str a)} [schema-tree-av av])])]))))


(defn schema-tree [schema]
  [:div
   (for [[a-ns :as x] schema]
     ^{:key (pr-str a-ns)} [schema-tree-ns x])])


(defn schema []
  (let [db-name (r/atom :mem)]
    (fn []
      (let [real-schema (reduce merge (generate-schema-datastructure true (:schema @(@db-name db-list))))
            inferred-schema (reduce merge (map (fn [d]
                                                 {(name (:a d)) (infer-schema-type (:v d))})
                                               (d/datoms @(@db-name db-list) :eavt)))
            merged-schemas (merge-schemas inferred-schema real-schema)]
        [:div
         [:label {:for "database"} "DB"]
         [:select {:class "border rounded ml-2 px-2 py-1"
                   :name "database"
                   :on-change #(reset! db-name (cljs.reader/read-string (.-value (.-target %))))}
          (for [d (keys db-list)]
            ^{:key (str d)} [:option {:value (pr-str d)} (pr-str d)])]
         [:div {:class "w-full h-full mt-4"}
            [:div {:class "flex justify w-full font-bold"}
             [:span {:class "w-1/2 pl-2"} "Attribute"]
             [:span {:class "w-1/2 pl-4"} "Value"]]
            [schema-tree merged-schemas]]]))))

