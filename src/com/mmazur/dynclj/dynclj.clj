(ns com.mmazur.dynclj.dynclj
  (:gen-class)
  (:use [clojure.contrib.base64 :only [encode-str]]
        [clojure.contrib.duck-streams :only [file-str writer read-lines append-spit]]
        [clojure.contrib.str-utils2 :only [split join]]
        [clojure.contrib.command-line :only [with-command-line]]
        [clojure.http.client :only [request]])
  (:import (java.util Date Calendar)))

(def *cache-file-name* (str (System/getenv "HOME") "/.dynclj/dynclj.cache"))
(def *log-file-name* (str (System/getenv "HOME") "/.dynclj/dynclj.log"))

; from http://www.dyndns.com/services/dns/dyndns/readme.html#abuse
(def *days-between-nochg-updates* 28)

; Format: Mon, 16 Nov 2009 03:00:26 GMT
(def *date-format* "E, d MMM yyyy HH:mm:ss z")

(defn serialize-date [d]
  (.format (java.text.SimpleDateFormat. *date-format*) d))

(defn deserialize-date [s]
  (.parse (java.text.SimpleDateFormat. *date-format*) s))

(def *now* (serialize-date (Date.)))

(def *verbose?* false)

(defn log [msg]
  (when *verbose?* (println msg))
  (append-spit *log-file-name* (str *now* " " msg "\n")))

(defn abort [msg]
  (binding [*verbose?* true]
    (log msg))
  (System/exit 1))

;;; DynDNS logic
(defn nochg-update-ok? [current last-update]
  (let [cal (Calendar/getInstance)]
    (.setTime cal (deserialize-date last-update))
    (.add cal Calendar/DATE *days-between-nochg-updates*)
    (.after (deserialize-date current) (.getTime cal))))

(defn update-needed? [current last-update]
  (or
    (not (= (:ip current) (:ip last-update)))
    (nochg-update-ok? (:date current) (:date last-update))))

(defn get-current-ip-address-from-dyndns []
  (let [response (request "http://checkip.dyndns.org/")
        response-body (first (:body-seq response))]
    (re-find #"\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}" response-body)))

;;; Cache
(defn write-cache
  ([cache] (write-cache cache *cache-file-name*))
  ([cache filename]
   (with-open [cache-file (writer (file-str filename))]
     (doall
       (for [cache-item cache]
         (.write cache-file (str cache-item "\n")))))))

(defn read-cache
  ([] (read-cache *cache-file-name*))
  ([filename]
   (try
     (reduce #(conj %1 (read-string %2)) [] (read-lines filename))
     (catch java.io.FileNotFoundException _ []))))

(defn get-host-record-from-cache [host-name cache]
  (first (filter #(= host-name (:host %)) cache)))

;;; Config file
(defn get-config-filename []
  (str (System/getenv "HOME") "/.dynclj/dynclj.conf"))

(defn comment-or-blank? [line]
  (or (if (re-matches #"^#.*" line) true false)
      (if (re-matches #"^\s*$" line) true false)))

(defn get-config [file]
  "Returns a map of option=value from the config file."
  (try
    (reduce #(assoc %1 (keyword (first %2)) (second %2))
            {}
            (for [line (remove comment-or-blank? (read-lines file))]
              (split line #"=")))
    (catch java.io.FileNotFoundException _
      (abort (str "ERROR: Config file " file " not found")))))

;;; Perform the update
(defn determine-hosts-to-update [config-map current-state]
  (let [configured-hosts (split (config-map :hosts) #",")
        cache (read-cache)]
    (vec (remove nil? (for [host configured-hosts]
                        (let [cache-item (get-host-record-from-cache host cache)]
                          (if (nil? cache-item)
                            host
                            (if (update-needed? current-state cache-item)
                              host))))))))

(defn response-successful? [response current-state]
  "Inspect response to determine whether request succeeded"
  (let [response-code (:code response)]
    (if (= 200 (:code response))
      (= (apply str (:body-seq response)) (str "good " (current-state :ip)))
      false)))

(defn perform-update [records current-state config-map]
  (let [username (:username config-map)
        password (:password config-map)
        headers {"Authorization" (str "Basic " (encode-str (str username ":" password)))}
        hosts (join "," records)
        update-url (str "https://members.dyndns.org/nic/update?hostname=" hosts "&myip=" (:ip current-state))
        _ (log (str "Update URL: " update-url))
        response (request update-url "GET" headers)]
    (if (response-successful? response current-state)
      (let [update-date (first (:date (:headers response)))
            new-cache (reduce #(conj %1 (assoc current-state :host %2 :date update-date)) [] records)]
        (log (str "Update response code: " (:code response) ", msg: " (:msg response) ", body: " (apply str (:body-seq response))))
        (log (str "Writing cache: " new-cache))
        (write-cache new-cache)
        :success)
      (do
        (log (str "Update failed with return code: " (:code response) ", msg: " (:msg response) ", body: " (apply str (:body-seq response))))
        :fail))))

(defn -main [& args]
  (with-command-line args
    "Update DynDNS host information"
    [[config-file c "use specified configuration file"]
     [ip "force update to use this IP"]
     [force? "force the update"]
     [verbose? v? "verbose output"]]
    (binding [*verbose?* verbose?]
      (let [config-map (get-config (or config-file (get-config-filename)))
            current-state {:ip (or ip (get-current-ip-address-from-dyndns))
                           :date *now*}
            hosts-to-update (if force?
                              (split (config-map :hosts) #",")
                              (determine-hosts-to-update config-map current-state))]
        (if (> (count hosts-to-update) 0)
          (do (log (str "Updating hosts: " (apply str (interpose \, hosts-to-update))))
            (perform-update hosts-to-update current-state config-map))
          (log "No update needed"))))))

(comment

  (def username "test")
  (def password "test")
  (def user-pass-base64-encoded (encode-str (str username ":" password)))
  (def update-url "https://members.dyndns.org/nic/update?hostname=test.dyndns.org&myip=110.24.1.55")
  (def additional-headers {"Authorization" (str "Basic " user-pass-base64-encoded)})

  (def response (request update-url "GET" additional-headers))
  (keys response)
  (:body-seq response)
  (:code response)
  (first (:date (:headers response)))
  (println response)

  )
