# 团队通用故障注入工具

这是一个基于 Alibaba JVM Sandbox 的可复用故障注入模块，用于团队统一模拟常见故障场景。

## 改造内容

- 去除了业务代码中的硬编码类名和异常。
- 增加了配置驱动注入（`fault-injection.properties`）。
- 提供了团队可复用的通用命令：
  - `reload-injection-config`
  - `show-injection-config`
  - `list-rules`
  - `show-stats`
  - `reset-stats`
  - `clear-rules`
  - `load-injection`
  - `preset-db-query-timeout`
  - `preset-db-timeout`
  - `preset-db-get-connection-fail`
  - `preset-dubbo-timeout`
  - `preset-dubbo-remoting-timeout`
  - `preset-dubbo-remoting-fail`
  - `preset-kafka-send-fail`
  - `preset-kafka-timeout`
  - `preset-kafka-network-fail`
  - `preset-kafka-serialization-fail`
  - `preset-redis-command-timeout`
  - `preset-redis-connection-fail`
  - `preset-http-socket-timeout`
  - `preset-http-connect-fail`
  - `preset-http-resource-access-fail`
  - `preset-thread-timeout`
  - `preset-thread-rejected`

## 构建

`jvm-sandbox 1.4.0` 的构建依赖要求 JDK 8。

```bash
mvn clean package -DskipTests
```

产物示例：

- `target/team-fault-injector-1.0-SNAPSHOT.jar`
- `target/team-fault-injector-1.0-SNAPSHOT-jar-with-dependencies.jar`

## 配置

默认配置文件（classpath）：

- `src/main/resources/fault-injection.properties`

也可以通过外部配置覆盖（推荐按环境管理）：

```bash
-Dfault.injector.config=/path/to/fault-injection.properties
```

配置项说明：

- `target.class`（必填）：目标全限定类名。
- `target.behavior`（可选）：目标方法名，或 `*`（全部方法）。
- `inject.timeout.enabled`：是否开启超时注入（`true/false`）。
- `inject.timeout.millis`：超时注入时延（毫秒）。
- `inject.exception.enabled`：是否开启异常注入（`true/false`）。
- `inject.exception.class`：注入异常类名。
- `inject.exception.message`：注入异常消息。
- `inject.return.enabled`：是否开启返回值注入（`true/false`）。
- `inject.return.type`：返回值类型，支持 `string`、`int`、`long`、`double`、`boolean`、`null`。
- `inject.return.value`：返回值内容。
- `inject.probability.percent`：命中概率，范围 `0-100`。
- `inject.max.trigger.count`：最大触发次数，`-1` 表示不限制。

## 命令使用

模块加载并激活后，按以下顺序执行： 

1. `reload-injection-config`：重新加载配置。
2. （可选）执行一个 `preset-*` 命令切换为常见故障场景。
3. `show-injection-config` 或 `list-rules`：确认当前生效配置。
4. `show-stats`：查看规则运行统计。
5. `load-injection`：按当前配置安装或启用注入规则。
6. `reset-stats`：重置规则统计。
7. `clear-rules`：清空当前规则并重置统计。

说明：

- `preset-*` 只会修改异常注入相关字段，保留 `target.class` 和 `target.behavior` 不变。
- 需要在 `load-injection` 前执行 `preset-*`。
- 如果 watcher 已安装，新的配置和预置场景会直接热更新到当前规则。
- `list-rules` 会显示当前规则配置、是否启用、watcher 是否安装。
- `show-stats` 会显示命中次数、实际触发次数、概率未命中次数、次数上限跳过次数、累计延迟、平均延迟、最近命中时间、最近触发时间、最近注入异常类、最近注入返回值、最近跳过原因。
- 当 `inject.max.trigger.count` 达到上限后，规则会自动关闭。
- 当 `inject.return.enabled=true` 且命中规则时，会直接返回配置值；如果同时开启异常注入，返回值注入会先结束方法，异常不会再抛出。

## 预置场景分类

- DB
  - `preset-db-query-timeout`
  - `preset-db-timeout`
  - `preset-db-get-connection-fail`

- Dubbo
  - `preset-dubbo-timeout`
  - `preset-dubbo-remoting-timeout`
  - `preset-dubbo-remoting-fail`

- Kafka
  - `preset-kafka-send-fail`
  - `preset-kafka-timeout`
  - `preset-kafka-network-fail`
  - `preset-kafka-serialization-fail`

- Redis
  - `preset-redis-command-timeout`
  - `preset-redis-connection-fail`

- HTTP
  - `preset-http-socket-timeout`
  - `preset-http-connect-fail`
  - `preset-http-resource-access-fail`

- 线程/并发
  - `preset-thread-timeout`
  - `preset-thread-rejected`

## 配置示例

```properties
target.class=com.example.**
target.behavior=**
inject.timeout.enabled=true
inject.timeout.millis=3000
inject.exception.enabled=false
inject.exception.class=java.lang.RuntimeException
inject.exception.message=simulated failure
inject.return.enabled=false
inject.return.type=string
inject.return.value=mocked-response
inject.probability.percent=30
inject.max.trigger.count=5
```
