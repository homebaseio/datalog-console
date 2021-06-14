(ns datalog-console.workspaces.chrome.formatter-cards
  {:no-doc true}
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]
            [datascript.core :as d]
            [clojure.test :refer [is]]
            [datalog-console.chrome.formatters :as formatters]
            [datalog-console.workspaces.entity-cards :refer [conn]]))

(formatters/install!)

(defn element [name props & children]
  (apply js/React.createElement name (clj->js props) children))

(ws/defcard chrome-formatters-card
  (do
    (js/console.log "The following entity should be formatted")
    (js/console.log (d/entity @conn 1))
    (js/console.log {:a "b" :1 [1 2 3] :laksdjflaskjdf "alskdjflaksdjflajdsflajsdflkjadlfkjasdlfkjalsdkjflaskdjf" 1 2 3 4 5 6 7 8 9 10})
    (ct.react/react-card
     (element "div" {:className "font-black"} "Open the chrome console"))))

(deftype SampleType [a b])
(defrecord SampleRecord [c d e])
(ws/deftest test-cljs$lang$type
  (is (= (.-cljs$lang$type SampleType) true))
  (is (= (pr-str SampleType) "datalog-console.workspaces.chrome.formatter-cards/SampleType"))
  (is (= (.-cljs$lang$type SampleRecord) true))
  (is (= (pr-str SampleRecord) "datalog-console.workspaces.chrome.formatter-cards/SampleRecord")))