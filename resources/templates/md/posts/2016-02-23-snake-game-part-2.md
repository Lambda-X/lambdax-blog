{:title "Snake the game - a tutorial for ClojureScript and re-frame, part 2/2"
 :description  "Simple tutorial on how to create Snake game using ClojureScript, reagent and re-frame, part 2/2"
 :layout :post
 :tags  ["Clojure" "ClojureScript" "Reagent" "re-frame"]
 :toc false
 :author "Dajana Štiberová"
 :image "snake-game.png"}

## Snake game

In the first part of our tutorial we created the data state and rendered the board with the snake. Now is the time to finish our game: 

1. [Making the snake move](#1._making_the_snake_move)
2. [Actions to perform when the food item is caught](#2._actions_to_perform_when_the_food_item_is_caught)
3. [Collisions](#3._collisions)
4. [Game over and replay](#4._game_over_and_replay)
5. [The end](#5._the_end)

## 1. Making the snake move

In order to make our snake move, we need to update its data at regular intervals.
But first, we need to create a function which animates the snake.
This function will take the snake map and create a new one, in which the snake's body is moved one step.
You can create this function by yourself or you can have a look at our implementation :)

<script src="https://gist.github.com/dstiberova/9cd7dfc668c7af2f0b28.js"></script>

In the let block we define the new position of the snake's head by applying the direction on the first snake segment (the snake head).
The new coordinates of the next segment are the old coordinates of the previous segment.

We know that if we want to make mutations in our code, we need to use handlers.
We will register a handler with the name `:next-state` and we will use our `move-snake` function to update our snake.
Before calling the `move-snake` in the `:next-state` handler, we have to check if the game is still going on (the value of the `:game-running?` in app state).
Otherwise, we won't do anything, and we will just return the unchanged `db`.

<script src="https://gist.github.com/dstiberova/ead3a55af334d8ddaf36.js"></script>

You can try `(dispatch [:next-state])` in your repl, and you'll see the snake is rendered in the new position.
But we want to keep our snake movement going on.
So we need to dispatch the `:next-state` event periodically.
For example 150ms.
This interval will reflect the speed of the "game world" and the difficulty as well :)
Here we're using js interop again.

<script src="https://gist.github.com/dstiberova/8e40c0dc4460099d3901.js"></script>

The snake is moving, that's nice, but we need to control him.

We will listen for specific [key codes](https://css-tricks.com/snippets/javascript/javascript-keycodes/): up (38), down (40), left (37) and right (39) to determine which arrow key was pressed.

So we want those keys to change the direction of the snake.
The natural way to express it would be to create just the mapping from key codes to the corresponding direction vectors.

<script src="https://gist.github.com/dstiberova/29b393eba05f7f6932b9.js"></script>

When the key is pressed, we will look up the corresponding direction vector from the `key-code->move` map.
Then we'll just update the snake with a new direction.

<script src="https://gist.github.com/dstiberova/7f0e0440b1bc91f78c44.js"></script>

This whole key lookup and dispatch will be performed when the `keydown` event is detected.

<script src="https://gist.github.com/dstiberova/f9482091ed78721327e7.js"></script>

But there's one little problem.
If we press the arrow opposite to the current snake direction (for example if the snake's going to the right and we'll press the left arrow), the snake will immediately go through itself.
We can either consider this as a collision and stop the game, or just ignore that move.
I chose the second option.

I created the function `change-snake-direction`.
This function takes a new direction and the current direction and compares them.
If this new direction is perpendicular to the old direction, the function returns a new direction, if not it just returns the old direction.

<script src="https://gist.github.com/dstiberova/0b1fe4352c2ec34997aa.js"></script>

Of course we need to use this function in the `:change-direction` handler.

<script src="https://gist.github.com/dstiberova/9a08ca320042a32ff6c8.js"></script>

Now we can fully control the snake's movement and the only things left are collision handling and food item catching.

## 2. Actions to perform when the food item is caught

To create the food item in a different place we don't need a new function. We can use our `rand-free-position` function.

When we want to make the snake longer, we need to create a new function `grow-snake`.
This function will use the last two parts of the snake and based on those create another part at the end of the snake.
We'll also create a small helper function `snake-tail`, which will compute `x` or `y` coordinate, based on the last two values of the said coordinate in the snake body.

For example, if this function gets `[1 2]` as arguments it will return `3`, if it gets `[2 1]` as arguments it will return `0`

<script src="https://gist.github.com/dstiberova/0d6ede73d35350319d6f.js"></script>

We will use this function in our `grow-snake` function.
This function will take the snake as an argument and based on the last two coordinates of the snake body, we will create another coordinate and `conj` it to the snake.

<script src="https://gist.github.com/dstiberova/857db40a6ba959797deb.js"></script>

To increase the score, we simply use the `inc` function on `:points` in db.

For putting all three functions together, we will use the `process-move` function.
This function will take a whole db as an argument, and if the first snake part (the head of the snake) has the same coordinate as the food item, we will update every key we need
`(:snake (grow-snake) :points (inc) :food (rand-free-position))`

<script src="https://gist.github.com/dstiberova/bdee06cd01afb3abc87d.js"></script>

We need to call the `process-move` function.
The best time to call this function is in the `:next-state` handler, chained after the `move-snake` call.
So the `:next-state` handler will look like this:

<script src="https://gist.github.com/dstiberova/4306ef43c2453faca05f.js"></script>

## 3. Collisions

Although we can play the snake game it's impossible to make it end.
It's also very easy to make the snake go outside the board but in this case still nothing happens.

We need to create two types of collision.
First, when the snake collides with the border, and the second one, when the snake collides with its own body.
The collision functions will take the snake and the board as arguments and return `true` if the collision occurred and `false` otherwise.

We need to realize that the future `x` index of the snake head (where the snake head will be in the next state) can't be -1 or the maximum of the x index of the board +1.
The future y index of the snake head can't be -1 or the maximum of the y index of the board + 1.
And the future head position [x y] can't be the same as the [x y] coordinates of the any snake body part.

<script src="https://gist.github.com/dstiberova/d735b421e3cb869778e4.js"></script>

We check for collisions in the `:next-state` handler.
If the collision happened, we just return the existing `db`, with the game state set to `false`.
Otherwise we will call next steps (`move-snake`, `process-move`).

<script src="https://gist.github.com/dstiberova/01b2a326c444352a4165.js"></script>

## 4. Game over and replay

If the collision happened, the game over overlay will be rendered.
In this last step we just need to make the refresh icon clickable.
Clicking on this icon will reset the datastate (we can achieve this very easily by dispatching `:initialize` event).
This event handler will call dispatch `:initialize`.
As you remember the `:initialize` handler merges `initial-state` into the app state.
So the state of the game will be exactly as when the game started.

<script src="https://gist.github.com/dstiberova/bd4971f71776dc61b763.js"></script>

## 5. The end

With the app state so nicely encapsulated in one single place it's very easy to add features like controlling the board size or pause the game.
Or you can think about some others cool features. It's up to you :)

Enjoy the game!

<div id="app"></div>
<script src="../scripts/snake-game/snake_game.js"></script>
<link rel="stylesheet" type="text/css" href="../scripts/snake-game/style.css" />


Full source code is available on [GitHub](https://github.com/Lambda-X/snake-game/tree/v1.0).

## Links

- [https://github.com/Day8/re-frame](https://github.com/Day8/re-frame)
- [https://reagent-project.github.io/](https://reagent-project.github.io/)
- [https://github.com/weavejester/hiccup](https://github.com/weavejester/hiccup)
