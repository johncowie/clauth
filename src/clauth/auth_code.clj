(ns clauth.auth-code
  (:require [clauth
             [store :as store]
             [token :as token]]
            [clj-time.core :as time]))

(defn oauth-code
  "The oauth-code defines supports various functions to verify the validity

  The following keys are defined:

  * auth-code - a unique auth-code identifying it
  * client - a map/record of the client app who was issued the auth-code
  * subject - the subject who authorized the auth-code - eg. user
  * redirect-uri - the redirect-uri passed during authorization
  * expires - Optional time of expiry
  * scope   - An optional vector of scopes authorized
  * object  - An optional object authorized. Eg. account, photo"

  ([attrs]
     (if attrs
       (let [attrs (if (:code attrs)
                     attrs
                     (assoc attrs :code (token/generate-token)))]
         (if (:expires attrs)
           attrs
           (assoc attrs :expires (clj-time.coerce/to-date (time/plus (time/now) (time/days 1))))))))
  ([client subject redirect-uri]
     (oauth-code client subject redirect-uri nil nil))
  ([client subject redirect-uri scope object]
     (oauth-code (token/generate-token)
                 client subject redirect-uri scope object))
  ([code client subject redirect-uri scope object]
     (oauth-code {:code code :client client :subject subject :redirect-uri redirect-uri :scope scope :object object})))

(defn reset-auth-code-store!
  "mainly for used in testing. Clears out all auth-codes."
  [auth-code-store]
  (store/reset-store! auth-code-store))

(defn fetch-auth-code
  "Find OAuth auth-code based on the auth-code string"
  [auth-code-store t]
  (oauth-code (store/fetch auth-code-store t)))

(defn revoke-auth-code!
  "Revoke the auth code so it can no longer be used"
  [auth-code-store code]
  (store/revoke! auth-code-store (:code code)))

(defn store-auth-code
  "Store the given OAuthCode and return it."
  [auth-code-store t]
  (store/store! auth-code-store :code t))

(defn auth-codes
  "Sequence of auth-codes"
  [auth-code-store]
  (map oauth-code (store/entries auth-code-store)))

(defn create-auth-code
  "create a unique auth-code and store it in the auth-code store"
  ([auth-code-store client subject redirect-uri]
     (create-auth-code auth-code-store (oauth-code client subject redirect-uri)))
  ([auth-code-store client subject redirect-uri scope object]
     (create-auth-code auth-code-store (oauth-code client subject redirect-uri scope object)))
  ([auth-code-store auth-code]
     (store-auth-code auth-code-store (oauth-code auth-code))))

(defn find-valid-auth-code
  "return a auth-code from the store if it is valid."
  [auth-code-store t]
  (if-let [oauth-code (fetch-auth-code auth-code-store t)]
    (if (token/is-valid? oauth-code) oauth-code)))
