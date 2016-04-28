{:title "Clojure Explained - Multimethods and Protocols"
 :description  "Abstractions are important both in life and programming and we use them quite often, even if sometimes we don't realize it. We saw in the previous posts that Clojure provides higher-order functions to make our code more re-usable and macros for eliminating boilerplate. Now let's see how we can use multimethods and protocols when we need something more sophisticated."
 :layout :post
 :tags  ["Clojure" "ClojureScript" "Fundamentals"]
 :toc false
 :author "Tomasz Biernacki"
 :image "clojure-explained-abstractions.png"}

As a programmer you likely encountered the term _abstraction_ before but in
order to understand why we need it let's start with an analogy: imagine you are
building a house. You have the general design ready; you know where bedrooms, kitchen
and bathroom will be collocated but maybe at this point you don't need or don't
want to know how the kitchen actually will look like. You only know that it
will occupy a given space inside the house. In this sense a _kitchen_ is an
abstraction. The same goes for objects inside a room: you may know that you will
need a bed, a wordrobe and a desk and where they will be placed but at this point
you don't need to know what concrete bed or desk you are going to buy.
All these objects act as abstractions.

Another good analogy is a trip from point A to B: imagine you are planning a
trip and know that upon arrival to the airport you will rent a car and drive to
the city center. That's all you know: that you will rent **a** car and you will
_drive_ it. You will decide at "run-time" which specific car it will be but
during planning time you don't need to worry about it.

This approach has two advantages:

1) You don't need to worry about the details too soon, you just need to have the
big picture.

2) You can change or extend your _concrete_ choices: say you bought a metal desk
but for aesthetic reasons you want to change it to a wooden one. And of course
you can, because both are just desks and thanks to the good design you did initially
almost any desk would fit into the room.

As we know, programming imitates life (or was it the other way around?) and it
has its tools for creating abstractions, too. Usually we reason about
algorithms (_behaviors_), implemented by functions or methods, just like the
_drive_ example above.

In Java the main abstraction tool is the `interface`, which contains a set
of methods without body which other classes can implement. The advantage of
course is that then we can use the interface instead of a concrete type in our code
(see point 1 above) and then create many classes (see point 2) which
provide different implementation of the same operation.

Abstractions are all about **reusability** and **extensibility**, so let's see
what tools Clojure provides to deal with them.

## Multimethods

A multimethod is nothing more than a way to group together functions under one
name and select which one to execute based on another function, called
_dispatching function_. This could be not very clear so let's see an example.

```clojure
(defmulti tribute (fn [musician] (:role musician)))

(defmethod tribute :drums
  [musician]
  (str (:name musician) " was really great at drums! Just like a metronome!"))

(defmethod tribute :vocal
  [musician]
  (str (:name musician) " had such a perfect voice!"))

(tribute {:name "Jim Morrison", :role :vocal})
;; "Jim Morrison had such a perfect voice!"

(tribute {:name "Keith Moon", :role :drums})
;; "Keith Moon was really great at drums! Just like a metronome!"
```

First we create a multimethod `tribute` by using `defmulti`. The _dispatching function_
tells us how to select the correct implementation, in this
case we dispatch on `(:role musician)`. This value will be used to determine the
concrete function to execute, in our case we have two implementations
(`defmethod`), one for the drummers and one for the vocalists. You can see that
then  in the calls at the end the correct version is selected. If we analyze the
first call it follows this flow:

1. `(tribute {:name "Jim Morrison", :role :vocal})` is executed and Clojure
   knows it's a multimethod.
2. The _dispatching function_ is fired and returns `:vocal` (called
   _dispatching value_).
3. Clojure looks for a `defmethod`s whose _dispatch value_ is equal to `:vocal`.
4. The correct method is found and evaluated, producing `"Jim Morrison had such a perfect voice!"`.

Let's continue and create an algorithm that will pay tribute to musicians whose
first name or surname begins with the letter passed as parameter:

