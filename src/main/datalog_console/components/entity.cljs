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

(defn handle-long-text-col []
  (let [expanded-text? (r/atom false)]
    (fn [col]
      [:span {:class (str "cursor-pointer "(when-not @expanded-text? "block"))
              :style {:min-width :max-content}
              :on-click #(reset! expanded-text? (not @expanded-text?))}
       (if expanded-text? col (str (subs col 0 45) "..."))])))


(defn render-col [col]
  (cond
    (set? col) (str "#[" (count col) " item" (when (< 1 (count col)) "s") "]")
    (entity? col) (str (select-keys col [:db/id]))
    (and (string? col) (< 45 (count col))) [handle-long-text-col col]
    :else (str col)))



(defn lookup-form []
  (let [lookup (r/atom "")
        input-error (r/atom nil)]
    (fn [conn on-submit]
      [:div
       [:form {:class "flex items-end"
               :on-submit
               (fn [e]
                 (.preventDefault e)
                 (try
                   (d/entity @conn (cljs.reader/read-string @lookup))
                   (on-submit @lookup)
                   (reset! lookup "")
                   (reset! input-error nil)
                   (catch js/Error e
                     (reset! input-error (goog.object/get e "message")))))}
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
         [:div {:class "bg-red-200 m-4 p-4 rounded"}
          [:p @input-error]])])))

(defn entity []
  (fn [conn entity-lookup-ratom]
    (let [entity (d/entity @conn (cljs.reader/read-string @entity-lookup-ratom))]
      [:div {:class "w-full h-full overflow-auto pb-5"}
       [lookup-form conn #(reset! entity-lookup-ratom %)]
       [:div {:class "pt-2"}
        (when entity
          [c.tree-table/tree-table
           {:caption (str "entity " (select-keys entity [:db/id]))
            :conn conn
            :head-row ["Attribute", "Value"]
            :rows (entity->rows entity)
            :expandable-row? expandable-row?
            :expand-row expand-row
            :render-col render-col}])]])))


