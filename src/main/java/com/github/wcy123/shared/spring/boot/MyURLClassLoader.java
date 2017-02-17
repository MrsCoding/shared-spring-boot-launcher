package com.github.wcy123.shared.spring.boot;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

public class MyURLClassLoader extends URLClassLoader {
    private final String name;

    public MyURLClassLoader(URL[] urls, ClassLoader parent, String name) {
        super(urls, parent);
        this.name = name;
    }

    public MyURLClassLoader(URL[] urls, String name) {
        super(urls);
        this.name = name;
    }

    public MyURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory,
            String name) {
        super(urls, parent, factory);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
