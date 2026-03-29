package com.classloader;

import sun.misc.Launcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

public class TestClassLoader  {


    public static void main(String[] args) throws MalformedURLException {
        //类加载器测试
        String myFilePath = "/Users/lishanjie/Downloads/";
        URL[] urls = new URL[1];
        urls[0] = new File(myFilePath).toURI().toURL();
        //实例化 自己的类加载器
        MyCustomClassLoader myClassLoader = new MyCustomClassLoader(urls);
        // 打印各个加载器的 加载路径
        //1.启动类加载器，加载路径
        System.out.println("启动类的加载路径: ");
        for(URL url : Launcher.getBootstrapClassPath().getURLs())
            System.out.println(url);
        System.out.println("----------------------------");
        //2.扩展类加载器，加载路径
        URLClassLoader urlClassLoaderParent = (URLClassLoader) ClassLoader.getSystemClassLoader().getParent();
        System.out.println(urlClassLoaderParent+"扩展类的加载路径: ");
        for(URL url : urlClassLoaderParent.getURLs())
            System.out.println(url);
        System.out.println("----------------------------");
        //3.应用类加载器，加载路径
        URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        System.out.println(urlClassLoader+"应用类的加载路径: ");
        for(URL url : urlClassLoader.getURLs())
            System.out.println(url);
        System.out.println("----------------------------");

        System.out.println(myClassLoader+"自定义类的加载路径: ");
        for(URL url : urls)
            System.out.println(url);
        System.out.println("----------------------------");

    }

}
