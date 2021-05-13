(ns datalog-console.workspaces.demo-cards
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]
            [clojure.test :refer [is]]))

; simple function to create react elemnents
(defn element [name props & children]
  (apply js/React.createElement name (clj->js props) children))

(ws/defcard hello-card
  (ct.react/react-card
   (element "div" {:className "font-black"} "Hello World")))

(ws/deftest sample-test
  (is (= 1 1)))

(ws/defcard counter-example-card
  (let [counter (atom 0)]
    (ct.react/react-card
     counter
     (element "div" {}
              (str "Count: " @counter)
              (element "button" {:onClick #(swap! counter inc)} "+")))))