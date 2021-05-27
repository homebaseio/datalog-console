(ns datalog-console.components.query
  (:require [datascript.core :as d]
            [reagent.core :as r]
            [cljs.reader]
            [cljs.pprint]
            [datalog-console.lib.localstorage :as localstorage]))

(def example-queries
  {"All attributes" "[:find [?attr ...] \n :where [_ ?attr]]"
   "All entities" "[:find ?e ?a ?v \n :where \n [?e ?a ?v]]"
   "Example and query" "[:find ?e \n :where \n [?e :attr1 \"value 1\"] \n [?e :attr2 \"value 2\"]]"})

(defn result []
  (let [sort-direction (r/atom 0)]
    (fn [result]
      [:<>
       [:div {:class "flex flex-row justify-between items-baseline mt-4 mb-2 "}
        [:span (str "Query results: " (count result))]
        [:button {:class "ml-1 py-1 px-2 rounded bg-gray-200 border w-24"
                  :on-click #(swap! sort-direction (fn [x] (mod (inc x) 3)))}
         (case @sort-direction
           0 "Sort"
           1 "↓"
           2 "↑")]]
       [:div {:class "border p-4 rounded overflow-auto"}
        [:pre  (with-out-str (cljs.pprint/pprint (case @sort-direction
                                                   0 result
                                                   1 (sort result)
                                                   2 (reverse (sort result)))))]]])))

(defn query []
  (let [saved-query (localstorage/get-item (str ::query-text))
        query-text (r/atom (or saved-query (get example-queries "All attributes")))
        query-result (r/atom nil)
        query-error (r/atom nil)]
    (fn [conn]
      [:div {:class "px-1"}
       [:p {:class "font-bold"} "Query Editor"]
       [:div {:class "flex justify-between mb-2 items-baseline"
              :style {:min-width "20rem"}}
        [:div {:class "-ml-1"}
         (for [[k v] example-queries]
           ^{:key (str k)}
           [:button {:class "ml-1 mt-1 py-1 px-2 rounded bg-gray-200 border"
                     :on-click #(reset! query v)} k])]]
       [:form {:on-submit (fn [e]
                            (.preventDefault e)
                            (try
                              (reset! query-result (d/q (cljs.reader/read-string @query-text) @conn))
                              (reset! query-error nil)
                              (catch js/Error e
                                (reset! query-result nil)
                                (reset! query-error (goog.object/get e "message")))))}
        [:div {:class "flex flex-col"}
         [:textarea
          {:style {:min-width "20rem"}
           :class        "border p-2"
           :rows          5
           :value        @query-text
           :on-change    (fn [e]
                           (reset! query-text (goog.object/getValueByKeys e #js ["target" "value"]))
                           (localstorage/set-item! (str ::query-text) @query-text))}]
         [:button {:type "submit"
                   :class "py-1 px-2 rounded-b bg-gray-200 border"}
          "Run query"]]]
       [:div {:style {:min-width "20rem"}}
        (when @query-error
          [:div {:class "bg-red-200 p-4 rounded"}
           [:p @query-error]])
        (when @query-result 
          [result @query-result])]])))
