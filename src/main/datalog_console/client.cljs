(ns datalog-console.client
  {:no-doc true}
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [datalog-console.components.schema :as c.schema]
            [datalog-console.components.entity :as c.entity]
            [datalog-console.components.entities :as c.entities]
            [datalog-console.components.query :as c.query]
            [datalog-console.components.transact :as c.transact]
            [datascript.core :as d]
            [goog.object :as gobj]
            [clojure.string :as str]
            [datalog-console.lib.messaging :as msg]
            [datalog-console.components.feature-flag :as feature-flag]
            [cljs.reader]))

(def r-db-conn (r/atom (d/create-conn {})))
(def r-error (r/atom nil))
(def entity-lookup-ratom (r/atom ""))
(def integration-version (r/atom nil))

(try
  (def background-conn (msg/create-conn {:to (js/chrome.runtime.connect #js {:name ":datalog-console.client/devtool-port"})
                                         :routes {:datalog-console.remote/version
                                                  (fn [msg-conn msg]
                                                    (reset! integration-version (:data msg)))

                                                  :datalog-console.remote/db-as-string
                                                  (fn [msg-conn msg] (reset! r-db-conn (d/conn-from-db (cljs.reader/read-string (:data msg)))))

                                                  :datalog-console.client.response/transact!
                                                  (fn [msg-conn msg] (if (:success (:data msg))
                                                                       (msg/send {:conn msg-conn
                                                                                  :type :datalog-console.client/request-whole-database-as-string})
                                                                       (reset! r-error (:error (:data msg)))))}
                                         :tab-id js/chrome.devtools.inspectedWindow.tabId
                                         :send-fn (fn [{:keys [tab-id to msg]}]
                                                    (.postMessage to
                                                                  (clj->js {(str ::msg/msg) (pr-str msg)
                                                                            :tab-id tab-id})))
                                         :receive-fn (fn [cb msg-conn]
                                                       (.addListener (gobj/get (:to @msg-conn) "onMessage")
                                                                     (fn [msg]
                                                                       (when-let [raw-msg (gobj/get msg (str ::msg/msg))]
                                                                         (cb (cljs.reader/read-string raw-msg))))))}))


  (msg/send {:conn background-conn
             :type :datalog-console.client/init!})
  (msg/send {:conn background-conn
             :type :datalog-console.client/request-integration-version})

  (catch js/Error _e nil))

(defn tabs []
  (let [active-tab (r/atom "Entity")
        tabs ["Entity" "Query" "Transact"]
        on-tx-submit (fn [tx-str]
                       (msg/send {:conn background-conn
                                  :type :datalog-console.client/transact!
                                  :data tx-str}))]
    @(r/track! #(do @entity-lookup-ratom
                    (reset! active-tab "Entity")))
    (fn [r-db-conn entity-lookup-ratom]
      [:div {:class "flex flex-col overflow-hidden col-span-2"}
       [:ul {:class "text-xl border-b flex flex-row"}
        (doall (for [tab-name tabs]
                 ^{:key (str tab-name)}
                 [:li {:class (str (when (= tab-name @active-tab) "border-b-4 border-blue-400 ") "px-2 pt-2 cursor-pointer hover:bg-blue-100 focus:bg-blue-100")
                       :on-click #(reset! active-tab tab-name)}
                  [:h2 tab-name]]))]
       (case @active-tab
         "Entity" [:div {:class "overflow-auto h-full w-full mt-2"}
                   [c.entity/entity @r-db-conn entity-lookup-ratom]]
         "Query"  [:div {:class "overflow-auto h-full w-full mt-2"}
                   [c.query/query @r-db-conn]]
         "Transact" [feature-flag/version-check
                     {:title "Transact"
                      :required-version "0.3.1"
                      :current-version @integration-version}
                     [:div {:class "overflow-auto h-full w-full mt-2"}
                      [c.transact/transact on-tx-submit r-error]]])])))

(defn root []
  (let [loaded-db? (r/atom false)]
    (fn []
      [:div {:class "relative text-xs h-full w-full grid grid-cols-4"}
       [:div {:class "flex flex-col border-r pt-2 overflow-hidden col-span-1 "}
        [:h2 {:class "pl-1 text-xl border-b flex center"} "Schema"]
        [:div {:class "overflow-auto h-full w-full"}
         [c.schema/schema @r-db-conn]]]
       [:div {:class "flex flex-col border-r overflow-hidden col-span-1 "}
        [:h2 {:class "px-1 text-xl border-b pt-2"} "Entities"]
        [:div {:class "overflow-auto h-full w-full"}
         [c.entities/entities @r-db-conn entity-lookup-ratom]]]
       [tabs r-db-conn entity-lookup-ratom]
       [:button
        {:class "absolute top-2 right-1 py-1 px-2 rounded bg-gray-200 border"
         :on-click (fn []
                     (when-not @loaded-db? (reset! loaded-db? true))
                     (msg/send {:conn background-conn
                                :type :datalog-console.client/request-whole-database-as-string}))}
        (if @loaded-db? "Refresh database" "Load database")]])))

(defn mount! []
  (rdom/render [root] (js/document.getElementById "root")))

(defn init! []
  (mount!))

(defn ^:dev/after-load remount!
  "Remounts the whole UI on every save. Def state you want to persist between remounts with defonce."
  []
  (mount!))