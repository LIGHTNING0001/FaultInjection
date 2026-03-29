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
import com.config.FaultInjectionConfig;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

@MetaInfServices(Module.class)
@Information(
        id = "fault-injector",
        author = "FaultInjectionTeam",
        version = "2.0.0",
        isActiveOnLoad = false
)
public class FaultInjectorModule implements Module, ModuleLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(FaultInjectorModule.class);

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    private volatile boolean isActive = false;
    private volatile FaultInjectionConfig config;
    private volatile boolean watcherInstalled = false;
    private volatile boolean ruleEnabled = false;
    private final AtomicLong matchCount = new AtomicLong(0);
    private final AtomicLong triggerCount = new AtomicLong(0);
    private final AtomicLong skippedByProbabilityCount = new AtomicLong(0);
    private final AtomicLong skippedByLimitCount = new AtomicLong(0);
    private final AtomicLong totalInjectedDelayMillis = new AtomicLong(0);
    private final AtomicLong lastTriggeredAtMillis = new AtomicLong(0);
    private final AtomicLong lastMatchedAtMillis = new AtomicLong(0);
    private final Random random = new Random();
    private volatile String lastTriggeredExceptionClass = "none";
    private volatile String lastTriggeredReturnValue = "none";
    private volatile String lastSkipReason = "none";

    @Override
    public void onLoad() {
        loadConfig();
        logger.info("FaultInjectorModule loaded");
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

    @Override
    public void loadCompleted() {
        logger.info("FaultInjectorModule load completed");
    }

    @Command("reload-injection-config")
    public void reloadInjectionConfig() {
        loadConfig();
        resetRuleStats();
        ruleEnabled = config != null && config.hasAnyInjectionEnabled();
    }

    @Command("show-injection-config")
    public void showInjectionConfig() {
        if (config == null) {
            logger.warn("Config is not loaded");
            return;
        }
        logger.info("Current config: {}, ruleEnabled={}, watcherInstalled={}, matchCount={}, triggerCount={}",
                config.toSummary(), ruleEnabled, watcherInstalled, matchCount.get(), triggerCount.get());
    }

    @Command("list-rules")
    public void listRules() {
        showInjectionConfig();
    }

    @Command("show-stats")
    public void showStats() {
        logger.info("Rule stats: ruleEnabled={}, watcherInstalled={}, matchCount={}, triggerCount={}, skippedByProbabilityCount={}, skippedByLimitCount={}, totalInjectedDelayMillis={}, averageInjectedDelayMillis={}, lastMatchedAtMillis={}, lastTriggeredAtMillis={}, lastTriggeredExceptionClass={}, lastTriggeredReturnValue={}, lastSkipReason={}",
                ruleEnabled,
                watcherInstalled,
                matchCount.get(),
                triggerCount.get(),
                skippedByProbabilityCount.get(),
                skippedByLimitCount.get(),
                totalInjectedDelayMillis.get(),
                calculateAverageInjectedDelayMillis(),
                lastMatchedAtMillis.get(),
                lastTriggeredAtMillis.get(),
                lastTriggeredExceptionClass,
                lastTriggeredReturnValue,
                lastSkipReason);
    }

    @Command("reset-stats")
    public void resetStats() {
        resetRuleStats();
        logger.info("Rule stats reset");
    }

    @Command("clear-rules")
    public void clearRules() {
        ruleEnabled = false;
        resetRuleStats();
        logger.info("Rules cleared. Watcher remains installed={}, target={}",
                watcherInstalled,
                config == null ? "none" : config.getTargetClass() + "." + config.getTargetBehavior());
    }

    @Command("preset-db-timeout")
    public void presetDbTimeout() {
        applyExceptionPreset(
                "java.sql.SQLTransientConnectionException",
                "Simulated DB connection timeout"
        );
    }

    @Command("preset-dubbo-timeout")
    public void presetDubboTimeout() {
        applyExceptionPreset(
                "org.apache.dubbo.rpc.RpcException",
                "Simulated Dubbo timeout"
        );
    }

    @Command("preset-kafka-send-fail")
    public void presetKafkaSendFail() {
        applyExceptionPreset(
                "org.apache.kafka.common.KafkaException",
                "Simulated Kafka send failure"
        );
    }

    @Command("preset-db-query-timeout")
    public void presetDbQueryTimeout() {
        applyExceptionPreset(
                "java.sql.SQLTimeoutException",
                "Simulated DB query timeout"
        );
    }

    @Command("preset-db-get-connection-fail")
    public void presetDbGetConnectionFail() {
        applyExceptionPreset(
                "org.springframework.jdbc.CannotGetJdbcConnectionException",
                "Simulated cannot get JDBC connection"
        );
    }

    @Command("preset-dubbo-remoting-timeout")
    public void presetDubboRemotingTimeout() {
        applyExceptionPreset(
                "org.apache.dubbo.remoting.TimeoutException",
                "Simulated Dubbo remoting timeout"
        );
    }

    @Command("preset-dubbo-remoting-fail")
    public void presetDubboRemotingFail() {
        applyExceptionPreset(
                "org.apache.dubbo.remoting.RemotingException",
                "Simulated Dubbo remoting failure"
        );
    }

    @Command("preset-kafka-timeout")
    public void presetKafkaTimeout() {
        applyExceptionPreset(
                "org.apache.kafka.common.errors.TimeoutException",
                "Simulated Kafka timeout"
        );
    }

    @Command("preset-kafka-network-fail")
    public void presetKafkaNetworkFail() {
        applyExceptionPreset(
                "org.apache.kafka.common.errors.NetworkException",
                "Simulated Kafka network failure"
        );
    }

    @Command("preset-kafka-serialization-fail")
    public void presetKafkaSerializationFail() {
        applyExceptionPreset(
                "org.apache.kafka.common.errors.SerializationException",
                "Simulated Kafka serialization failure"
        );
    }

    @Command("preset-redis-command-timeout")
    public void presetRedisCommandTimeout() {
        applyExceptionPreset(
                "io.lettuce.core.RedisCommandTimeoutException",
                "Simulated Redis command timeout"
        );
    }

    @Command("preset-redis-connection-fail")
    public void presetRedisConnectionFail() {
        applyExceptionPreset(
                "io.lettuce.core.RedisConnectionException",
                "Simulated Redis connection failure"
        );
    }

    @Command("preset-http-socket-timeout")
    public void presetHttpSocketTimeout() {
        applyExceptionPreset(
                "java.net.SocketTimeoutException",
                "Simulated HTTP socket timeout"
        );
    }

    @Command("preset-http-connect-fail")
    public void presetHttpConnectFail() {
        applyExceptionPreset(
                "java.net.ConnectException",
                "Simulated HTTP connect failure"
        );
    }

    @Command("preset-http-resource-access-fail")
    public void presetHttpResourceAccessFail() {
        applyExceptionPreset(
                "org.springframework.web.client.ResourceAccessException",
                "Simulated HTTP resource access failure"
        );
    }

    @Command("preset-thread-timeout")
    public void presetThreadTimeout() {
        applyExceptionPreset(
                "java.util.concurrent.TimeoutException",
                "Simulated thread/task timeout"
        );
    }

    @Command("preset-thread-rejected")
    public void presetThreadRejected() {
        applyExceptionPreset(
                "java.util.concurrent.RejectedExecutionException",
                "Simulated task rejected by thread pool"
        );
    }

    @Command("load-injection")
    public void loadInjection() {
        if (config == null) {
            logger.warn("Config is not loaded, skip watcher install");
            return;
        }
        if (!config.hasAnyInjectionEnabled()) {
            logger.warn("No injection enabled, skip watcher install");
            return;
        }
        if (watcherInstalled) {
            ruleEnabled = true;
            logger.info("Watcher already installed. Rule re-enabled with config: {}", config.toSummary());
            return;
        }

        new EventWatchBuilder(moduleEventWatcher)
                .onClass(config.getTargetClass())
                .onBehavior(config.getTargetBehavior())
                .onWatch(new AdviceListener() {
                    @Override
                    protected void before(Advice advice) throws Throwable {
                        if (!isActive) {
                            return;
                        }
                        FaultInjectionConfig local = config;
                        if (!shouldTrigger(local)) {
                            return;
                        }
                        if (local.isTimeoutEnabled()) {
                            injectDelay(local, advice);
                        }
                        if (local.isReturnEnabled()) {
                            injectReturnValue(local);
                        }
                        if (local.isExceptionEnabled()) {
                            injectException(local);
                        }
                    }
                });

        watcherInstalled = true;
        ruleEnabled = true;
        logger.info("Watcher installed for {}.{}", config.getTargetClass(), config.getTargetBehavior());
    }

    private void injectDelay(FaultInjectionConfig local, Advice advice) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(local.getTimeoutMillis());
        totalInjectedDelayMillis.addAndGet(local.getTimeoutMillis());
        int argsSize = advice.getParameterArray() == null ? 0 : advice.getParameterArray().length;
        logger.info("Timeout injected: {}ms, target={}.{}, args={}",
                local.getTimeoutMillis(), local.getTargetClass(), local.getTargetBehavior(), argsSize);
    }

    private void injectException(FaultInjectionConfig local) throws Throwable {
        Throwable throwable = buildThrowable(local.getExceptionClass(), local.getExceptionMessage());
        lastTriggeredExceptionClass = throwable.getClass().getName();
        logger.info("Exception injected: {}", throwable.getClass().getName());
        ProcessController.throwsImmediately(throwable);
    }

    private void injectReturnValue(FaultInjectionConfig local) throws Throwable {
        Object returnValue = parseReturnValue(local.getReturnType(), local.getReturnValue());
        lastTriggeredReturnValue = String.valueOf(returnValue);
        logger.info("Return value injected: type={}, value={}", local.getReturnType(), lastTriggeredReturnValue);
        ProcessController.returnImmediately(returnValue);
    }

    private Throwable buildThrowable(String exceptionClassName, String message) {
        try {
            Class<?> rawClass = Class.forName(exceptionClassName);
            if (!Throwable.class.isAssignableFrom(rawClass)) {
                return new RuntimeException("Configured class is not Throwable: " + exceptionClassName);
            }
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> throwableClass = (Class<? extends Throwable>) rawClass;
            try {
                return throwableClass.getConstructor(String.class).newInstance(message);
            } catch (NoSuchMethodException e) {
                return throwableClass.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            return new RuntimeException(message, e);
        }
    }

    private void loadConfig() {
        try {
            this.config = FaultInjectionConfig.load();
            this.ruleEnabled = this.config.hasAnyInjectionEnabled();
            logger.info("Config loaded: {}", this.config.toSummary());
        } catch (IOException e) {
            this.config = null;
            this.ruleEnabled = false;
            logger.error("Failed to load config", e);
        }
    }

    private void applyExceptionPreset(String exceptionClass, String exceptionMessage) {
        if (config == null) {
            loadConfig();
        }
        if (config == null) {
            logger.warn("Cannot apply preset because config is empty");
            return;
        }
        if (watcherInstalled) {
            logger.info("Watcher already installed. Preset will update active rule in place.");
        }
        this.config = this.config.withExceptionOnly(exceptionClass, exceptionMessage);
        this.ruleEnabled = true;
        logger.info("Preset applied: {}", this.config.toSummary());
    }

    private Object parseReturnValue(String type, String value) {
        String normalizedType = type == null ? "string" : type.trim().toLowerCase();
        String normalizedValue = value == null ? "" : value.trim();
        try {
            if ("null".equals(normalizedType)) {
                return null;
            }
            if ("boolean".equals(normalizedType)) {
                return Boolean.parseBoolean(normalizedValue);
            }
            if ("int".equals(normalizedType) || "integer".equals(normalizedType)) {
                return Integer.parseInt(normalizedValue);
            }
            if ("long".equals(normalizedType)) {
                return Long.parseLong(normalizedValue);
            }
            if ("double".equals(normalizedType)) {
                return Double.parseDouble(normalizedValue);
            }
            if ("string".equals(normalizedType)) {
                return normalizedValue;
            }
            logger.warn("Unknown return type '{}', fallback to string", type);
            return normalizedValue;
        } catch (Exception e) {
            logger.warn("Failed to parse return value, type={}, value={}, fallback to string", type, value, e);
            return normalizedValue;
        }
    }

    private boolean shouldTrigger(FaultInjectionConfig local) {
        if (local == null || !ruleEnabled) {
            return false;
        }
        long currentMatchCount = matchCount.incrementAndGet();
        lastMatchedAtMillis.set(System.currentTimeMillis());
        long maxTriggerCount = local.getMaxTriggerCount();
        if (maxTriggerCount >= 0 && triggerCount.get() >= maxTriggerCount) {
            ruleEnabled = false;
            skippedByLimitCount.incrementAndGet();
            lastSkipReason = "max_trigger_count_reached";
            logger.info("Rule auto-disabled because max trigger count {} was reached", maxTriggerCount);
            return false;
        }
        int probability = local.getProbabilityPercent();
        if (probability <= 0) {
            skippedByProbabilityCount.incrementAndGet();
            lastSkipReason = "probability_zero";
            return false;
        }
        if (probability >= 100) {
            return reserveTriggerSlot(maxTriggerCount);
        }
        boolean hit = random.nextInt(100) < probability;
        if (!hit) {
            skippedByProbabilityCount.incrementAndGet();
            lastSkipReason = "probability_miss";
            logger.debug("Rule skipped by probability check: matchCount={}, probabilityPercent={}",
                    currentMatchCount, probability);
        }
        if (!hit) {
            return false;
        }
        return reserveTriggerSlot(maxTriggerCount);
    }

    private void resetRuleStats() {
        matchCount.set(0);
        triggerCount.set(0);
        skippedByProbabilityCount.set(0);
        skippedByLimitCount.set(0);
        totalInjectedDelayMillis.set(0);
        lastTriggeredAtMillis.set(0);
        lastMatchedAtMillis.set(0);
        lastTriggeredExceptionClass = "none";
        lastTriggeredReturnValue = "none";
        lastSkipReason = "none";
    }

    private boolean reserveTriggerSlot(long maxTriggerCount) {
        long currentTriggerCount = triggerCount.incrementAndGet();
        if (maxTriggerCount >= 0 && currentTriggerCount > maxTriggerCount) {
            triggerCount.decrementAndGet();
            ruleEnabled = false;
            skippedByLimitCount.incrementAndGet();
            lastSkipReason = "max_trigger_count_reached";
            logger.info("Rule auto-disabled because max trigger count {} was reached", maxTriggerCount);
            return false;
        }
        lastTriggeredAtMillis.set(System.currentTimeMillis());
        lastSkipReason = "none";
        if (maxTriggerCount >= 0 && currentTriggerCount == maxTriggerCount) {
            logger.info("Rule reached max trigger count {}", maxTriggerCount);
        }
        return true;
    }

    private long calculateAverageInjectedDelayMillis() {
        long currentTriggerCount = triggerCount.get();
        if (currentTriggerCount <= 0) {
            return 0L;
        }
        return totalInjectedDelayMillis.get() / currentTriggerCount;
    }
}
