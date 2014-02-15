Groovy EGL - Exploratory Groovy Looper
========================================

**tl;dr: Jump to the Example.**

If you have a situation where some costly setup code must be run before you can experiment on the result (e.g. load several million transactions into memory, or spinning up a big Spring context), and then want to code-experiment on the result, e.g. summing this way and that through all those transaction, you typically end up with very many restarts, giving the setup-hit each time. Even if you get that down to just a few seconds, the continuous hit of that setup can severely hamper your exploratory hacking, reducing your actual coding time to just percentages of "wall time".
 
Instead - _given that what you work on is NOT the setup-code, but the use of the resulting objects of that setup code_ - you can make a Groovy script whose first line is a call to one of the loop methods in this class. You provide a Closure that performs the load and provides the result.

The looper will then fire up a script Binding, sticking in that result, bound to a key of your choosing, and then run the exact same script again, using a GroovyShell.

Since the first line in the script is the call to the loop method, it will instantly go in there again - however, this time a ThreadLocal will notice that the setup is already performed (or you can check the current script Binding for the presence of the special value SETUP_DONE), and instead of executing the setup-Closure, the loop method will just exit. This results in the rest of the script being run - which have the result of the costly setup-code in its Binding.

When the script exits, the looper will wait for you hitting Enter, and then re-run the script.

... And this is where the entire point of this class kicks in: Since the script is evaluated afresh each time it is being invoked, you can go totally nuts in your editing, with full IDE capabilities. Any Exception is caught, so even heavy syntax errors will be handled smoothly.

Example
--------
You for example make a file "TransactionExploratoryScript.groovy":
```groovy
 import com.stolsvik.tools.ExploratoryGroovyLooper
 ExploratoryGroovyLooper.loop this, { _txs = Transactions.loadAll() }

 // Just to get proper IDE typing help inside the script
 Transaction[] txs = _txs
 
 def sum = 0
 txs.forEach{ t -> sum += t.amount }
 println sum
```

If you now want to explore what the sum is if you only include the transactions where the amount is > 1000, or where the purchase was done on a Saturday, then you just change the code, and hit Enter in the console, and your forEach line will instantly run, since the costly loading of the transactions setup code is already done.

_Tip: If you do things like the transaction-example, and speed is of the essence, explore both Martin Davis' DoubleDouble (instead of Groovy's insistence on BigDecimal), and Groovy's @CompileStatic annotation (Note: that annotation can not be put on scripts, but on methods, which you can have inside of scripts), and of course the Fork-Join framework. Notice also that Groovy will convert every double to Double (long to Long etc), and that you cannot make Closures which return a primitive double: since Closures are built upon Callable, it must return an object. But you can force it through by making your own DoubleProvider interface which have an e.g. "double call(V v)" method, and using @CompileStatic liberally (You'll instantly see the results on the run times). Also, instead of a ".forEach{}"-loop, check out how the "for(int i;...;i++)"-loop handles._

**This code is hereby put in the Public Domain - or can be licensed using the BSD license.**