```clojure
(defn eligible-to-tribute?
  [musician letter]
  (some #(= letter %) (->> (clojure.string/split (:name musician) #" ")
                           (map first)
                           set)))

(defn pay-tribute
  [musicians letter]
  (let [eligible-musicians (filter #(eligible-to-tribute? % letter) musicians)]
    (doseq [musician eligible-musicians]
      (println (tribute musician)))))

(def musicians [{:name "Jim Morrison" :role :vocal}
                {:name "Keith Moon" :role :drums}
                {:name "John Bonham" :role :drums}
                {:name "Kurt Cobain" :role :vocal}])

(pay-tribute musicians \K)
;; Keith Moon was really great at drums! Just like a metronome!
;; Kurt Cobain had such a perfect voice!
```

First we define the `eligible-to-tribute?` function which checks whether a
given musician's name or surname begins with the given letter. Our
main "algorithm" is in the `pay-tribute` function, which simply
filters the musicians vector and use `tribute` to polymorphically pay
tribute to them. It's a very simple function but pretend for a moment
that the algorithm is actually more complicated. We test the function
against the `\K` letter and we see the correct result.

But what if we want to add another musician whose role is neither
`:vocals` nor `:drums`?

```clojure
(pay-tribute (conj musicians {:name "Jaco Pastorius" :role :bass}) \J)
;; [...]  No method in multimethod 'tribute' for dispatch value: :bass
```

And here's the main point of multimethods (and abstractions in
general): we just need to create another implementation,
**without modifying** our main algorithm, and everything will work as
expected.

```clojure
(defmethod tribute :bass
  [musician]
  (str (:name musician) " played the best bass lines ever!"))

(pay-tribute (conj musicians {:name "Jaco Pastorius" :role :bass}) \J)
;; Jim Morrison had such a perfect voice!
;; John Bonham was really great at drums! Just like a metronome!
;; Jaco Pastorius played the best bass lines ever!
```

We can also create a default implementation, for example we might
still need a bit of time to come up with a good tribute quote for
guitar players.

```clojure
(defmethod tribute :default
  [musician]
  (str (:name musician) " was very talented."))

(pay-tribute (conj musicians {:name "Jimi Hendrix" :role :guitar}) \J)
;; Jim Morrison had such a perfect voice!
;; John Bonham was really great at drums! Just like a metronome!
;; Jimi Hendrix was very talented.
```

You can see how easy is to provide new implementations for different
roles, or in other words to extend the algorithm with new
behaviors -- again: without having to modify any existing code. Before
we cover protocols few notes:

* `(defmulti tribute (fn [musician] (:role musician)))` could be
  written as `(defmulti tribute :role)` since keywords are functions.
* This is a simple dispatch function but could be as complicated as we
  want, for example we might dispatch not only by role but also by
  genre: `(fn [musician] [(:role musician) (:genre musician)])`.
  Actually, the dispatching function can return any value using all
  or some of its arguments.
* In case `:default` means something in your domain logic you can choose
  another default key in the `defmulti` definition:
  `(defmulti name dispatch-fn :default default-value) `
* We can dispatch by the type of the argument (`class`) similarly to
  what we do in OOP (aka _polymorphism_).
