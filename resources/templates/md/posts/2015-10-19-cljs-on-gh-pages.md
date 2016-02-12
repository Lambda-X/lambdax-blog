{:title "How we embedded ClojureScript in our GitHub pages"
 :description  "A brief guide on how to write ClojureScript blog posts if you are using GitHub pages."
 :layout :post
 :tags  ["Clojure" "ClojureScript" "GitHub" "Reagent"]
 :toc false
 :author "Andrea Richiardi"
 :image "require-cljs.png"}


Ever wondered if you can embed ClojureScript in a GitHub blog? The answer is:
 of course you can! At the end of the day, ClojureScript just translates to
 plain old JavaScript that can be included in any web page.
 
## Why would you want that?

Here at Scalac, we are always trying to be innovative and creative in
everything we do. This is why when I heard that we were planning to write some
blog post on Clojure/ClojureScript, I asked myself: why can't I write a
ClojureScript post in ClojureScript itself?

The advantage of this is evident, our pages will be way more interactive and
therefore interesting to read, play with and learn from.

## How does the ClojureScript compiler work?

In order to start, I want first to give the reader some general notion on how
the compiler works and why it is so powerful. I am not going to spend too much
time on it because you can find better detailed articles out there.

The most significant difference between Clojure and ClojureScript is that
ClojureScript concretely isolates the code interpretation phase (reading in
Lisp terms) from the actual compilation phase (analysis and emission). The text
that a programmer writes is read by the ClojureScript reader, then passed to
the macro expansion stage and ultimately to the analyzer. The outcome is the
*abstract syntax tree (AST)*, a tree-like metadata-reach version of it.

