{:title "Self-hosted ClojureScript - The AST innards"
 :description "Delving into Replumb's enhancements and the new Abstract
 Syntax Tree facilities for self-hosted ClojureScript environments."
 :layout :post
 :tags  ["Clojure" "ClojureScript" "Replumb"]
 :toc false
 :author "Andrea Richiardi"
 :image "replumb-logo.png"}

The new [replumb](https://github.com/Lambda-X/replumb) `0.2.0` not only
provides a couple of long-standing fixes but also brings to life quite a useful
new namespace - `replumb.ast`. The goal this time is to ease the burden, as the
README states, of dealing with the huge bag of compiler state data. Let's see
how.

## The ClojureScript Abstract Syntax Tree

In order to be DRY, we will just quickly summarize what is an AST, for we have
already had the pleasure of describing it in an
[old blog post](http://lambdax.io/blog/posts/2015-10-19-cljs-on-gh-pages.html).
The AST is none other than another representation of the code you write, and
more specifically a data representation. It is the output of the read plus
analyze phase, it precedes the compilation (or more correctly
[trans-compilation](https://en.wikipedia.org/wiki/Source-to-source_compiler)),
to JavaScript and it is stored in `cljs.env/*compiler*` as part of the compiler
state.

Self-hosted ClojureScript equips the developer with AST super powers: we can
interact with it. With great power comes great responsibility though and we
need to deal with the complexities of the AST ourselves in a bunch of cases
where you wouldn't think it is necessary.

First of all we need to create it and store it in our code as the classic entry
point, `cljs.js/eval`, demands it:

```clojure
;; from clojurescript/src/main/cljs/cljs/js.cljs
(defn eval
  "Evaluate a single ClojureScript form. The parameters:

   state (atom)
     the compiler state

   form (s-expr)
     the ClojureScript source

   opts (map)
     compilation options.

   cb (function)
     callback, will be invoked with a map.

   ...
```

The usual way to do this is to have a
[def](https://github.com/Lambda-X/replumb/blob/0.2.0/src/cljs/replumb/repl.cljs#L27)
in our namespace, historically called `st`, which will contain the AST
atom. Interestingly enough, `cljs.js/empty-state` creates and return the atom
for you by delegating to `cljs.env/default-compiler-env` and then injecting the
whole `cljs.core` in it:

```clojure
;; from clojurescript/src/main/cljs/cljs/js.cljs
(defn empty-state
  "Construct an empty compiler state. Required to invoke analyze, compile,
   eval and eval-str."
  ([]
   (doto (env/default-compiler-env)
     (swap!
       (fn [state]
         (-> state
           (assoc-in [::ana/namespaces 'cljs.core] (dump-core)))))))
  ([init]
   (doto (empty-state) (swap! init))))
```

<div class="greg-box">

For production builds, it is advisable to load the analysis cache for
`cljs.core` in a separate step at runtime. In
[clojurescript.io](http://www.clojurescript.io) we
[dump it](https://github.com/Lambda-X/cljs-repl-web/blob/0.2.6/src/clj/cljs_utils/caching.clj)
beforehand and then
[load it](https://github.com/Lambda-X/cljs-repl-web/blob/0.2.6/src/cljs/cljs_repl_web/io.cljs#L32)
it when the SPA is up.

</div>

Most importantly, we need a way to query the AST and understand what individual
pieces represent. We need a jargon to abstract over it, together with helper
functions. This is exactly what `replumb.ast` provides us.

## The Replumb AST jargon

A big premise of what follows is that the problem that triggered the creation
of this namespace was pretty concrete and the solution was actually obtained
through trial and error. That is to say that once again we really enjoyed
Clojure's data inspection at the REPL: a pretty veiled data structure can be
deftly unfolded in a blink of an eye (and some patience). Another warning, we
have tests in place against this of course, but we are dipping our feet into
implementation details that might change anytime. Unlikely but not impossible.

Back to the problem, in replumb's
[require unit tests](https://github.com/Lambda-X/replumb/tree/0.2.0/test/cljs/replumb/require_test.cljs)
we have always striven to clean up `cljs.user` before moving from one test to
another. The simplest problem to foresee here is that back-to-back `require`s of
the same namespace would have skipped loading from the file-system (the state
is already filled up and why would you do it again after all?). This is only
one of the many annoying quirks we encountered.

A test can of course use the full spectrum of ClojureScript forms and therefore
to solve this we want to be able to remove from the AST any arbitrary
symbol. Replumb has therefore given birth to
[`ns-unmap`](http://clojuredocs.org/clojure.core/ns-unmap) on steroids.

Before proceeding we need to introduce our jargon, which slightly differs from
what you will see in the actual AST state, hopefully improving on it. Each
following example can be tried out at the
[clojurescript.io](http://www.clojurescript.io) REPL.

Every REPL command affects the `:cljs.analyzer/namespaces` value as shown with
the following hack:

```clojure
cljs.user=> (require 'clojure.set)
nil
cljs.user=> (keys (get-in @replumb.repl.st [:cljs.analyzer/namespaces]))
(cljs.user cljs.core clojure.set)
```

Yet this is not the only place where assoc/dissoc is used:

```clojure
cljs.user=> (get-in @replumb.repl.st [:cljs.analyzer/namespaces 'cljs.user :requires])
{clojure.set clojure.set}
```

And things starts to get interesting when `require` has `:refer`:

```clojure
cljs.user=> (require '[clojure.set :refer [difference]])
nil
cljs.user=> (get-in @replumb.repl.st [:cljs.analyzer/namespaces 'cljs.user :uses])
{difference clojure.set}
cljs.user=> (get-in @replumb.repl.st [:cljs.analyzer/namespaces 'clojure.set :uses])
nil
```

The pattern is easy to see, so replumb unsurprisingly baptizes the namespace
where the commands are evaluated in as `requirer-ns`, and the other namespace
`required-ns`. Five "getters" were added in order to avoid repeating the above
over and over, all of them accepting `state` and `requirer-ns` as arguments:

<div class="forms">

| Replumb helper               | AST key           | Return sample                    |
| ---------------------------- | ----------------- | -------------------------------- |
| `replumb.ast/requires`       | `:requires`       | `{clojure.set clojure.set, ...}` |
| `replumb.ast/macro-requires` | `:require-macros` | `{cljs.test cljs.test, ...}`     |
| `replumb.ast/imports`        | `:imports`        | `{Uri goog.Uri, ...}`            |
| `replumb.ast/symbols`        | `:uses`           | `{difference clojure.set, ...}`  |
| `replumb.ast/macros`         | `:use-macros`     | `{is cljs.test, ...}`            |

</div>

The mapping is almost one-to-one but there are some cases where replumb gives a
better idea of what you are taking out of the AST.

<div class="greg-box">

The `import` command in ClojureScript can be quite deceiving. It is demanded to
be compatible with the Google Closure Library, and sometimes it is
[misused](http://clojurescriptmadeeasy.com/blog/when-do-i-use-require-vs-import.html).

Try:

```clojure
cljs.user=> (import 'goog.Uri)
nil
cljs.user=> (get-in @replumb.repl.st [:cljs.analyzer/namespaces 'cljs.user :imports]))
{Uri goog.Uri}
cljs.user=> (get-in @replumb.repl.st [:cljs.analyzer/namespaces 'cljs.user :requires])
{Uri goog.Uri} ;; ???
```

Weird? Replumb takes care of that.

</div>

## Purge entire namespaces

A common operation you'd want to perform on the AST becomes then `dissoc`. Here
too Replumb provides a whole set of battery-included helpers that library users
can employ: `dissoc-require`, `dissoc-macro-require`, `dissoc-import`,
`dissoc-macro` and `dissoc-symbol`.

I agree this is all pretty standard, however it is worth expanding a bit
further on `replumb.ast/dissoc-all`. The docstring explains it all:

> Dissoc all the required-ns symbols from requirer-ns.
> 
> There are five types of symbol in replumb jargon, which loosely map to
> `cljs.js` ones. These optionally go in the type parameter as keyword:
>
> - `:symbol`, the default, for instance my-sym in `(def my-sym 3)`
>
> - `:macro`, which comes from a `(defmacro ...)`
>
> - `:import`, for instance User in `(import 'foo.bar.User)`
>
> - `:require`, which is the namespace symbol in a `(require ...)`
>
> - `:macro-require`, which is the namespace symbol in a `(require-macros ...)`
>
> This is the only function in the lot that also reliably clears
> namespace aliases.

This is a bulk operation, used to remove all the namespace references of a
certain symbol type from the AST, as we can see by peeking at another new
addition in `replumb.repl`:

```
(defn purge-symbols!
  "Get rid of all the compiler state references to required-ns macros
  namespaces and symbols from requirer-ns."
  [requirer-ns required-ns]
  (swap! st #(-> %
                 (ast/dissoc-all requirer-ns required-ns :require)
                 (ast/dissoc-all requirer-ns required-ns :macro-require)
                 (ast/dissoc-all requirer-ns required-ns :macro)
                 (ast/dissoc-all requirer-ns required-ns :symbol)
                 (ast/dissoc-all requirer-ns required-ns :import))))
```

Simple, right? But now test execution is order independent and flying colors,
especially green, are all over the code base. We can even dare publish our
`repl-reset!` in `replumb.core`, enabling REPL implementations to reset
themselves to the initial state.

```
(defn repl-reset!
  "Reset the repl and the current compiler state.

  It performs the following (in order):

  1. removes `cljs.js/*loaded*` namespaces from the compiler environment
  2. calls `(read-eval-call (in-ns 'cljs.user))`
  3. resets the last warning
  4. sets `*e` to nil
  5. resets the init options (the next eval will trigger an init)"
  [opts]
  ;; Clean cljs.user
  (when-not (repl/empty-cljs-user?)
    (repl/purge-cljs-user!))
  ;; Back to cljs.user, has to be first
  (repl/read-eval-call opts identity "(in-ns 'cljs.user)")
  ;; Other side effects
  (repl/reset-last-warning!)
  (repl/read-eval-call opts identity "(set! *e nil)")
  (repl/reset-init-opts!))
```

## Conclusion

We have acquired some new insights about what is inside the Abstract Syntax
Tree for self-hosted ClojureScript solutions and explored the new features of
replumb `0.2.0`. It was a short tour, yet we laid the basis for a far more
powerful set of use cases for our flagship library.

## Links

* [`replumb.core/repl-reset!`](https://github.com/ScalaConsultants/replumb/tree/0.2.0/src/cljs/replumb/core.cljs#L219)
* [`replumb.repl/purge-symbols!`](https://github.com/Lambda-X/replumb/tree/0.2.0/src/cljs/replumb/repl.cljs#L249)
* [Bootstrapped ClojureScript FAQ](https://github.com/clojure/clojurescript/wiki/Bootstrapped-ClojureScript-FAQ)
* [Clojurescript Production Builds](https://github.com/clojure/clojurescript/wiki/Optional-Self-hosting#production-builds)
