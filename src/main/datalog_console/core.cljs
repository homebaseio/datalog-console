(ns datalog-console.core
  (:require [reagent.dom :as rdom]))

(defn root []
  [:div "YOLO"])

(defn mount! []
  (rdom/render [root] (js/document.getElementById "root")))

(defn init! []
  (mount!))

(defn ^:dev/after-load remount!
  "Remounts the whole UI on every save. Def state you want to persist between remounts with defonce."
  []
  (mount!))