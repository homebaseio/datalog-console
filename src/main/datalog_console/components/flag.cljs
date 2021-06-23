(ns datalog-console.components.flag
  (:require [clojure.string :as str]
            [datalog-console.lib.version :as dc])
  (:refer-clojure :exclude [compare]))

(defn parse-int [s]
  (js/parseInt s))

(defn parse-version [s]
  (mapv parse-int (str/split s #"\.")))

(defn vector-compare [[value1 & rest1] [value2 & rest2]]
  (let [result (cljs.core/compare value1 value2)]
    (cond
      (not (zero? result)) result
      (nil? value1) 0
      :else (recur rest1 rest2))))

(defn compare [a b]
  (vector-compare
   (parse-version a)
   (parse-version b)))

(defn overlay [feature-v integration-v comp]
  (if (<= 0 (compare integration-v feature-v))
    comp
    [:div {:class "relative h-full"}
     [:div {:class "opacity-50"} comp]
     [:div {:class "absolute top-0 w-full h-full bg-gray-500 bg-opacity-80 flex justify-center items-center"}
      [:div {:class "mx-4 p-4 bg-gray-100 rounded"}
       [:span {:class "block"} "Feature is not supported with this console integration version"]
       [:div {:class "flex flex-row mt-4"}
        [:span {:class "block p-2 border"} "Console " dc/version]
        [:span {:class "block p-2 border ml-4"} "Integration " integration-v]]]]]))

