(ns datalog-console.components.entity
  (:require [datascript.core :as d]
            [reagent.core :as r]))

(def conn
  (let [conn (d/create-conn
              {:name {:db/unique :db.unique/identity}
               :person/parents {:db/valueType :db.type/ref
                                :db/cardinality :db.cardinality/many}
               :person/friends {:db/valueType :db.type/ref
                                :db/cardinality :db.cardinality/many}
               :employer/person {:db/valueType :db.type/ref
                                 :db/cardinality :db.cardinality/one}})]
    (d/transact! conn [{:db/id -1
                        :name "A"
                        :description "I'm baby truffaut craft beer cold-pressed sartorial listicle aesthetic normcore edison bulb XOXO seitan celiac. Blog irony succulents synth shabby chic. Health goth hexagon semiotics wolf jean shorts narwhal, intelligentsia hell of hot chicken. La croix pour-over coloring book wayfarers austin, asymmetrical whatever messenger bag four dollar toast activated charcoal vice banjo vegan portland. Twee selfies chillwave normcore 3 wolf moon af coloring book flexitarian plaid poke mlkshk pitchfork bicycle rights. Franzen gluten-free lyft snackwave meditation man braid, tousled shabby chic banjo. Selfies tote bag chicharrones, hammock fam umami woke typewriter poutine fanny pack artisan copper mug."
                        :employer/person -2
                        :item/size 2
                        :item/age 3
                        :item/with-a-very-very-very-long-name 3
                        :item/thingy 4}
                       {:db/id -2
                        :name "B"
                        :person/parents [-1 -3]}
                       {:db/id -3
                        :name "C"}
                       {:db/id -4
                        :name "D"
                        :person/parents [-1 -3]}
                       {:db/id -5
                        :name "E"
                        :person/friends [-1]}
                       {:db/id -6
                        :name "F"
                        :employer/person -1}])
    conn))

(defn entity? [v]
  (try
    (not (nil? (:db/id v)))
    (catch js/Error e false)))

(declare 
 entity-tree-avs
 entity-tree-table)

(defn entity-tree-av [[a v] indent]
  (let [entity? (entity? (if (set? v) (first v) v))
        open? (r/atom false)]
    (fn []
      [:<>
       [:tr {:class "odd:bg-gray-100"}
        [:td {:title (str a)
              :style {:padding-left (str (+ 0.25 indent) "rem")
                      :max-width "16rem"}
              :class "pr-1 box-content truncate"}
         (if entity?
           [:button {:class "pr-1 focus:outline-none"
                     :on-click #(reset! open? (not @open?))}
            (if @open? "▼" "▶")]
           [:span {:class "pr-1 invisible"} "▶"])
         (str a)]
        [:td {:title (str v)
              :style {:max-width 0
                      :min-width 100}
              :class "pl-1 truncate w-full"}
         (if (and entity? (set? v))
           (str "[" (count v) " item" (when (< 1 (count v)) "s") "]")
           (str (if entity?
                  (select-keys v [:db/id])
                  v)))]]
       (when @open?
         (if (set? v)
           (for [[i e] (map-indexed vector v)]
             ^{:key (str "refs" a i "-" (:db/id e) "-" indent)}
             [entity-tree-av [(str a " " i) e] (inc indent)])
           [:tr {:class "border-t border-gray-300"}
            [:td {:col-span 2 :class "p-0 relative"}
             [:button {:title (str "Collapse  " a "  " (select-keys v [:db/id]))
                       :on-click #(reset! open? false)
                       :class "absolute w-full bottom-0 left-0 border-gray-500 border-b hover:border-b-6 focus:outline-none"}]
             [entity-tree-table v false (inc indent)]]]))])))

(defn entity-tree-avs [eid avs indent]
  [:<>
   (for [[a :as av] (sort avs)]
     ^{:key (str eid "-" a)} [entity-tree-av av indent])])

(defn make-keyword-reverse-ref [kw]
  (keyword (str (namespace kw) "/_" (name kw))))

(defn reverse-refs [entity]
  (let [rev-ref-attrs-and-eids (d/q '[:find ?ref-attr ?e
                                          :in $ [?ref-attr ...] ?ref-id
                                          :where [?e ?ref-attr ?ref-id]]
                                        @conn
                                        (for [[attr props] (:schema @conn)
                                              :when (= :db.type/ref (:db/valueType props))]
                                          attr)
                                        (:db/id entity))
        rev-ref-eids-grouped-by-attr (group-by first rev-ref-attrs-and-eids)
        rev-ref-entities-grouped-by-attr (reduce-kv (fn [acc k v]
                                                          (conj acc [(make-keyword-reverse-ref  k)
                                                                     (set (map (fn [[_ eid]] (d/entity @conn eid)) v))]))
                                                        [] rev-ref-eids-grouped-by-attr)]
    rev-ref-entities-grouped-by-attr))

(defn entity-tree [entity indent]
  [:<>
   ^{:key (str (:db/id entity) "-" :db/id)} [entity-tree-av [:db/id (:db/id entity)] indent]
   [entity-tree-avs (:db/id entity) (seq entity) indent]
   [entity-tree-avs (:db/id entity) (reverse-refs entity) indent]])

(defn entity-tree-table [entity head? indent]
  [:table {:class "table-auto w-full"}
   (when head?
     [:thead
      [:tr {:class "font-bold text-left"}
       [:th {:class "px-1"} "Attribute"]
       [:th {:class "pl-1"} "Value"]]])
   [:tbody
    [entity-tree entity indent]]])

(defn entity []
  (let [lookup (r/atom "")
        entity (r/atom 
                (d/entity @conn 1) ; TODO: replace with nil
                )]
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
       [entity-tree-table @entity true 0]])))
