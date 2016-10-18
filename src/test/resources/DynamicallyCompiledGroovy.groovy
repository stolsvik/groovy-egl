package com.stolsvik.tools

/**
 * Created by endre on 19.10.16.
 */
class DynamicallyCompiledGroovy {

    String someMethod() {
        return getMore() + ' Tralala!'
    }

    private String getMore() {
        return 'Hello'
    }
}
