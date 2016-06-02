(ns examples.hello
  (:use [tangle.core]))

(def nodes [:a :b :c :d])
(def edges [[:a :b] [:a :c] [:c :d] [:a :c {:label "another" :style :dashed}]])
(def dot (graph->dot nodes edges {:node {:shape :box}}))

