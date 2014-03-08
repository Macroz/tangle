tangle
======

Tangle is a Clojure library to visualize your tangle of data with [GraphViz](http://www.graphviz.org/).

1 Minute version
----------------

```clj
[tangle "0.1.0"]

> (use 'tangle.core)
nil
> (dot->image
       [:a :b :c :d]
       [[:a :b] [:a :c] [:c :d]]
       {:shape :box})
```

Background
----------

Through the years I have used GraphViz for many projects. I prefer to draw diagrams by programming and use it for layout. Despite its limitations, I have used it successfully and delivered many a graph.

To use GraphViz in Clojure, you may consider [Dorothy](https://github.com/daveray/dorothy) and [Rhizome](https://github.com/ztellman/rhizome). I find Dorothy a little bit too simple, you basically need a DSL on top of it. Rhizome does that but doesn't support multiple edges between same nodes conveniently. Hence I have decided to write my own. 

Tangle is quite compatible with Rhizome, if you decide you want to pop up Swing frames with images.

License
-------

Copyright © 2014 Markku Rontu

Distributed under the Eclipse Public License, the same as Clojure.
