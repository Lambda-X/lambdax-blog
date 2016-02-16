{:title "Require in self-hosted ClojureScript"
 :description  "An informative tour of ClojureScript's require in Replumb."
 :layout :post
 :tags  ["Clojure" "ClojureScript" "Replumb"]
 :toc false
 :author "Andrea Richiardi"
 :image "require-cljs.png"}

<div class="alert alert-info fade in">
<a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>
  This post has been originally posted on the
  <a href="http://blog.scalac.io/2015/12/21/cljs-replumb-require.html">
Scalac blog</a>.
</div>

Replumb is a new library we decided to hack together as single point of
reference for future implementations of ClojureScript
[self-hosted](https://en.m.wikipedia.org/wiki/Self-hosting_compiler) REPLs. At
the moment its current version is displayed in all its beauty on
[clojurescript.io](http://clojurescript.io/).

The ClojureScript community has already put in a huge amount of effort in order
to introduce this very cool feature of having a
[bootstrapping](https://en.m.wikipedia.org/wiki/Bootstrapping_%28compilers%29)
compiler and as we both are a new member of it and immensely keen on building
cool stuff, we thought we could add some glue for it.

This blog post unveils the magic and the gotchas behind
[require](http://clojuredocs.org/clojure.core/require), the omnipresent
Clojure(Script) function that loads your symbols in the current namespace.

## Require and load namespaces

Everything starts from the amazing job done by David Nolen and Mike Fikes in
the ClojureScript core's namespace `cljs.js`. The namespace contains pretty
much all you need to implement a self-hosted REPL and, needless to say, it is
the main building block of replumb.

When a `require` is triggered, replumb, actually following its
[predecessors](http://blog.fikesfarm.com/posts/2015-10-07-ambly-require-reload.html),
converts it to a `ns` call against the current namespace, so that `cljs.user=> (require 'foo.bar.baz)`
evaluates `(ns cljs.user (:require 'foo.bar.baz)` instead.
 
There is a valuable reason to do this: `ns` is very solid and already handles
things like dependent namespaces loading and `require` option parsing. From
what I gathered David Nolen suggested there was no need to reinvent this
particular wheel and this is very in tune with Clojure's pragmatic design.
 
What happens next is not very surprising: the namespace needs to be mapped to a
file, this file needs to be loaded somehow and forms need to be evaluated (for
example `def` and `defn` will define vars).  

Duly, `cljs.js` abstracts the functionality that all JavaScript
developers miss from their browser toolbox: `IO`. This is even more clear when
reading the first lines of `cljs.js/*load-fn*` docstring:

> Each runtime environment provides a different way to load a library.
> Whatever function \*load-fn\* is bound to will be passed two arguments - a
> map and a callback function: The map will have the following keys:
>
>     :name   - the name of the library (a symbol)
>     :macros - modifier signaling a macros namespace load
>     :path   - munged relative library path (a string)

Replumb takes the same approach, as not having ties with the environment in
which you execute is a good thing, but it does better.
 
Alongside with the `replumb.core/read-eval-call`'s option `:load-fn!`, which
entirely replaces `cljs.js/*load-fn*`, since version `0.1.3` replumb can be
customized with what is the very basis of loading: a function that given a file
path, returns its content.

The name of the option is `:read-file-fn!`, an asynchronous 2-arity function
with signature `[file-path src-cb]`. The function will receive the complete
file path and a callback `src-cb` that should be called with either the file
content or `nil` as argument.

By using this option replumb users can gloss over the repetitive but necessary
logic of converting the namespace to the file path for instance and
[the rest of the *load-fn* protocol](https://github.com/clojure/clojurescript/blob/r1.7.170/src/main/cljs/cljs/js.cljs#L59)
can be internally handled more in a more robust way.

The two handy
[replumb.core/browser-options](https://github.com/ScalaConsultants/replumb/blob/0.1.3/src/cljs/replumb/core.cljs#L129)
and
[replumb.core/nodejs-options](https://github.com/ScalaConsultants/replumb/blob/0.1.3/src/cljs/replumb/core.cljs#L169)
were also born to provide an easy external API for it.

## The source job

We already know that namespace segments map to a precise file path and we can
now read that file, but we need to know the root folder in which look for our
files.

Even in this case, replumb has a configurable key in its option map:
`:src-paths`. It accepts a sequence of strings which represent file paths. Note
that it *has* to be
[sequential](http://clojuredocs.org/clojure.core/sequential_q) or no
`*load-fn*` will be added, resulting in the dreaded `"No *load-fn* set"` error.

This opens up another big chapter in the require story, which is how to provide
the source files to our environment. It should be now clear that in order for
this to work:

<script src="https://gist.github.com/arichiardi/808b2cf9a60273b60cf9.js"></script>

The file `clojure/string.cljs` should be available in `:src-paths` or replumb
won't be able to employ its `:read-file-fn!` on it in order to read the
docstring for `trim`.

For the seasoned ClojureScript developer this might trigger one or two alarm
bells. ClojureScript core did so much in order to integrate the Google Closure
compiler so that it could shed all of the unused dependencies and now you
are telling me that I need them even if I don't use them?

Yes, this is the biggest disadvantage of embedding a REPL in your client-side
JavaScript app. You need all your source files for `doc`, `source` and other
REPL perks to work. Not only that, in the particular case of `clojure.string`
you also need the Google Closure library. The reason is obvious if we peek
[under the carpet](https://github.com/clojure/clojurescript/blob/r1.7.170/src/main/cljs/clojure/string.cljs#L11):
`goog.string` is imported as dependency, and might potentially import other
Google Closure libraries.

In replumb and [clojurescript.io](http://clojurescript.io) we met and solved
this in a hackish way. The ClojureScript compiler, if `:optimizations` is set
to `:none`, creates the whole set of dependencies for you and copies
them in the specified `:output-dir` folder. We only then needed to mirror this
folder on our web server (it could have been any remote location) and implement
a
[file fetching](https://github.com/ScalaConsultants/replumb/blob/0.1.3/src/browser/replumb/browser/io.cljs#L4)
`:read-file-fn!`.

The problem is that these files were just our app dependencies. We were still
missing, for instance, the
[Clojure core namespaces](https://github.com/clojure/clojurescript/wiki/Differences-from-Clojure#other-libraries)
that we were not explicitely requiring. So a better solution still needed to be
thought of.

## A Possible Solution

[Planck](https://github.com/mfikes/planck) employs the best solution at the
moment embedding everything in its executable file. In a browser of course you
have to minimize the size of the files you serve and moreover you have the
cloud at your disposal.

An idea would be then to simply centralize in a kind of oracle all the
necessary source files needed for all the REPL apps out there.

The solution of course is costly in terms of maintenance and for this reason it
should be shared by the whole Clojure community. However, we have already
examples like [Clojars](https://salt.bountysource.com/teams/clojars) and
[cider](ofhttps://salt.bountysource.com/teams/cider) which are crowd-founded
and self-hosting REPL could be part of this world as well.

## Conclusion

To wrap up, self-hosted solutions are definitely worth exploring as they
educate on how things work under the hood. 

We just scratched the surface, but enough to appreciate what `require` needs in
order to work, which is very nice and useful.

We now understand that a fully functioning REPL in your client-side code needs
some effort but that replumb can aid you in that.

That's all folks!

## Links

- [Cljs.js source](https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/js.cljs)
- [Namespace to file path](http://www.braveclojure.com/organization/#Real_Project_Organization)
- [Replumb repo](https://github.com/ScalaConsultants/replumb)
- [Wiki page (same content)](https://github.com/ScalaConsultants/replumb/wiki/Require-and-providing-source-files)
