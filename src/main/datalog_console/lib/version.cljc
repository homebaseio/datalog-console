(ns datalog-console.lib.version
  #?(:cljs (:require-macros [datalog-console.lib.version :refer [fetch-version]]))
  #?(:clj (:require [clojure.data.xml :as xml])))

#?(:clj (defmacro fetch-version []
          (let [pom-xml (java.io.StringReader. (slurp "pom.xml"))
                pom-version (first (:content (first (filter #(= (:tag %) :version) (:content (xml/parse pom-xml))))))]
            pom-version)))

(def datalog-version (fetch-version))