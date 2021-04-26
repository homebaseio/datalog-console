(ns datalog-console.components.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [datalog-console.components.schema :as c.schema]
            [datalog-console.components.entity :as c.entity]
            [datascript.core :as d]
            [goog.object :as gobj]
            [cljs.reader]))

; get the serialized DB off of a postMessage
; created a new db from that string
; re-render

(def rconn (r/atom (d/create-conn {})))

(def current-tab-id js/chrome.devtools.inspectedWindow.tabId)

(def create-port #(js/chrome.runtime.connect #js {:name %}))
(def devtool-port (create-port "devtool"))

(defn post-message [port type data]
  (.postMessage port #js {:devtool-message (pr-str {:type type :data data :timestamp (js/Date.)})
                          :tab-id        current-tab-id}))



(let [port devtool-port]
  (.addListener (gobj/get port "onMessage")
                (fn [msg]
                  (js/console.log "message" msg)
                  (when-let [db-str (gobj/getValueByKeys msg "datalog-remote-message")]
                    (js/console.log #js {:name "datalog-remote-message" :foo :bar :msg msg})
                    (let [_ (js/console.log "before conn-from-db")]
                      (reset! rconn (d/conn-from-db (cljs.reader/read-string db-str)))))))

  (.postMessage port #js {:name "init" :tab-id current-tab-id}))

  
(defn root []
  [:div {:class "font-sans text-xs h-screen w-full flex flex-row"}
                    [:button
                     {:class "p-2 bg-green-700 rounded border solid font-bold text-white"
                      :on-click #(do
                                   (println "*panel* making a *db-request*")
                                   (post-message devtool-port :db-request {}))}  ;; TODO: rename to request whole database as string
                     "Refresh database"]
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