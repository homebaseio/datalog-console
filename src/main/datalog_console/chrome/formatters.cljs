(ns datalog-console.chrome.formatters
  (:require [datascript.impl.entity :as de]
            [datalog-console.components.entity :as c.entity]
            [devtools.core :as devtools]
            [devtools.formatters.templating :refer [render-markup]]
            [devtools.protocols :refer [IFormat]]))

(extend-type de/Entity
  IFormat
  (-header [entity]
           (render-markup
            [[:span "color:white;background-color:#999;padding:0px 4px;"]
             (str "Entity: " (select-keys entity [:db/id]))]))
  (-has-body [_] true)
  (-body [entity]
         (render-markup
          (into [[:table "border:1px solid black;"]]
                (for [[k v] (c.entity/entity->rows entity)]
                  [[:tr]
                   [[:td] (pr-str k)]
                   [[:td] (cond
                            (c.entity/entity? v) #js ["object" #js {"object" v}]
                            (and (set? v) (c.entity/entity? (first v))) #js ["object" #js {"object" v}]
                            :else (pr-str v))]])))))

(defn install! []
  (devtools/set-pref! :disable-advanced-mode-check true)
  (devtools/install! [:formatters :hints]))