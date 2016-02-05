{:title "Snake the game - a tutorial for ClojureScript and re-frame, part 1/2"
 :layout :post
 :tags  ["ClojureScript" "Reagent" "re-frame"]
 :toc false
 :author "Dajana Stiberova" }

I was thinking about writing a tutorial in order to show people how to create a simple interactive application using ClojureScript/re-frame.
And then I realized that an arcade game would be a great example.
One of the first games I played was Snake.
Most often secretly under the desk during the classes in school on my old Nokia 3310 :)

So I decided to make a virtue of necessity and the result is this tutorial.

The first part of our tutorial has three goals:

1. [Setting up the project](#setting_up_the_project)
2. [Creating the data model](#creating_the_data_model)
3. [Implementing the static view](#implementing_the_static_view)

## Setting up the project

Download and install [leiningen](http://leiningen.org/).

Create the project by running `lein new figwheel snake-game` in your console.

In your [project.clj](https://github.com/Lambda-X/snake-game/blob/v1.0/project.clj#L10#L11) add to `:dependencies` reagent `[reagent "0.5.0"]` and re-frame `[re-frame "0.6.0"]`.

Rewrite your `core.cljs` (`src/snake_game/core.cljs`) to look like below.
In this part we are requiring everything we need for our project.

<script src="https://gist.github.com/DajanaStiberova/35c39e99abca511b0036.js"></script>

You also need a CSS file in order to setup some style.
So you can copy it from [here](https://github.com/DajanaStiberova/snake-game/blob/v1.0/resources/public/css/style.css) and paste it to `resources/public/css/style.css`.

Run `lein figwheel`, open [localhost:3449](http://localhost:3449/) in your browser and you should see that your HTML and CSS were loaded (black background).

## Creating the data model

So, it's time to create our data model.
Let's think about what we need.

1. We need a board where the snake will move
2. We need the snake (its position and head direction)
3. The food item
4. The score
5. The game state (playing or game-over)

**The initial state**

- All the needed data will be stored in the `initial-state` map.

<script src="https://gist.github.com/DajanaStiberova/64cd34c4d8550ba208a5.js"></script>

At the beginning every map value is `nil`. So let's fill the map with our data.

**The board**

- The `:board` has two values: `x` (width) and `y` (height). Our board's width is 35 and height 25.

<script src="https://gist.github.com/DajanaStiberova/b971b37e6259893eee1b.js"></script>

**The snake**

- We will store the snake's `:body` in a vector of vectors `[[3 2][2 2] ..]`. Every vector is a pair of cell coordinates for a particular body part.
- The `:direction` will be stored in a vector as well.

<script src="https://gist.github.com/DajanaStiberova/d51b209f4fac547a0a46.js"></script>

**The food item**

- The food item's position will be stored in `:points` and should have a random position which is not colliding with the snake's body.
We will need to write a function which takes the snake and board as arguments and returns the first free random position.
If there's no available position, the function will return `nil`.

<script src="https://gist.github.com/DajanaStiberova/4ce13481641693d8168e.js"></script>

**The score**

- The default value of the score is 0 and is stored in `:points`.

**The game state**

- When we start the game, the state of the `:game-running?` key is set to `true` which means that the game is running.
It will become `false` when the game is stopped for any reason.

Now the `initial-state` is filled and looks like this:

<script src="https://gist.github.com/DajanaStiberova/d61199d296561017f13f.js"></script>

## Implementing the static view

Before explaining how we implemented the UI, we should talk a little bit about [re-frame](https://github.com/Day8/re-frame).
In re-frame, we make mutations to the application state (or simply `db`, the atom defined by `re-frame`) only through the so called handlers.
For the sake of simplicity, we can perceive a handler as a function which takes as input the current application state with some additional arguments and returns a new application state.
This function is registered using a regular ClojureScript keyword (handler-identity) and the only way to run it is to dispatch an event, where the event is a vector containing a handler-identity with some additional arguments.

We create handlers and register them by using `re-frame/register-handler`.

The `:initialize` handler is the first handler we need.
It will merge our `initial-state` into the application state.

<script src="https://gist.github.com/DajanaStiberova/fcb001c1042d3811e09d.js"></script>

If you write `@re-frame.db/app-db` in your repl, you should see the application state.
But at this time there's only an empty map.
That's because our `:initialize` handler hasn't been called yet.
We'll do this by using `:dispatch-sync`.

<script src="https://gist.github.com/DajanaStiberova/fcdef3122bb98b4590d5.js"></script>

And now we just need to call the `run` function.

<script src="https://gist.github.com/DajanaStiberova/b2ce9cd323c5c5ba4d58.js"></script>

You can try `@re-frame.db/app-db` in the repl and you'll see that the application state is now:

```
{:board [35 25],
 :snake {:direction [1 0],
         :body [[3 2] [2 2] [1 2] [0 2]]},
 :food [15 13],
 :points 0,
 :game-running? true}
```

But in your browser you still see nothing, right? So let's fix that.

We need to tell our application where we want to render our app.
We want to render it into the `div` with the id `app` in our HTML.
For this to happen we need to use the `reagent/render` function.
This function takes a `reagent` component (it will be called `game` in our case) and a DOM node inside which the rendering will happen.
We can get the DOM node by using JavaScript interop `(.getElementById js/document "...")`.

We will modify our `run` function to render our component.

<script src="https://gist.github.com/DajanaStiberova/4b624fdf87395cf04991.js"></script>

Let's create our `game` (main rendering) function.
This function just creates an empty `div` this time.
To represent HTML elements we use plain Clojure data structures (if you want to know more about this syntax, visit [Hiccup](https://github.com/weavejester/hiccup)).

<script src="https://gist.github.com/DajanaStiberova/ba7219c8106a1f210746.js"></script>

If you open the developer console in your browser ([localhost:3449](http://localhost:3449/)) you'll see the new empty `div` inside the `div` with id `app`.

It's time to render the board.
To get the board data from the app state, we'll need to register a subscription.
A subscription is the `re-frame` way to change the UI state every time the `db` changes.
As with handlers, each subscription is registered using a keyword, which is `:board` in this case.
This subscription function takes a `db` as argument and any number of optional arguments.
In the body of the function, we will dereference the `db`, extract the `:body` and wrap it in the reaction `(reaction (:board @db))`.

<script src="https://gist.github.com/DajanaStiberova/31da0bdb2b80e314178e.js"></script>

If you write `@(subscribe [:board])` in your repl, you'll see the board value `[35 25]`. `reagent/subscribe` returns a `reagent/reaction` which returns a deref-able object behaving like a read-only `ratom` (reactive atom).

Now we can connect it to our view.
We will display our board as an HTML table by creating the `render-board` function.
As you can see, we are using a `subscribe` in our `let` block to get the `:board` value.

<script src="https://gist.github.com/DajanaStiberova/fc8ac5bb14d7fc15f543.js"></script>

And calling it in our main rendering function.
We will render the game board inside the existing `div`.


<script src="https://gist.github.com/DajanaStiberova/dd82f67fd533643678d2.js"></script>

After saving, you can see an empty board in your browser.

In this step we'll modify the `render-board` function to show the snake and the food item as well.

In order to react to direction changes and to the new food item location we need to create a subscription for the snake and the food item as well.

<script src="https://gist.github.com/DajanaStiberova/1c09e5416579f0a6094b.js"></script>

And modify our `render-board` function. We will additionally `subscribe` the snake and the food item, and based on their indexes, use an appropriate CSS class on the table cell.

<script src="https://gist.github.com/DajanaStiberova/c24ae08c282d647f9483.js"></script>

The score part is the simplest one of the view.
We need to create a subscription and a `div` where the score will be located.

<script src="https://gist.github.com/DajanaStiberova/6f3ef6ba51fe8d9dc5ec.js"></script>

And add the `score` function to the `game` function.

<script src="https://gist.github.com/DajanaStiberova/d755f3faa376c30a8845.js"></script>

The last part of the view is the game-over overlay. This is the overlay when the `:game-running?` in the `app-state` is `false`.

I'm sure you know what's coming:
we need to create a new subscription plus a new function which renders a `div` styled as an overlay.
This function will have a simple condition.
If the value of `:game-running?` is `true`, the function returns an empty `div`, otherwise a `div` with the `class` attribute set to `overlay` (all classes are defined in [resources/public/css/style.css](https://github.com/DajanaStiberova/snake-game/blob/v1.0/resources/public/css/style.css#L51)).
We will also render a `h1` with the symbol ↺ there, to provide a clickable target for refreshing the game.


<script src="https://gist.github.com/DajanaStiberova/dd2653694c594f5f9f4f.js"></script>

And modify the `game` function to render `game-over`.

<script src="https://gist.github.com/DajanaStiberova/7a02f3006f50f5cf2c94.js"></script>

Now you can see the full board with the snake, the food item and the score in your browser.

![static-part](http://i.imgur.com/adu7rFv.png?1)

If you change the value of the `:game-running?` to `false` in the `initial state`, you'll see a `game-over` overlay.

![static-game-over](http://i.imgur.com/bGioI8c.png?1)

Stay tuned, the second part is comming soon!
We will make the snake move, feed it with the food item and check for collisions.

Full source code is available on [GitHub](https://github.com/Lambda-X/snake-game/tree/v1.0).

## Links

- [https://github.com/Day8/re-frame](https://github.com/Day8/re-frame)
- [https://reagent-project.github.io/](https://reagent-project.github.io/)
- [https://github.com/weavejester/hiccup](https://github.com/weavejester/hiccup)


