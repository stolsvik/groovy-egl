package com.stolsvik.tools

import com.stolsvik.tools.GroovyDynaCompile

/**
 * Created by endre on 18.10.16.
 */
class GdcExample {
    static void main(String[] args) {
        GroovyDynaCompile cl = GroovyDynaCompile.fromClassLoader('DynamicallyCompiledGroovy.groovy')
        cl.setPostInstantiationClosure { println "Class of the loaded instance: ${it.getClass()}@${Integer.toHexString(System.identityHashCode(it.getClass()))}"}
        while (true) {
            println cl.getInstance().someMethod()
            Thread.sleep(2000)
        }
    }
}
