package com.stolsvik.tools
import com.stolsvik.tools.ExploratoryGroovyLooper

/**
 * A Groovy script exemplifying the {@link ExploratoryGroovyLooper}.
 */

ExploratoryGroovyLooper.loop this, {
    // :: This is the setup-closure. Any variables set here will be available in the "exploratory code" below.
    // Emulate a tedious, time-stealing setup procedure (e.g. loading lots of data)
    _number = new BigDecimal(0.1d)
    Thread.sleep(5000)
}

// :: Below here, the "exploratory code" is. It will be recompiled and run each time you hit enter in the console.
// Any exceptions, even if the compilation fails, will be caught, and you won't loose the variables from the setup.

// To get proper IDE typing help inside the script, we cast the object from the setup closure to actual class.
BigDecimal number = _number

println "This is 0.1 in Double-representation: ${number.toPlainString()}"

return number
