# zza-cloud

Spring Cloud 微服务工程的骨架：**JDK 17 + Spring Boot 3.3.5 + Spring Cloud 2023.0.3 + Spring Cloud Alibaba 2023.0.3.2**。

## 坐标与包名约定

| 项 | 值 |
| --- | --- |
| groupId | `com.wxy` |
| 父工程 artifactId | `zza-cloud` |
| 依赖管理模块 artifactId | `dependencies` |
| 公共模块 | 聚合模块 `common` + 9 个子模块（core、webmvc、webflux、redis、mybatis、security、storage、mq、feign）→ 包名 `com.wxy.common.core`、`com.wxy.common.webmvc`… |
| 业务服务 | 嵌套 `user/api` + `user/biz`，artifactId 为 `user-api`、`user-biz` → 包名 `com.wxy.user.api`、`com.wxy.user.biz` |

规则一句话：**groupId 统一 `com.wxy`，包名 = `com.wxy` + artifactId（连字符换成点）**。所以 `common-core` → `com.wxy.common.core`，`user-api` → `com.wxy.user.api`。

### 子模块要不要自己引入 `dependencies`

不用。父工程 `zza-cloud` 已经用 `import` 把 `dependencies` 引进来了，子模块只要 `<parent>` 指向 `zza-cloud`，所有依赖（第三方 + 工程内模块）都能直接用管理好的版本，**一律不写 `<version>`**；工程内模块也登记在 `dependencies` 的 BOM 里，新增模块时补一行登记。

- 不要把 `dependencies` 写进 `<dependencies>`：它是 `<packaging>pom</packaging>`，当依赖引用会报错；
- 只有 parent 不是 `zza-cloud` 的独立工程，才需要自己 `import` 一次。

## 目录结构

```
zza-cloud
├── pom.xml           父工程：parent=spring-boot-starter-parent，聚合模块 + 插件配置 + 导入依赖管理
├── dependencies       依赖管理模块（BOM）：所有依赖版本只在这里定义
└── common            公共能力聚合（自己不放代码）
    ├── common-core       统一响应、错误码、异常、分页、通用工具（零外部依赖）
    ├── common-webmvc     全局异常处理、端前缀配置、参数校验、接口文档（Servlet 栈）
    ├── common-webflux    网关异常处理（只有网关引，WebFlux 栈）
    ├── common-redis      RedisUtil（共用）+ CommonRedisKeyConstant（全局 key 前缀）
    ├── common-mybatis    MyBatis-Plus 配置、BasePO、审计字段填充、Druid
    ├── common-security   JwtUtil / JwtProperties（JWT 签发与解析）
    ├── common-storage    MinioUtil / MinioProperties（对象存储）
    ├── common-mq         CommonMqConstant（common 自己的 topic / tag，无第三方依赖）
    └── common-feign      Feign 透传登录上下文、统一远端调用异常

将来新增业务服务时（以 user 为例）：
user/
├── api/              artifactId user-api：对外 DTO、Feign 客户端接口、对外常量
└── biz/              artifactId user-biz：服务实现，依赖 user-api
```

## 各 pom 的职责

| 文件 | 职责 |
| --- | --- |
| `pom.xml` | `<packaging>pom</packaging>`；parent 是 `spring-boot-starter-parent`；声明 `<modules>`；定义 JDK 17、编码；`dependencyManagement` 里以 `import` 方式导入 `dependencies` |
| `dependencies/pom.xml` | `<packaging>pom</packaging>`，只有 `<properties>` + `<dependencyManagement>`；parent 是 `spring-boot-starter-parent`，导入 **Spring Cloud** 与 **Spring Cloud Alibaba** 两份 BOM，并统一管理 MyBatis-Plus、Druid、Hutool、Knife4j、JJWT 等第三方依赖版本 |
| `common/pom.xml` | 聚合模块（`<packaging>pom</packaging>`），只声明 4 个子模块，自己不放代码；子模块按能力引依赖，例如 `common-redis` 才引 Redis、`common-mybatis` 才引 JDBC |

### 依赖管理里都有什么

`dependencies` 里只有两类内容：

- **BOM 导入**：`spring-cloud-dependencies:2023.0.3`（Gateway、OpenFeign、LoadBalancer、Bus、Resilience4j 等官方组件）、`spring-cloud-alibaba-dependencies:2023.0.3.2`（Nacos、Sentinel、Seata、RocketMQ）。Spring Boot 的版本由 parent（`spring-boot-starter-parent`）提供；
- **BOM 没覆盖的第三方**：MyBatis-Plus、Druid、Hutool、Fastjson2、JJWT、MinIO、Knife4j、MapStruct、RocketMQ、Lombok。

所以这些依赖在业务模块里直接写坐标即可，不用写 version：

```xml
<dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-gateway</artifactId></dependency>
<dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-openfeign</artifactId></dependency>
<dependency><groupId>com.alibaba.cloud</groupId><artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId></dependency>
<dependency><groupId>com.alibaba.cloud</groupId><artifactId>spring-cloud-starter-alibaba-sentinel</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
<dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId></dependency>
```

实际解析到的版本（已实测）：gateway 4.1.5、openfeign 4.1.3、loadbalancer 4.1.4、circuitbreaker-resilience4j 3.1.2、nacos-discovery 2023.0.3.2、sentinel 2023.0.3.2。

### 一个必须避开的坑

BOM 已经管理的依赖，**不要在 `dependencies` 里再声明一遍**，尤其是“声明了但不写 `<version>`”。Maven 的 dependencyManagement 是“先声明者优先”，这条没有版本的空声明会把 BOM 里的版本顶掉，子模块直接报：

```
'dependencies.dependency.version' for org.springframework.cloud:spring-cloud-starter-gateway:jar is missing
```

要加别的组件时，要么只加 BOM 导入（推荐），要么给明确的 `<version>` 属性（像 MyBatis-Plus 那样）。

### 为什么 `dependencies` 的 parent 不是 `zza-cloud`

父工程要 `import` 这个 BOM，如果 BOM 又反过来继承父工程，Maven 会报循环引用：`The dependencies of type=pom and with scope=import form a cycle`。所以 BOM 独立继承 `spring-boot-starter-parent`，改 Spring Boot 版本时两个 pom 的 parent 版本要一起改。

## 新增业务模块

在根 `pom.xml` 的 `<modules>` 里加上模块名，然后新建模块 pom：

```xml
<parent>
    <groupId>com.wxy</groupId>
    <artifactId>zza-cloud</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>

<artifactId>gateway</artifactId>

<dependencies>
    <dependency>
        <groupId>com.wxy</groupId>
        <artifactId>common-core</artifactId>
    </dependency>
</dependencies>
```

所有依赖都不用写 `version`，由 `dependencies` 统一管理（工程内模块也登记在里面）。服务模块的 `<build>` 里加上 `spring-boot-maven-plugin` 即可打成可执行 jar，启动类放在模块对应的包下（如 `com.wxy.gateway`）。

需要对外提供接口的服务按服务目录拆：目录 `服务名/api` 与 `服务名/biz`，artifactId 为 `服务名-api`、`服务名-biz`，`biz` 依赖 `api`（两者都登记在 `dependencies` BOM 里，依赖时不写版本）。

## 构建

```bash
mvn -DskipTests clean install
```

> 父工程以 `import` 方式导入同一次构建中的 `dependencies`，所以必须从根目录执行（一起构建），不要单独只构建子模块。

建议每次加完模块后跑一次 `mvn -DskipTests clean install`；只写 pom、不用任何托管依赖时，不会暴露版本问题，等写业务代码才会报错。
