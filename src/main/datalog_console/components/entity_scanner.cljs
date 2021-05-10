(ns datalog-console.components.entity-scanner
  (:require [datascript.core :as d]
            [cljs.reader]
            [goog.object]))


(defn ^:export entity-agg [db]
  (->> (d/q '[:find (pull ?e [*])
             :where [?e _ _]]
           @db)
       flatten
       (group-by :db/id)))


(defn entity-scan []
  (fn [conn entity-lookup-ratom]
    [:ul {:class "w-full h-full overflow-auto pb-5"}
     (for [[id] (entity-agg conn)]
       ^{:key id}
       [:li
        {:class "truncate odd:bg-gray-100 cursor-pointer"
         :on-click #(reset! entity-lookup-ratom (str id))}
        (str (into {:db/id id} (d/entity @conn id)))])]))
