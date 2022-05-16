# MoonKeki
A 2D game development framework with OpenGL backend, written in Java.

**Attention!** This is work in progress. The API might and is very likely to
change substantially. Tests are absent or minimally performed.

___

### Is this a library, framework or game engine?
I'd like to think of the above as:
- **Libraries**: Code that **your code calls**.
- **Frameworks**: Code that **calls your code**.
- **Engines**: Platforms that support game development thoroughly.

That being said, this is probably more like a **framework**. It has library 
attributes too though.

### Is this 3D too?
**No, it is only 2D.** It is not 3D or 4D or 3.14D or a coffee machine.

### Ok, so what type of 2D games?
Pixel art variety. To be fair though, it draws textures and runs shaders, so I 
think you could go for other styles too.

### How it looks like?
Drawing on the screen:
```java
Texture image = new Texture("path.."); //We load an image from disk
InstantRenderer renderer = InstantRenderer.getDefault();
renderer.drawCommand()
        .ofPixmap(image)
        .draw(); //We're done
```
Drawing on a texture:
```java
Texture canvas = ..
InstantRenderer r = InstantRenderer.builder()
                                   .ofCanvas(canvas)
                                   .build();
```
Drawing a portion of a texture:
```java
Texture image = ..
//A region of image, defined as [x=100, y=200, width=50, height=250]
Pixmap region = image.subRegion(100, 200, 50, 250);
InstantRenderer renderer = InstantRenderer.getDefault();
renderer.drawCommand()
        .ofPixmap(region) //Only the defined region will be drawn
        .draw();
```
Drawing **on** a portion of a texture:
```java
Texture image = ..
Pixmap canvas = image.subRegion(200, 100, 250, 50);
InstantRenderer r = InstantRenderer.builder()
                                   .ofCanvas(canvas)
                                   .build();
```
Post-processing effects:
```java
ShaderProgram blur = ..
ShaderProgram negative = ..
ShaderProgram chromatic = ..
ShaderRenderer renderer = ShaderRenderer.getDefault();
renderer.shaderCommand()
        .add(blur)
        .add(negative)
        .add(chromatic)
        .apply();
```

### Is this cross-platform?
It is desktop cross-platform. You can't run it on mobile or playstation or xbox
or switch :( And even on desktop, you probably won't be able to use it with
macOS, as Apple deprecated OpenGL. So, it works on Windows and Linux (possibly
on Stream Deck as well). I have high hopes for 
[Fuchsia OS](https://fuchsia.dev/) to solve the cross-platform problems. At the
end of the day don't forget that to run Java code you only need a JVM, and a JVM
can run pretty much anywhere.

### Is this an experiment or just a *made for fun* game framework?
Neither. I'm creating a not-so-shy Steam game with it, as we speak.

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
screen and a few extras. The difference between this and other frameworks that 
are exclusively for applications is that those frameworks work with events to 
draw stuff, but this one uses a busy loop, possibly drawing many times per 
second.

### Why Java?
People shame Java for game development, and those that don't, use it like they 
would use C++. If you are going to create software with Java, you do it because 
you adhere to its
[values](https://en.wikipedia.org/wiki/Java_(programming_language)#Principles).
To make the most out of Java, you have to play around its principles. If you
won't do that, then don't use Java.

Ok, so why people don't like Java for game development? Long story short, it's
the **garbage collector**. They are afraid that it will kick in to do work,
**lagging** the game in the process, so people just avoid using Java. Fair
enough, no one wants to play a lagging game. Now, there are people that know
that garbage collection is a bad thing in games, and that a strong part of Java 
is that it is garbage collected (mutually exclusive things) and still use Java..
The problem here is that they do all shorts of hacks to avoid the garbage 
collection by using object pools, avoiding creating new objects and reusing 
everything they can, making the code hard to maintain, possibly buggy and 
wasting a lot of time into things they shouldn't, because the garbage collector
should deal with them. If you don't like garbage collection, don't use Java. 
Make your game in C++. It has similar syntax, no garbage collector, and it's 
actually easier to find bindings for OpenGL.

Alright, so why this framework is written in Java then? Java's
[ZGC](https://wiki.openjdk.java.net/display/zgc/Main) is the answer. ZGC stands
for **Z** **G**arbage **C**ollector, and it is a garbage collector that has 
**max pause** times of **<1ms** (the actual number is around **200μs** (0.2ms)) 
(a 60fps game spends 16.6ms on each frame) and **average** of around **30μs** 
(0.03ms). And that's still not all, ZGC most likely will become generational, 
tracking a young and old generation, that is, short-lived objects should be very
easy and fast to garbage collect, allowing for even greater allocation rates.

With ZGC in mind, use Java, create as many objects as you want, even in the main
game loop, don't reuse them, drop your object pools, create short-lived objects
as you please, use Java as it was meant to be used, not as a hack. Your game
won't lag from the garbage collector.

Ok, enough with the garbage collection, what else can Java offer for game
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
The latest there is with **previews enabled** (happens to be **18** atm). I don't 
care about LTS versions, sure they have value, but I think the best LTS is 
staying with the latest version.

### Will this software be backwards or forwards compatible with itself?
Probably **no**. That is, let `a, b (a < b)` two versions of this software, then
code written in version `a` **may** or **may not** run on version `b` and the 
same holds for the other way around. A true hero would bite the bullet and take
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
feedback is greatly appreciated!

### How to use this software?
For now, you have to git clone or copy the code. Don't forget to use Java 18
with preview features enabled. Also, ZGC is not enabled by default, so if you 
want to use it, and you probably should, then you have to add `-XX:+UseZGC` in
the VM options. I tried to keep the dependencies as low as possible (check
`build.gradle` file), so right now it only depends on
[LWJGL3](https://github.com/LWJGL/lwjgl3).

In the future I will probably create a GitHub package with it, so you can start
developing your game just with a simple few lines dependency declaration from
your build automation tool.

### Where is the starting point of this framework?
I will create tutorials in the future, but for now, take a look at the
`Application` class from the `app` package. Basically, in the main method write:
```java
Application.configuration()
           .ofWindowPosition(...)
           .ofWindowSize(...)
           .ofWindowTitle(...)
           .build(...); //You supply the starting point here
```
Important! Don't use any class unless the .build() in the above code has been
called! Keep in mind that the `render` package is more mature than the `app`.

### How to process input?
As of now, you can't. An input system is on the works.

### How to leave feedback?
You can open an issue or engage in the GitHub Discussions.

### I created a game with this framework, how to deploy?
The hard part is over, a few formalities left. What you need to be aiming for,
is to package a .jar of your game alongside a JVM (don't forget to use ZGC).
Take a look at [Packaging Tool](https://openjdk.java.net/jeps/392). That's all I
can say for now. When I'll deploy my game, I'll let you know how I did it.
