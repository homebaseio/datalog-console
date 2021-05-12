(ns datalog-console.chrome.extension.devtool.main
  (:require [clojure.edn]
            [cljs.reader]
            [reagent.dom :as rdom]
            [datalog-console.client :as console]))


(println ::loaded)


(defn mount! []
  (rdom/render [console/root] (js/document.getElementById "root")))

(defn init! []
  (mount!))

(defn ^:dev/after-load remount!
  "Remounts the whole UI on every save. Def state you want to persist between remounts with defonce."
  []
  (mount!))

(mount!)