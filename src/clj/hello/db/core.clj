(ns hello.db.core
  (:require
    [conman.core :as conman]
    [mount.core :refer [defstate]]
    [hello.config :refer [env]]))

(defstate ^:dynamic *db*
          :start (conman/connect!
                   {:datasource
                    (doto (org.h2.jdbcx.JdbcDataSource.)
                          (.setURL (env :database-url))
                          (.setUser "")
                          (.setPassword ""))})
          :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defn get-world-random
  "Query a random World record between 1 and 10,000 from the database"
  []
  (get-world {:id (inc (rand-int 9999))}))

(defn get-query-count [queries]
  "Parse provided string value of query count, clamping values to between 1 and 500."
  (let [n (try (Integer/parseInt queries)
               (catch Exception e 1))] ; default to 1 on parse failure
    (cond
      (< n 1) 1
      (> n 500) 500
      :else n)))

(defn run-queries
  "Run the specified number of queries, return the results"
  [queries]
  (flatten (repeatedly (get-query-count queries) get-world-random)))

(defn get-fortunes []
   "Fetch the full list of Fortunes from the database, sort them by the fortune
  message text, and then return the results."
  (sort-by
   :message
   (conj (get-all-fortunes {})
         {:id 0 :message "Additional fortune added at request time."})))

(defn update-and-persist
  "Changes the :randomNumber of a number of world entities.
  Persists the changes to sql then returns the updated entities"
  [queries]
  (for [world (-> queries run-queries)]
    (let [updated-world (assoc world :randomNumber (inc (rand-int 9999)))]
      (update-world<! updated-world)
      updated-world)))
