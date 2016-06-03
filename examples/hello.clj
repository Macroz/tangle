(ns examples.hello
  (:use [tangle.core]
        [clojure.java.io]))

(def html-node {:id "html" :color "blue" :label [:TABLE {:BORDER 0} [:TR [:TD "hic"] [:TD {:BORDER 1} "cup"]]]})

(def nodes [:a :b :c :d html-node])

(def edges [[:a :b] [:a :c] [:c :d] [:a :c {:label "another" :style :dashed}] [:a :html]])

(def dot (graph->dot nodes edges {:node {:shape :box}
                                  :node->id (fn [n] (if (keyword? n) (name n) (:id n)))
                                  :node->descriptor (fn [n] (when-not (keyword? n) n))
                                  }))

(copy (dot->image dot "png") (file "examples/hello.png"))
(copy (dot->svg dot) (file "examples/hello.svg"))
