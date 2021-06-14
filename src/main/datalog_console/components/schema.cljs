(ns datalog-console.components.schema
  {:no-doc true}
  (:require [datascript.core :as d]
            [datalog-console.components.tree-table :as c.tree-table]))

(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
            (if (and (map? v1) (map? v2))
              (merge-with deep-merge v1 v2)
              v2))]
    (when (some identity vs)
      (reduce #(rec-merge %1 %2) v vs))))

(defn v->type [v]
  (cond
    (string? v) :db.type/string
    (number? v) :db.type/long
    :else (pr-str (type v))))

(defn infer-schema [v]
  {:db/valueType  {::inferred {(v->type v) (v->type v)}}})

(defn expandable-row? [[a v]]
  (and 
   (coll? v)
   (not (set? v)) 
   (not (::inferred v))))

(defn expand-row [[a v]]
  (sort v))

(defn render-col [col]
  (cond
    (expandable-row? [nil col]) nil
    (::inferred col) (let [a (str (set (keys (::inferred col))))]
                       [:div {:title a :class "w-full"}
                        [:span {:title "This database attribute is inferred based on data in the DB. It is not hardcoded or enforced in any way."}
                         "ℹ️ "]
                        a])
    :else (str col)))

(defn coll->schema [coll a-fn v-fn]
  (reduce
   (fn [acc x]
     (deep-merge
      acc
      {(str ":" (when-let [n (namespace (a-fn x))]
                  (str n "/")))
       {(name (a-fn x)) 
        (v-fn x)}}))
   {} coll))

(defn schema [conn]
  (when conn
    (let [db @conn
          real-schema (coll->schema (:schema db) first last)
          inferred-schema (coll->schema (d/datoms db :eavt) :a #(infer-schema (:v %)))
          merged-schemas (deep-merge inferred-schema real-schema)]
      [:div {:class "flex pb-5 w-full"}
       [c.tree-table/tree-table
        {:caption "schema"
         :head-row ["Attribute", "Value"]
         :full-width? true
         :rows (sort merged-schemas)
         :expandable-row? expandable-row?
         :expand-row expand-row
         :render-col render-col}]])))