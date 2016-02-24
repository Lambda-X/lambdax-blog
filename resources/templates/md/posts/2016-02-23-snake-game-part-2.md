{:title "Snake the game - a tutorial for ClojureScript and re-frame, part 2/2"
 :description  "Simple tutorial on how to create Snake game using ClojureScript, reagent and re-frame, part 2/2"
 :layout :post
 :tags  ["Clojure" "ClojureScript" "Reagent" "re-frame"]
 :toc false
 :author "Dajana Štiberová"
 :image "snake-game.png"}

In the [first part](http://lambdax.io/blog/posts/2016-01-19-snake-game-part-1.html)
of our tutorial we created the data model and rendered the board with the snake
in the browser. Now it's time for the second part:

1. [Make the snake move](#1._make_the_snake_move)
2. [Actions to perform when the food item is caught](#2._actions_to_perform_when_the_food_item_is_caught)
3. [Collisions](#3._collisions)
4. [Game over and replay](#4._game_over_and_replay)
5. [The end](#5._the_end)

## 1. Make the snake move

In order to make the snake move, we need to update its data at regular intervals.
But first, we need to create a function which animates the snake.
This function will take the snake map as argument and create a new one, in which
the snake's body is moved one step forward.

<script src="https://gist.github.com/dstiberova/9cd7dfc668c7af2f0b28.js"></script>

In the `let` block we define the new snake's head position by applying
the direction on the first segment (the snake head).
The new coordinates of the next segment are the old coordinates of the previous
segment.

We know that if we want to make mutations in our code, we need to use handlers.
We will register one named `:next-state` and we will use our `move-snake` function
to update the snake.
Before calling `move-snake` in the `:next-state` handler, we have to check
whether the game is still going on (the value of `:game-running?` in app-state).
Otherwise, we won't do anything and we will just return the unchanged `db`.

<script src="https://gist.github.com/dstiberova/ead3a55af334d8ddaf36.js"></script>

You can try `(dispatch [:next-state])` in your repl, and you'll see the snake is
rendered in the new position.
But we want to keep our snake moving, so we need to dispatch the `:next-state`
event periodically, for example every 150ms.
This interval will reflect the "speed of the game" and hence also the
difficulty :)

In order to achieve this we'll use JavaScript interop again.

<script src="https://gist.github.com/dstiberova/8e40c0dc4460099d3901.js"></script>

The snake is moving but now we need to control him.

We will listen for specific
[key codes](https://css-tricks.com/snippets/javascript/javascript-keycodes/):
_up_ (38), _down_ (40), _left_ (37) and _right_ (39) to determine which arrow
key was pressed.

So we want those keys to change the direction of the snake.
The natural way to express it would be to create just a mapping from key codes
to the corresponding direction vectors.

<script src="https://gist.github.com/dstiberova/29b393eba05f7f6932b9.js"></script>

When the key is pressed, we'll look up the corresponding direction vector
from the `key-code->move` map and then we'll just update the snake with a new
direction.

<script src="https://gist.github.com/dstiberova/7f0e0440b1bc91f78c44.js"></script>

The key lookup and dispatch will be performed when the `keydown` event
is detected.

<script src="https://gist.github.com/dstiberova/f9482091ed78721327e7.js"></script>

But there's still one little problem.
If we press the arrow pointing to the opposite direction to the snake's
current one, the snake will collide with itself.
We can either consider this as a collision and stop the game, or just ignore that move.
I chose the second option.

The `change-snake-direction` function takes a new direction and the current
one and compares them.
If this new direction is perpendicular to the old direction, the function
returns a new direction, otherwise just the old direction.

<script src="https://gist.github.com/dstiberova/0b1fe4352c2ec34997aa.js"></script>

Of course we need to use this function in the `:change-direction` handler.

<script src="https://gist.github.com/dstiberova/9a08ca320042a32ff6c8.js"></script>

Now we can fully control the snake's movement and the only things left are
collision handling and food item catching.

## 2. Actions to perform when the food item is caught

To create the food item in a new position we don't need a new function, we
can re-use `rand-free-position`, but to make the snake longer we need to create
a new function, `grow-snake`.

This function will use the last two parts of the snake and based on those
create another part at the end of the snake.
We'll also create a small helper function `snake-tail`, which will compute
`x` or `y` coordinate, based on the last two values of the said coordinate in
the snake body.

For example, if this function gets `[1 2]` as arguments it will return `3`,
if `[2 1]` it will return `0`.

<script src="https://gist.github.com/dstiberova/0d6ede73d35350319d6f.js"></script>

We will use it in our `grow-snake` function.
This function will take the snake as argument and based on the last two
coordinates of the snake body, it will create another coordinate and `conj`
it to the snake.

<script src="https://gist.github.com/dstiberova/857db40a6ba959797deb.js"></script>

To increase the score, we simply use the `inc` function on `:points` in db.

For putting all the three functions together, we will use the `process-move`
function.
This function will take a whole db as argument, and if the first snake part
(the head of the snake) has the same coordinate as the food item, we will
update every key we need:
`(:snake (grow-snake) :points (inc) :food (rand-free-position))`

<script src="https://gist.github.com/dstiberova/bdee06cd01afb3abc87d.js"></script>

The best time to call the `process-move` function is in the `:next-state` handler,
chained after the `move-snake` call.

<script src="https://gist.github.com/dstiberova/4306ef43c2453faca05f.js"></script>

## 3. Collisions

Although we can play the snake game it's impossible to make it end.
It's also very easy to make the snake go outside the board but still
nothing will happen.

We need to create two types of collision.
The first, when the snake collides with the border, and the second one, when the
snake collides with its own body.
The collision functions will take the snake and the board as arguments and
return `true` if the collision occurred and `false` otherwise.

We need to realize that the next `x` index of the snake head (that is, the position
in which the snake head will be in the next state) can't be neither `-1` nor
the width of the board incremented of `1`.
The next `y` index of the snake head can't be neither `-1` nor the height
of the board incremented of `1`.
Similarly, the next head position `[x y]` can't be equal to any coordinate
of the snake body.

<script src="https://gist.github.com/dstiberova/d735b421e3cb869778e4.js"></script>

We check for collisions in the `:next-state` handler.
If a collision occurred, we just return the existing `db`, with `:game-running?`
set to `false`, otherwise we call the next steps (`move-snake`, `process-move`).

<script src="https://gist.github.com/dstiberova/01b2a326c444352a4165.js"></script>

## 4. Game over and replay

If a collision occurred, the game over overlay will be rendered.
In this last step we just need to make the refresh icon clickable.
Clicking on this icon will reset the datastate. We can achieve this very easily
by dispatching the `:initialize` event that, as you probably remember,
merges `initial-state` into the app-state so that the state
of the game will be exactly the same as when the game started.

<script src="https://gist.github.com/dstiberova/bd4971f71776dc61b763.js"></script>

## 5. The end

With the app-state so nicely encapsulated in one single place it's very easy to
add features like changing the board size or pause the game. If you want to know
more about this style of programming and state management I highly recommend reading
the [re-frame tutorial](https://github.com/Day8/re-frame).

Meanwhile, enjoy the game!

<div id="app"></div>
<script src="../scripts/snake-game/snake_game.js"></script>
<link rel="stylesheet" type="text/css" href="../scripts/snake-game/style.css" />

Full source code is available on [GitHub](https://github.com/Lambda-X/snake-game/tree/v1.0).

## Links

- [https://github.com/Day8/re-frame](https://github.com/Day8/re-frame)
- [https://reagent-project.github.io/](https://reagent-project.github.io/)
- [https://github.com/weavejester/hiccup](https://github.com/weavejester/hiccup)
