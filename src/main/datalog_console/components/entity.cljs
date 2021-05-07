(ns datalog-console.components.entity
  (:require [datascript.core :as d]
            [reagent.core :as r]
            [cljs.reader]
            [goog.object]
            [clojure.string :as str]
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



(defn entity-lookup-form-validation [conn lookup]
  ;; We probably want to do a lot more here
  ;; Such as adding more helpful hints for user input mistakes
  (when-not (str/blank? lookup) ;skip if empty string
    (let [lookup (str/trim lookup)
          seq-str (seq lookup)]
      (if (and (= "[" (first seq-str))
               (= "]" (last seq-str)))
        (let [read-lookup-result (try
                                   (cljs.reader/read-string lookup)
                                   (catch js/Error _e {:error "Invalid form input"}))]
          (if-not (:error read-lookup-result)
            (let [kw (first read-lookup-result)]
              (if (keyword? kw)
                (when-not (contains? (set (keys (filter (fn [[_k v]] (contains? v :db/unique))
                                                        (:schema @conn))))
                                     kw)
                  {:error (str "No unique attribute found for " kw " in schema")})
                {:error "No keyword provided for unique attribute"}))
            read-lookup-result))
        (if (integer? (js/parseInt (first seq-str)))
          (when-not (every? true? (map integer? (map #(js/parseInt %) lookup))) ;skip if all input is numbers
            {:error "Input starts as number but there seems to be a mistake with the rest of input"})
          {:error "No vector provided for unique attribute lookup"})))))



(defn lookup-form []
  (let [lookup (r/atom "")
        entity-lookup-ratom-cache (r/atom "")
        input-error (r/atom nil)]
    (fn [conn entity-lookup-ratom]
      (when-not (= @entity-lookup-ratom @entity-lookup-ratom-cache)
        (reset! entity-lookup-ratom-cache @entity-lookup-ratom)
        (reset! input-error nil)
        (reset! lookup @entity-lookup-ratom))
      [:div
       [:form {:class "flex items-end"
               :on-submit
               (fn [e]
                 (.preventDefault e)
                 (if-let [error (:error (entity-lookup-form-validation conn @lookup))]
                   (do 
                     (reset! input-error error))
                   (reset! entity-lookup-ratom @lookup)))}
        [:label {:class "block pl-1"}
         [:p {:class "font-bold"} "Entity lookup"]
         [:input {:type "text"
                  :name "lookup"
                  :value @lookup
                  :on-change (fn [e] (reset! lookup (goog.object/getValueByKeys e #js ["target" "value"])))
                  :placeholder "id or [:uniq-attr1 \"v1\" ...]"
                  :class "border py-1 px-2 rounded w-56"}]]
        [:button {:type "submit"
                  :class "ml-1 py-1 px-2 rounded bg-gray-200 border shadow-hard btn-border"}
         "Get entity"]]
       (when @input-error
         [:div {:class "bg-red-300"}
          [:p @input-error]])])))

(defn entity []
  (fn [conn entity-lookup-ratom]
    (let [entity (d/entity @conn (cljs.reader/read-string @entity-lookup-ratom))]
      [:div {:class "w-full h-full overflow-auto pb-5"}
       [lookup-form conn entity-lookup-ratom]
       (when entity
         [c.tree-table/tree-table
          {:caption (str "entity " (select-keys entity [:db/id]))
           :conn conn
           :head-row ["Attribute", "Value"]
           :rows (entity->rows entity)
           :expandable-row? expandable-row?
           :expand-row expand-row
           :render-col render-col}])])))


