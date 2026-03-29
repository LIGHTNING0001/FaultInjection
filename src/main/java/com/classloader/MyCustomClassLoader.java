package com.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

public class MyCustomClassLoader extends URLClassLoader{

    public MyCustomClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public MyCustomClassLoader(URL[] urls) {
        super(urls);
    }

    public MyCustomClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }
}
