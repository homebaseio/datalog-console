(ns datalog-console.chrome.panel)

(defn install! [conn]
  (js/chrome.devtools.panels.create
   "Datalog DBs"
   "favicon.png"
   "index.html"
   (fn [panel] (js/console.log "panel loaded!" panel))))