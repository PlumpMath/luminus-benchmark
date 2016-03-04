(ns hello.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [hello.layout :refer [error-page]]
            [hello.routes.home :refer [home-routes]]
            [compojure.route :as route]
            [hello.middleware :as middleware]))

(def app
  (routes
    #'home-routes
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))