* For more advanced topics (`isa?`, `derive`, `prefer-method`) see
  [the official docs](http://clojure.org/reference/multimethods) and
  relative chapter in
  [Programming Clojure](https://pragprog.com/book/shcloj2/programming-clojure).
* Multimethods are rarely used; they are slow as they are fully dynamic (the
  dispatch function will be always evaluated). Nevertheless, you can see an
  interesting use-case in the ClojureScript compiler
  [here](https://github.com/clojure/clojurescript/blob/v1.8/src/main/clojure/cljs/compiler.cljc#L200).

## Protocols

Another tool that Clojure provides is the concept of protocol. Whereas
multimethods enable a very flexible and expressive polimorphism,
protocols focus on dispatching on the type of the first argument. If
you have difficulties understanding this think of protocols as a
specialized multimethod dispatched on `class`. As it happens many
times in Clojure the reason of this "duplication" is just
performance. Moreover, multimethods imply one single operation, while
a protocol is a set of polymorphic operations (called _methods_ in
this case).

The closest construct in the Java world is the `interface`:
* Both build a layer of abstraction by providing only specification,
not implementation, of behaviors.
* Both are dispatched on the type of the first parameter passed in the
  call. In Java this is just the object that implements the interface.
* Datatypes can implement multiple protocols.
* If you try to implement a protocol without providing all
  needed methods an error will be thrown.

One big difference is that in Java existing types cannot be extended
to implement new interfaces without rewriting them, which is totally
possible in Clojure.

Let's see them in action.

```clojure
(defprotocol Sanitizer
  "Custom printing and sanitizing for no particular reason."
  (my-print [this])
  (sanitize [this]))
```

This is not something you would normally write in production but bare
with me as this is only an example. The first thing we do is use
`defprotocol` to define a new protocol and we can note that we grouped
together two methods. `this` refers to the object passed as first parameter
of course and on which type we'll dispatch the correct function.

If we now try to run `(my-print 1)` we'll receive an error:

```clojure
(my-print 1)
;; IllegalArgumentException No implementation of method: :my-print of
;; protocol: #'app.core/Sanitizer found for class: java.lang.Long [...]
```

That's because `1` is a number and numbers don't implement the
`Sanitizer` protocol by default. More precisely `1` is a
`java.lang.Long`:

```clojure
(class 1)
;; java.lang.Long
```

So `1`'s type is `Long` and that's what we need to know to implement
the `Sanitizer` protocol. In order to do this we'll use `extend-type`.

```clojure
(extend-type java.lang.Long
  Sanitizer
  (my-print [this]
    (println "Here is your number: " this))
  (sanitize [this]
    (if (pos? this)
      this
      0)))

(my-print 1)
;; Here is your number:  1

(sanitize 10)
;; 10

(sanitize -10)
;; 0
```

As we can see now everything works as expected because now `Long`
implements the methods of the `Sanitizer` protocol. The implementation
is meaningless in this case but of course the logic could be more
complicated. Let's test it against a Ratio, for eg. `22/7`:

```clojure
(my-print 22/7)
;; No implementation of method: :my-print of protocol:
;; #'app.core/Sanitizer found for class: clojure.lang.Ratio [...]

(class 22/7)
;; clojure.lang.Ratio
```

It throws an exception and after inspecting the type of `22/7` we
should not wonder why. A `Ratio` is not a `Long` so the dispatch
doesn't work. But what we want is exactly the same behavior as for
`Long`. We could be tempted to re-write the exact same code as in
`extend-type java.lang.Long` for the `Ratio` type, maybe refactoring
the implementation to avoid duplicate code. But it turns out that we
don't need to do it because both `Long` and `Ratio` are `Number`s.

```clojure
(isa? java.lang.Long clojure.lang.Ratio)
;; false

(isa? clojure.lang.Ratio java.lang.Long)
;; false

(isa? clojure.lang.Ratio java.lang.Number)
;; true

(isa? java.lang.Long java.lang.Number)
;; true
```

So what we actually need to do is extend `java.lang.Number` and it
will work for both longs and ratios:

```clojure
(extend-type java.lang.Number
  Sanitizer
  (my-print [this]
    (println "Here is your number: " this))
  (sanitize [this]
    (if (pos? this)
      this
      0)))

(class 10)
;; java.lang.Long
(my-print 10)
;; Here is your number: 10

(class 22/7)
;; clojure.lang.Ratio
(my-print 22/7)
;; Here is your number:  22/7

;; will work also for double, since double are numbers
(class 10.0)
;; java.lang.Double
(my-print 10.0)
;; Here is your number:  10.0
```

Now we can do the same for other types, for example strings:

```clojure
(extend-type java.lang.String
  Sanitizer
  (my-print [this]
    (println "Here is your string: " this))
  (sanitize [this]
    (if (> (count this) 20)
      (-> (take 20 this)
          (clojure.string/join)
          (str "..."))
      this)))

(my-print "my string")
;; Here is your string:  my string

(sanitize "a very long string that needs to be reduced")
;; "a very long string t..."
```

Everything seems to be working but there is one last thing: there are
still many unimplemented types like maps, vectors, lists, ect. We
would like to provide a default implementation like we did with
`:default` for multimethods. We can do it by implementing
`java.lang.Object`, because every datatype inherit from it. This time
instead of using `extend-type` we will put everything together using
`extend-protocol` which allows us to implement different types at the
same time.

```clojure
(extend-protocol Sanitizer
  java.lang.Number
  (my-print [this]
    (println "Here is your number: " this))
  (sanitize [this]
    (if (pos? this)
      this
      0))

  java.lang.String
  (my-print [this]
    (println "Here is your string: " this))
  (sanitize [this]
    (if (> (count this) 20)
      (-> (take 20 this)
          (clojure.string/join)
          (str "..."))
      this))

  java.lang.Object
  (my-print [this]
    (println "Generic implementation: " this))
  (sanitize [this]
    ;; just return the object itself
    this))

(my-print 10)
;; Here is your number:  10

(my-print "my string")
;; Here is your string:  my string

(my-print [1 2 3])
;; Generic implementation:  [1 2 3]

```

## Datatypes

So far we've implemented protocols using existing datatypes but what we'll
discover soon is that we can create new ones in Clojure as well. In Clojure,
we use either `defrecord` or `deftype` when we want to create a new datatype.
See [here](http://www.lispcast.com/deftype-vs-defrecord) for an explanation
about the differences between them. For the purpose of this post I will use
only `defrecord`.

```clojure
(defrecord Person [name surname age])
```

Here we just create a record named `Person` with the `name`, `surname` and `age`
_fields_. You can think of it like a map with the difference that a record has
a unique class, either named or anonymous. This brings two interesting properties:

1. We can access the record's fields by using Java interop syntax, for example
  `(.name (Person. "John" "Smith" 40))` which is faster than accessing map values;
    we can also treat them just like maps: `(:name ((Person. "John" "Smith" 40))`.
    That's because records implement `PersistenMap`.

2. Records can implement protocols.

So in other words, records are basically maps that can implement protocols.
Let's do that and implement the previously defined `Sanitizer` protocol.

```clojure
(defrecord Person [name surname age]
  Sanitizer
  (my-print [this]
    (println "My name is" name surname "and I'm" age "years old."))
  (sanitize [this]
    (Person. (.toUpperCase name) (.toUpperCase surname) age)))

(my-print (Person. "Tomasz" "Biernacki" 26))
;; My name is Tomasz Biernacki and I'm 26 years old.

(sanitize (Person. "Tomasz" "Biernacki" 26))
;; #app.core.Person{:name "TOMASZ", :surname "BIERNACKI", :age 26}
```

The code is pretty self-explanatory and we can see that we were able to
implement the `Sanitizer` protocol and use its methods on a `Person`
object.

Few notes on datatypes:

* Datatypes are declared inside namespaces like everything else so if
  you want to use them in antoher namespace you need to _import_ them.
* You can use the `reify` macro to create an anonymous instance of a datatype
  (that implements a protocol).
* If you `assoc` or `update` a record, a new record will be returned:
  ```clojure
  (assoc (Person. "Tomasz" "Biernacki" 26) :nationality :Polish)
  ;; #app.core.Person{:name "Tomasz", :surname "Biernacki", :age 26, :nationality :Polish}
   ```
* If you `dissoc` an optional field (that is a field that wasn't specified in
  the original definition of the record, like `:nationality` above) a new record
  will be returned as well:
  ```clojure
  (-> (Person. "Tomasz" "Biernacki" 26)
      (assoc :nationality :Polish)
      (dissoc :nationality))
  ;; #app.core.Person{:name "Tomasz", :surname "Biernacki", :age 26}
   ```
* If you `dissoc` a field that was in the original definition of the record
  (that it, _not_ optional), a _plain map_ will be returned:
  ```clojure
  (dissoc (Person. "Tomasz" "Biernacki" 26) :age)
  ;; {:name "Tomasz", :surname "Biernacki"}
  ```
* Apart from the `Record. <&parmas>` syntax for creating new instances you can
  use the automatically created `->Record` and `map->Record` functions. See
  [here](http://www.braveclojure.com/multimethods-records-protocols/#Records)
  for more details.

## Conclusion

A lot of code this time :)

Even if multimethods and protocols are not widely used it's good to know them
as sometimes they can really be the right tool for the job.

Hope you enjoyed and stay tuned for the next post!
