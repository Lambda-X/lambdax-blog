{:title "Clojure Explained - Macros demystified"
 :description "All Lisp-based languages share a common feature called macros.
Often seen as magical, macros are actually based on a simple idea: to be able
to transform arbitrary expressions into valid code, just before it is evaluated
by the compiler. Let's look at how macros work in Clojure in more detail."
 :layout :post
 :tags  ["Clojure" "Fundamentals"]
 :toc false
 :author "Tomasz Biernacki"
 :image "clojure-explained-macros.png"}

## Macros to the rescue

In the [last post](http://lambdax.io/blog/posts/2015-12-03-clojure-explained-recursion.html)
we learnt how functions are the main building blocks of any Clojure application
and how easy is to compose them thanks to higher-order functions.

In this post we'll go back to the basis and examine what Clojure code is actually
made of and why we should care. We'll discover what _macros_ are and how they fit
into the Clojure evaluation model.

Macros are really the killer-feature of Lisp-based languages and after reading
this post you should have a better understanding of the concepts behind them.
Even though I will provide some hands-on examples this post is *not* about
writing macros: the goal is to lay a conceptual foundation for the subject, and in
the [links section](#conclusion_and_links) I will suggest materials for further
reading.

## Code is data, data is code

The Clojure syntax, mostly inherited from Lisp, is pretty simple. Actually, one
could argue that there's no syntax at all. When you write Clojure code your write
valid _forms_ and before I show few examples let's have a quick recap:

<div class="forms">

| Form       | Examples                                  |
| ---------- | ----------------------------------------- |
| Boolean    | `true`, `false`                           |
| Character  | `\a` `\b` `\c`                            |
| Keyword    | `:a` `:b` `:key`                          |
| List       | `(1 2 3 4 5)`, `(\c :a-keyword "string")` |
| Map        | `{:name "Tomek" :surname "Biernacki"}`    |
| Nil        | `nil`                                     |
| Number     | `1` `2.3`                                 |
| Set        | `#{:banana :apple :orange}`               |
| String     | `"Hello, world!"`                         |
| Vector     | `[10 20 30]`                              |
| Symbol     | `user/a-symbol` `point`                   |

</div>

For a remainder and detailed explanation of each form see
[here](https://en.wikibooks.org/wiki/Learning_Clojure/Data_Types).

As we will see soon, the **list** is a very important form.

Let's see [an example](http://blog.jayfields.com/2010/07/clojure-destructuring.html)
of Clojure code and try to examine it closer.

```clojure
user=> (def point {:y 7})
#'user/point

user=> (let [{:keys [x y] :or {x 0 y 0}} point]
         (println "x:" x "y:" y))
x: 0 y: 7
```

If you look at the table above and then at the example you might recognise a pattern
here. From a syntactic point of view, `(def point {:y 7})` is nothing more than
a *list* made of three elements: two _symbols_, `def` and `point`, and a _map_, which
in turn contains a _keyword_ and a _number_.

The `let` construct follows the same pattern: everything is inside a _list_,
which also is made of three elements: the _symbol_ `let`, a _vector_ and another
_list_.
The vector contains a _map_ and a _symbol_ (`point`). The map contains a _keyword_,
and a _vector_ and so on and so on.

Here comes the first important realisation: **Clojure code is made of its own forms**.

The **list** is a very important one because it can hold both code that will be
evaluated as well as simple data:

```
user=> (map inc '(1 2 3))
(2 3 4)
```

Again, from a syntactic point of view what we see is just a _list_ made of three
elements: the _symbols_ `map` and `inc`, and another _list_ `'(1 2 3)`.
**Clojure will treat every list as code**, unless we indicate not to do so.
One way is to prefix the list with the `'` character (more precisely, a reader
macro). The outermost list will be treated as code, while the innermost
just as plain data.

At this point what you should understand is that every piece of code is made
of Clojure forms, and in particular the list has the job of both representing
data as well as code.

Let's see in more detail what are the different phases of compilation and how
Clojure evaluates these lists.

## The evaluation model

The Clojure compiler does its job in two **separate** and **independent** phases:
first, a stream of characters is passed to the _reader_ (from a source file
or the REPL) which transforms it into Clojure forms. Then, these forms
are passed as input to the _evaluator_ which, as the name suggests, evaluates
them and produces a result.

### The reader

The beauty of Lisp, and hence Clojure, is that we can interact with the reader and
evaluator from our code. Let's start with the reader by using the `read-string`
function:

```clojure
user=> (read-string "(* 10 10)")
(* 10 10)
```

You can note that the input is a string, in this case `"(* 10 10)"` and the resulting
form is the list `(* 10 10)`. Please note that even if they have the same representation
they are not the same: a string is just a series of characters, while the output
form is an in-memory list.

Probably you have also encountered in Clojure expressions like `@value` or
`#(do-something %)`. These are just syntactic sugar to make Clojure code more
succinct:

```clojure
user=> (read-string "@value")
(clojure.core/deref value)
```

This feature goes under the name of _reader macros_ (not to confuse with macros) and
allow us to use _macro characters_ like `@` or `'` to abbreviate _reader forms_ that
will be then expanded by the reader.
You can find more details [here](http://clojure.org/reference/reader#macrochars).

The important idea is that the reader transforms text into forms, a tree-like
structure made of Clojure lists, and this structure can be passed to the next
phase.

### The evaluator

The job of the _evaluator_ is to take a valid form and evaluate it accordingly
to certain rules. For a more detailed explanation about the evaluation rules
see [here](http://www.braveclojure.com/read-and-eval/#The_Evaluator),
but in short, the evaluation rules depend on the type of the form:

* if it's a _symbol_, Clojure will perform a var lookup and determine what that
 symbol refers to, which can be a special form like `if`, a local binding or a var
 defined by `def`:

```clojure
user=> (def a 33)
#'user/a
user=> a
33
```

* values like strings, numbers and keywords evaluate to themselves:

```clojure
user=> 1
1
user=> :keyword
:keyword
user=> {:a 2}
{:a 2}
```

* if it's a **list**, Clojure will take the first argument and perform a call to
 a function. All the arguments will be evaluated recursively until the final
 value is returned. An exception to that are _special forms_, like `if` of
 `def`, that, as the name suggests, perform special behaviour (usually they are
 implemented at the very core of the language). Another exception is a macro call,
 but we'll see that in the next section. The main difference is that with functions
 all arguments are fully evaluated, while with special forms and macros it's not
 the case (in macros for example symbols are _not_ resolved).

```clojure
user=> (+ 1 2)  ;; this is just a list, but Clojure knows that we want to evaluate it,
                ;; so it takes the first argument `+`, which refers to the addition
                ;; function, and calls it with the arguments 1 and 2, producing the
                ;; value 3
;;=> 3

user=> (+ 1 (* 2 3))  ;; this will be evaluated recursively as (+ 1 6), which will
                      ;; yield the value 7
;;=> 7

;; `if`, a special form in Clojure - you can note that (println "false") was
;; neither evaluated nor called

user=> (if true (println "true") (println "false"))
true
nil
```

That's it. But the next realisation is that the evaluator does not care whether the
input comes from the _reader_ or not. We stated at the beginning the Clojure
evaluates lists, so why couldn't we create our own list programmatically and
pass it to the evaluator? Of course we can, so let's experiment with the
_evaluator_ by using the `eval` function:

```clojure
user=> (eval (read-string "(* 10 10)"))
100

user=> (def my-list '(* 10 10))
#'user/my-list
user=> my-list
(* 10 10)
user=> (eval my-list)
100
```

In the first example we pass to the evaluator the result of the reader, but in
the second we build the `my-list` list ourselves and we can observe that the result
is identical. That's because the output of the reader is the same as `my-list`,
they are the same form, and that's the only thing the evaluator needs to know.

Basically, we are telling the evaluator to take that list made of data and now
treat it like code, and that's what it does: it evaluates it to the value `100`.

And here is where the magic begins: wouldn't it be even greater if we could
**modify our original list in our program** and only than pass it to the
evaluator? After all, it's simply data...

```clojure
user=> (eval (concat my-list [10]))
1000
```

What just happened here? Well, if we think about it, nothing special at all! It's
really simple: we've `concat` the value `10` to the original list so it has become
`(* 10 10 10)` and only then pass it to the evaluator which unsurprisingly returned
the value `1000`. The bonus we receive is that we can reason about our code as
a data structure that can be manipulated and transformed however we want.

<hr/>

Let's recap what we've learnt so far:

* **Clojure code is made of Clojure forms**, in particular a very important one is
 the **list**. This should be even more clear if we seek what "LISP" derives
 its name from: "LISt Processor". The key point to understand here is that Lisp,
 and also Clojure, uses lists to hold both _a list of things_ (data) as well
 as _code_, and for this reason we can mix them together.
 Code is just data, and data can be treated like code. Languages that have this
 property are called
 [homoiconic](https://en.wikipedia.org/wiki/Homoiconicity#Homoiconicity_in_Lisp).

* The compiler will evaluate lists as code unless we indicate not to do so.
 In order to notify the compiler that we want just a list (and not to evaluate
 it) we need to use the `'` syntactic sugar, which is an abbreviation for `quote`
 (see
 [here](http://stackoverflow.com/questions/3896542/in-lisp-clojure-emacs-lisp-what-is-the-difference-between-list-and-quote)
 for the difference between `list` and `quote`)

* The reader and evaluator perform their job behind the scenes but we
 can interact with them by using `read-string` and `eval`. We saw how
 we can skip the reading part and pass to the evaluator a user defined list.

* Not only that: we can also perform various transformations to the list
 and only then pass it to the evaluator! But working with `eval` directly is
 cumbersome and that's why macros exist.

## Macros and macro expansion

Macros are nothing else than a mechanism to transform an arbitrary expression
into valid Clojure code. This way we can extend the language however we want.

Instead of working directly with `eval` we can just define a macro with `defmacro`
and the compiler will do the rest of the work behind the scenes.
Macros are expanded during a phase which is called _macro expansion_ which is
performed just before the evaluation phase. Let's see an example:

```clojure
user=> (defmacro inc10
        [n]
        (list + 10 n))
#'user/inc10
user=> (macroexpand '(inc10 8))
(#function[clojure.core/+] 10 8)
user=> (inc10 8)
18

```

We define a macro with `defmacro` and it will accept a parameter `n`. The macro
will return a list which represents valid Clojure code and we can check that with
the `macroexpand` function. The output list of `macroexpand` is the actual list that
will be passed to the evaluator that in this case will evaluate `(+ 10 8)`, producing
the value `18`. Also remember that, as we stated above, the parameters are _not_
evaluated, which is one of the main differences between macro and funtion calls:

```clojure
user=> (macroexpand '(inc10 (+ 1 2)))
(#function[clojure.core/+] 10 (+ 1 2))
```

### An example from the Clojure core

If you wonder why we might need such a strange feature consider the `when` ...macro.
Yes, it's a macro!

```clojue
user=> (clojure.repl/source when)
(defmacro when
  "Evaluates test. If logical true, evaluates body in an implicit do."
  {:added "1.0"}
  [test & body]
  (list 'if test (cons 'do body)))
nil
```

When you write an `if` expression you can only evaluate one form if the
condition is true, and if you need more you have to wrap them in a `do`:

```clojure
user=> (if true
         (println "True"))
True
nil
user=> (if true
         (do (print "Absolutely ")
             (println "True")))
Absolutely True
nil
```

But this is such a common pattern that it would be nice to have a construct that
wraps all those forms in a `do` behind the scenes, and that's exactly what `when`
does:

```clojure
user> (macroexpand '(when true (print "Absolutely") (println "True")))
(if true (do (print "Absolutely") (println "True")))
```

So, nothing more than a list transformation. And it's even more clear if you look
at the implementation: `when` is a macro that accepts two parameters: `test` and
a list of forms (`body`). Then it returns a new list with the symbol `if` as first
element, then the `test` and the same `body` list, but with a `do` as first element.
This is also confirmed by the `macroexpand` function.

If you dig more into the standard library you will notice that many "functions"
are not functions, but [macros](http://clojure.org/reference/macros).
Few notable examples are the `->`, `..` and `for` macros.

## Conclusion and links

Macros are often seen as _magical_ but as soon as we realise that Clojure (like
Lisp) uses the same data structure for both code and data everything becomes more
clear. Code and data are interchangeable and macros offer an elegant and practical
way to modify data before it's passed to the evaluator as code.

In this post I just scratched the surface of this subject and from here you
should be comfortable with the following reading list:

* [S-expressions](http://stackoverflow.com/questions/10771107/lisp-list-vs-s-expression)
* [Writing your own macros](http://www.braveclojure.com/writing-macros/)
* [Quick macros overview](https://learnxinyminutes.com/docs/clojure-macros/)
* Chapter 8 of Joy of Clojure for more examples and applications
* [Macros in ClojureScript](https://github.com/clojure/clojurescript/wiki/Differences-from-Clojure#macros)
* [Mike Fikes' Blog](http://blog.fikesfarm.com/index.html) for interesting stuff
 about ClojureScript and macros in ClojureScript

Hope you enjoyed and see you soon!
