(ns com.mmazur.dynclj.test.dynclj
  (:use [com.mmazur.dynclj.dynclj] :reload)
  (:use [clojure.test]
        [clojure.contrib.duck-streams :only [spit]]
        [clojure.http.client :only [request]]))

(def cache-test-filename "/tmp/dynclj-test.cache")

(def config-with-one-entry {:hosts "test.dyndns.org", :password "test", :username "test"})
(def config-with-two-entries {:hosts "test.dyndns.org,test.dnsalias.net", :password "test", :username "test"})

(def cache-with-one-entry [{:host "test.dyndns.org" :ip "100.99.88.77" :date "Mon, 16 Nov 2009 11:22:52 SGT"}])
(def cache-with-one-entry-string "{:host \"test.dyndns.org\", :ip \"100.99.88.77\", :date \"Mon, 16 Nov 2009 11:22:52 SGT\"}\n")

(def cache-with-two-entries [{:host "test.dyndns.org" :ip "95.209.0.35" :date "Wed, 11 Nov 2009 19:12:03 SGT"}
                             {:host "test.dnsalias.net" :ip "95.209.0.35" :date "Wed, 11 Nov 2009 19:12:03 SGT"}])
(def cache-with-two-entries-string (str "{:host \"test.dyndns.org\", :ip \"95.209.0.35\", :date \"Wed, 11 Nov 2009 19:12:03 SGT\"}\n"
                                        "{:host \"test.dnsalias.net\", :ip \"95.209.0.35\", :date \"Wed, 11 Nov 2009 19:12:03 SGT\"}\n"))
(def cache-with-two-entries-different-times [{:host "test.dyndns.org" :ip "95.209.0.35" :date "Wed, 11 Nov 2009 19:12:03 SGT"}
                                             {:host "test.dnsalias.net" :ip "95.209.0.35" :date "Sat, 21 Nov 2009 10:02:31 SGT"}])

(def config-test-filename "/tmp/dynclj-test.conf")

(def sample-config-1 "username=test_user\npassword=baloney\nhosts=test.dyndns.org,test.dnsalias.org")

