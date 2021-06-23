(ns datalog-console.components.transact
  (:require [reagent.core :as r]
            [cljs.reader]))

(defn transact []
  (let [transact-text (r/atom nil)]
    (fn [on-tx-submit rerror]
      [:div {:class "px-1"}
       [:p {:class "font-bold"} "Transact Editor"]
       [:div {:class "flex justify-between mb-2 items-baseline"
              :style {:min-width "20rem"}}]
       [:form {:on-submit (fn [e]
                            (.preventDefault e)
                            (on-tx-submit @transact-text))}
        [:div {:class "flex flex-col"}
         [:textarea
          {:style {:min-width "20rem"}
           :class        "border p-2"
           :rows          5
           :value        @transact-text
           :on-change    (fn [e]
                           (reset! transact-text (goog.object/getValueByKeys e #js ["target" "value"])))}]
         [:button {:type "submit"
                   :class "py-1 px-2 rounded-b bg-gray-200 border"}
          "Transact"]]]
       [:div {:style {:min-width "20rem"}}
        (when rerror
         [:div {:class "bg-red-200 p-4 rounded"}
          [:p rerror]])]])))