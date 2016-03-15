{:title "Clojure Explained - In the world of functions"
 :description "For many Clojure is not only a new language but a new way of thinking. Let's start exploring some of the concepts that are hard to grasp when you first encounter this language."
 :layout :post
 :tags  ["Clojure" "ClojureScript" "Fundamentals"]
 :toc false
 :author "Tomasz Biernacki"
 :image "clojure-explained-functions.png"}

<div class="alert alert-info fade in">
  <a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>
  This post has been originally posted on the
  <a href="http://blog.scalac.io/2015/12/14/clojure-explained-recursion.html">
  Scalac blog</a>.
</div>

## Why this post?

Learning a new skill which is not related to anything you already know can be
challenging. You have to overcome many obstacles from the very beginning because
everything is different. The new ideas and concepts are often confusing and seem
counter-intuitive.

Consider speaking a new language. If it's in the same language family, as the 
one you already know, you can use your previous knowledge. If it's not - you 
have to learn from scratch. If you already know Italian for example, learning a 
similar language like Spanish will be fairly easy. Even learning German, which 
belongs to another language branch, wouldn’t be that hard neither. That's 
because all the mentioned languages belong to the same 
[Indo-European](https://en.wikipedia.org/wiki/Indo-European_languages)
language family, so even if they are clearly different, they also share many 
common aspects. Sticking with our example, consider now learning Mandarin 
Chinese (or vice versa): there are less concepts you can relate to, and not 
only the words and grammar are different but even the writing system and the 
culture (which influences the language) are totally new.

We can use this analogy in the world of programming languages, too: if you 
already know Python, learning Ruby will be easy. Even switching from Java to 
Python is not that complicated: you need time to learn the syntax and some 
technical details but many concepts like variables, classes, loops are 
basically the same. 

And here comes Clojure: if you have an imperative, OOP background, like many of 
us have, the challenge consists not only in learning a new language but a new 
way of thinking, a new programming paradigm.

If you have searched for Clojure before you could have found that it is a 
general-purpose, dynamic programming language with the following characteristics:

* it's a functional language
* encourages immutability
* provides the REPL, making development more interactive
* it's a hosted language (JVM, CLR, ClojureScript for the browser)
* being a dialect of Lisp, it allows you to write powerful macros
* simplifies multi-threaded programming

You can find more information [here](http://clojure.org/features) and 
[here](http://clojure.org/getting_started).

Considering that many programmers are intimidated by the parenthesis alone,
I decided to write a series of articles to help you switch your thinking to the 
Clojure way.

Ideally you are already familiar with the basis of the language and you know how
to use the REPL. If  this is not the case please read first
[this tutorial](http://learnxinyminutes.com/docs/clojure/) and 
[this one](https://github.com/shaunlebron/ClojureScript-Syntax-in-15-minutes).

You can use the standard Clojure REPL to follow along or experiment with 
[our ClojureScript repl](http://www.clojurescript.io).

## Functions

### What we want to do

In this first post we'll be writing our own implementation of one of the core 
Clojure functions: `map`. Along the way, I'll explain what functions and 
higher-order functions are, how we can use recursion to iterate through a 
collection, and why we'd want to use lazy sequences. 

First let's understand what we want to achieve. `map` is a built-in function 
that takes as arguments another function `f` and a collection `coll`. It then 
builds a new collection by applying `f` to each element. To clarify, let's see 
an example: 

<script src="https://gist.github.com/tomasz-biernacki/d3c0270b73dc465db141.js">
</script>

Here `map` takes the elements `1 2 3`, increments each one (`inc` function)
and returns a new collection with the incremented values `2 3 4`.

So let's build step by step our own implementation of `map`.

### Functions and higher-order functions

In OOP, the building blocks of an application are classes: when we need to solve
a problem we usually think of creating a new class, adding some fields and 
methods, then maybe create a bunch of other classes and connect them together.

In Clojure on the other hand, and FP in general, functions do the heavy lifting.
You pass them data as input. They give you the result as output. This output can
be passed as input to another function and so on until you find what you were 
looking for.

Of course this is a simplification, but it's also the first step to "get" 
Clojure.

Let's define our first function. In Clojure we do that with the `defn` macro.

<script src="https://gist.github.com/tomasz-biernacki/c56bda08a7b40c16b4a2.js">
</script>

The function is called `compute-number` (line `1`) and has one parameter
`n` (inside square brackets on line `2`). The return value of the function is the
last evaluated expression and in our case is the incremented number (line `3`).
You can see that by calling `compute-number` with *10* as argument we get back 
*11* (lines `5-6`).

Functions are so important and central that they are treated like any other 
value: for example we can pass them as arguments to another function. Let's try
to do that in `compute-number`:

<script src="https://gist.github.com/tomasz-biernacki/29d67dbf8f5dbaf15833.js">
</script>

On line `2` we added a parameter `f` inside square brackets and we expect it to
be a function. Then on line `3` we call `f` with `n` as argument. Line `2` and 
`3` are similar but the former is the parameter list and the latter is the actual
function call. The return value of `compute-number` will depend on what function
we pass to it and you can see that in the examples on lines `5`, `8` and `11`.
The first time we increment `n` (passing the built-in function `inc`), than we 
decrement it (`dec`) and in the last example we ask if *10* is even, which of 
course is true.

Passing functions as arguments is common practice and this is exactly what is 
happening in the `map` function we saw earlier. Let's see another similar 
example, `filter`:

<script src="https://gist.github.com/tomasz-biernacki/973e1231150b49aa8ba0.js">
</script>

`filter` returns the items in a collection for which the function passed as first 
parameter returns true, in this case `odd?`, and we can see that the new collection
consists only of odd numbers.

Functions like `map` and `filter` (and also `compute-number`) are called 
[higher-order functions](https://en.wikipedia.org/wiki/Higher-order_function), 
because they take another one as argument.

We are ready now to start writing our own implementation of `map`:

<script src="https://gist.github.com/tomasz-biernacki/d2fa1c069f1a4dd03d7f.js">
</script>

The function will be called `my-map` (line `1`) and will take a function `f` and a 
collection `coll` as parameters (line `2`). Since we don't know yet how to 
iterate through a collection let's limit ourself to applying `f` to the first 
element (line `3`): we can see in the examples on lines `5` and `8` that we were
able to call the function correctly, the first time incrementing the first element
and the second time decrementing it. 

Now let's see how we can apply `f` to each element.

## Recursion

To iterate through a collection in languages like Java or C the idiomatic way is
to use a `for` loop. Clojure has no for loop as we know it from imperative 
languages and no direct mutable variables.
We need to use recursion instead. You probably already know that a recursive
function is a function that calls itself directly or indirectly. 
But what does it mean? How can we apply recursion to iterate through a collection?

First we need a way to isolate one element at a time: we already did this
with the first element, but we need to isolate the second, the third and so on.
It turns out that Clojure provides another useful function, `rest`, to retrieve
all the elements after the first. 

So let's consider as example the list `'(1 2 3 4)` and let's try to use `first`
and `rest` to manually iterate through each element: 

1. at the beginning, `first` will return `1` and `rest` will return a new 
collection `'(2 3 4)`.
2. with this new collection, `first` will be `2` and the `rest` will be `'(3 4)`
3. `first` will be `3` and `rest` will be `'(4)`
4. `first` will be `4` and `rest`, since there are no more elements, just an 
empty list, `()`
5. the empty list is passed: `first` will be `nil`...and `rest`? `rest` will be 
again an empty list.

Sooner or later we need to stop this process, and in our case it's when the list 
is empty. We can see that what we did is recursive by nature: we are using `first`
and `rest` again and again, but each time with a different input. We are taking
the first element from a collection, doing something with it, and repeating the 
same process for the rest of the collection.

<script src="https://gist.github.com/tomasz-biernacki/2a0dc6d5baa994336421.js"></script>

A recursive function is tricky because many things are happening in the same 
place: if the collection is not empty (line `3`) go ahead, do something with the
first element (e.g. apply a function to it, line `4`) and than process the rest
of the collection recursively, using the same `my-map` function (line `5`). If 
the collection is empty, return it (line `6`) and end the recursive calls. Note 
that `(seq coll)` is just the
[idiomatic way](https://clojuredocs.org/clojure.core/empty_q) to check if the
collection is not empty.

For now we need to wrap lines `4` and `5` with the special form `do` because
these are two separate expressions. We'll get rid of it in the next step. 
Also note that by calling `(f (first coll))` we create a new value and don't
mutate the original collection in any way. Immutability is a core concept in 
Clojure but it won't be covered in this post.
Let's test our function so far:

<script src="https://gist.github.com/tomasz-biernacki/24e9e0eb2c1ed35e0d2e.js">
</script>

On line `1` we use the `println` function for test purpose and we can see that 
we are getting each element one at a time. We also cover the edge case of an 
empty collection (line `8`).

At this point we know how to pass a function to another one and how to apply it 
to each element but you see in line `11` that we are not getting back a new 
collection. We are applying `f` to a single element but then we are not adding
the new value anywhere. Let's do that.

## Building a new collection

We need a way to build a new collection, and that's what the `cons` function is
for: given an element and a collection, it returns a new collection with that
element added to the front. Another useful and often used function is
[conj](https://clojuredocs.org/clojure.core/conj) but 
[for our last example](#lazy-sequences) we'll stick with 
[cons](http://stackoverflow.com/questions/12389303/clojure-cons-vs-conj-with-lazy-seq) .

<script src="https://gist.github.com/tomasz-biernacki/6fb5f0c18155a36b270e.js">
</script>

You can note the special cases of *consing* an element to an empty list and a 
`nil` value, they will be useful soon.

This is also the trickiest part because since `cons` adds an element to the 
front we need to build our new collection from the last element, and *cons* the 
previous ones backwards.

Consider how we could build `'(11 21 31)` out of `(my-map inc '(10 20 30))`:

1. `'(11 21 31)` is `11` added to the front of `'(21 31)`
2. `'(21 31)` is `21` added to the front of `'(31)`
3. `'(31)` is `31` added to the front of an empty list or `nil`

So what we need to do is `cons` each element to the rest of the collection, 
which will be calculated recursively. This is how our function calls work at 
this moment:

```no-highlight
(1) (my-map inc '(10 20 30))
    |
(2) +----> (inc 10)
(3)        (my-map inc '(20 30))
           |
(4)        +--> (inc 20)
(5)             (my-map inc '(30))
                |
(6)             +--> (inc 30)
(7)                  (my-map inc ()) = ()
```

We need (5) to be the result of *consing* (6) with (7), that is 
`(cons (inc 30) ())` which gives `'(31)`.

Than (3) is the result of *consing* (4) and (5), that is 
`(cons (inc 20) '(31))` which gives `'(21 31)`.

Eventually, (1) is the result of *consing* (2) and (3), that is 
`(cons (inc 10) '(21 31))` which gives the expected result of `'(11 21 31)`. 

Our new version of `my-map` looks like this (note that since `cons` on line `4`
is just one expression we are able to remove the `do` special form):

<script src="https://gist.github.com/tomasz-biernacki/5cf1be3dd90f439c1c38.js">
</script>

It's like starting from one point and by recursive calls going forward

```no-highlight
 * -------> * -------> * ---------> *
 11         21         31           ()
                                    |
                        '(31) <-----+
                         |
             '(21 31) <--+
              |
'(11 21 31) <-+
```

and than going back and *consing* the elements.

We have an additional bonus here: in Clojure `first`, `rest` and `cons` are 
generic functions that work with any built-in data structure, and since we are
using only those functions inside `my-map`, we can pass any collection as 
argument. Before we dive into lazy sequences let's see some examples:

<script src="https://gist.github.com/tomasz-biernacki/b2afe9a120849ada6c40.js">
</script>

I won't cover the topic in this post but it's interesting to note that our 
function works perfectly fine with vectors, lists, sets and any other data 
structure that obeys to the `first/rest/cons` contract. You can read more about
programming to abstractions
[here](http://www.braveclojure.com/core-functions-in-depth/#Programming_to_Abstractions).

## Lazy sequences

There is only one more problem with our implementation: imagine that we have a 
vector which contains the monthly average temperatures for a given city. We'll 
take as example the beautiful city of 
[Gdansk](http://www.holiday-weather.com/gdansk/averages/),
where [our office is located](https://www.scalac.io):
`[-2 -2 1 6 11 14 17 17 12 8 2 -2]`.

We would like to convert these temperatures from Celsius to Fahrenheit, and by
now it works well:

<script src="https://gist.github.com/tomasz-biernacki/791c9e220d237d0c24ae.js">
</script>

But what if we needed only the first 3 elements? Maybe we are planning a holiday 
in winter :)

<script src="https://gist.github.com/tomasz-biernacki/171ec6d3e3c914f3427a.js">
</script>

It's still working but the problem is that even if need only the first 3 
elements, `my-map` will compute the new values for the entire collection, and we 
don't want that.

Clojure helps us with the idea of lazy sequences: these are sequences whose 
items are evaluated only when needed. A lazy sequence can be thought of as a 
recipe for retrieving the next elements, which will be computed only if we ask 
for them. Another common way of saying that the elements are computed is to 
say that they are realized. In our example we need only three elements, so only 
three will be realized. It's also very simple to create a lazy sequence: all we 
need to do is to wrap our function body with the `lazy-seq` macro.

<script src="https://gist.github.com/tomasz-biernacki/b37e7982cd5516b78f2e.js">
</script>

You can note that the output on line `9` is the same as before, but this time 
only three elements were actually realized. To verify that you can add a `println` 
inside the function and compare the version with `lazy-seq` and the one without.

<script src="https://gist.github.com/tomasz-biernacki/7ae3c00e0a42baa15291.js">
</script>

This of course is a trivial example but it's easy to imagine the benefit of lazy 
sequences when we are dealing with *larger* collections. Keep also in mind that 
many Clojure built-in functions that produce lazy sequences don't realize one 
element at a time like in our example. Elements are rather realized ahead of 
time in chunks (32 elements at a time). This is done for performance reasons 
and it won't be covered here. 
You can read more [here](http://clojure-doc.org/articles/language/laziness.html).

### Final note

In our own implementation we have the *else* branch because when an empty 
collection is passed as argument we want to return it back instead of `nil`. But 
with lazy sequences we don't need to do that because `(lazy-seq nil)` is equal 
to `()`, so we can change the `if` to `when` and get rid of one line. 

<script src="https://gist.github.com/tomasz-biernacki/cb11481365077cbd403f.js">
</script>

All the examples above will work and the above will be our last version.

## Conclusion

Wow! What a fantastic journey, just to explain 5 lines of code :)

We saw that functions are the core elements of Clojure and that we can pass them 
as input to other functions. Then we used recursion to "loop" through a 
collection and used `cons` to build a new one. And finally we made our sequence 
lazy in order to avoid computation where not needed.

Thanks for reading and happy Clojure hacking!

