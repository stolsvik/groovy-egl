package com.stolsvik.tools

import groovy.transform.CompileStatic

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * <b>tl;dr: Jump to the Example.</b>
 * <p>
 * If you have a situation where some costly setup code must be run before you can experiment on the result 
 * (e.g. load several million transactions into memory, or spinning up a big Spring context), and then want to
 * code-experiment on the result, e.g. summing this way and that through all those transaction, you typically end up
 * with very many restarts, giving the setup-hit each time. Even if you get that down to just a few seconds, the
 * continuous hit of that setup can severely hamper your exploratory hacking, reducing your actual coding time to just
 * percentages of "wall time".
 * <p>
 * Instead - <i>given that what you work on is NOT the setup-code, but the use of the resulting objects of that setup
 * code</i> - you can make a Groovy script whose first line is a call to one of the loop methods in this class. You
 * provide a Closure that performs the load and provides the result.
 * <p>
 * The looper will then fire up a script {@link Binding}, sticking in that result, bound to a key of your choosing,
 * and then run the exact same script again, using a {@link GroovyShell}.
 * <p>
 * Since the first line in the script is the call to the loop method, it will instantly go in there again - however,
 * this time a ThreadLocal will notice that the setup is already performed (or you can check the current script
 * Binding for the presence of the special value {@link #SETUP_DONE}), and instead of executing the setup-Closure,
 * the loop method will just exit. This results in the rest of the script being run - which have the result of the
 * costly setup-code in its Binding.
 * <p>
 * When the script exits, the looper will wait for you hitting Enter, and then re-run the script.
 * <p>
 * <b>... And <u>this</u> is where the entire point of this class kicks in</b>: Since the script is evaluated afresh
 * each time it is being invoked, you can go totally nuts in your editing, with full IDE capabilities. Any Exception is
 * caught, so even heavy syntax errors will be handled smoothly.
 * <p>
 * <h2>Example</h2>
 * You for example make a file "TransactionExploratoryScript.groovy":
 * <pre>
 * import com.stolsvik.tools.ExploratoryGroovyLooper
 * ExploratoryGroovyLooper.loop this, { _txs = Transactions.loadAll() }
 *
 * // Just to get proper IDE typing help inside the script
 * Transaction[] txs = _txs
 * 
 * def sum = 0
 * txs.forEach{ t -> sum += t.amount }
 * println sum
 * </pre>
 * <p>
 * If you now want to explore what the sum is if you only include the transactions where the amount is &gt; 1000,
 * or where the purchase was done on a Saturday, then you just change the code, and hit Enter in the console, and
 * your forEach line will instantly run, since the costly loading of the transactions setup code is already done.
 * <p>
 * <i>Tip: If you do things like the transaction-example, and speed is of the essence, explore both
 * Martin Davis' <a href="http://tsusiatsoftware.net/dd/main.html"><code>DoubleDouble</code></a> (instead of
 * Groovy's insistence on {@link java.math.BigDecimal BigDecimal}), and Groovy's {@literal @}CompileStatic annotation
 * (Note: that annotation can not be put on scripts, but on methods, which you can have inside of scripts), and of
 * course the <a href="http://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html">Fork-Join</a>
 * framework. Notice also that Groovy will convert every double to Double (long to Long etc), and that you
 * cannot make Closures which return a primitive double: since Closures are built upon {@link java.util.concurrent.Callable Callable},
 * it must return an object. But you can force it through by making your own DoubleProvider interface which
 * have an e.g. "double call(V v)" method, and using {@literal @}CompileStatic liberally (You'll instantly
 * see the results on the run times). Also, instead of a ".forEach{}"-loop, check out how the "for(int i;...;i++)"-loop
 * handles.</i> 
 * <p>
 * <b>This code is hereby put in the Public Domain - or can be licensed using the BSD license.</b>
 * 
 * @author Endre Stølsvik, 2014 - http://endre.stolsvik.com/
 */
@CompileStatic
class ExploratoryGroovyLooper {

    static String SETUP_DONE = "SETUP_DONE"
    static DecimalFormat df = new DecimalFormat('# ##0.000;-#', new DecimalFormatSymbols(Locale.US));
    static ThreadLocal<Boolean> __alreadyInvoked = new ThreadLocal<>()

    // :: The Delegate should have been its own inner class, but due to GROOVY-5875 it can't
    private Binding binding
    ExploratoryGroovyLooper(Binding binding) {
        this.binding = binding;
    }
    def methodMissing(String name, Object[] args) {
        this.binding.setProperty(name, args[0])
    }
    def propertyMissing(String name, value) {
        this.binding.setProperty(name, value)
    }

    /**
     * Re-runs the script that performs this invocation, but with the properties and method calls performed in the
     * provided Closure bound as properties in the Binding of the new invocation. Repeats the invocation
     * when the user hits Enter, but then without performing the Binding again. Read class javadoc.
     * <h2>Example</h2>
     * You for example make a file "TransactionExploratoryScript_Closure.groovy":
     * <pre>
     * import com.stolsvik.tools.ExploratoryGroovyLooper
     * ExploratoryGroovyLooper.loop(this) { 
     *     test1 = "Property Set style"
     *     test2("Method Call style")
     *     test3 "Also Method Call style w/o parens"
     *     
     *     _txs = Transactions.loadAll()
     * }
     *
     * println test1
     * println test2
     * println test3
     * // Just to get proper IDE typing help inside the script
     * Transaction[] txs = _txs
     * 
     * def sum = 0
     * txs.forEach{ t -> sum += t.amount }
     * println sum
     * </pre>
     * @param thisScript You write "this" as the first parameter, thus providing referring to the script class instance.
     * @param keyValueClosure A {@link Closure} whose both method calls and property setters will delegate to
     *              become properties on the {@link Binding}
     */
    static void loop(Script thisScript, Closure keyValueClosure) {
        if (__alreadyInvoked.get()) { return }

        Binding binding = new Binding()

        def b = new ExploratoryGroovyLooper(binding)
        keyValueClosure.delegate = b;
        keyValueClosure.resolveStrategy = Closure.DELEGATE_FIRST
        keyValueClosure()

        loop(thisScript, binding)
    }

    /**
     * A more basic, and muce more boring, variant of {@link #loop(Script, Closure)} - which is used internally by
     * that method.
     * <h2>Example</h2>
     * You for example make a file "TransactionExploratoryScript_Binding.groovy":
     * <pre>
     * import com.stolsvik.tools.ExploratoryGroovyLooper
     * if (!ExploratoryGroovyLooper.isSetup(binding)) {
     *     binding.setProperty("_txs", Transactions.loadAll())
     *     ExploratoryGroovyLooper.loop this, binding
     * }
     *
     * // Just to get proper IDE typing help inside the script
     * Transaction[] txs = _txs
     * 
     * def sum = 0
     * txs.forEach{ t -> sum += t.amount }
     * println sum
     * </pre>
     * @param thisScript
     * @param binding
     */
    static void loop(Script thisScript, Binding binding) {
        if (__alreadyInvoked.get() || isSetup(binding)) { return }

        __alreadyInvoked.set(true)
        binding.setProperty(SETUP_DONE, SETUP_DONE)

        def thisScriptPath = thisScript.getClass().protectionDomain.codeSource.location.path

        while (true) {
            println "\n\n============================================================================================="
            println " Running script '$thisScriptPath', class:${thisScript.getClass().getName()}"
            println "---------------------------------------------------------------------------------------------"
            long nanosStart = System.nanoTime()
            GroovyShell shell = new GroovyShell(binding)
            Object value;
            try {
                value = shell.evaluate(new File(thisScriptPath))
                double msTaken = (System.nanoTime() - nanosStart) / 1000000d;
                println "-----------\n  Took ${df.format(msTaken)} ms - Script returned: $value"
            }
            catch (Throwable t) {
                double msTaken = (System.nanoTime() - nanosStart) / 1000000d;
                System.err.println("WHOOPS! GOT A THROWABLE!   (${df.format(msTaken)} ms since start)")
                sleep 100 // To flush any std-out output
                t.printStackTrace()
                sleep 100 // To flush any std-err output
            }
            println "---------------------------------------------------------------------------------------------"

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in))
            print "\nHit enter to run again, or 'x' and enter to exit."
            def userInput = br.readLine()
            if (userInput == "x") {
                System.exit(0)
            }
        }
    }

    /**
     * @param binding the current script {@link Binding}
     * @return whether Setup is already performed: <code>binding.variables.containsKey(ContinuousScriptLooper.SETUP_DONE)</code>
     */
    static boolean isSetup(Binding binding) {
        binding.variables.containsKey(ExploratoryGroovyLooper.SETUP_DONE)
    }

}
