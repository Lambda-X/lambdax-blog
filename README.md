# LambdaX Team Blog

We are a fully remote team of skilled software developers dedicated to Clojure,
ClojureScript, and functional programming. Our mission goes behind
self-improvement: we also like to share our knowledge and discoveries with
others, and this is what this blog is all about. To discover more about us
visit our website on [lambdax.io](https://lambdax.io).

# How to blog

Make sure you have [Leiningen](http://leiningen.org/) installed.

Clone the repository and create a branch `your-topic` with the `master` branch
as your starting point.

Create a `.md` file in `/resources/templates/md/posts/` with the following format:
`yyyy-mm-dd-name-of-the-post.md`.

* `name-of-the-post` has to be a meaningful name since will be displayed in the
final URL.
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
which is the name of the file that will be displayed in the preview.
The image should be squared and located in `resources/templates/img`.

The body is markdown.
The parsing library is [markdown-clj](https://github.com/yogthos/markdown-clj).

# Community

Many thanks to @lacarmen for providing
[cryogen](https://github.com/cryogen-project/cryogen), the engine we use for
this blog.
