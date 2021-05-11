(ns datalog-console.components.tree-table
  (:require [reagent.core :as r]
            [clojure.set]))

(declare tree-table)

(defn table-row []
  (let [open? (r/atom false)]
    (fn [{:keys [level row expandable-row? expand-row render-col full-width? _conn] :as props}]
      [:<>
       [:tr {:class "odd:bg-gray-100"}
        (doall (for [[i col] (map-indexed vector row)]
                 ^{:key (str {:level level :col col})}
                 [:<>
                  (if (= 0 i)
                    [:td {:title (str col)
                          :style {:padding-left (str (+ 0.25 level) "rem")
                                  :max-width "16rem"}
                          :class "pr-1 box-content truncate align-top"}
                     (if (expandable-row? row)
                       [:button {:class "pr-1 focus:outline-none"
                                 :on-click #(reset! open? (not @open?))}
                        (if @open? "▼" "▶")]
                       [:span {:class "pr-1 invisible"} "▶"])
                     (render-col col)]
                    [:td {:title (str col)
                          :class (if full-width? "pl-3 w-full align-top" "pl-3 w-full align-top")} ;truncate
                     (render-col col)])]))]
       (when @open?
         [:tr
          [:td {:col-span (count row) :class "p-0 relative align-top"
                :style {:min-width :max-content}}
           [tree-table
            (merge props {:level (inc level)
                          :caption nil
                          :rows (expand-row row)})]
           [:button {:title (str "Collapse  " (pr-str row))
                     :on-click #(reset! open? false)
                     :style {:margin-left (str (+ 0.4 level) "rem")}
                     :class "top-0 bottom-0 absolute transition-all duration-100 ease-in border-r border-gray-300 hover:border-gray-400 w-1 hover:bg-gray-400"}]]])]))) ; border-gray-300 border-l hover:translate-x-1 hover:border-l-6 ;top-0 left-2.5 absolute h-full border-gray-300 border-l transform hover:border-l-6 hover:-translate-x-1 focus:outline-none

(defn tree-table
  "Renders `rows` of data in a table `[[col1 col2] [col1 col2]]`. 
   If the row is `(expandable-row? row)` then it will render a caret
   to toggle the `(expand-row row)` function and step down a level in the
   tree. `expand-row` should return a new sequence of rows."
  [{:keys [level caption head-row rows render-col _expandable-row? _expand-row _full-width? _conn]
    :as props}]
  (let [level (or level 0)
        render-col (or render-col str)
        props (merge props {:level level :render-col render-col})]
    [:table {:class "table-auto w-full relative"}
     (when caption
       [:caption {:class "sr-only"}
        caption])
     [:thead {:class (if (= 0 level) "" "sr-only")}
      [:tr {:class "pl-1 font-bold text-left"}
       ^{:key (str "first th " (first head-row))}
       [:th {:class "pl-1"} (first head-row)]
       (for [col (rest head-row)]
         ^{:key (str "th-" level "-" (pr-str col))}
         [:th {:class "pl-3"} col])]]
     [:tbody
      (for [[i row] (map-indexed vector rows)]
        ^{:key (str "tr-" level "-" i)}
        [table-row (merge props {:row row})])]]))