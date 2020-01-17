# Using RxJava for creating reactive JavaFx UI

In this article, I would like to show you the benefits of RxJava in practical example - desktop JavaFx GUI application. If you are developing Android or any other apps that "compute and render content" at the same time read on!

## An intro you can skip if you know JavaFx

If you never heard of JavaFx before don't feel bad. On the other hand, if you thought JavaFx project is long dead, well... I don't blame you. But believe it or not it's [alive and kickin'](https://openjfx.io/). Since it was open-sourced and separated from JDK it has become the only reasonable choice for building desktop apps in Java. 

> Why on Earth am I writing about technology for creating desktop applications in Java in 2020?

I was firstly introduced to JavaFx in 2013, when it was still part of JDK and it was a proper replacement for Swing, the former UI library, that was... let just say unpopular. But JavaFx was like a fresh breeze and version 2.0 introduced concept of `FXML` files, which allowed you to define the looks and style of your components in similar fashion as HTML and CSS.

> Why on Earth would anyone want to build desktop applications in 2020 anyway?

There are several reasons. First of all some users still prefer it to web-based services. If you don't have internet connection, you cannot access the site (if it is not offline-enabled). Also, these kind of applications are perfect for communicating with filesystem or underlying OS (e.g. we had an utility that connected through ssh and executed a script, whose results were displayed in the UI). I think there are a lot of benefits there, but this is not the topic for today.

## Do you want to develop an app?

Let's say you're writing simple UI in JavaFx. You define your layout, create first components, start adding behavior to them and you expect results. Everything works, technically, but sometimes it _does not feel right_. The UI is lagging when operations are performed and you get the impression you are doing something wrong.

To illustrate this I created a simple application with just two components:

- **Combobox** (a.k.a. SelectBox), which would perform some background tasks whenever user selects different value
- **List view** of results from these long running tasks. This list will show the name of the task and time taken to execute it (up to 1s) in order of execution. And if time is higher than 500 ms it will mark it as `slow`.

The code to execute this task is fairly simple (ignore the `Result` class for now, it's just a simple POJO):


    private Result runTask(Integer i) {
        long currentTime = System.currentTimeMillis();

        String name = "Task" + i;
        long sleepDuration = (long) (Math.random() * 1000);

        try {
            Thread.sleep(sleepDuration);
            return new Result(name, sleepDuration);
        } catch (Exception e) {
            return new Result("-", 0);
        } finally {
            System.out.println(name + " took " + (System.currentTimeMillis() - currentTime) + " ms");
        }
    }

The long running task will be fairly simple. We define the `NUMBER_OF_TASKS` that should be executed and collect results in an `ObservableList` that will be used as backing collection for the `ListView`.

> For those not familiar with JavaFx, the `ListView` works with a special collection wrapper (`ObservableList`) that is bound with the component. So whenever its content is changed, it is also changed in the UI. Simple as that.

And hey! Since it is 2020 we're going to use `Stream`s, which highly increase readability of what's happening:

    private void runTasksJavaFx(ObservableList<String> observableList) {
        IntStream.range(1, NUMBER_OF_TASKS) // Stream API way of iterating
                .mapToObj(this::runTask) // Execute and map the results of our long-running task
                .map(result -> result.time > 500 ? new Result(result.name + " (slow)", result.time) : result) // "Annotate" those that took too long
                .forEach(result -> observableList.add(result.toString())); // And push them to result list so that they are displayed in UI
    }
    
This looks nice, but you can probably _feel_ that it's not right. When you try it this is what you'll get:

[!image1]

> As you can see when item is selected the whole GUI freezes until computation is finished (you can see that in the console output on the right).

## Use the threads Luke

This is not an issue limited to JavaFx and it surely was a nightmare in Swing as well. The problem here is that the application code is running on the same thread as UI code, which means whenever there's some _long running task_ it will block updates of UI until it is finished. And that's exactly what's happening above.

There're, of course, ways to fix this. One of them is to use JavaFx's `Platform.runLater()` method that takes standard `Runnable` as a single parameter. Second is to use JavaFx's `Task` and together with `ExecutorsService` (and related). To keep it simple, we'll use first one:

    private void runTasksLaterJavaFx(ObservableList<String> observableList) {
        IntStream.range(1, NUMBER_OF_TASKS) // Still Java 8, yaaay!
                .forEach(i -> Platform.runLater(() -> { // We're using lambda for Runnable, so we cannot map the result
                    Result result = runTask(i); // So we go one Java version down with the code style
                    if (result.time > 500) {
                        result = new Result(result.name + " (slow)", result.time);
                    }
                    observableList.add(result.toString());
                }));
    }
    
Ok, there're few things here and the most important one is that since Runnable returns `void`, we need to process results "the old fashioned way". My personal opinion: it's ugly. And it will get uglier if the logic is more complex.

And then there's this nice note in the `Platform.runLater` method's documentation:

> NOTE: applications should avoid flooding JavaFX with too many pending Runnables. Otherwise, the application may become unresponsive. Applications are encouraged to batch up multiple operations into fewer runLater calls. Additionally, long-running operations should be done on a background thread where possible, freeing up the JavaFX Application Thread for GUI operations.

That's not very encouraging, isn't it? So when it gets complex, you need to introduce logic for batching the updates. You could use the Task/Executors solution, but that's even more code and if there's one thing that developers hate is reading huge amounts of code to understand what's happening.

But okay, let's see what it does when we execute this method:

[!image2]

The improvement here is that we don't block the UI, but as you can see, the updates are periodically displayed to the console output, but they are rendered once everything is finished. This can be of course improved, but at what cost?

## You know what Luke? Let's revisit this threads idea again

There are, of course, better options than fighting threads. Since we want to push our code to the third decade of 21st century, we will use Reactive Extensions (RX)!

> With RX it is as with anything that's currently trending like Cloud computing or Blockchain. The concept is not new, it's just a matter of putting everything together to solve a specific problem. The main marketing pitch for RX is _The Observer pattern done right_. I won't go into too much details about RX so I suggest you to read a thing or two on [reactivex.io](http://reactivex.io/) website.

The Observer pattern in a nutshell is about creating an object that would emit changes (`Observable`) and registering some handler that would execute action whenever needed (`Observer`). RX goes further with that since it _is a combination of the best ideas from the Observer pattern, the Iterator pattern, and functional programming_. There are multiple implementations of RX in various languages, so we're going to use [RxJava](https://github.com/ReactiveX/RxJava).

I'll go ahead and post a code example, which I will explain in detail line by line:

    private void runTasksRxJavaFx(ObservableList<String> observableList) {
        Observable.range(1, NUMBER_OF_TASKS) // 1
                .subscribeOn(Schedulers.computation()) // 2
                .map(this::runTask) // 3
                .map(result -> result.time > 500 ? new Result(result.name + " (slow)", result.time) : result) // 3
                .observeOn(JavaFxScheduler.platform()) // 4
                .forEach(result -> observableList.add(result.toString())); // 5
    }
    
- Line `// 1` looks very similar to what we have in first example (the blocking one). But instead of using regular `IntStream` we are using one of RxJava's utility classes to generate an observable collection. Specifically in this case it will `emit` each iterated element, so that observers are notified.
- Line `// 2` is probably the most difficult concept to understand here. This is because RxJava is **not multi-threaded by default**. To enable multi-threading, you need to use `Schedulers`, to off-load the execution. Specifically `subscribeOn` method is used to describe how you want to schedule your background processing. There are [multiple schedulers you can use](https://www.baeldung.com/rxjava-schedulers) based on the type of work. Specifically `Schedulers.computation()` will use bounded thread-pool with the size of up to number of available cores.
- Lines marked with `// 3` are the same as in first example, see?
- Line `// 4` is similar to `subscribeOn`, but `observeOn` instead declares where you want to schedule your updates. If you look at the flow of the code, at this point, we would like to `emit` changes back to the UI thread. In this case, we will use special type of scheduler which is part of (additional) [RxJavaFx](https://github.com/ReactiveX/RxJavaFX) library and it uses - guess what - the Java FX GUI thread.
- Line `// 5` is also same as in the first example, but in this step we are actually instructing what should be done after `observeOn` was called.

And it finally looks like an application users want to use:

[!image3]

> There's of course more to it. RxJavaFx project has nice API for creating RX-ready components. Truth to be told the learning curve might be "gentle", especially when it comes to RxJava itself. But when it is used right, you can tune it to handle large amounts of changes without sacrificing UX or performance of your application.

This was just a fly-by of what you can do with RxJava and I omitted a lot of details (including the setup of JavaFx project), so for those of you that read it until here I have prepared a [Git repository](https://github.com/rapasoft/JavaFxSkeletonApp) with template project that you can use for developing JavaFx applications and a [branch with fully working example](https://github.com/rapasoft/JavaFxSkeletonApp/tree/example-app) of what I just described here.
