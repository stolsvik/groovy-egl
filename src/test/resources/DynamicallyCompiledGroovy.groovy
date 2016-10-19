package test

/**
 * Created by endre on 19.10.16.
 */
class DynamicallyCompiledGroovy {

    String someMethod() {
        return getMore() + ' World!'
    }

    private String getMore() {
        return 'Hello'
    }
}
