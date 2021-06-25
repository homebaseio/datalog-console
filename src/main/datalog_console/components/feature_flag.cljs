(ns datalog-console.components.feature-flag
  {:no-doc true}
  (:require [clojure.string :as str])
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

(defn version-check [{:keys [title required-version current-version]} comp]
  (if (<= 0 (compare current-version required-version))
    comp
    [:div {:class "relative h-full"}
     [:div {:class "opacity-50"} comp]
     [:div {:class "absolute top-0 w-full h-full bg-gray-500 bg-opacity-80 flex justify-center items-center"}
      [:div {:class "mx-4 p-4 bg-gray-100 rounded"}
       [:span {:class "block"} 
        title 
        " requires " 
        [:strong required-version]
        " or higher of the "
        [:a {:href "https://clojars.org/io.homebase/datalog-console" 
             :target "_blank"
             :class "text-blue-500"} 
         "datalog-console"]
        " integration. Please upgrade the version in the dependencies of the connected application, or ask the maintainer to upgrade."]
       [:div {:class "flex flex-row mt-4"}
        [:span {:class "block p-2 border"} "Current Integration Version: " (if current-version current-version "Unknown Version")]]]]]))