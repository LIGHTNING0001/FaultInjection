package com;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ModuleLifecycle;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;


@MetaInfServices(Module.class)
@Information(id = "test-module", author = "shanjie_li", version = "1.0.0")
public class Baofootest implements Module, ModuleLifecycle {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Command("mock")
    public void mockReturn(){

        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com.example.mockshenyu.controller.TestController")
                .includeBootstrap()
                .onBehavior("echo")
                .onWatch(new AdviceListener(){
                    @Override
                    protected void before(Advice advice) throws Throwable {
                        System.out.println(" before...方法名: " + advice.getBehavior().getName());
                        advice.changeParameter(0, "jvm-sandbox");
                    }


                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        System.out.println("after method: " + advice.getBehavior().getName());
                    }

                    @Override
                    protected void after(Advice advice) throws Throwable {
                        super.after(advice);
                    }

                    @Override
                    protected void afterThrowing(Advice advice) throws Throwable {
                        super.afterThrowing(advice);
                    }

                    @Override
                    protected void beforeCall(Advice advice, int callLineNum, String callJavaClassName, String callJavaMethodName, String callJavaMethodDesc) {
                        super.beforeCall(advice, callLineNum, callJavaClassName, callJavaMethodName, callJavaMethodDesc);
                    }

                    @Override
                    protected void afterCallReturning(Advice advice, int callLineNum, String callJavaClassName, String callJavaMethodName, String callJavaMethodDesc) {
                        super.afterCallReturning(advice, callLineNum, callJavaClassName, callJavaMethodName, callJavaMethodDesc);
                    }

                    @Override
                    protected void afterCallThrowing(Advice advice, int callLineNum, String callJavaClassName, String callJavaMethodName, String callJavaMethodDesc, String callThrowJavaClassName) {
                        super.afterCallThrowing(advice, callLineNum, callJavaClassName, callJavaMethodName, callJavaMethodDesc, callThrowJavaClassName);
                    }

                    @Override
                    protected void afterCall(Advice advice, int callLineNum, String callJavaClassName, String callJavaMethodName, String callJavaMethodDesc, String callThrowJavaClassName) {
                        super.afterCall(advice, callLineNum, callJavaClassName, callJavaMethodName, callJavaMethodDesc, callThrowJavaClassName);
                    }

                    @Override
                    protected void beforeLine(Advice advice, int lineNum) {
                        super.beforeLine(advice, lineNum);
                    }
                });

    }

    @Override
    public void onLoad() throws Throwable {

    }

    @Override
    public void onUnload() throws Throwable {

    }

    @Override
    public void onActive() throws Throwable {

    }

    @Override
    public void onFrozen() throws Throwable {

    }

    @Override
    public void loadCompleted() {



    }
}
