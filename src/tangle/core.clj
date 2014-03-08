(ns tangle.core
  (:use [clojure.test])
  (:use [flatland.ordered.map :only [ordered-map]])
  (:require [clojure.string :as str]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io]))



(def ^:private default-graph-options
  "Default options for a graph"
  {:dpi 100 :rankdir :TP})

(def ^:private default-node-options
  "Default options for a node"
  {})

(def ^:private default-edge-options
  "Default options for an edge"
  {})



(defn- escape-cmap
  "Character map for escaping DOT values"
  [c]
  (when ((set "|:\"{}") c)
    (str "\\" c)))

(defn- escape
  "Helper function for escaping strings to valid DOT values"
  [s]
  (str/escape s escape-cmap))

(deftest escape-test
  (are [e x] (= e (escape x))
       "foobar" "foobar"
       "foo\\|bar" "foo|bar"
       "\\{foobar\\}" "{foobar}"
       "foo\\:bar" "foo:bar"
       ))



(defn- wrap-brackets-if
  "Wrap brackets to x if t is not empty"
  [t x]
  (if-not (empty? t)
    (str "[" x "]")
    x))

(deftest wrap-brackets-if-test
  (are [e t x] (= e (wrap-brackets-if t x))
       "foo" nil "foo"
       "foo" [] "foo"
       "[foo]" [1] "foo"
       ))



(declare format-record)

(defn- format-record-wrap
  "Recursively format record type labels changing direction"
  [x]
  (cond (nil? x) ""
        (string? x) x
        (sequential? x) (str "{"
                             (->> x
                                  (map format-record-wrap)
                                  (interpose "|")
                                  (apply str))
                             "}")
        :else (pr-str x)))

(defn- format-record
  "Recursively format record type labels without changing direction."
  [x]
  (cond (nil? x) ""
        (string? x) x
        (sequential? x) (->> x
                             (map format-record-wrap)
                             (interpose "|")
                             (apply str))
        :else (pr-str x)))

(deftest format-record-test
  (are [e x] (= e (format-record x))
       "" nil
       "" []
       "a" "a"
       "a" ["a"]
       "a|b" ["a" "b"]
       "{a|b}" [["a" "b"]]
       "{a|b}|c" [["a" "b"] "c"]
       "{a|b}|{c|d}" [["a" "b"] ["c" "d"]]
       ))



(defn- format-label
  "Format label into DOT value.

  Handles regular Clojure data types.
  Sequential data are turned into record type labels."
  [x]
  (cond (nil? x) ""
        (string? x) x
        (sequential? x) (format-record x)
        :else (pr-str x)))

(deftest format-label-test
  (are [e x] (= e (format-label x))
       "" (format-label nil)
       "foobar" (format-label "foobar")
       ":a|:b|{:c|{:d|:e}}" (format-label [:a :b [:c [:d :e]]])
       "42" 42
       ":foobar" :foobar
       ))



