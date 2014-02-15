Groovy EGL - Exploratory Groovy Looper
========================================

This simple class is meant to help doing Exploratory Programming with your full IDE support when you have a situation where some costly setup code must be performed before you can explore your ideas: The setup code is done once, and then the script file in question is run repeatedly with the result of the setup code kept in memory - and the big point is that you can edit the script file in between the runs.

I ended up creating this it since I found both the _groovysh_ "REPL" and _Groovy Console_ to be lacking for what I wanted to do.

**tl;dr: Jump to the Example.**

So, if you have a situation where some costly setup code must be run before you can experiment on the result (e.g. load several million transactions into memory, or spinning up a Spring context), and then want to code-experiment on the result, e.g. summing this way and that through all those transaction, you typically end up with very many restarts, hitting you with the setup time each time. Even if you get the setup cost down to just a few seconds, the continuous hit of some seconds between each iteration can severely hamper your exploratory hacking, reducing your actual coding time to just percentages of "wall time".
 
Instead - _given that what you work on is NOT the setup-code, but the use of the resulting objects of that setup code_ - you can make a Groovy script whose first line is a call to one of the loop methods in this class. You provide a Closure that performs the load and provides the result.

The looper will then fire up a script Binding, sticking in that result, bound to a key of your choosing, and then run the exact same script again, using a GroovyShell.

Since the first line in the script is the call to the loop method, it will instantly go in there again - however, this time a ThreadLocal will notice that the setup is already performed (or you can check the current script Binding for the presence of the special value SETUP_DONE), and instead of executing the setup-Closure, the loop method will just exit. This results in the rest of the script being run - which have the result of the costly setup-code in its Binding.

When the script exits, the looper will wait for you hitting Enter, and then re-run the script.

**... And this is where the entire point of this class kicks in:** Since the script is **evaluated afresh** for each time it is being invoked, you can go totally nuts in your editing, with full IDE capabilities. Any Exception is caught, so even heavy syntax errors will be handled smoothly.

Example
--------
Say you have some "Transactions.loadAll()" method that takes 20 seconds to perform.
You'd make a "TransactionExploratoryScript.groovy" script like this:
```groovy
com.stolsvik.tools.ExploratoryGroovyLooper.loop this, { _txs = Transactions.loadAll() }

// Just to get proper IDE typing help inside the script
Transaction[] txs = _txs
 
def sum = 0
txs.forEach{ t -> sum += t.amount }
println sum
```

If you now want to explore what the sum is if you only include the transactions where the amount is > 1000, or where the purchase was done on a Saturday, then you just change the code, and hit Enter in the console, and your forEach line will instantly run, since the costly loading of the transactions setup code is already done.

_Tip: If you do things like the transaction-example, and speed is of the essence, explore both [Martin Davis](http://lin-ear-th-inking.blogspot.no/)' excellent [`DoubleDouble`](http://tsusiatsoftware.net/dd/main.html) (instead of Groovy's insistence on BigDecimal), and Groovy's [`@CompileStatic`](http://groovy.codehaus.org/gapi/groovy/transform/CompileStatic.html) annotation (Note: that annotation can not be put on scripts, but on methods, which you can have inside of scripts), and of course the Fork-Join framework. Notice also that Groovy will convert every double to Double (long to Long etc), and that you cannot make Closures which return a primitive double: since Closures are built upon Callable, it must return an object. But you can force it through by making your own DoubleProvider interface which have an e.g. "double call(V v)" method, and using @CompileStatic liberally (You'll instantly see the results on the run times). Also, instead of a ".forEach{}"-loop, check out how the "for(int i;...;i++)"-loop handles._

**This code is hereby put in the Public Domain - or can be licensed using the BSD license.**
