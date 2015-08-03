(ns clauth.token
  (:require [clauth.store :as store]
            [crypto.random :as random]
            [clj-time
             [core :as time]
             [coerce :as coerce]]))

(defprotocol Expirable
  "Check if object is valid"
  (is-valid? [t] "is the object still valid"))

(extend-protocol Expirable clojure.lang.IPersistentMap
  (is-valid? [t] (if-let [expiry (:expires t)]
                   (time/after? (coerce/to-date-time expiry)
                                (time/now))
                   true)))

(extend-protocol Expirable nil (is-valid? [t] false))

(defn generate-token "generate a unique token" [] (random/base32 20))

(defn oauth-token
  "The oauth-token defines supports various functions to verify the validity

  The following keys are defined:

  * token - a unique token identifying it
  * client - a map/record of the client app who was issued the token
  * subject - the subject who authorized the token - eg. user
  * expires - Optional time of expiry
  * scope   - An optional vector of scopes authorized
  * object  - An optional object authorized. Eg. account, photo"

  ([attrs] ; Swiss army constructor. There must be a better way.
     (if attrs
       (if (:token attrs)
         attrs
         (assoc attrs :token (generate-token)))
       )
     )
  ([client subject]
     (oauth-token client subject nil nil nil))
  ([client subject expires scope object]
     (oauth-token (generate-token) client subject expires scope object))
  ([token client subject expires scope object]
     (oauth-token {:token token :client client :subject subject :expires expires :scope scope :object object})))

(defn reset-token-store!
  "mainly for used in testing. Clears out all tokens."
  [store]
  (store/reset-store! store))

(defn fetch-token
  "Find OAuth token based on the token string"
  [store t]
  (oauth-token (store/fetch store t)))

(defn store-token
  "Store the given OAuthToken and return it."
  [store t]
  (store/store! store :token t))

(defn revoke-token
  "Revoke the given OAuth token, given either a token string or object."
  [store t]
  (cond
   (instance? java.lang.String t) (store/revoke! store t)
   :default (store/revoke! store (:token t))))

(defn tokens
  "Sequence of tokens"
  [store]
  (map oauth-token (store/entries store)))

(defn create-token
  "create a unique token and store it in the token store"
  ([store client subject]
     (create-token store (oauth-token client subject)))
  ([store client subject scope object]
     (create-token store client subject nil scope object))
  ([store client subject expires scope object]
     (create-token store (oauth-token client subject expires scope object)))
  ([store token]
     (store-token store (oauth-token token))))

(defn find-valid-token
  "return a token from the store if it is valid."
  [store t]
  (if-let [token (fetch-token store t)]
    (if (is-valid? token) token)))

(defn find-tokens-for
  "return tokens matching a given criteria"
  [store criteria]
  (filter #(= criteria (select-keys % (keys criteria))) (tokens store)))
