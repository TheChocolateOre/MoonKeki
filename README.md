# MoonKeki
A 2D game development framework with OpenGL backend, written in Java.

**Attention!** This is work in progress. The API might and is very likely to
change substantially. Tests are absent or minimally performed.

___

### Is this a library, framework or game engine?
I think of libraries as code that your code calls. Frameworks, as code that 
calls your code and engines as platforms that support game development 
thoroughly. That being said, this is probably more like a **framework**. It has
library attributes too though.

### Is this 3D too?
**No, it is only 2D.** It is not 3D or 4D or 3.14D or a coffee machine.

### Ok, so what type of 2D games?
Probably you can make any type of 2D game, but there is a slight bias towards
the pixel art variety.

### Another game development framework?
Let's take a look at what this framework is trying to achieve:
1. [Dependability](https://en.wikipedia.org/wiki/Dependability). I don't like
bugs. Who does? I really don't like bugs.
2. Code as close as possible to the Java API. Learning a new technology takes
time. This framework is trying to minimize that time by _mimicking_ an extension
of the Java API. As an example, the custom data structures of this framework 
will extend the JDK collections framework, when possible. Also, resources that
must be closed when are no longer needed will implement the JDK interface 
`AutoCloseable` and can be used in a try-with-resources statement.
3. Intuition and zero or low boilerplate code. No time should be spent, trying 
to achieve basic stuff. Take a look at the snippet below, on how to draw images:
   ```java
   Texture image = new Texture("path.."); //We load an image from disk
   Renderer r = new Renderer(); //A Renderer draws images
   r.draw(image); //We draw image at (0, 0)
   r.draw(image, 100, 200); //We draw image again at (100, 200)
   r.flush(); //The actual drawing happens now
   ```
   As you can see, there is no `r.begin()` or `r.end()`. We just tell the
Renderer what we want to draw, and when we are ready we `r.flush()` it. We can
   even draw on textures, not only on the screen:
   ```java
   Texture image = new Texture("path..");
   Renderer r = new Renderer();
   r.setCanvas(image); //All the drawing happens on image now
   .
   .
   r.flush();
   r.setCanvas(Canvas.WINDOW) //Now we can continue drawing on the screen
   ```
   Also, we can easily create post processing effects:
   ```java
   //Our post processing effects
   ShaderProgram blur = ...
   ShaderProgram negative = ...
   ShaderProgram chromatic = ...
   
   Renderer r = new Renderer();
   r.setPostPrograms(List.of(blur, negative, chromatic));
   .
   .
   r.flush();
   r.applyPost(); //The post processing happens here
   ```
   You can even combine the post processing with rendering to a texture. And of
course you could define effects that apply when drawing, not only for post 
processing, with `r.setBaseProgram(..)`. Also, the `r.applyPost()` method is not
related to `r.draw(..)`, so you can just apply the post processing effects,
without drawing images in advance, and as many times as you like.
   
   Sometimes you might want to draw a portion of an image:
   ```java
   Texture image = ...
   //A region of image defined by x: [100, 200) and y: [50, 250)
   Pixmap region = image.subRegion(100, 200, 50, 250);
   Renderer r = new Renderer();
   r.draw(region); //Only the defined region will be drawn
   r.flush();
   ```
   You can even use that image region as a canvas destination to draw on, using
the method `r.setCanvas(region)` like the previous example. As you can see, 
there is no need to be an OpenGL expert and create ping-pong framebuffers and 
textures and bindings and whatever, just to simply have post processing effects
or to draw a portion of an image or to draw on an image.
4. Ease of codebase understanding for new users and contributors. I don't like 
hacks, things like _"as long as it works it's ok"_. Everything must be 
thoroughly thought. The solutions of today are the problems of tomorrow. The 
codebase is intuitive and as loosely coupled as possible. I want it to be easy 
to learn and **easy** to master. Also, there will be javadoc in every corner and
dark place, even though now that's not true, but I'm working towards that with a
high priority.
   
### What are you sacrificing to achieve the above values?
1. Ok, now this is the part that will scare most of you away, but first: The 
motto of this framework is: **no bugs > performance**, that is, performance 
might get a small hit in return for a software with fewer bugs and easier to 
maintain. Let me elaborate. I am not saying that there will be asymptotic 
performance hits, no, that will be as good as possible, but there might be a few
extra guards here and there and a deeper abstraction, so yes there might be an 
extra constant overhead. But, why? aren't games supposed to be super extra 
blazing fast? They will be, we are in the 2020s, hardware is more than enough 
powerful for **2D** games and especially of the pixel art variety.
2. Power. This framework is not as powerful as pure OpenGL is. That is, a lot of
OpenGL functions didn't make it here, to keep things clean. That being said, you
can still do everything a 2D game needs. And if there is something that you 
need but is absent, leave an issue, and we might expand. But, if you want a low
level graphic card control, then this framework is probably not for you.

### Is this cross platform?
It is desktop cross platform. You can't run it on mobile or playstation or xbox
or switch :( And even on desktop, you probably won't be able to use it with 
macOS, as Apple deprecated OpenGL. So it works on Windows and Linux. I have high
hopes for [Fuchsia OS](https://fuchsia.dev/) to solve the cross platform 
problems. At the end of the day don't forget that to run Java code you only need
a JVM, and a JVM can run pretty much anywhere. OpenGL is probably the problem 
not Java.

### Is this an experiment or just a *made for fun* game framework?
Neither. I'll try to make it production ready. After that, I will create a 
not-so-shy Steam game with it.
   
### Do I need OpenGL knowledge to use this framework?
No. Even though it is being used in the backend of this framework, you don't 
have to know anything about OpenGL or how graphic cards work. That being said, 
at some point you might want to add effects, or the so-called shaders into the 
game, and that requires to write a bit of code with the GLSL language. You can't
access OpenGL directly with this framework.

### What version of OpenGL this framework uses?
3.3 core and below.

### I can only create games?
You can create **applications** too! This framework basically draws stuff on the
screen and also has an abstraction like Android's activities. The difference
between this and other frameworks that are exclusively for applications is that
those frameworks work with events to draw stuff, but this one uses a loop that
draws many times a second.

### Why Java?
I ask the same question too. People shame Java for game development, and those
that don't, use it like they would use C++. If you are going to create software
with Java, you do it because you adhere to its 
[values](https://en.wikipedia.org/wiki/Java_(programming_language)#Principles).
To make the most out of Java, you have to play around its principles. If you 
won't do that, then don't use Java.

Ok, so why people don't like Java for game development? Long story short, it's
the **garbage collector**. They are afraid that it will kick in to do work,
**lagging** the game in the process, so people just avoid using Java. Fair
enough, no one wants to play a lagging game. Now, there are people that know
that garbage collection is a bad thing in games, and that a strong part of Java is that
it is garbage collected (mutually exclusive things) and still use Java.. The
problem here is that they do all shorts of hacks to avoid the garbage collection
by using object pools, avoiding creating new objects and reusing everything they
can, making the code hard to maintain, possibly buggy and wasting a lot of time
into those things that they shouldn't, because the garbage collector should deal
with them. If you don't like garbage collection, don't use Java. Make your game in 
C++. It has similar syntax, no garbage collector and it's actually easier to 
find bindings for OpenGL.

Alright, so why this framework is written in Java then? Java's 15 
[ZGC](https://wiki.openjdk.java.net/display/zgc/Main) is the answer. ZGC stands 
for **Z** **G**arbage **C**ollector, and it is a garbage collector that has max 
pause times of 10ms (a 60fps game spends 16.6ms for each frame). But wait, 
that's not all, [Java's 16 ZGC](https://openjdk.java.net/jeps/376) will have max
pause times of **less than 1ms**, and don't quote me on that, but if I'm not 
wrong, I think the developers working on it said that the average pause time 
will be around 100μs (0.1ms). And that's still not all, if I'm not wrong, the 
developers said that ZGC will become generational at some point, tracking a 
young and old generation, that is, short-lived objects should be very easy and 
fast to garbage collect.

With ZGC in mind, use Java, create as many objects as you want, even in the main
game loop, don't reuse them, drop your object pools, create short-lived objects
as you please, use Java as it is meant to be used, not as a hack. Your game
won't lag from the garbage collector.

Ok, enough with the garbage collection, what else Java can offer for game
development? I think Java's future is bright. Take a look at 
[Project Panama](https://openjdk.java.net/projects/panama/) and 
[Project Valhalla](https://openjdk.java.net/projects/valhalla/). I think those
2 can positively impact game development with Java from the performance 
perspective, and there are even more interesting ones, like 
[Project Loom](https://openjdk.java.net/projects/loom/). And of course, lets not
forget [Project Amber](https://openjdk.java.net/projects/amber/) which increases
productivity and makes your code easier to read and maintain.

I like to think of Java as a **safe**, **powerful** with great **performance**
language, that truly lives to its **general purpose** attribute. There is no
reason why you shouldn't create games with Java.

### What version of Java this framework uses?
The latest there is. I don't care about LTS versions, sure they have value, but
I think the best LTS is staying with the latest version. As a matter of fact,
this framework even uses preview features, but probably on stable versions we'll
stick with the latest non-preview ones.

### Will this software be backwards or forwards compatible with itself?
Probably no. That is, let `a, b (a < b)` two versions of this software, then
code written in version `a` **may not** run on version `b` and the same holds 
for the other way around. A true hero would bite the bullet and take 
responsibility to keep things compatible, but I don't like compromises on good
design ideas, just to stay compatible. Although, effort will go towards 
deprecation before removal, and hopefully, dramatic changes won't happen 
overnight, at least for the later stable versions. Changes will be stated and 
migration guides will probably exist.

### Why using `double` and not `float`, won't that burn my pc?
Welcome to the 2020s, it rains chips that were considered supercomputers in the
80s. In the GPU, we use `float` though.

### Can I create a game with this framework today, now?
If you are brave enough, but I would advise against it. There are many things 
that need to be added, and it is not fully tested yet. That being said, you 
should be able to run it, and render stuff on the screen. Its core has been 
established, it just needs to mature a little. I encourage you to try it, your 
feedback is greatly appreciated! (the power of open source software)
  
### How to use this software?
For now, you have to git clone or copy the code. Don't forget to use Java 15 and
enable preview features. Also, ZGC is not enabled by default, so if you want to
use it and you probably should, then you have to add `-XX:+UseZGC` in the VM
options. I tried to keep the dependencies as low as possible (check
`build.gradle` file), so right now it only depends on 
[LWJGL3](https://github.com/LWJGL/lwjgl3).

In the future I will probably create a github package with it, so you can start
developing your game just with a simple few lines dependency declaration from
your build automation tool.

### Where is the starting point of this framework?
I will create tutorials in the future, but for now, take a look at the 
`Application` and `Screen` classes from the `app` package. Basically, in the
main method write:
```java
Application.configuration()
           .setWindowPosition(...)
           .setWindowSize(...)
           .setWindowTitle(...)
           .build(...); //You supply the starting Screen here
```
Important! Don't use any class unless the .build() in the above code has been
called! Keep in mind that the `render` package is more mature than the `app`.

### How to process input?
There is an input system present, but in an early form. Consider the example 
below:
```java
//We are interesting when the "X" button is pressed.   Or false for release
ButtonEvent xButtonEvent = ButtonEvent.of(Keyboard.Key.X, true);
if (xButtonEvent.hasOccurred()) {
    player.attack();
}
```
The above `if` condition will pass even if the `X` button is not currently being
pressed. The `hasOccurred()` method tracks whether the event happened since the
last `hasOccurred()` or `reset()` call. You might feel confused on why we care
about whether the event occurred in the past. Clearly, we want the player to
unleash an attack when the `X` button is pressed, so why don't we query just
that, and not the past. Ok, so to query that we would use the `isHappening()`
method, which returns `true` *iff* the event (`X` button press in our case) is
happening at this moment. Now's the catch. The `X` button might be pressed, but
at the time we query that with the `isHappening()` it might be already released.
The user pressed the button, but no attack was unleashed. The event got buried.
That's the value of `hasOccurred()`, it makes sure to miss nothing. Ok, so when
to use `hasOccurred()` and when to use `isHappening()`? Use the former if you
care about the existence of the event, that is, I need to know if the event
occurred no matter what. Use the latter if you want to know if the event is
happening at this very moment, e.g. as long as that button is down, do that.
Most of the times though, you'll probably want to use the `hasOccurred()`.

Ok, let's go crazy with input events:
```java
ButtonEvent event = ButtonEvent.builder()
                               .add(Mouse.Button.LEFT, true)
                               .add(Keyboard.Key.A, true)
                               .add(Keyboard.Key.V, false)
                               .build();
if (event.hasOccurredForAtLeast(0.5, true)) {
    ...
}
```
Above, we create a `ButtonEvent` that tracks a **pressed** `LEFT` mouse click, a
**pressed** `A` keyboard button and a **released** `V` keyboard button. That is,
we are interested in whether the above 3 independent button states occurred at 
the **same time**. The `hasOccurredForAtLeast()` method takes a duration in 
seconds and returns `true` *iff* the event occurred and lasted at least as long
as the given duration, at least once. The second boolean argument indicates if
the first threshold is inclusive or not. In the above example, we just
want to know if those 3 buttons were at their desired states concurrently for
at least 0.5 seconds. And yes, we keep track if the `V` keyboard button is not
pressed, i.e. released (`false` value).

Ok, I get all that, but what's the point of 2 events that track the same 
buttons? Aren't they the same? No, they are different, and here's why.
`ButtonEvent`s are in a sense consumables, every time a method like 
`hasOccurred()` is called, their state resets. With that in mind, we could have
2 events that track the same button-states, but return different values when
their consumable methods are called, and that's because 1 might be already
consumed, while the other not. That's perfectly ok. Now, it might not seem that
obvious on why we might want that, especially when considering the identical 
button-state case. It seems more clear when we have events that share a few
buttons. It would be impossible to consume 1 event, without destroying the 
state of the others, if the individual buttons where universally tracked.

Most of the time we just want an action to be performed when a button is pressed
or released. If that's your case, then consider the utility-ish class 
`ButtonEventProcessor`:
```java
ButtonEventProcessor attackProcessor = ButtonEventProcessor.ofButtonPress(
        Keyboard.Key.X, player::attack);
```
After that, you must call its `process()` method on **every** game loop, and it 
will do all the work for you. But again, you already can achieve that with a
`ButtonEvent`, this way just has less boilerplate.

### How to leave feedback?
You can open an issue or engage in the GitHub Discussions.

### I created a game with this framework, how to deploy?
The hard part is over, a few formalities left. What you need to be aiming for,
is to package a .jar of your game alongside a JVM (don't forget to use ZGC). 
Take a look at [Packaging Tool](https://openjdk.java.net/jeps/392). That's all I
can say for now. When I'll deploy my game, I'll let you know how I did it.

### Future features?
1. ~~Input system.~~ (Basic addition)
2. Sprite-like objects.
3. Collision system.
4. Excessive javadoc.
5. Enchantments in the `app` package.
6. GitHub wiki tutorials.
7. Texture atlas.
8. Sound system. (OpenAL is probably an overkill. The Java Sound API seems
   sufficient)
9. Text rendering? (You can achieve that as of now with characters in textures)
10. Geometric shapes rendering? (for debugging)

### Will you continue development or take the year off?
Ok, you got me. I will be taking most of the year (2021) off. This repository
might seem abandoned at some point, but I will come back eventually...late 2021?
