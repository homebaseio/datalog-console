(ns datalog-console.components.entity
  (:require [datascript.core :as d]
            [reagent.core :as r]))

(def conn
  (let [conn (d/create-conn
              {:person/parents {:db/valueType :db.type/ref
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
 entity-tree)

(defn entity-tree-av [[a v] indent]
  (let [entity? (entity? (if (set? v) (first v) v))
        open? (r/atom false)]
    (fn []
      [:<>
       [:tr {:class "odd:bg-gray-100"}
        [:td {:title (str a)
              :style {:padding-left (str (+ 0.25 (* 1.5 indent)) "rem")}
              :class "whitespace-nowrap"}
         (if entity?
           [:button {:class "pr-1 focus:outline-none"
                     :on-click #(reset! open? (not @open?))}
            (if @open? "▼" "▶")]
           [:span {:class "pr-1 invisible"} "▶"])
         (str a)]
        [:td {:title (str v)
              :class "pl-2 line-clamp-1"}
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
           [entity-tree v (inc indent)]))])))

(defn entity-tree-avs [avs indent]
  [:<>
   (for [[a :as av] (sort avs)]
     ^{:key (str a)} [entity-tree-av av indent])])

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

(defn entity-tree
  ([entity] (entity-tree entity 0))
  ([entity indent]
    [:<>
     [entity-tree-av [:db/id (:db/id entity)] indent]
     [entity-tree-avs (seq entity) indent]
     [entity-tree-avs (reverse-refs entity) indent]]))

(defn entity-tree-table [entity]
  [:table {:class "table-auto"}
   [:thead
    [:tr {:class "font-bold text-left"}
     [:th {:class "pl-1"} "Attribute"]
     [:th {:class "pl-2"} "Value"]]]
   [:tbody
    [entity-tree entity]]])

(defn entity []
  (let [lookup (r/atom "")
        entity (r/atom 
                (d/entity @conn 1) ; TODO: replace with nil
                )]
    (fn []
      [:div {:class "w-full h-full"}
       [:label [:p {:class "font-bold"} "Entity lookup"]
        [:input {:type "text"
                 :placholder "Entity id or lookup..."
                 :class "border py-1 px-2 rounded"
                 :value @lookup
                 :on-change #(reset! lookup (.-value (.-target %)))
                 :on-key-down #(when (= "Enter" (.-key %))
                                 (reset! entity (d/entity @conn (cljs.reader/read-string @lookup))))}]]
       [entity-tree-table @entity]])))
