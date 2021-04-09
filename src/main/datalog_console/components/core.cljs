(ns datalog-console.components.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [datalog-console.components.schema :as c.schema]
            [datalog-console.components.entity :as c.entity]
            [datascript.core :as d]))

; get the serialized DB off of a postMessage
; created a new db from that string
; re-render

(def rconn (r/atom (d/create-conn {})))

(defn root []
  [:div {:class "font-sans text-xs h-screen w-full flex flex-row"}
   [:div {:class "w-80 border-r"}
    [c.schema/schema @rconn]]
   [:div {:class "flex-auto"}
    [c.entity/entity @rconn]]])

(defn mount! []
  (rdom/render [root] (js/document.getElementById "root")))

(defn init! []
  (mount!))

(defn ^:dev/after-load remount!
  "Remounts the whole UI on every save. Def state you want to persist between remounts with defonce."
  []
  (mount!))