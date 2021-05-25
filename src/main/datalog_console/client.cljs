(ns datalog-console.client
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [datalog-console.components.schema :as c.schema]
            [datalog-console.components.entity :as c.entity]
            [datalog-console.components.entities :as c.entities]
            [datalog-console.components.query :as c.query]
            [datascript.core :as d]
            [goog.object :as gobj]
            [clojure.string :as str]
            [cljs.reader]))



(def rconn (r/atom (d/create-conn {})))
(def entity-lookup-ratom (r/atom ""))

(try
  (def current-tab-id js/chrome.devtools.inspectedWindow.tabId)

  (def create-port #(js/chrome.runtime.connect #js {:name %}))
  (def devtool-port (create-port ":datalog-console.client/devtool-port"))

  (defn post-message [port type data]
    (.postMessage port #js {":datalog-console.client/devtool-message" (pr-str {:type type :data data :timestamp (js/Date.)})
                            :tab-id        current-tab-id}))


  (let [port devtool-port]
    (.addListener (gobj/get port "onMessage")
                  (fn [msg]
                    (when-let [db-str (gobj/getValueByKeys msg ":datalog-console.remote/remote-message")]
                      (reset! rconn (d/conn-from-db (cljs.reader/read-string db-str))))))

    (.postMessage port #js {:name ":datalog-console.client/init" :tab-id current-tab-id}))
  (catch js/Error _e nil))

(defn db-actions []
  (let [action (r/atom :entity)
        tabbed-views [:entity :query]]
    (fn [rconn entity-lookup-ratom]
      [:div {:class "flex flex-col overflow-hidden col-span-2"}
       [:ul {:class "text-xl border-b flex flex-row"}
        (doall (for [view-type tabbed-views]
                 ^{:key (str view-type)}
                 [:li {:class (str (when (= view-type @action) "border-b-4 border-blue-400 ") "px-2 pt-2 cursor-pointer hover:bg-blue-100 focus:bg-blue-100")
                       :on-click #(reset! action view-type)}
                  [:h2 (str/capitalize (name view-type))]]))]
       (case @action
         :entity [:div {:class "overflow-auto h-full w-full mt-2"}
                  [c.entity/entity @rconn entity-lookup-ratom]]
         :query  [:div {:class "overflow-auto h-full w-full mt-2"}
                  [c.query/query @rconn]])])))

(defn root []
  (let [loaded-db? (r/atom false)]
    (fn []
      [:div {:class "relative text-xs h-full w-full grid grid-cols-4"}
       [:div {:class "flex flex-col border-r pt-2 overflow-hidden col-span-1 "}
        [:h2 {:class "pl-1 text-xl border-b flex center"} "Schema"]
        [:div {:class "overflow-auto h-full w-full"}
         [c.schema/schema @rconn]]]
       [:div {:class "flex flex-col border-r overflow-hidden col-span-1 "}
        [:h2 {:class "px-1 text-xl border-b pt-2"} "Entities"]
        [:div {:class "overflow-auto h-full w-full"}
         [c.entities/entities @rconn entity-lookup-ratom]]]
       [db-actions rconn entity-lookup-ratom]
       [:button
        {:class "absolute top-2 right-1 py-1 px-2 rounded bg-gray-200 border"
         :on-click (fn []
                     (when-not @loaded-db? (reset! loaded-db? true))
                     (post-message devtool-port :datalog-console.client/request-whole-database-as-string {}))}
        (if @loaded-db? "Refresh database" "Load database")]])))

(defn mount! []
  (rdom/render [root] (js/document.getElementById "root")))

(defn init! []
  (mount!))

(defn ^:dev/after-load remount!
  "Remounts the whole UI on every save. Def state you want to persist between remounts with defonce."
  []
  (mount!))