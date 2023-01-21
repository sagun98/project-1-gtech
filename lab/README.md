# Project 1: Intro
Project 1 serves as a brief introduction to the interface you'll be programming
against, as well as the testing infrastructure. All implementations you need are
already available in this doc. Just fill them to the right position in source code.

1. [Project Setup](#project-setup)
2. [Warm Up](#warm-up)
3. [General Logic](#general-logic)
4. [Implementation](#implementation)
5. [Test Your Implementation](#test-your-implementation)
    1. [Hello, World!](#hello-world!)
    2. [When Things Go Wrong](#when-things-go-wrong)
6. [Submission](#submission)
7. [Project Question List](#project-question-list)
8. [Recommend Reading List](#recommend-reading-list)

## Project Setup
We assume you have already configured your virtual machine with Ubuntu 18.04, downloaded Java 8, installed git, and have an IDE for your implementation.
If not, please refer to the `environment` repo in our organization.

You will first clone the project repo from our organization. Please use the following command for cloning from our organization repo.
You should replace `gtAccount` with your gtAccount, `course-organization` to the
current term organization, and `project-repo` to the project repo name. You will
be asked to enter a password for access. The password is the same as your GT
account. Note that a gtAccount is usually made up of your initials and a number,
such as ag117, and that combination is unique to you.

```shell script
git clone https://<gtAccount>@github.gatech.edu/<course-organization>/<project-repo.git>
```

The project repos are private and we only grant access to enrolled students. If you have enrolled but cannot clone project repos (in general, if you can see this repo, you should be able to clone it), please contact the course TAs for addressing the issues. We only allow read and clone permissions for students without push. Please initial your repo inside your GitHub account and update this repo to it for further usage. We have provided detailed commands to clone and create copy inside your private Gatech GitHub in the `environment` repo under the same organization.

After cloning the project repo, open the project directory with IDE (IntelliJ). Also, open a terminal and enter the project directory. Enter ``make`` for the crash will automatically download all dependencies and you should no longer see import reference issues in IDE. In case of build failure shows as below, `sudo chmod +x gradlew` should fix your problem.

<div style="text-align:center;"><img src="/lab/pic/makeerror.png"/></div>

## Warm Up
In this project, we'll take a look at a very
simple distributed system: a server which responds to pings and clients which ping that server.
You will implement it using the provided interface and several self-defined classes as extension.

## General Logic
Generally, all projects follows an uniform workflow. Client initializes the communication with
servers by sending requests and wait for responses from servers. After receiving some responses,
client will first check whether the response is the one that it is waiting for.
Just like *TCP*, if client does not receive desired response from server after a certain time,
it will suspect that if there is network failure and perform resending. Client will need timer to check for timeout.
With resending mechanism, then there is duplication threat in network. Client would thus need to add more logic to
check whether it has received the response before. Similarly, servers also perform certain action when
receiving msg from clients and its peers.

The behavior for server and client indicates a typical state machine, with msg and timer as inputs.
The client and server will perform certain actions to dealing with the inputs given it current states,
i.e. waiting responses.

## Implementation
Let's start with the `PingApplication` and see how we can implement the logic using interfaces and classes.
`Application`s in this framework are simple state machines. They consume `Command`s, update internal state, and
return `Result`s. Notice that `Application`, `Command` and `Result` are all interfaces. Your future implementation
will need to implement these interface and employ polymorphism design pattern.

The `PingApplication` is just quite a simple one. It defines one `Command` (`Ping`) and one `Result` (`Pong`).
Whenever it gets a `Ping`, it returns a `Pong` with the same value. The `PingApplication` lives on the `PingServer`.
After you understand the code, remember to fill the `TODO` in `PingApplication`.

```java
@ToString
@EqualsAndHashCode
public class PingApplication implements Application {
    @Data
    public static final class Ping implements Command {
        @NonNull private final String value;
    }

    @Data
    public static final class Pong implements Result {
        @NonNull private final String value;
    }

    public PingApplication() {}

    // copy constructor
    public PingApplication(PingApplication application) {}

    @Override
    public Pong execute(Command command) {
        //TODO: fill this function as document (first check command validity, then generate result)
        if (!(command instanceof Ping)) {
            throw new IllegalArgumentException();
        }

        Ping p = (Ping) command;

        return new Pong(p.value());
    }
}
```

Now, we proceed to server and see what behaviors are expected for server.
The `PingServer` is a `Node` — the basic unit of computation in a distributed
system. It holds a `PingApplication`, does nothing on initialization, and
defines a single message handler for the `PingRequest` message. That handler
simply passes the `Ping` the message contains to the `PingApplication`. Having a
`PingApplication` instead of handling the `Ping` directly on the server may seem
a little contrived, but it will make more sense in later labs when we build
systems which can support *any* application. After you understand the code,
remember to fill the `TODO` in `PingServer`.

```java
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PingServer extends Node {
    private final PingApplication app = new PingApplication();

    /* -------------------------------------------------------------------------
        Construction and Initialization
       -----------------------------------------------------------------------*/
    public PingServer(Address address) {
        super(address);
    }

    @Override
    public void init() {
        // No initialization necessary
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private void handlePingRequest(PingRequest m, Address sender) {
        //TODO: fill this function as document
        //Local application execute request; Send response to client
        Pong p = app.execute(m.ping());
        send(new PongReply(p), sender);
    }
}
```

Speaking of messages, they're how your `Node`s speak to each other. This system
just has two. They're pretty self-explanatory. For your future implementation,
you can add more information in the message for more complex systems.

```java
@Data
class PingRequest implements Message {
    private final Ping ping;
}

@Data
class PongReply implements Message {
    private final Pong pong;
}
```

Finally, we come to the other side of this rather uncomplicated conversation,
the client. The `PingClient` is a `Node` which allows the outside world to make
use of our system.

```java
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class PingClient extends Node implements Client {
    private final Address serverAddress;

    private Ping ping;
    private Pong pong;

    /* -------------------------------------------------------------------------
        Construction and Initialization
       -----------------------------------------------------------------------*/
    public PingClient(Address address, Address serverAddress) {
        super(address);
        this.serverAddress = serverAddress;
    }

    @Override
    public synchronized void init() {
        // No initialization necessary
    }

    /* -------------------------------------------------------------------------
        Client Methods
       -----------------------------------------------------------------------*/
    @Override
    public synchronized void sendCommand(Command command) {
        //TODO: fill this function as document
        //Check command validity; Send request to server; Set timer for possible resend due to network loss
        if (!(command instanceof Ping)) {
            throw new IllegalArgumentException();
        }

        Ping p = (Ping) command;

        ping = p;
        pong = null;

        send(new PingRequest(p), serverAddress);
        set(new PingTimer(p), RETRY_MILLIS);
    }

    @Override
    public synchronized boolean hasResult() {
        return pong != null;
    }

    @Override
    public synchronized Result getResult() throws InterruptedException {
        //TODO: fill this function as document
        //Check and wait for response from server
        while (pong == null) {
            wait();
        }

        return pong;
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private synchronized void handlePongReply(PongReply m, Address sender) {
        if (ping != null && Objects.equal(ping.value(), m.pong().value())) {
            ping = null;
            pong = m.pong();
            notify();
        }
    }

    /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    private synchronized void onPingTimer(PingTimer t) {
        if (ping != null && Objects.equal(ping, t.ping())) {
            send(new PingRequest(ping), serverAddress);
            set(t, RETRY_MILLIS);
        }
    }
}
```

`PingClient` implements the `Client` interface. You should read the
documentation for that interface carefully, as you will soon have to implement
it yourself! When the `PingClient` gets a `Ping` from the calling code, it sends
the `PingRequest` over the network to the server and sets a `PingTimer`. Once
the `PongReply` is received (with the necessary value), the client stores the
result and notifies the calling code which may be waiting.

```java
@Data
final class PingTimer implements Timer {
    static final int RETRY_MILLIS = 10;
    private final Ping ping;
}
```

Once this time elapses after it is set, it will be re-delivered to the
`PingClient`. If the `PingClient` receives this timer and still hasn't received
the necessary `Pong` (e.g., because the `PingRequest` was dropped on the
network), it will send the request again and re-set the timer. In this way, the
`PingClient` continually retries until it gets a response.

## Test Your Implementation
We will see the test results of your implementation, and play around this simple project to get familar with the framework and test suits.

### Hello World!
Now that we have everything in place, let's run our system! We have defined
several basic tests for Project 1. The first sets up a single client and a server and
has the client send "Hello, World!" to the server. Let's run it.

This project use **Gradle**, a build management tool, and ``Makefile`` to compile your
implementation and test cases. We now assume you are in the home directory for this repo.

```shell script
make all  # this would download dependencies and generate a build directory for you
cd build/handout  # you will run test script under this directory
```

We use the default Python 3 in Ubuntu 18.04 to run the test script.

The first test is just a trivial test for deepcopy constructor since there is no internal field.
We now check the second test.

```shell script
$ python3 run-tests.py --lab 1 --test-num 2

--------------------------------------------------
TEST 2: Single client ping test [RUN] (0pts)

...PASS (0.044s)
==================================================

Tests passed: 1/1
Points: 20/20
Total time: 0.114s

ALL PASS
==================================================
```

That happened a little fast. Let's turn on the built-in logging to watch it in
gory detail.

```
$ python3 run-tests.py --lab 1 --test-num 2 --log-level FINEST

--------------------------------------------------
TEST 2: Single client ping test [RUN] (0pts)

[FINEST ] [2021-01-21 22:08:17] [dslabs.framework.Node]
  Message(client1 -> pingserver,
    PingRequest(ping=PingApplication.Ping(value=Hello, World!)))
[FINEST ] [2021-01-21 22:08:17] [dslabs.framework.Node]
  Message(pingserver -> client1,
    PongReply(pong=PingApplication.Pong(value=Hello, World!)))
[FINEST ] [2021-01-21 22:08:17] [dslabs.framework.Node]
  Timer(-> client1, PingTimer(ping=PingApplication.Ping(value=Hello, World!)))
...PASS (0.141s)
==================================================

Tests passed: 1/1
Points: 20/20
Total time: 0.155s

ALL PASS
==================================================
```

It's alive! We can also use the visual debugger to see our system in action. Let's do that.
You can refer to `handout` to check more usage of visual debugger.

```
$ python3 run-tests.py --lab 1 --debug 1 1 "Hello World,Goodbye World"
```

This starts the system with a single server and a single client (in fact, the
first argument to debug, the number of servers, is ignored for this lab and only
used in later labs); the third argument is a comma-separated list defining the
workload given to our client as the values of `Ping`s.

Finally, let's run all of the tests.

```shell script
$ python3 run-tests.py --lab 1

--------------------------------------------------
TEST 1: Test for ping application deep copy [RUN] (20pts)

...PASS (0.007s)
--------------------------------------------------
TEST 2: Single client ping test [RUN] (20pts)

...PASS (0.082s)
--------------------------------------------------
TEST 3: Multiple clients can ping simultaneously [RUN] (20pts)

...PASS (0.008s)
--------------------------------------------------
TEST 4: Client can still ping if some messages are dropped [RUN] [UNRELIABLE] (20pts)

...PASS (0.79s)
--------------------------------------------------
TEST 5: Single client repeatedly pings [SEARCH] (20pts)

Checking that the client can finish all pings
Starting breadth-first search...
  Explored: 0, Depth exploring: 0 (0.00s, 0.00K states/s)
  Explored: 20, Depth exploring: 19 (0.04s, 0.49K states/s)
Search finished.

Checking that all of the returned pongs match pings
Starting breadth-first search...
  Explored: 0, Depth exploring: 0 (0.01s, 0.00K states/s)
  Explored: 20, Depth exploring: 19 (0.02s, 1.00K states/s)
Search finished.

...PASS (0.066s)
==================================================

Tests passed: 5/5
Points: 100/100
Total time: 1.207s

ALL PASS
==================================================
```

That's it! You have passed all test cases. However, programming with asynchronous setting
is much more difficult that this. You would encounter problems when there is divergence
between you understanding and desired correct behaviors. Let's proceed to follow sections
to see what will happen when there is error in your implementation.

### When Things Go Wrong
Seeing tests pass is great, but it's not all that exciting. Let's break things
and see what happens.

First, notice that test 4 is marked as "UNRELIABLE." This means that the network
can (and will) randomly drop messages without delivering them. You can check its
implementation in `tst` to gain deeper understanding for this framework.
Let's comment-out a crucial line in `PingClient`. Without re-setting the timer, if one of the
messages gets dropped in the network *again*, the system will be stuck.

```java
private synchronized void onPingTimer(PingTimer t) {
    if (ping != null && Objects.equal(ping, t.ping())) {
        send(new PingRequest(ping), serverAddress);
        // set(t, RETRY_MILLIS);
    }
}
```

And now let's re-run test 4.

```shell script
$ python3 run-tests.py --lab 1 --test-num 4

--------------------------------------------------
TEST 4: Client can still ping if some messages are dropped [RUN] [UNRELIABLE] (20pts)

org.junit.runners.model.TestTimedOutException: test timed out after 5000 milliseconds
  at java.lang.Object.wait(Native Method)
  ...

...FAIL (5.033s)
==================================================

Tests passed: 0/1
Points: 0/20
Total time: 5.103s

FAIL
==================================================
```

What's that you say? Liveness isn't interesting? You want to violate a safety
property? Okay. Let's get rid of the other crucial check in `PingClient`.

```java
private synchronized void handlePongReply(PongReply m, Address sender) {
    // if (ping != null && Objects.equal(ping.value(), m.pong().value())) {
        ping = null;
        pong = m.pong();
        notify();
    // }
}
```

Now, if the client gets an old `Pong` (with an incorrect value), it will
mistakenly accept it and return it to the calling code. This is unlikely to
happen in the first two tests. It might in the third, but this depends on
timing. Instead, we can use the "SEARCH" test to search through all possible
executions of our system for some workload (for more on search tests, see the
[top-level README](../README.md)).

```shell script
$ python3 run-tests.py --lab 1 --test-num 5

--------------------------------------------------
TEST 5: Single client repeatedly pings [SEARCH] (20pts)

Checking that the client can finish all pings
Starting breadth-first search...
  Explored: 0, Depth exploring: 0 (0.00s, 0.00K states/s)
  Explored: 19038, Depth exploring: 11 (1.10s, 17.34K states/s)
Search finished.

Checking that all of the returned pongs match pings
Starting breadth-first search...
  Explored: 0, Depth exploring: 0 (0.00s, 0.00K states/s)
  Explored: 5, Depth exploring: 4 (0.00s, 5.00K states/s)
Search finished.

State(nodes={pingserver=PingServer(super=Node(subNodes={}),
app=PingApplication()),
client1=ClientWorker(client=PingClient(super=Node(subNodes={}),
serverAddress=pingserver, ping=PingApplication.Ping(value=ping-1), pong=null),
results=[])}, network=[Message(client1 -> pingserver,
PingRequest(ping=PingApplication.Ping(value=ping-1)))], timers={pingserver=[],
client1=[Timer(-> client1,
PingTimer(ping=PingApplication.Ping(value=ping-1)))]})

  Message(client1 -> pingserver, PingRequest(ping=PingApplication.Ping(value=ping-1)))

State(nodes={pingserver=PingServer(super=Node(subNodes={}),
app=PingApplication()),
client1=ClientWorker(client=PingClient(super=Node(subNodes={}),
serverAddress=pingserver, ping=PingApplication.Ping(value=ping-1), pong=null),
results=[])}, network=[Message(client1 -> pingserver,
PingRequest(ping=PingApplication.Ping(value=ping-1))), Message(pingserver ->
client1, PongReply(pong=PingApplication.Pong(value=ping-1)))],
timers={pingserver=[], client1=[Timer(-> client1,
PingTimer(ping=PingApplication.Ping(value=ping-1)))]})

  Message(pingserver -> client1, PongReply(pong=PingApplication.Pong(value=ping-1)))

State(nodes={pingserver=PingServer(super=Node(subNodes={}),
app=PingApplication()),
client1=ClientWorker(client=PingClient(super=Node(subNodes={}),
serverAddress=pingserver, ping=PingApplication.Ping(value=ping-2), pong=null),
results=[PingApplication.Pong(value=ping-1)])}, network=[Message(client1 ->
pingserver, PingRequest(ping=PingApplication.Ping(value=ping-2))),
Message(client1 -> pingserver,
PingRequest(ping=PingApplication.Ping(value=ping-1))), Message(pingserver ->
client1, PongReply(pong=PingApplication.Pong(value=ping-1)))],
timers={pingserver=[], client1=[Timer(-> client1,
PingTimer(ping=PingApplication.Ping(value=ping-1))), Timer(-> client1,
PingTimer(ping=PingApplication.Ping(value=ping-2)))]})

  Message(pingserver -> client1, PongReply(pong=PingApplication.Pong(value=ping-1)))

State(nodes={pingserver=PingServer(super=Node(subNodes={}),
app=PingApplication()),
client1=ClientWorker(client=PingClient(super=Node(subNodes={}),
serverAddress=pingserver, ping=PingApplication.Ping(value=ping-3), pong=null),
results=[PingApplication.Pong(value=ping-1),
PingApplication.Pong(value=ping-1)])}, network=[Message(client1 -> pingserver,
PingRequest(ping=PingApplication.Ping(value=ping-2))), Message(client1 ->
pingserver, PingRequest(ping=PingApplication.Ping(value=ping-1))),
Message(pingserver -> client1,
PongReply(pong=PingApplication.Pong(value=ping-1))), Message(client1 ->
pingserver, PingRequest(ping=PingApplication.Ping(value=ping-3)))],
timers={pingserver=[], client1=[Timer(-> client1,
PingTimer(ping=PingApplication.Ping(value=ping-1))), Timer(-> client1,
PingTimer(ping=PingApplication.Ping(value=ping-2))), Timer(-> client1,
PingTimer(ping=PingApplication.Ping(value=ping-3)))]})

dslabs.framework.testing.junit.VizClientStarted: State violates "Clients got expected results"
Error info: client1 got PingApplication.Pong(value=ping-1), expected PingApplication.Pong(value=ping-2)
See above trace.

  at dslabs.framework.testing.junit.BaseJUnitTest.invariantViolated(BaseJUnitTest.java:270)
  ...

...FAIL (1.727s)
==================================================

Tests passed: 0/1
Points: 0/20
Total time: 1.829s

FAIL
==================================================
```

That's a lot of information, but it's important information telling us what went
wrong (the client returned a `Pong` with the wrong value), and gives us a
concrete execution of the system which leads to that problem. Here, the client
sends the first ping to the server. The server responds, and the client sends
out the next ping. Then, the server's response is delivered again (the network
in these labs can duplicate messages unless otherwise stated).

Reading through traces, while useful, can be tedious. Let's visualize it!

```shell script
$ python3 run-tests.py --lab 1 --test-num 5 --start-viz
```

Make sure to understand the invariant being violated: the test code "knows" what
to expect as a reply from the ping server (that the reply should match what was
sent). Catching this is simpler than it might be because we assume that the
client only sends one ping at a time, and waits for the previous ping to be
acknowledged before sending a different ping. What should be the client's
behavior when it receives a duplicate (late) message?

## Submission
Please fill your implementation as the doc and pass all tests. You should also write a simple report.
The report rubrics are already available in Piazza. We have provided you the general structure in `REPORT.md`.

For submission, you should submit both your implementation and report. As for report, fill the content in  `REPORT.md`. A `submit.sh` under `lab` is ready to use. Run that script as follows and enter your gtAccount. A zip file with the same name as your gtAccount, `gtAccount.zip`, will be generated. The zip file should contain your implementation source code and `REPORT.md`. Submit the `zip` file to the corresponding project in GradeScope. Then, you are done! We will use your last submission for project grading, and we reserve the right to re-run the autograder. The running setting of autograder in GradeScope is 4 CPUs with 6G RAM.

```shell script
$ submit.sh gtAccount
```

***Note**: Make sure you do not include **print** or **log** statements in your implementation. Also, do not include or zip extra files for your submissions. We will check the completeness and validity of submission before grading.
If your submission fails to satisfy the submission requirement or could not compile, you will see feedback from GradeScope indicating that and receive 0 for that submission.*

***
### Submission Metrics
- `gtAccount.zip` (Implementation Correctness 90%, Report 10%)


## Project Question List

***

- Can the project run in a Windows or Mac environment?

	As we illustrated in the setup, this project should be implemented in the Linux environment (especially Ubuntu 18.04). We encourage you to use a virtual machine and install **IntelliJ IDEA** for implementation. This would ease some pains in terms of compilation errors and debugging. The addon for **Lombok** in **IDEA** would also bring you convenience.

***

- Any preferred setting for a virtual machine?

	There are multi-threading settings for projects, in both runs and tests. With experiences from the previous semester, 4 CPU cores and 4G RAM (my personal setting as 6 cores and 12G RAM) should be enough and bring positive feedbacks in test speed and duration. To avoid some inconvenience as the course delivery, make sure that your virtual machine has enough disk space (20GB recommended).

***

- Any advice for JAVA beginner?

	All programming assignments emphasize system design and algorithm implementation, rather than language-specific features. Good code structure and well-illustrated design would bring you abundant convenience in debugging, especially with *distributed* and *asynchronous* setting. In general to say, there is no steep programming language skill required to finish projects. Basic *Object Oriented* knowledge like inheritance, polymorphism, and visibility is enough. As for the container, **HashMap**, **HashSet** and **LinkedList** would be your good friends. A clear understanding of shallow and deep copy for objects is expected. Also, basic usage of **Lombok** is required.

***

- Do we need to consider thread-safe when programming?

	Yes but very little. I personally don't have any difficulty in thread-safe until the last lab, **HashMap** in specific. Once you follow the given code snippets and understand the test logic, you should not encounter problems relating to thread-safe.

***

- After I look up the doc and paper, there is randomness when testing. How do I make sure my implementation will solidly pass the test cases?

	**This answer applies to all programming assignments.** As illustrated in the original paper, test a distributed system is much more difficult, comparing with a single-server setting. Given the constraints in time and search space, pass the tests does not equal the absolute correctness of your implementation in an asynchronous setting. For tests in **GradeScope**, we have configured certain tests to run multiple times due to the randomness, according to the test logic and our past experiences. There is no uniform random seed, indicating the difference in network uncertainty for several runs. You should test multiple times repeatedly for your local runs and make sure that they are all passed.


## Recommend Reading List

- [dslabs design](https://ellismichael.com/papers/dslabs-eurosys19.pdf)
- [dslabs repo](https://github.com/emichael/dslabs)
- [UW CSE452](https://courses.cs.washington.edu/courses/cse452/)
- [MIT 6.824](http://nil.csail.mit.edu/6.824/2020/)
- [Object Oriented Programming with Java](https://docs.oracle.com/javase/tutorial/java/concepts/index.html)
- [Java Inheritance](https://docs.oracle.com/javase/tutorial/java/IandI/subclasses.html)
- [Java Polymorphism](https://docs.oracle.com/javase/tutorial/java/IandI/polymorphism.html)
- [Java HashMap](https://www.baeldung.com/java-hashmap)
- [lombok](https://projectlombok.org/)