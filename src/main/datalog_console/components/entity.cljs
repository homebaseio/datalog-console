(ns datalog-console.components.entity
  (:require [datascript.core :as d]
            [reagent.core :as r]
            [datalog-console.components.tree-table :as c.tree-table]))

(defn entity? [v]
  (try
    (not (nil? (:db/id v)))
    (catch js/Error e false)))

(defn expandable-row? [[a v]]
  (if (set? v)
    (entity? (first v))
    (entity? v)))

(defn make-keyword-reverse-ref [kw]
  (keyword (str (namespace kw) "/_" (name kw))))

(defn reverse-refs [entity]
  (let [rev-ref-attrs-and-eids (d/q '[:find ?ref-attr ?e
                                      :in $ [?ref-attr ...] ?ref-id
                                      :where [?e ?ref-attr ?ref-id]]
                                    (d/entity-db entity)
                                    (for [[attr props] (:schema (d/entity-db entity))
                                          :when (= :db.type/ref (:db/valueType props))]
                                      attr)
                                    (:db/id entity))
        rev-ref-eids-grouped-by-attr (group-by first rev-ref-attrs-and-eids)
        rev-ref-entities-grouped-by-attr (reduce-kv (fn [acc k v]
                                                      (conj acc [(make-keyword-reverse-ref  k)
                                                                 (set (map (fn [[_ eid]] (d/entity (d/entity-db entity) eid)) v))]))
                                                    [] rev-ref-eids-grouped-by-attr)]
    rev-ref-entities-grouped-by-attr))

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

(defn entity [conn]
  (let [lookup (r/atom "")
        entity (r/atom nil)]
    (fn []
      [:div {:class "w-full h-full overflow-auto pb-5"}
       [:form {:class "flex items-end"
               :on-submit
               (fn [e]
                 (.preventDefault e)
                 (reset! entity (d/entity @conn (cljs.reader/read-string @lookup))))}
        [:label {:class "block pt-1 pl-1"}
         [:p {:class "font-bold"} "Entity lookup"]
         [:input {:type "text"
                  :placeholder "id or [:uniq-attr1 \"v1\" ...]"
                  :class "border py-1 px-2 rounded w-56"
                  :value @lookup
                  :on-change #(reset! lookup (.-value (.-target %)))
                  }]]
        [:button {:type "submit" 
                  :class "ml-1 py-1 px-2 rounded bg-gray-200 border"} 
         "Get entity"]]
       (when @entity
         [c.tree-table/tree-table
          {:caption (str "entity " (select-keys @entity [:db/id]))
           :head-row ["Attribute", "Value"]
           :rows (entity->rows @entity)
           :expandable-row? expandable-row?
           :expand-row expand-row
           :render-col render-col}])])))
