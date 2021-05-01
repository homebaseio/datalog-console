(ns datalog-console.components.entity-scanner
  (:require [datascript.core :as d]
            [reagent.core :as r]
            [cljs.reader]
            [goog.object]
            [datalog-console.components.tree-table :as c.tree-table]))





(defn ^:export entity-agg [db]
  (->> (d/q '[:find (pull ?e [*])
             :where [?e _ _]]
           @db)
       flatten
       (group-by :db/id)))







(defn render-col [col]
  (js/console.log "render-col-scan called with: " col)
  (cond
    (vector? col)
    (str "#[" (- (count (first col)) 1) " item" (when (< 1 (- (count (first col)) 1)) "s") "]")

    (map? col)
    col

    :else (str col) ))

(defn expandable-row? [[a v]]
  (js/console.log "-------")
  (js/console.log "the v in expandable row: " v)
  (js/console.log "the number?: " a)
  (js/console.log "the v in expandable row: " )
  (if (or (number? a) (boolean (when (vector? v) (:db/id (first v)))))
    true
    false))




(defn expand-row [[_a v]]
  (when (vector? v) (first v)))




(defn entity-scan []
  (fn [conn]
    (c.tree-table/tree-table
     {:caption "Table of entities in the database" 
      :head-row ["EID", "Entity"]
      :rows (entity-agg conn)
      :expandable-row? expandable-row?
      :expand-row expand-row
      :render-col render-col})))
