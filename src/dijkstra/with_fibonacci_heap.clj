(ns dijkstra.with-fibonacci-heap
  (:use [clojure.pprint])
  (:require [clojure.zip :as z])
  (:require [fibonacci-heap.core :as fib])
  (:require [dijkstra.read-data :as read-data]))

(defn node-filter [id]
  (fn [node]
    (= id (-> node :data :id))))

(defn consider-neighbour [neighbour current {heap :heap nodes :nodes}]
  (let [tentative-length (+ (-> current :key) (-> neighbour :length))
        neighbour-node (get nodes (-> neighbour :id))]
    (if (< tentative-length (-> neighbour-node :key))
      (let [neighbour-loc (fib/search heap
                                      (node-filter (-> neighbour-node :id))
                                      (-> neighbour-node :key))]
        {:heap (fib/decrease-key heap neighbour-loc tentative-length)
         :nodes (assoc-in nodes [(-> neighbour :id) :key] tentative-length)})
        {:heap heap :nodes nodes}))) ;; no change

(defn consider-neighbours [neighbours current unvisited]
  (if (empty? neighbours)
    unvisited
    (consider-neighbours (rest neighbours) current
                         (if (get (-> unvisited :nodes) (-> (first neighbours) :id))
                           (consider-neighbour (first neighbours) current unvisited) ;; handle
                           unvisited)))) ;; skip

(defn get-best-node [heap]
  (let [best-heap-node (fib/find-min heap)]
    (cond
      (nil? best-heap-node) nil
      (= (-> best-heap-node :key) read-data/infinity) nil
      :default {:id (-> best-heap-node :data :id)
                :key (-> best-heap-node :key)})))

(defn dijkstra [{heap :heap nodes :nodes} visited]
  (let [best-heap-node (get-best-node heap)
        best-id (-> best-heap-node :id)]
    (if (nil? best-heap-node)
      visited ;; finished
        (let [{new-heap :heap considered-nodes :nodes}
              (consider-neighbours (:neighbours (get nodes best-id))
                                   best-heap-node
                                   {:heap (fib/extract-min heap) :nodes nodes})
              new-visited (assoc visited best-id (get considered-nodes best-id))
              new-nodes (dissoc considered-nodes best-id)]
          (dijkstra {:heap new-heap :nodes new-nodes} new-visited)))))

(defn nodes-to-heap [nodes]
  (reduce (fn [heap x]
            (fib/heap-merge heap {:id (-> x :id)} (-> x :key)))
          (fib/create-heap)
          (vals nodes)))

(defn start-dijkstra [nodes]
  (dijkstra {:heap (nodes-to-heap nodes) :nodes nodes} {}))