(defn- format-id
  "Formats an id value in DOT format with proper escaping"
  [x]
  (cond
   (string? x) (str \" (escape x) \")
   (keyword? x) (name x)
   :else (str x)))

(deftest format-id-test
  (are [e x] (= e (format-id x))
       "\"42\"" "42"
       "42" 42
       "42" :42
       ))



(defn- format-option-value
  "Formats an option value in DOT format with proper escaping"
  [x]
  (cond
   (string? x) (str \" (escape x) \")
   (keyword? x) (name x)
   (coll? x) (str "\""
                  (->> x
                       (map format-option-value)
                       (interpose ",")
                       (apply str))
                  "\"")
   :else (str x)))

(deftest format-option-value-test
  (are [e x] (= e (format-option-value x))
       "42" 42
       "foo" :foo
       "\"foo\"" "foo"
       "\"0,1,2\"" (range 3)
       ))

(defn- format-option
  "Formats a single option in DOT format"
  [[k v]]
  (str (name k) "=" (format-option-value v)))

(deftest format-option-test
  (are [e x] (= e (format-option x))
       "x=1" [:x 1]
       "x=\"foobar\"" [:x "foobar"]
       "x=\"0,1,2\"" [:x (range 3)]
       ))

(defn- format-options
  "Formats a map of options in DOT format"
  [opts]
  (if (empty? opts) ""
      (->> (if (:label opts)
             (update-in opts [:label] format-label)
             opts)
           (map format-option)
           (interpose ", ")
           (apply str))))

(deftest format-options-test
  (are [e x] (= e (format-options x))
       "" {}
       "x=1" {:x 1}
       "x=1, y=2" (ordered-map :x 1 :y 2)
       "label=\"a b\"" {:label "a b"}
       ))



(defn- format-node
  "Formats the node as DOT node.

  Note the :id option is removed before formatting options."
  [id options]
  (let [options (dissoc options :id)]
    (str id (wrap-brackets-if options (format-options options)))))

(deftest format-node-test
  (are [e id opts] (= e (format-node id opts))
       "5" 5 {}
       "5" 5 {:id :id-is-disregarded}
       "5[foo=bar, baz=\"quux\"]" 5 {:id 42 :foo :bar :baz "quux"}
       ))



(defn- format-edge
  "Formats the edge as DOT node."
  ([src dst]
     (format-edge src dst {}))
  ([src dst options]
     (let [directed? (:directed? options)
           options (dissoc options :directed?)
           arrow (if directed? " -> " " -- ")]
       (str (format-id src) arrow (format-id dst)
            (wrap-brackets-if options (format-options options))))))

(deftest format-edge-test
  (are [e src dst opts] (= e (format-edge src dst opts))
       "\"a\" -- \"b\"" "a" "b" {}
       "a -- b" :a :b {}
       "a -> b" :a :b {:directed? true}
       "a -- \"b\"[label=\"foobar\", weight=1]" :a "b" (ordered-map :label "foobar" :weight 1)
       ))

(defn map-edges [m]
  (mapcat (fn [[k vs]]
            (for [v vs] [k v]))
          m))



(defn graph->dot
  "Transforms a graph of nodes and edges into GraphViz DOT format"
  [nodes edges options]
  (let [directed? (:directed? options false)
        node->descriptor (:node-descriptor options (constantly nil))
        edge->descriptor (:edge-descriptor options (constantly nil))
        node->id (comp format-id (:node->id options identity))
        node->cluster (:node->cluster options)
        cluster->parent (:cluster->parent options (constantly nil))
        cluster->id (:cluster->id options identity)
        cluster->descriptor (:cluster->descriptor options (constantly nil))

        current-cluster (::cluster options)
        cluster->nodes (if node->cluster
                         (group-by node->cluster nodes)
                         {nil nodes})
        clusters (keys cluster->nodes)]

    (apply str
           (cond current-cluster (str "subgraph cluster_" (cluster->id current-cluster))
                 directed? "digraph"
                 :else "graph")
           " {\n"

           (when-not current-cluster
             (let [graph-options (merge default-graph-options (:graph options))
                   edge-options (merge default-node-options (:edge options))
                   node-options (merge default-edge-options (:node options))]
               (str
                (when-not (empty? graph-options) (str "graph[" (format-options graph-options) "]\n"))
                (when-not (empty? node-options) (str "node[" (format-options node-options) "]\n"))
                (when-not (empty? edge-options) (str "edge[" (format-options edge-options) "]\n")))))

           ;; format nodes in current cluster
           (apply str (let [nodes-in-cluster (cluster->nodes current-cluster)]
                        (->> nodes-in-cluster
                             (map #(format-node (node->id %) (node->descriptor %)))
                             (interpose "\n"))))
           "\n"

           ;; format subclusters
           (let [clusters (->> clusters
                               (filter #(= (cluster->parent %) current-cluster))
                               (remove nil?))]
             (apply str (for [cluster clusters]
                          (graph->dot nodes [] (assoc options ::cluster cluster)))))

           "\n"

           (when-not current-cluster
             ;; format edges
             (apply str (->> edges
                             (map #(apply format-edge %))
                             (interpose "\n"))))
           "\n"

           ["}\n"])))



(defn dot->image
  "Uses GraphViz to render the DOT into an image"
  [dot format]
  (let [{:keys [out err]} (sh/sh "dot" (str "-T" format) :in dot :out-enc :bytes)]
    (javax.imageio.ImageIO/read (io/input-stream out))))

