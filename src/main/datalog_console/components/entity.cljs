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

(defn entity [conn]
  (let [lookup (r/atom "")
        entity (r/atom nil)
        counter (r/atom 0) ; counter for experiment
        entity-view-count (r/atom 0) ; counter for experiment
        ]
    (fn []
      [:div {:class "w-full h-full overflow-auto pb-5"}
       ;; experiment for the database changes 
       [:p (str conn)]
       [:button {:on-click #(do
                              (swap! counter inc)
                              (let [counter @counter]
                                (d/transact! conn [{:db/id 1
                                                    :name "A"
                                                    :description (str "change counter " counter)}])))} "change db"]
       [:p (str "changed the db " @counter " times")]
       [:p (str "viewed entity " @entity-view-count " times")]
       ;; end experiment
       [:form {:class "flex items-end"
               :on-submit
               (fn [e]
                 (.preventDefault e)
                 (swap! entity-view-count inc) ; for experiment
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


