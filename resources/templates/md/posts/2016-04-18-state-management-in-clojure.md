{:title "The approach to state management in Clojure"
:description  "Introduction to mutable state in Clojure. Simon's Cat will show you an effective way to manage change in your application, and you'll find out that cats love candies, too. :)"
:layout :post
:tags  ["Clojure" "ClojureScript" "Fundamentals"]
:toc false
:author "Dajana Štiberová"
:image "simons-cat2.jpg"}

Long time ago, during one interview, I was asked to describe the difference between `vars` and `atoms`.
It was a pretty simple question, but at that time for me it was not so easy to describe the difference.
Now I'm more skilled in Clojure but I still think that it was an interesting question and when
I was looking for a topic for a blog post, I decided to use this one.

So if you want to read something about state management, or you want to know why Simon's Cat doesn't want to go to the Halloween party, this post is for you :)

## State and Identity

First, we need to know that in Clojure there is a difference between _state_ and _identity_.

_State_ refers to immutable data: a state is the value an identity refers to at a particular point in time.
State never changes.

An _identity_ unifies different values over the time.
Identity types are mutable references to immutable values
and can be modeled by using one of the four reference types: _vars_, _atoms_, _agents_ and _refs_.

### *Example*

In order to explain better the concepts behind state management we'll use the
best cat on Earth, Simon's Cat, as our example.
Simon's Cat is an identity.
Simon's Cat promised to his girlfriend to go to the Halloween party, but he's really scared of spiders,
and at the Halloween party there will be many of them everywhere.
So he's trying to hide into a box, and as you can see, the box is too small.
Every move he makes can be thought as one state.

