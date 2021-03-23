(ns datalog-console.workspaces.chrome.formatter-cards
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]
            [datascript.core :as d]
            [datascript.impl.entity :as de]
            [clojure.string :as string]
            [devtools.formatters.markup :as m]
            [devtools.formatters.templating :refer [render-markup]]
            [devtools.protocols :refer [IFormat]]
            [devtools.core :as devtools]
            ;; [datalog-console.chrome.formatters :as f]
            [datalog-console.components.entity :as c.entity]
            [datalog-console.workspaces.entity-cards :refer [conn]]))

(devtools/install!)

(defn element [name props & children]
  (apply js/React.createElement name (clj->js props) children))

;; (f/install)
(extend-type de/Entity
  IFormat
  (-header [entity] (render-markup [["span" "color:white;background-color:#999;padding:0px 4px;"] (str "Entity: " (select-keys entity [:db/id]))]))
  (-has-body [_] true)
  (-body [entity] (render-markup
                   (into [[:table "border:1px solid black;"]]
                         (for [[k v] (c.entity/entity->rows entity)]
                           [[:tr]
                            [[:td] (pr-str k)]
                            [[:td] (cond
                                     (c.entity/entity? v) #js ["object" #js {"object" v}]
                                     (and (set? v) (c.entity/entity? (first v))) #js ["object" #js {"object" v}]
                                     :else (pr-str v))]])))))

(ws/defcard chrome-formatters-card
  (do
    (js/console.log "The following entity should be formatted")
    (js/console.log (d/entity @conn 1))
    (js/console.log {:a "b" :1 [1 2 3] :laksdjflaskjdf "alskdjflaksdjflajdsflajsdflkjadlfkjasdlfkjalsdkjflaskdjf" 1 2 3 4 5 6 7 8 9 10})
    (ct.react/react-card
     (element "div" {:className "font-black"} "Open the chrome console"))))
