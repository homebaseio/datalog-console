(ns datalog-console.chrome.extension.devtool.main
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.edn]
   [cljs.reader]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [goog.object :as gobj]
   [taoensso.timbre :as log]
   [datalog-console.workspaces.entity-cards :refer [conn]]
   [datalog-console.components.schema :as c.schema]
   [datalog-console.components.entity :as c.entity]
   [datascript.core :as d]
   ))


(println ::loaded)

(def remote-conn (r/atom nil))


(def current-tab-id js/chrome.devtools.inspectedWindow.tabId)

(def create-port #(js/chrome.runtime.connect #js {:name %}))
(def devtool-port (create-port "devtool"))

(defn post-message [port type data]
  (.postMessage port #js {:devtool-message (pr-str {:type type :data data :timestamp (js/Date.)})
                          :tab-id        current-tab-id}))



(let [port devtool-port]
  (.addListener (gobj/get port "onMessage")
                (fn [msg]
                  (when-let [db-str (gobj/getValueByKeys msg "datalog-remote-message")]
                    (js/console.log #js {:name "datalog-remote-message" :msg msg})
                    (let [db-conn (d/conn-from-db (clojure.edn/read-string
                                                   {:readers d/data-readers} db-str))]

                      (reset! remote-conn {:db db-conn :time (js/Date.)})))))

  (.postMessage port #js {:name "init" :tab-id current-tab-id})
  #_(post-message port :hello-console/type {}))




(defn root []
  (let [view-state (r/atom nil)]
    (fn []
      [:div {:class "my-4 mx-6"}
       [:div {:class "flex flex-wrap mb-6 align-center"}
        [:h1 {:class "text-3xl mr-4"} "Datalog Console"]
        [:div {:class "flex flex-wrap justify-between items-center"}
         [:button
          {:class "p-2 bg-green-700 rounded border solid font-bold text-white" 
           :on-click #(do
                       (println "*panel* making a *db-request*")
                       (post-message devtool-port :db-request {}))}
          "Refresh database"]
         (when @remote-conn [:span {:class "ml-4"} "Last refresh: " (str (:time @remote-conn))])]]

       (if @remote-conn
         [:div {:class "flex flex-wrap"}
          [:div {:class "[ w-96 border rounded-md ] [ md:w-1/4 ]"}
           [c.schema/schema (:db @remote-conn)]]
          [:div {:class "[ w-96 border rounded-md mt-4 ] [ md:ml-4 md:flex-grow md:mt-0 ]"}

           [c.entity/entity (:db @remote-conn)]]]
         
         [:h2 "No database available"])])))

(defn mount! []
  (rdom/render [root] (js/document.getElementById "root")))

(defn init! []
  (mount!))

(defn ^:dev/after-load remount!
  "Remounts the whole UI on every save. Def state you want to persist between remounts with defonce."
  []
  (mount!))

(mount!)