Simon's Cat has a series of different values (moves) over time (states `1`-`9`).
We need to realize that we can transition to a new state but it is not possible to change or modify the previous ones.
The identity (Simon's Cat) can be modeled by using a reference type pointing to the value,
and subsequently calling the appropriate functions to create new states.
<div>
<div id="identity">
![Identity](../img/simons-cat2.jpg)
</div>
<div id="state">
![States](../img/simons-cat-box.jpg)
</div>
</div>
NOTE:
If you don't have any knowledge about threads in Clojure, you should read [this](http://blog.clojuregirl.com/clojure_threads.html) first.

## How we can manage state (or how the data might change over time)
<div>
<div id="simons-text">

There are four ways to model an identity: _vars_ , _atoms_ , _refs_ and _agents_.

Continuing with our example, we know that Simon's Cat has a girlfriend, let's call her Simona, and they plan to go to a party.
Because it's Halloween party, apart from scary spiders, there are a lot of sweets everywhere.
Cats love candies and as we know, their favorite ones are yummy marshmallows and gummy bears.
</div>
<div id="simons-pic">
![simon's cat's gf](../img/simons-cat-with-gf.jpg "Marshmallow")
</div>
</div>

### Vars

We can create a _var_ by using `def`: `(def marshmallows-in-the-bag)`.
This _var_ will be a reference to the amount of marshmallows in one bag.

If we define a _var_ as `^:dynamic` (by adding the `^:dynamic` metadata part),
we'll also be able to rebound it in a different context later and this rebinding will be available only within its dynamic scope.

```clojure
(def ^:dynamic *marshmallows-in-the-bag* 30)
```

In Clojure, dynamic vars are typically surrounded by `*` in what is called the earmuff convention.

We will now look what the dynamic scope is, and at the difference between lexical and dynamic scope.

### Difference between the lexical and the dynamic scope

First, we need to clarify what the scope is: simply put, the scope is a
context created to summarize symbols at a given point in time.
Without this feature all the symbols would be global and this is something
we want to avoid.

### Lexical Scope

In this first case, the scope is the code block where we define the symbols. These symbols are valid only inside the code block (including any nested forms).
The lexical scope can be defined for example by the `let` block.

Let's imagine that in the bag there are `30` marshmallows.
We will define `marshmallows-in-the-bag` with an initial value of `30`.

```clojure
(def marshmallows-in-the-bag 30)
```

Now we will define a function `marshmallows-left` which takes one argument, `marshmallows-eaten`.
In a `let` block we will define the `new-amount-of-marshmallows` var, and its new value will be the difference between `marshmallows-in-the-bag` and `marshmallows-eaten`.
Inside the `let` block we use `println` to display the value of `new-amount-of-marshmallows`.

```clojure
(defn marshmallows-left
  [marshmallows-eaten]
  (let [new-amount-of-marshmallows (- marshmallows-in-the-bag marshmallows-eaten)]
     (println (str "Marshmallows in the bag: " new-amount-of-marshmallows))))
```

If we call the `marshmallows-left` function with the argument `10` (because Simon's Cat ate ten marshmallows),
we'll find out that there are `20` marshmallows left in the bag.

```clojure
(marshmallows-left 10)
;;=> Marshmallows in the bag: 20
```

### Dynamic Scope

In contrast to _lexical scope_, the _dynamic scope_ does not depend on the code block, but on the runtime call stack.

The best way to understand the difference is to look at the code below.

```clojure
(def ^:dynamic *marshmallows-in-the-bag* 30)

(defn marshmallows-left []
  (println (str "Marshmallows in the bag: " *marshmallows-in-the-bag*)))

(defn number-of-marshmallows-left
  [marshmallows-eaten]
  (binding [*marshmallows-in-the-bag* (- *marshmallows-in-the-bag* marshmallows-eaten)]
    (marshmallows-left)))
```

We created the `marshmallows-left` function, which prints the value of the `*marshmallows-in-the-bag*` dynamic var.
And then another function, `number-of-marshmallows-left` with one argument, `marshmallows-eaten`.
In this function we bind to `*marshmallows-in-the-bag*` the difference between `*marshmallows-in-the-bag*` and `marshmallows-eaten`.

If we now call `number-of-marshmallows-left` with `10` as argument we'll see the correct result in the output.

```clojure
(number-of-marshmallows-left 10)
;;=> Marshmallows: 20
```

To show the difference between the two scopes we'll do the same using the `let` block:

```clojure
(def ^:dynamic *marshmallows-in-the-bag* 30)

(defn marshmallows-left []
  (println (str "Marshmallows: " *marshmallows-in-the-bag*)))

(defn number-of-marshmallows-left [marshmallows-eaten]
  (let [*marshmallows-in-the-bag* (- *marshmallows-in-the-bag* marshmallows-eaten)]
    (marshmallows-left)))
```

And if we call `numbers-of-marshmallows-left` with `10` as argument, the result will be different than before.

```clojure
(number-of-marshmallows-left 10)
;;=> Marshmallows: 30
```

That's because we defined `marshmallows-in-the-bag` in the `let` code block and as we know the new value of the `marshmallows-in-the-bag` is valid only within its lexical scope (hence not in the function `marshmallows-left`, for which the value of `*marshmallows-in-the bag*` is still 30).

### Atoms

Clojure's _atom_ reference type is another way to manage state.
They are updated synchronously and are ideal for managing the state of independent identities.

We will use the same example as in the _vars_ section above but in this case
we will define `marshmallows-in-the-bag` as an atom.

```clojure
(def marshmallows-in-the-bag (atom 30))
```

To get the current state of the atom, we need to dereference it using `clojure.core/deref` or the `@` reader macro.

```clojure
@marshmallows-in-the-bag
;;=> 30
```

To update the atom's value (change what the atom refers to), we need to use the `swap!` function.
Remember that the value that the atom is pointing to is not changed; what _is_ being changed it's
the atom itself, that is the reference, which is assigned a _new_ value.

This is how `swap!` behaves under the hood:

1. Read the atom's current state.
2. Compare the current state of the `atom` and the state where the `swap!` started with transaction.
3. If there's difference, retry the transaction.
4. If there's not difference, update the state.

```clojure
(defn number-of-marshmallows-left
  [marshmallows-eaten]
  (swap! marshmallows-in-the-bag - marshmallows-eaten))
```
We created a `number-of-marshmalloes-left` function, which takes one argument `marshmallows-eaten`.
We passed to `swap!` a function which will be called with the current value of `marshmallows-in-the-bag`
as first argument. This function subtracts `marshmallows-eaten` from the current state.

```clojure
(number-of-marshmallows-left 10)
;;=> 20
```
If we dereference the atom we will see that the `marshmallows-in-the-bag` value is updated:

```clojure
@marshmallows-in-the-bag
;;=> 20
```

## Agents

While updates to _atoms_ happen "immediately", _agents_ are updated
asynchronously and we cannot be sure when they will be performed.
_Agents_ are good for functions you want to get done, but it doesn't matter
when it happens.

We will define `marshmallows-in-the-bag` as an _agent_.

```clojure
(def marshmallows-in-the-bag (agent 30))
```

To get the current value of the agent we will dereference it the same way we did with _atoms_.

```clojure
@marshmallows-in-the-bag
;;=> 30
```

To update the agent value, we can use either `send` or `send-off`.

The difference is that `send` will use a fixed-size thread pool (in which the
number of threads is slightly greater than the number of physical processors),
while `send-off` an expandable thread pool. You should not use `send` for
blocking operations, or other agents may not be able to make progress. With
`send-off` you can use actions that may block, like reading a file, because
each task will get potentially a dedicated thread. More info
[here](http://stackoverflow.com/questions/1646351/what-is-the-difference-between-clojures-send-and-send-off-functions-with-re)
and [here](http://clojure-doc.org/articles/language/concurrency_and_parallelism.html#agents).

```clojure
(defn number-of-marshmallows-left
  [marshmallows-eaten]
  (send marshmallows-in-the-bag #(- % marshmallows-eaten)))
```

If you call the `number-of-marshmallows-left` function,
there's a possibility that the value is still not updated.

```Clojure
@marshmallows-in-the-bag
;;=> 30
```
Few seconds later...

```Clojure
@marshmallows-in-the-bag
;;=> 20
```

## Refs

_Refs_ are ideal for coordinated changes, that is a change that involves two or more identities.

Let's imagine for example that at the Halloween party there is one bag with gummy bears and another one with marshmallows.
Simon's Cat and Simona are eating from the both bags.
So when a cat takes a candy, we need to update both the state of candies eaten
by the cat as well as the state of candies in the bag.

How can we model this situation? We will use _refs_.

First we need to define the _refs_ for `marshmallow`, `gummy-bears`,
`simons-cat` and `simona`.

```clojure
(def marshmallow (ref {:name "marshmallow"
                       :count-in-the-bag 30}))

(def gummy-bears (ref {:name "gummy bears"
                       :count-in-the-bag 20}))

(def simons-cat (ref {:candies-eaten 0}))

(def simona (ref {:candies-eaten 0}))
```

We can dereference _refs_ as usual:

```clojure
@simona
;;=> {:candies-eaten 0}
```

Now we are prepared for the transfer (move one candy from the bag to... cat's stomach).
We need to decrease the items in the bag and increase the number of candies
eaten by a cat _at the same time_.
We modify _refs_ using `alter` and we need to wrap the whole process in a `dosync`
block. `dosync` is one of the Clojure [STM](http://clojure.org/reference/refs) primitives.
It means that _refs_ will be updated in a transactional, coordinated way.

```clojure
(defn eat-candy
  [cat candy]
  (if (> (:count-in-the-bag @candy) 0)
    (dosync
     (alter candy update-in [:count-in-the-bag] dec)
     (alter cat update-in [:candies-eaten] inc))
  (println (str "No candies in the " (:name @candy) " bag"))))
```

Let's see what happens when Simona eats one gummy bear:

```clojure
(eat-candy simona gummy-bears)
;;=> {:candies-eaten 1}

@simons-cat
;;=> {:candies-eaten 0}

@simona
;;=> {:candies-eaten 1}

@marshmallow
;;=> {:name "marshmallow", :count-in-the-bag 30}

@gummy-bears
;; => {:name "gummy-bears", :count-in-the-bag 19}
```

<div>
<div id="simons-text-end">

We can see that both `simona` and `gummy-bears` refs were updated correctly.

That's all. I hope you learned something new about state management and if not
at least now you know that cats love candies and some of them are scared of spiders :)

</div>
<div id="simons-pic-end">
![halloween-simons-cat](../img/simons-halloween.png)
</div>

## Links

- [http://blog.clojuregirl.com/clojure_threads.html](http://blog.clojuregirl.com/clojure_threads.html)
- [http://clojure.org/reference/refs](http://clojure.org/reference/refs)

</div>
