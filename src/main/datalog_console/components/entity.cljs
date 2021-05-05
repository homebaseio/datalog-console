(ns datalog-console.components.entity
  (:require [datascript.core :as d]
            [reagent.core :as r]
            [cljs.reader]
            [goog.object]
            [datalog-console.components.tree-table :as c.tree-table]))

(defn entity? [v]
  (try
    (not (nil? (:db/id v)))
    (catch js/Error _e false)))

(defn expandable-row? [[_a v]]
  (if (set? v)
    (entity? (first v))
    (entity? v)))

(defn keyword->reverse-ref [kw]
  (keyword (str (namespace kw) "/_" (name kw))))

(defn ^:export reverse-refs [entity]
  (->> (d/q '[:find ?ref-attr ?e
              :in $ ?ref-id [?ref-attr ...]
              :where [?e ?ref-attr ?ref-id]]
            (d/entity-db entity)
            (:db/id entity)
            (for [[attr props] (:schema (d/entity-db entity))
                  :when (= :db.type/ref (:db/valueType props))]
              attr))
       (group-by first)
       (reduce-kv (fn [acc k v]
                    (conj acc [(keyword->reverse-ref  k)
                               (set (for [[_ eid] v] (d/entity (d/entity-db entity) eid)))]))
                  [])))

(defn entity->rows [entity]
  (concat
   [[:db/id (:db/id entity)]]
   (sort (seq entity))
   (sort (reverse-refs entity))))

(defn expand-row [[a v]]
  (cond
    (set? v) (map-indexed (fn [i vv] [(str a " " i) vv]) v)
    (entity? v) (entity->rows v)))

(defn render-col [col]
  (cond
    (set? col) (str "#[" (count col) " item" (when (< 1 (count col)) "s") "]")
    (entity? col) (str (select-keys col [:db/id]))
    :else (str col)))



(defn entity []
  (let [lookup (r/atom "")]
    (fn [conn]
      (let [entity (d/entity @conn (cljs.reader/read-string @lookup))]
        [:div {:class "w-full h-full overflow-auto pb-5"}
         [:form {:class "flex items-end"
                 :on-submit
                 (fn [e]
                   (.preventDefault e)
                   (reset! lookup (goog.object/getValueByKeys e #js ["target" "elements" "lookup" "value"])))}
          [:label {:class "block pt-1 pl-1"}
           [:p {:class "font-bold"} "Entity lookup"]
           [:input {:type "text"
                    :name "lookup"
                    :placeholder "id or [:uniq-attr1 \"v1\" ...]"
                    :class "border py-1 px-2 rounded w-56"}]]
          [:button {:type "submit"
                    :class "ml-1 py-1 px-2 rounded bg-gray-200 border"}
           "Get entity"]]
         (when entity
           [c.tree-table/tree-table
            {:caption (str "entity " (select-keys entity [:db/id]))
             :conn conn
             :head-row ["Attribute", "Value"]
             :rows (entity->rows entity)
             :expandable-row? expandable-row?
             :expand-row expand-row
             :render-col render-col}])]))))
