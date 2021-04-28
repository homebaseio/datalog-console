(ns datalog-console.components.tree-table
  (:require [reagent.core :as r]
            [clojure.set]))

(declare tree-table)



;; TODO:
;; (1) When updating deeply nested values they do not trigger a refresh of the UI
;; (2) Refactor unique row id generation. Perhaps see if this can be done in the `entity` ns

(defn subnest? [current-nest other-nest]
    (let [[current-c other-c] (map count [current-nest other-nest])]
      (if (< other-c current-c)
        true
        (let [subvec? (= (subvec other-nest 0 current-c)
                         current-nest)]
          subvec?))))

(defn table-row [{:keys [level row expandable-row? expand-row render-col full-width? view-state table-id] :as props}]
  (let [open? (r/atom false)]
    (fn []
      [:<>
       [:tr {:class "odd:bg-gray-100"}
        (doall
         (for [[i col] (map-indexed vector row)]
           (let [row-id {:level level :col col :table-id table-id}]
             ^{:key (str row-id)}
             [:<>
              (if (= 0 i)
                [:td {:title (str col)
                      :style {:padding-left (str (+ 0.25 level) "rem")
                              :max-width "16rem"}
                      :class "pr-1 box-content truncate"}
                 (if (expandable-row? row)
                   (do (when view-state (reset! open? (contains? @view-state row-id)))
                       [:button {:class "pr-1 focus:outline-none"
                                 :on-click (fn []
                                             (when view-state
                                               (if (contains? @view-state row-id)
                                                 (let [orig-view-state @view-state
                                                       xform (comp
                                                              (filter #(< level (:level %)))
                                                              (filter #(subnest? table-id (:table-id %))))
                                                       new-view-state (clojure.set/difference orig-view-state (into #{} xform orig-view-state))]
                                                   (reset! view-state new-view-state)
                                                   (swap! view-state disj row-id))
                                                 (swap! view-state conj row-id)))
                                             (reset! open? (not @open?)))}
                        (if @open? "▼" "▶")])
                   [:span {:class "pr-1 invisible"} "▶"])
                 (render-col col)]
                [:td {:title (str col)
                      :style {:max-width 0
                              :min-width (when-not full-width? 100)}
                      :class (if full-width? "pl-3 w-full" "pl-3 truncate w-full")}
                 (render-col col)])])))]
       (when @open?
         [:tr
          [:td {:col-span (count row) :class "p-0 relative"}
           [tree-table
            (merge props {:level (inc level)
                          :caption nil
                          :rows (expand-row row)
                          :table-id-acc table-id})]
           [:button {:title (str "Collapse  " (pr-str row))
                     :on-click (fn []
                                 (let [row-id {:level level :col (first row)}]
                                   (swap! view-state disj row-id)
                                   (reset! view-state (set (filter #(and (<= (:level %) level) 
                                                                         (= (:col @view-state) (:col row-id))) 
                                                                   @view-state))))
                                 (reset! open? false))
                     :class "absolute h-full top-0 left-2.5 border-gray-300 border-l transform hover:border-l-6 hover:-translate-x-0.5 focus:outline-none"}]]])])))

(defn tree-table 
  "Renders `rows` of data in a table `[[col1 col2] [col1 col2]]`. 
   If the row is `(expandable-row? row)` then it will render a caret
   to toggle the `(expand-row row)` function and step down a level in the
   tree. `expand-row` should return a new sequence of rows."
  [{:keys [level caption head-row rows expandable-row? expand-row render-col full-width? table-id-acc] 
    :as props}]
  (let [level (or level 0)
        render-col (or render-col str)
        props (merge props {:level level :render-col render-col})]
    [:table {:class "table-auto w-full"}
     (when caption
       [:caption {:class (if (= 0 level) "px-1 text-left" "sr-only")}
        caption])
     [:thead {:class (if (= 0 level) "" "sr-only")}
      [:tr {:class "pl-1 font-bold text-left"}
       ^{:key (str "first th " (first head-row))}
       [:th {:class "pl-1"} (first head-row)]
       (for [col (rest head-row)]
         ^{:key (str "th-" level "-" (pr-str col))}
         [:th {:class "pl-3"} col])]]
     [:tbody
      (for [row rows]
        ^{:key (str "tr-" level "-" (pr-str row))}
        [table-row (merge props
                          {:row row
                           :table-id (vec (conj table-id-acc (or (-> rows first second :db/id) (-> rows first second))))})])]]))

