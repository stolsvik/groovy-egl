package com.stolsvik.tools

import groovy.transform.CompileStatic

/**
 * @author Endre Stølsvik, 2016-10-18 23:35 - http://endre.stolsvik.com/
 */
@CompileStatic
class GroovyDynaCompile {

    private String _fileContent
    private Class _class
    private Object _instance

    private String _classLoaderResourceName

    static GroovyDynaCompile fromCl(String classLoaderResourceName) {
        getClassLoaderResourceAsInputStream(classLoaderResourceName) // Fail fast if it isn't there.
        return new GroovyDynaCompile(_classLoaderResourceName: classLoaderResourceName)
    }

    Object getInstance() {
        loadIfFileChanged()
        return _instance
    }

    private void loadIfFileChanged() {
        InputStream stream = getClassLoaderResourceAsInputStream(_classLoaderResourceName)
        String fileContent = stream.getText("UTF-8")
        if (fileContent.equals(_fileContent)) {
            // -> File is identical, do nothing.
            return
        }
        println "Loading class due to new content of '$_classLoaderResourceName'"
        _fileContent = fileContent
        GroovyClassLoader loader = new GroovyClassLoader()
        _class = loader.parseClass(fileContent, _classLoaderResourceName)
        _instance = _class.newInstance()
    }

    private static InputStream getClassLoaderResourceAsInputStream(String classLoaderResourceName) {
        InputStream stream = GroovyDynaCompile.class.getClassLoader().getResourceAsStream(classLoaderResourceName)
        if (stream == null) {
            throw new IllegalArgumentException("No ClassLoader Resource of name '$classLoaderResourceName' found.")
        }
        return stream
    }

}
