tangle
======

Tangle is a Clojure library to visualize your tangle of data with [GraphViz](http://www.graphviz.org/).

1 Minute version
----------------

Add to your project.clj:

```clj
[tangle "0.1.2"]
```

Run in your favourite REPL:

```clj
> (use 'tangle.core)
> (def nodes [:a :b :c :d])
> (def edges [[:a :b] [:a :c] [:c :d] [:a :c {:label "another" :style :dashed}]])
> (def i (dot->image (graph->dot nodes edges {:shape :box}) "png"))
```

Now do something with the image!

```clj
> (use 'rhizome.viz)
nil
> (view-image i)
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
