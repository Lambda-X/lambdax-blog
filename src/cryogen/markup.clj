(ns cryogen.markup
  (:require [cryogen-core.markup :refer [markup-registry rewrite-hrefs]]
            [markdown.core :refer [md-to-html-string]]
            [markdown.transformers :refer [transformer-vector]]
            [markdown.common :refer [freeze-string]]
            [clojure.string :as s])
  (:import cryogen_core.markup.Markup))

(defn rewrite-hrefs-transformer
  "A :replacement-transformer for use in markdown.core that will inject the
  given blog prefix in front of local links."
  [{:keys [blog-prefix]} text state]
  [(rewrite-hrefs blog-prefix text) state])

(defn ignore-script-tags
  "When the line begins with a script tag, ignore the parsing. This is useful when
  you have markdown characters in you script tag."
  [text state]
  (if (re-seq #"^<script" text)
    (freeze-string text state)
    [text state]))

(def img-link "<img class=\"header-link-icon\" src=\"/blog/img/link-icon.png\" />")

(defn make-headers-linkable
  "Add to each header a link (hidden image)."
  [text state]
  [(clojure.string/replace text #"(<h[1-6]><a name=\"(.+)\"></a>.+)(</h[1-6]>)"
                           (str "$1&nbsp;<a href=\"#$2\">" img-link "</a>$3")) state])

(defn markdown
  "Returns a Markdown (https://daringfireball.net/projects/markdown/)
  implementation of the Markup protocol."
  []
  (reify Markup
    (dir [this] "md")
    (ext [this] ".md")
    (render-fn [this]
      (fn [rdr config]
        (md-to-html-string
          (->> (java.io.BufferedReader. rdr)
            (line-seq)
            (s/join "\n"))
          :reference-links? true
          :heading-anchors true
          :footnotes? true
          :replacement-transformers (cons ignore-script-tags
                                          (concat transformer-vector [(partial rewrite-hrefs-transformer config)
                                                                       make-headers-linkable])))))))

(defn init []
  (swap! markup-registry conj (markdown)))
