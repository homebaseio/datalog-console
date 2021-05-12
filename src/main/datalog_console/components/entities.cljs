(ns datalog-console.components.entities
  (:require [datascript.core :as d]
            [cljs.reader]
            [goog.object]))


(defn ^:export entity-agg [db]
  (->> (d/q '[:find (pull ?e [*])
             :where [?e _ _]]
           @db)
       flatten
       (group-by :db/id)))


(defn entities []
  (fn [conn entity-lookup-ratom]
    (let [truncate-long-strings #(map (fn [[k v]]
                                        {k (if (and (string? v) (< 100 (count v)))
                                             (str (subs v 0 100) "...")
                                             v)}) %)]
      [:ul {:class "w-full flex flex-col pb-5"}
       (for [[id] (entity-agg conn)]
         ^{:key id}
         [:li
          {:class "odd:bg-gray-100 cursor-pointer min-w-max"
           :title (str (into {:db/id id} (d/entity @conn id)))
           :on-click #(reset! entity-lookup-ratom (str id))}
          (str (into {:db/id id} (truncate-long-strings (d/entity @conn id))))])])))