(def mock-checkip-result "91.162.234.119")
(def mock-checkip-response {:body-seq '("<html><head><title>Current IP Check</title></head><body>Current IP Address: 91.162.234.119</body></html>"),
                            :code 200,
                            :msg "OK",
                            :method "GET",
                            :headers {:content-length '("106"),
                                      :content-type '("text/html"),
                                      :connection '("close"),
                                      :server '("DynDNS-CheckIP/1.0"),
                                      :cache-control '("no-cache"),
                                      :pragma '("no-cache")},
                            :get-header identity,
                            :cookies nil,
                            :url "http://checkip.dyndns.org/"})

(deftest test-get-current-ip-address-from-dyndns
  (testing
    (binding [request (fn [x] mock-checkip-response)]
      (is (= mock-checkip-result
             (get-current-ip-address-from-dyndns))))))

(deftest test-update-needed?
  (testing "IP is the same and last update was less than 28 days ago"
    (is (= false
           (update-needed? {:ip "220.255.77.147" :date "Sun, 01 Nov 2009 21:04:47 SGT"}
                           {:ip "220.255.77.147" :date "Sat, 30 Oct 2009 04:54:23 SGT"}))))
  (testing "IP is the same but last update was more than 28 days ago"
    (is (= true
           (update-needed? {:ip "220.255.77.147" :date "Sun, 29 Nov 2009 21:04:47 SGT"}
                           {:ip "220.255.77.147" :date "Sun, 01 Nov 2009 21:04:23 SGT"}))))
  (testing "IP is different"
    (is (= true
           (update-needed? {:ip "220.255.77.147" :date "Sun, 01 Nov 2009 21:04:47 SGT"}
                           {:ip "220.255.88.158" :date "Sat, 30 Oct 2009 04:54:23 SGT"})))))

(deftest test-filter-with-update-needed?-no-results
  (is (= []
         (filter #(update-needed? {:ip "95.209.0.35" :date "Thu, 12 Nov 2009 04:58:27 SGT"} %) cache-with-two-entries))))

(deftest test-filter-with-update-needed?-one-result
  (is (= [{:host "test.dyndns.org" :ip "95.209.0.35" :date "Wed, 11 Nov 2009 19:12:03 SGT"}]
         (filter #(update-needed? {:ip "95.209.0.35" :date "Wed, 9 Dec 2009 19:12:04 SGT"} %) cache-with-two-entries-different-times))))

(deftest test-filter-with-update-needed?-all-results-time-expired
  (is (= cache-with-two-entries
         (filter #(update-needed? {:ip "95.209.0.35" :date "Wed, 9 Dec 2009 19:12:04 SGT"} %) cache-with-two-entries))))

(deftest test-filter-with-update-needed?-all-results-ip-changed
  (is (= cache-with-two-entries
         (filter #(update-needed? {:ip "109.44.1.8" :date "Wed, 11 Nov 2009 19:15:00 SGT"} %) cache-with-two-entries))))

(deftest test-write-cache-with-one-entry
  (is (= cache-with-one-entry-string
         (do
           (write-cache cache-with-one-entry cache-test-filename)
           (slurp cache-test-filename)))))

(deftest test-write-cache-with-one-entry-global-cache-filename
  (binding [*cache-file-name* cache-test-filename]
    (is (= cache-with-one-entry-string
           (do
             (write-cache cache-with-one-entry)
             (slurp cache-test-filename))))))

(deftest test-write-cache-with-two-entries
  (is (= cache-with-two-entries-string
         (do
           (write-cache cache-with-two-entries cache-test-filename)
           (slurp cache-test-filename)))))

(deftest test-write-cache-with-two-entries-global-cache-filename
  (binding [*cache-file-name* cache-test-filename]
    (is (= cache-with-two-entries-string
           (do
             (write-cache cache-with-two-entries)
             (slurp cache-test-filename))))))

(deftest test-read-cache-with-one-entry
  (is (= cache-with-one-entry
         (do
           (spit cache-test-filename cache-with-one-entry-string)
           (read-cache cache-test-filename)))))

(deftest test-read-cache-with-one-entry-global-cache-filename
  (binding [*cache-file-name* cache-test-filename]
    (is (= cache-with-one-entry
           (do
             (spit cache-test-filename cache-with-one-entry-string)
             (read-cache))))))

(deftest test-read-cache-with-two-entries
  (is (= cache-with-two-entries
         (do
           (spit cache-test-filename cache-with-two-entries-string)
           (read-cache cache-test-filename)))))

(deftest test-read-cache-with-two-entries-global-cache-filename
    (binding [*cache-file-name* cache-test-filename]
      (is (= cache-with-two-entries
             (do
               (spit cache-test-filename cache-with-two-entries-string)
               (read-cache))))))

(deftest test-read-cache-with-non-existent-filename
  (is (= []
         (read-cache "/tmp/this-file-does-not-exist"))))

(deftest test-read-config-file
  (is (= {:username "test_user" :password "baloney" :hosts "test.dyndns.org,test.dnsalias.org"}
         (do
           (spit config-test-filename sample-config-1)
           (get-config config-test-filename)))))

(deftest test-determine-hosts-to-update-no-ip-change
  (binding [*cache-file-name* cache-test-filename]
    (is (= []
           (let [config-map config-with-one-entry
                 current-state {:ip "100.99.88.77" :date "Mon, 16 Nov 2009 12:09:14 SGT"}]
             (spit cache-test-filename cache-with-one-entry-string)
             (determine-hosts-to-update config-map current-state))))))

(deftest test-determine-hosts-to-update-one-host-ip-change
  (binding [*cache-file-name* cache-test-filename]
    (is (= ["test.dyndns.org"]
           (let [config-map config-with-one-entry
                 current-state {:ip "100.99.88.6" :date "Mon, 16 Nov 2009 12:09:14 SGT"}]
             (spit cache-test-filename cache-with-one-entry-string)
             (determine-hosts-to-update config-map current-state))))))

;(defn my-run-tests
;   []
;   (binding [*test-out* *out*] (run-tests)))
;(my-run-tests)
