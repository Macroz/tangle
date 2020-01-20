(ns tangle.core-test
  (:use [flatland.ordered.map :only [ordered-map]])
  (:use [clojure.test]
        [tangle.core]))

(deftest escape-test
  (are [e x] (= e (#'tangle.core/escape x))
       "foobar" "foobar"
       "foo\\|bar" "foo|bar"
       "\\{foobar\\}" "{foobar}"
       "foo\\:bar" "foo:bar"
       "\\<\\[foobar\\]\\>" "<[foobar]>"
       ))



(deftest wrap-brackets-if-test
  (are [e t x] (= e (#'tangle.core/wrap-brackets-if t x))
       "foo" nil "foo"
       "foo" [] "foo"
       "[foo]" [1] "foo"
       ))



(deftest format-record-test
  (are [e x] (= e (#'tangle.core/format-record x))
       "" nil
       "" []
       "a" "a"
       "a" ["a"]
       "a|b" ["a" "b"]
       "{a|b}" [["a" "b"]]
       "{a|b}|c" [["a" "b"] "c"]
       "{a|b}|{c|d}" [["a" "b"] ["c" "d"]]
       ))



(deftest format-hiccup-test
  (are [e x] (= e (#'tangle.core/format-hiccup x))
       "<<TABLE><TR><TD>foo</TD><TD>1</TD></TR><TR><TD>bar</TD><TD>abc</TD></TR></TABLE>>" [:TABLE [:TR [:TD "foo"] [:TD 1]] [:TR [:TD "bar"] [:TD "abc"]]]
       "<<TABLE><TR><TD>foo</TD><TD><TABLE><TR><TD>bar</TD><TD>42</TD></TR></TABLE></TD></TR></TABLE>>" [:TABLE [:TR [:TD "foo"] [:TD [:TABLE [:TR [:TD "bar"] [:TD 42]]]]]]))



(deftest format-label-test
  (are [e x] (= e (#'tangle.core/format-label x))
       "" nil
       "foobar" "foobar"
       "a|:b|{:c|{:d|:e}}" ["a" :b [:c [:d :e]]]
       "42" 42
       ":foobar" :foobar
       ))



(deftest format-id-test
  (are [e x] (= e (#'tangle.core/format-id x))
       "\"42\"" "42"
       "42" 42
       "\"42\"" :42
       ))



(deftest format-option-value-test
  (are [e x] (= e (#'tangle.core/format-option-value x))
       "42" 42
       "foo" :foo
       "\"foo\"" "foo"
       "\"0,1,2\"" (range 3)
       ))

(deftest format-option-test
  (are [e x] (= e (#'tangle.core/format-option x))
       "x=1" [:x 1]
       "x=\"foobar\"" [:x "foobar"]
       "x=\"0,1,2\"" [:x (range 3)]
       ))

(deftest format-options-test
  (are [e x] (= e (#'tangle.core/format-options x))
       "" {}
       "x=1" {:x 1}
       "x=1, y=2" (ordered-map :x 1 :y 2)
       "label=\"a b\"" {:label "a b"}
       ))



(deftest format-node-test
  (are [e id opts] (= e (#'tangle.core/format-node id opts))
       "5" 5 {}
       "\"k->w\"" :k->w {}
       "\"ns/k->w\"" :ns/k->w {}
       "5[id=42, foo=bar, baz=\"quux\"]" 5 (ordered-map :id 42 :foo :bar :baz "quux")
       ))



(deftest format-edge-test
  (are [e src dst opts dir] (= e (#'tangle.core/format-edge src dst opts dir))
       "\"a\" -- \"b\"" "a" "b" {} false
       "\"a\" -- \"b\"" :a :b {} false
       "\"a\" -> \"b\"" :a :b {} true
       "\"ns/a\" -> \"ns/b\"" :ns/a :ns/b {} true
       "\"a\" -- \"b\"[label=\"foobar\", weight=1]" :a "b" (ordered-map :label "foobar" :weight 1) false
       ))
