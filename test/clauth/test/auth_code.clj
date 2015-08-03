(ns clauth.test.auth-code
  (:require [clojure.test :refer :all]
            [clauth
             [auth-code :as base]
             [token :as token]]
            [clj-time.core :as time]
            [clauth.store :as store]))

(def auth-code-store (store/create-memory-store))

(deftest auth-code-records
  (let [record (base/oauth-code "my-client" "user" "http://test.com/redirect")]
    (is (= "my-client" (:client record)) "should have client")
    (is (= "user" (:subject record)) "should have subject")
    (is (= "http://test.com/redirect" (:redirect-uri record))
        "should have redirect-uri")
    (is (not (nil? (:code record))) "should include code field")
    (is (token/is-valid? record) "should be valid by default")))

(deftest auth-code-creation
  (base/reset-auth-code-store! auth-code-store)
  (is (= 0 (count (base/auth-codes auth-code-store))) "starts out empty")
  (let [record (base/create-auth-code auth-code-store "my-client" "my-user"
                                      "http://test.com/redirect")]
    (is (= "my-client" (:client record)) "should have client")
    (is (= "my-user" (:subject record)) "should have subject")
    (is (= "http://test.com/redirect" (:redirect-uri record))
        "should have redirect-uri")
    (is (not (nil? (:code record))) "should include auth-code field")
    (is (= 1 (count (base/auth-codes auth-code-store))) "added one")
    (is (= record (first (base/auth-codes auth-code-store))) "added one")
    (is (= record (base/find-valid-auth-code auth-code-store (:code record)))))

  (let [record (base/create-auth-code auth-code-store {:client       "my-client"
                                                       :subject      "my-user"
                                                       :redirect-uri "http://test.com/redirect"})]
    (is (= "my-client" (:client record)) "should have client")
    (is (= "my-user" (:subject record)) "should have subject")
    (is (= "http://test.com/redirect" (:redirect-uri record))
        "should have redirect-uri")
    (is (not (nil? (:code record))) "should include auth-code field")
    (is (= 2 (count (base/auth-codes auth-code-store))) "added one")
    (is (= record (first (base/auth-codes auth-code-store))) "added one")
    (is (= record (base/find-valid-auth-code auth-code-store (:code record))))))

(deftest auth-code-validity
  (is (token/is-valid? {}) "by default it's valid")
  (is (not (token/is-valid? nil)) "nil is always false")
  (is (token/is-valid? {:expires (time/plus (time/now) (time/days 1))})
      "valid if expiry date is in the future")
  (is (not (token/is-valid? {:expires (time/date-time 2012 3 13)}))
      "expires if past expiry date"))

(deftest auth-code-store-implementation
  (base/reset-auth-code-store! auth-code-store)
  (is (= 0 (count (base/auth-codes auth-code-store))) "starts out empty")
  (let [record (base/oauth-code
                 "my-client" "my-user" "http://test.com/redirect")]
    (is (nil? (base/fetch-auth-code auth-code-store (:code record))))
    (do (base/store-auth-code auth-code-store record)
        (is (= record (base/fetch-auth-code auth-code-store (:code record))))
        (is (= 1 (count (base/auth-codes auth-code-store))) "added one"))))
