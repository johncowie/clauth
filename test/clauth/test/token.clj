(ns clauth.test.token
  (:require [clojure.test :refer :all]
            [clauth.token :as base]
            [clj-time.core :as time]
            [clauth.store :as store]))

(def test-store (store/create-memory-store))

(deftest token-records
  (let [record (base/oauth-token "my-client" "user")]
    (is (= "my-client" (:client record)) "should have client")
    (is (= "user" (:subject record)) "should have client")
    (is (not (nil? (:token record))) "should include token field")
    (is (base/is-valid? record) "should be valid by default")))

(deftest token-creation
  (base/reset-token-store! test-store)
  (is (= 0 (count (base/tokens test-store))) "starts out empty")
  (let [record (base/create-token test-store "my-client" "my-user")]
    (is (= "my-client" (:client record)) "should have client")
    (is (= "my-user" (:subject record)) "should have subject")
    (is (not (nil? (:token record))) "should include token field")
    (is (= 1 (count (base/tokens test-store))) "added one")
    (is (= record (first (base/tokens test-store))) "added one")
    (is (= record (base/find-valid-token test-store (:token record)))))

  (base/reset-token-store! test-store)
  (let [record (base/create-token test-store {:client  "my-client"
                                              :subject "my-user"})]
    (is (= "my-client" (:client record)) "should have client")
    (is (= "my-user" (:subject record)) "should have subject")
    (is (not (nil? (:token record))) "should include token field")
    (is (= 1 (count (base/tokens test-store))) "added one")
    (is (= record (first (base/tokens test-store))) "added one")
    (is (= record (base/find-valid-token test-store (:token record))))))

(deftest token-validity
  (is (base/is-valid? {}) "by default it's valid")
  (is (not (base/is-valid? nil)) "nil is always false")
  (is (base/is-valid? {:expires (time/plus (time/now) (time/days 1))})
      "valid if expiry date is in the future")
  (is (not (base/is-valid? {:expires (time/date-time 2012 3 13)}))
      "expires if past expiry date"))

(deftest token-store-implementation
  (base/reset-token-store! test-store)
  (is (= 0 (count (base/tokens test-store))) "starts out empty")
  (let [record (base/oauth-token "my-client" "my-user")]
    (is (nil? (base/fetch-token test-store (:token record))))
    (do (base/store-token test-store record)
        (is (= record (base/fetch-token test-store (:token record))))
        (is (= 1 (count (base/tokens test-store))) "added one"))
    (do (base/revoke-token test-store record)
        (is (= nil (base/fetch-token test-store (:token record))))
        (is (= 0 (count (base/tokens test-store))) "revoked one"))))

(deftest find-matching-tokens-in-store
  (base/reset-token-store! test-store)
  (is (empty? (base/find-tokens-for
                test-store
                {:client  "my-client"
                 :subject "my-user"})))
  (let [record (base/create-token test-store {:client  "my-client"
                                              :subject "my-user"})]
    (is (= [record]
           (base/find-tokens-for
             test-store
             {:client  "my-client"
              :subject "my-user"})))
    (is (empty? (base/find-tokens-for
                  test-store
                  {:client  "my-client"
                   :subject "other-user"})))
    (is (empty? (base/find-tokens-for
                  test-store
                  {:client  "other-client"
                   :subject "my-user"})))))
