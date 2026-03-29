package com;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ModuleLifecycle;
import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static java.lang.invoke.MethodHandles.throwException;

@MetaInfServices(Module.class)
@Information(id = "fault-injector",
        author = "SandboxDemo",
        version = "1.0.0",
        isActiveOnLoad = false)  // 默认不激活
public class FaultInjectorModule implements Module, ModuleLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(FaultInjectorModule.class);

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    private boolean isActive = false;

    @Override
    public void onLoad() {
        logger.info("FaultInjectorModule loaded successfully");
    }

    @Override
    public void onUnload() {
        logger.info("FaultInjectorModule unloaded");
    }

    @Override
    public void onActive() {
        isActive = true;
        logger.info("FaultInjectorModule activated");
    }

    @Override
    public void onFrozen() {
        isActive = false;
        logger.info("FaultInjectorModule frozen");
    }

    // ============ 超时故障注入 ============

    @Command("mapping-timeout")
    public void injectTimeout() {
        logger.info("Injecting timeout fault to OrderConversionManagerImpl");

        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com.mandao.gateway.manager.impl.OrderConversionManagerImpl")
                .onBehavior("*")
                .onWatch(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        if (!isActive) return;

                        logger.info("Injecting 3 seconds timeout for insertSelective, userId: {}",
                                advice.getParameterArray()[0]);

                        // 注入10秒延迟
                        TimeUnit.SECONDS.sleep(10);

                        logger.info("Timeout injection completed");
                    }

                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        logger.info("orderConversionMapper.insertSelective completed normally after timeout");
                    }

                    @Override
                    protected void afterThrowing(Advice advice) throws Throwable {
                        logger.error("orderConversionMapper.getUserInfo threw exception after timeout",
                                advice.getThrowable());
                    }
                });
    }


    @Command("mapping-exception")
    public void injectionDataException(){
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com.mandao.gateway.manager.impl.OrderConversionManagerImpl")
                .onBehavior("*")
                .onWatch(new AdviceListener(){
                    @Override
                    protected void before(Advice advice) throws Throwable {
                        if (!isActive) return;
                        ProcessController.throwsImmediately(new Exception("org.springframework.jdbc.CannotGetJdbcConnectionException"));
                    }
                });
    }

    @Command("mysql-timeout")
    public void injectionMysqlTimeoutException(){

        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com/mandao/aggregate/manager/storage/impl/MobilePayManager")
                .onBehavior("createAcctBankDepositRecord")
                .onWatch(new AdviceListener(){
                    @Override
                    protected void before(Advice advice) throws Throwable {
                        if (!isActive) return;
                        ProcessController.throwsImmediately(new Exception("com.mysql.cj.jdbc.exceptions.MySQLTimeoutException"));
                    }
                });

    }


    @Command("error-morethan-30")
    public void injectionErrorMoreThan(){

        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com.mandao.gateway.manager.impl.OrderConversionManagerImpl")
                .onBehavior("*")
                .onWatch(new AdviceListener(){
                    @Override
                    protected void before(Advice advice) throws Throwable {
                        if (!isActive) return;
                    }
                });

    }

    @Override
    public void loadCompleted() {
        logger.info("FaultInjectorModule load completed");
    }
}