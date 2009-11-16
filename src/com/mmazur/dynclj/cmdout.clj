(ns com.mmazur.dynclj.cmdout
  (:import (java.io BufferedReader InputStreamReader)))

(defn cmd [p] (.. Runtime getRuntime (exec (str p))))
(defn cmdout [o]
  (let [r (BufferedReader.
            (InputStreamReader.
              (.getInputStream o)))]
    (first (line-seq r))))

;(def current-actual-ip (cmdout (cmd "./get_ip_address.sh")))
;(def current-configured-ip (last (clojure.contrib.str-utils/re-split #" " (cmdout (cmd "host bt.selfip.com")))))
