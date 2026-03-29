package com.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FaultInjectionConfig {

    private static final String DEFAULT_CONFIG_RESOURCE = "fault-injection.properties";
    private static final String CONFIG_PATH_PROPERTY = "fault.injector.config";

    private final String targetClass;
    private final String targetBehavior;
    private final boolean timeoutEnabled;
    private final long timeoutMillis;
    private final boolean exceptionEnabled;
    private final String exceptionClass;
    private final String exceptionMessage;
    private final boolean returnEnabled;
    private final String returnType;
    private final String returnValue;
    private final int probabilityPercent;
    private final long maxTriggerCount;

    private FaultInjectionConfig(String targetClass,
                                 String targetBehavior,
                                 boolean timeoutEnabled,
                                 long timeoutMillis,
                                 boolean exceptionEnabled,
                                 String exceptionClass,
                                 String exceptionMessage,
                                 boolean returnEnabled,
                                 String returnType,
                                 String returnValue,
                                 int probabilityPercent,
                                 long maxTriggerCount) {
        this.targetClass = targetClass;
        this.targetBehavior = targetBehavior;
        this.timeoutEnabled = timeoutEnabled;
        this.timeoutMillis = timeoutMillis;
        this.exceptionEnabled = exceptionEnabled;
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
        this.returnEnabled = returnEnabled;
        this.returnType = returnType;
        this.returnValue = returnValue;
        this.probabilityPercent = probabilityPercent;
        this.maxTriggerCount = maxTriggerCount;
    }

    public static FaultInjectionConfig load() throws IOException {
        Properties properties = new Properties();

        String externalPath = System.getProperty(CONFIG_PATH_PROPERTY);
        if (externalPath != null && !externalPath.trim().isEmpty()) {
            try (InputStream in = new FileInputStream(externalPath.trim())) {
                properties.load(in);
            }
        } else {
            try (InputStream in = FaultInjectionConfig.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
                if (in == null) {
                    throw new IOException("Missing classpath resource: " + DEFAULT_CONFIG_RESOURCE);
                }
                properties.load(in);
            }
        }

        String targetClass = normalizeClassName(require(properties, "target.class"));
        String targetBehavior = properties.getProperty("target.behavior", "*").trim();

        boolean timeoutEnabled = Boolean.parseBoolean(properties.getProperty("inject.timeout.enabled", "false").trim());
        long timeoutMillis = parseLong(properties.getProperty("inject.timeout.millis", "0"), 0L);
        if (timeoutEnabled && timeoutMillis < 0) {
            timeoutMillis = 0;
        }

        boolean exceptionEnabled = Boolean.parseBoolean(properties.getProperty("inject.exception.enabled", "false").trim());
        String exceptionClass = properties.getProperty("inject.exception.class", "java.lang.RuntimeException").trim();
        String exceptionMessage = properties.getProperty("inject.exception.message", "fault injected").trim();
        boolean returnEnabled = Boolean.parseBoolean(properties.getProperty("inject.return.enabled", "false").trim());
        String returnType = properties.getProperty("inject.return.type", "string").trim();
        String returnValue = properties.getProperty("inject.return.value", "").trim();
        int probabilityPercent = parseInt(properties.getProperty("inject.probability.percent", "100"), 100);
        if (probabilityPercent < 0) {
            probabilityPercent = 0;
        } else if (probabilityPercent > 100) {
            probabilityPercent = 100;
        }
        long maxTriggerCount = parseLong(properties.getProperty("inject.max.trigger.count", "-1"), -1L);

        return new FaultInjectionConfig(
                targetClass,
                targetBehavior,
                timeoutEnabled,
                timeoutMillis,
                exceptionEnabled,
                exceptionClass,
                exceptionMessage,
                returnEnabled,
                returnType,
                returnValue,
                probabilityPercent,
                maxTriggerCount
        );
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return value.trim();
    }

    private static String normalizeClassName(String className) {
        return className.replace('/', '.');
    }

    public boolean hasAnyInjectionEnabled() {
        return timeoutEnabled || exceptionEnabled || returnEnabled;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public String getTargetBehavior() {
        return targetBehavior;
    }

    public boolean isTimeoutEnabled() {
        return timeoutEnabled;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public boolean isExceptionEnabled() {
        return exceptionEnabled;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public boolean isReturnEnabled() {
        return returnEnabled;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getReturnValue() {
        return returnValue;
    }

    public int getProbabilityPercent() {
        return probabilityPercent;
    }

    public long getMaxTriggerCount() {
        return maxTriggerCount;
    }

    public String toSummary() {
        return "target=" + targetClass + "." + targetBehavior
                + ", timeoutEnabled=" + timeoutEnabled
                + ", timeoutMillis=" + timeoutMillis
                + ", exceptionEnabled=" + exceptionEnabled
                + ", exceptionClass=" + exceptionClass
                + ", exceptionMessage=" + exceptionMessage
                + ", returnEnabled=" + returnEnabled
                + ", returnType=" + returnType
                + ", returnValue=" + returnValue
                + ", probabilityPercent=" + probabilityPercent
                + ", maxTriggerCount=" + maxTriggerCount;
    }

    public FaultInjectionConfig withExceptionOnly(String newExceptionClass, String newExceptionMessage) {
        return new FaultInjectionConfig(
                this.targetClass,
                this.targetBehavior,
                false,
                0L,
                true,
                newExceptionClass,
                newExceptionMessage,
                false,
                this.returnType,
                this.returnValue,
                this.probabilityPercent,
                this.maxTriggerCount
        );
    }
}