This is paramount for a bunch of obvious reasons, but mostly because you have
separation of concerns between tools that understand text and tools that
understand ASTs, like compilers. Indeed this is why ClojureScript can actually
target [disparate](http://cljsrn.org/)
[kinds](https://github.com/mfikes/planck) of
[platforms](https://github.com/takeoutweight/clojure-scheme): in order to emit
instructions belonging to the `X` programming language you "only" need your own
compiler to read the AST, prune it and emit. This is also how currently the
Google Closure Compiler is employed in order to emit optimized JavaScript.

## Automating post creation

GitHub blog posts are nothing but standard `.md` files that Jekyll builds
locally and then serves. The Markdown syntax allows adding standard HTML tags
and consequently JavaScript `<script>` tags. Therefore, embedding ClojureScript
was relatively easy once I got pass configuring it. This is
mainly why I thought writing a blog post might be a good idea and save some
folk's time.

I wanted to be able to plug my changes in transparently, still allowing to
create posts without Clojure. A simple `start.sh` was previously used to create
`.md` files in Jekyll's `_posts` folder that then had to be worked on by the
author and committed.

Instead, I needed to create fully-fledged ClojureScript projects, potentially
one per blog post, somewhere. I chose to hide them in a brand new `_cljs`
folder as `git` submodules and for this reason I added a few lines to
`start.sh` for:

1. Performing `git submodule add`. 

2. Materializing the project with `lein new <template> <project-name>`.

3. As before, copy the `.md` template to `_posts`.

Now switching to the project folder was showing me the reassuring sight of the
`project.clj` file.

## Configuring and building

I wanted to be smarter about emitting JavaScript and tried to deploy directly
inside Jekyll's `scripts` folder. According to my template default, the
JavaScript was emitted to `<project-name>/resources/public/js/compiled` so I
changed my `<project-name>/project.clj` to:

<script src="https://gist.github.com/pjazdzewski1990/08a003abc428dded1f40.js"></script>
    
Remember that we are inside a project folder under `_cljs`, therefore Jekyll's
root is two directories up. Typically, only `:output-to` is significant for the
final version as this option contains the path of the generated (and minified)
`.js` file. In `jekyll-dev` though, you can also specify `:output-dir`, which
is where <q>temporary files used during compilation</q> are written, and
`:asset-path`, that sets where to find `:output-dir` files at run-time. This
way you have full visibility of the output.

Now I was finally able to `cd` to my project, execute `lein cljsbuild once
jekyll`, and see my generated `.js` in `scripts`. Hooray!

## The magic bit

The last piece of the puzzle was to run the actual JavaScript code inside the
Markdown blog page. There are many ways to do this, but the one I found most
intuitive and straightforward was by using
[reagent](https://github.com/reagent-project/reagent). This is not a post about
`reagent` per se (we wrote about it some time
[ago](http://blog.scalac.io/2015/04/02/clojurescript-reactjs-reagent.html)),
but its lean and unopinionated architecture struck me as <q>the way to
go</q>. Reagent, a React wrapper, dynamically mounts DOM elements and
re-renders them when necessary, effectively hiding the complications of
managing React component's life cycle.

Consequently, on the HTML side I needed to: define a <q>mounting point</q>,
include the compiled `.js` and trigger the JavaScript `main()` which mounts my
app. My `.md` became:

<script src="https://gist.github.com/pjazdzewski1990/f10d270c5232f145d13c.js"></script>

Note that it is very important to prepend a slash to `script` and replace
dashes with underscores in the last JavaScript call. The reason is that the
compiler always transforms namespace dashes in underscores.

On the ClojureScript side instead I needed to ensure that the `<div>` with id
`cljs-on-gh-pages` was correctly mounted:

<script src="https://gist.github.com/pjazdzewski1990/f63d42e2347035916792.js"></script>

Now every time the blog post page is shown, `reagent` intercepts the `div` and
renders anything our `main()` returns, typically
[Hiccup](https://github.com/weavejester/hiccup)-crafted `react` components,
like `page` above.

## The perk

If you have had the patience of reading till the end, here is a reward for you: a
**ClojureScript REPL** to toy with!

Thanks to highly skilled
[ClojureScript](https://github.com/swannodette/cljs-bootstrap)
[hackers](https://github.com/kanaka/cljs-bootstrap) and Clojure being a
[homoiconic](https://en.wikipedia.org/wiki/Homoiconicity) language, it is not
surprising that it can compile itself and run into a self-hosted environment.

Self-hosted means that the language provides itself the environment where to
run, which in this case is the set of JavaScript functions that are performing
the code evaluation. The JavaScript, in turn, being compiled from ClojureScript
source. Convoluted and awesome.

Not everything works in this <q>ClojureScript-in-ClojureScript</q> habitat at
the moment. However, thanks to other inspiring implementations, here too
you have access to Clojure's superpowers. Note that the REPL has history (`up`
to start, `up`/`down` to navigate) plus
[other handy shortcuts](https://github.com/replit/jq-console#default-key-config). Enjoy!

<br/>

<script src="/scripts/cljs-on-gh-pages/js/jquery.min.js" type="text/javascript" charset="utf-8"></script>
<script src="/scripts/cljs-on-gh-pages/js/jqconsole.min.js" type="text/javascript"></script>
<script src="/scripts/cljs-on-gh-pages/js/jq_readline.js" type="text/javascript"></script>
<script src="/scripts/cljs-on-gh-pages/js/repl-web.js" type="text/javascript"></script>
<script src="/scripts/cljs-on-gh-pages/js/repl-main.js" type="text/javascript"></script>

<div id="cljs-on-gh-pages"></div>
<script src="/scripts/cljs-on-gh-pages/js/compiled/cljs-on-gh-pages.js"></script>
<script>cljs_on_gh_pages.core.main();</script>
    
<link rel="stylesheet" type="text/css" href="/scripts/cljs-on-gh-pages/styles/console.css" />

<br/>

## Links

- [http://blog.fogus.me/2012/04/25/the-clojurescript-compilation-pipeline](http://blog.fogus.me/2012/04/25/the-clojurescript-compilation-pipeline)
- [http://www.infoq.com/presentations/clojure-scheme](http://www.infoq.com/presentations/clojure-scheme)
- [https://github.com/clojure/clojurescript/wiki/Compiler-Options](https://github.com/clojure/clojurescript/wiki/Compiler-Options)
- [https://github.com/clojure/clojurescript/wiki/Google%20Closure#compiler](https://github.com/clojure/clojurescript/wiki/Google%20Closure#compiler)
- [https://facebook.github.io/react/docs/working-with-the-browser.html](https://facebook.github.io/react/docs/working-with-the-browser.html)
