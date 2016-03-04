(ns hello.core
  (:require [hello.handler :as handler]
            [hello.routes.home :refer [io-routes db-routes]]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]
            [luminus-migrations.core :as migrations]
            [hello.config :refer [env]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [hello.env :refer [defaults]]
            [luminus.logger :as logger]
            [mount.core :as mount])
  (:gen-class))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate http-server
                :start
                (->
                   (http/start
                     (-> env
                        (assoc
                          :handler handler/app
                          :worker-threads 200
                          :io-threads (* 2 (.availableProcessors (Runtime/getRuntime))))
                        (update :port #(or (-> env :options :port) %))))
                  (http/wrap-handler io-routes {:path "/io" :dispatch? false})
                  (http/wrap-handler db-routes {:path "/db" :dispatch? false}))
                :stop
                (http/stop http-server))

(mount/defstate repl-server
                :start
                (when-let [nrepl-port (env :nrepl-port)]
                  (repl/start {:port nrepl-port}))
                :stop
                (when repl-server
                  (repl/stop repl-server)))

(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (logger/init (:log-config env))
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  ((:init defaults))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& args]
  (cond
    (some #{"migrate" "rollback"} args)
    (do
      (mount/start #'hello.config/env)
      (migrations/migrate args (env :database-url))
      (System/exit 0))
    :else
    (start-app args)))

