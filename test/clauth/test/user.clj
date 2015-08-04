(ns clauth.test.user
  (:require [clojure.test :refer :all]
            [clauth.user :as base]
            [clauth.store :as store]))

(def user-store (store/create-memory-store))

(deftest user-registration
  (base/reset-user-store! user-store)
  (let [record (base/register-user user-store "john@example.com" "password" "John Doe"
                                   "http://example.com")]
    (is (= "John Doe" (:name record)) "should add extra attributes to user")
    (is (= 1 (count (base/users user-store))) "added one")
    (is (= record (first (base/users user-store))) "added one")
    (is (= record (base/authenticate-user user-store "john@example.com" "password"))
        "should authenticate user")
    (is (nil? (base/authenticate-user user-store "john@example.com" "bad"))
        "should not authenticate user with wrong password")
    (is (nil? (base/authenticate-user user-store "idontexist" "bad"))
        "should not authenticate user with wrong id")))

(deftest user-store-implementation
  (base/reset-user-store! user-store)
  (is (= 0 (count (base/users user-store))) "starts out empty")
  (let [record (base/new-user "john@example.com" "password")]
    (is (nil? (base/fetch-user user-store "john@example.com")))
    (do
      (base/store-user user-store record)
      (is (= record (base/fetch-user user-store "john@example.com")))
      (is (= 1 (count (base/users user-store))) "added one"))))
