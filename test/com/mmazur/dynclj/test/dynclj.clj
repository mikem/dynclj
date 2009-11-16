(ns com.mmazur.dynclj.test.dynclj
  (:use [com.mmazur.dynclj.dynclj] :reload)
  (:use [clojure.test]
        [clojure.contrib.duck-streams :only [spit]]
        [clojure.http.client :only [request]]))

(def cache-test-filename "/tmp/dynclj-test.cache")

(def cache-with-one-entry [{:host "test.dyndns.org" :ip "100.99.88.77" :date "Mon, 16 Nov 2009 11:22:52 SGT"}])
(def cache-with-one-entry-string "{:host \"test.dyndns.org\", :ip \"100.99.88.77\", :date \"Mon, 16 Nov 2009 11:22:52 SGT\"}\n")

(def cache-with-two-entries [{:host "test.dyndns.org" :ip "95.209.0.35" :date "Wed, 11 Nov 2009 19:12:03 SGT"}
                             {:host "test.dnsalias.net" :ip "95.209.0.35" :date "Wed, 11 Nov 2009 19:12:03 SGT"}])
(def cache-with-two-entries-string (str "{:host \"test.dyndns.org\", :ip \"95.209.0.35\", :date \"Wed, 11 Nov 2009 19:12:03 SGT\"}\n"
                                        "{:host \"test.dnsalias.net\", :ip \"95.209.0.35\", :date \"Wed, 11 Nov 2009 19:12:03 SGT\"}\n"))

(defn id [a] a)

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
                            :get-header id,
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

(deftest test-write-cache
  (testing "Write cache containing one entry"
    (is (= cache-with-one-entry-string
           (do
             (write-cache cache-with-one-entry cache-test-filename)
             (slurp cache-test-filename)))))
  (testing "Write cache containing one entry, using the global *cache-file-name*"
    (binding [*cache-file-name* cache-test-filename]
      (is (= cache-with-one-entry-string
             (do
               (write-cache cache-with-one-entry)
               (slurp cache-test-filename))))))
  (testing "Write cache containing two entries"
    (is (= cache-with-two-entries-string
           (do
             (write-cache cache-with-two-entries cache-test-filename)
             (slurp cache-test-filename)))))
  (testing "Write cache containing two entries, using the global *cache-file-name*"
    (binding [*cache-file-name* cache-test-filename]
      (is (= cache-with-two-entries-string
             (do
               (write-cache cache-with-two-entries)
               (slurp cache-test-filename)))))))

(deftest test-read-cache
  (testing "Read cache containing one entry"
    (is (= cache-with-one-entry
           (do
             (spit cache-test-filename cache-with-one-entry-string)
             (read-cache cache-test-filename)))))
  (testing "Read cache containing one entry, using the global *cache-file-name*"
    (binding [*cache-file-name* cache-test-filename]
      (is (= cache-with-one-entry
             (do
               (spit cache-test-filename cache-with-one-entry-string)
               (read-cache))))))
  (testing "Read cache containing two entries"
    (is (= cache-with-two-entries
           (do
             (spit cache-test-filename cache-with-two-entries-string)
             (read-cache cache-test-filename)))))
  (testing "Read cache containing two entries, using the global *cache-file-name*"
    (binding [*cache-file-name* cache-test-filename]
      (is (= cache-with-two-entries
             (do
               (spit cache-test-filename cache-with-two-entries-string)
               (read-cache)))))))

(defn my-run-tests
   []
   (binding [*test-out* *out*] (run-tests)))

(my-run-tests)
