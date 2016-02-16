# LambdaX Team Blog

We are a fully remote team of skilled software developers dedicated to Clojure,
ClojureScript, and functional programming. Our mission goes behind
self-improvement and continuos learning: we are also eager to share our
knowledge and discoveries with others, and this is what this blog is all about.
You can find more info about us at [lambdax.io](https://lambdax.io).

## How to blog

Make sure you have [Leiningen](http://leiningen.org/) installed.

Clone the repository and create a `your-topic` branch  with `master`
as your starting point.

Create a `.md` file in `/resources/templates/md/posts/` with the following format:
`yyyy-mm-dd-name-of-the-post.md`.

* `name-of-the-post` has to be a meaningful name since it will be displayed in
the URL.
* The date is also meaningful and will determine the order in which the posts
will be displayed.

Every post consists of two parts: metadata and body.

* The metadata is a regular Clojure map. You can find a detailed explanation
of the available keys
[here](http://cryogenweb.org/docs/writing-posts.html#post_contents).
You can use the following snippet as starting point:

```nohighlight
{:title "Insert you title here"
 :description  "A brief description here"
 :layout :post
 :tags  ["Mind the tags" "Another one"]
 :toc false
 :author "You"
 :image "sample-200px.png"}
```

In addition to the standard keys you should also specify the `:image` value,
which is the name of the file that will be displayed in the home page.
The image should be squared and located in `/resources/templates/img`.

The body is markdown.
The parsing library is [markdown-clj](https://github.com/yogthos/markdown-clj).

For additional info visit the
[Cryogen documentation website](http://cryogenweb.org/index.html).

To run the server locally execute `lein ring server`.

## Blog with ClojureScript

If you additionally want to include custom JavaScript code (generated from
ClojureScript) proceed as follows:

1. Create a private repository in [LambdaX](https://github.com/Lambda-X) with
the same name as you topic branch.
2. Clone it in `/resources/templates/cljs`.
3. Add it as submodule to the blog repository:
`git submodule add git@github.com:LambdaX/<NAME> cljs/<NAME>`
4. Work on your project with ClojureScript.
5. Make sure the the output files will be located in `resources/templates/scripts/<NAME>/`.
You can do it by copying the output folder manually or set the appropriate
flag in your build tool.
6. In your post reference the created scripts as you would do in normal HTML,
keeping in mind the relative paths. If your script contains markdown characters,
for example `my\_init\_module.init()` make sure that your line starts with
the `script` tag and not any other character/tag/word:
`<script>my\_init\_module.init()</script>`
This way that line will be ignored and not interpreted as markdown.

# Community

Many thanks to @lacarmen for providing
[cryogen](https://github.com/cryogen-project/cryogen), the engine we use for
this blog.
