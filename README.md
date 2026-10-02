# zza-cloud

Spring Cloud 微服务工程的骨架：**JDK 17 + Spring Boot 3.3.5 + Spring Cloud 2023.0.3 + Spring Cloud Alibaba 2023.0.3.2**。

## 坐标与包名约定

| 项 | 值 |
| --- | --- |
| groupId | `com.wxy` |
| 父工程 artifactId | `zza-cloud` |
| 依赖管理模块 artifactId | `dependencies` |
| 公共模块 artifactId | `common` → 包名 `com.wxy.common` |
| 业务服务 | 嵌套 `user/api` + `user/biz`，artifactId 为 `user-api`、`user-biz` → 包名 `com.wxy.user.api`、`com.wxy.user.biz` |

规则一句话：**groupId 统一 `com.wxy`，包名 = `com.wxy` + artifactId（连字符换成点）**。所以 `common` → `com.wxy.common`，`user-api` → `com.wxy.user.api`。

### 子模块要不要自己引入 `dependencies`

不用。父工程 `zza-cloud` 已经用 `import` 把 `dependencies` 引进来了，子模块只要 `<parent>` 指向 `zza-cloud`，第三方依赖就能直接用管理好的版本（不写 `<version>`）；工程内模块（`common`、`xxx-api`）没做登记，依赖时手动写 `<version>${project.version}</version>`。

- 不要把 `dependencies` 写进 `<dependencies>`：它是 `<packaging>pom</packaging>`，当依赖引用会报错；
- 只有 parent 不是 `zza-cloud` 的独立工程，才需要自己 `import` 一次。

## 目录结构

```
zza-cloud
├── pom.xml           父工程：parent=spring-boot-starter-parent，聚合模块 + 插件配置 + 导入依赖管理
├── dependencies       依赖管理模块（BOM）：所有依赖版本只在这里定义
└── common            公共模块：通用异常、工具类、常量（内容自己加）

将来新增业务服务时（以 user 为例）：
user/
├── api/              artifactId user-api：对外 DTO、Feign 客户端接口、对外常量
└── biz/              artifactId user-biz：服务实现，依赖 user-api
```

## 各 pom 的职责

| 文件 | 职责 |
| --- | --- |
| `pom.xml` | `<packaging>pom</packaging>`；parent 是 `spring-boot-starter-parent`；声明 `<modules>`；定义 JDK 17、编码；`dependencyManagement` 里以 `import` 方式导入 `dependencies`（工程内模块不登记，用的时候手动写版本） |
| `dependencies/pom.xml` | `<packaging>pom</packaging>`，只有 `<properties>` + `<dependencyManagement>`；parent 是 `spring-boot-starter-parent`，导入 **Spring Cloud** 与 **Spring Cloud Alibaba** 两份 BOM，并统一管理 MyBatis-Plus、Druid、Hutool、Knife4j、JJWT 等第三方依赖版本 |
| `common/pom.xml` | 普通 jar 模块，继承父工程。代码放 `common/src/main/java/com/wxy/common`，依赖版本同样由 `dependencies` 管 |

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
        <artifactId>common</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

第三方依赖不用写 `version`，由 `dependencies` 统一管理；工程内模块（`common`、`xxx-api`）没做登记，依赖时要手动写 `<version>${project.version}</version>`。服务模块的 `<build>` 里加上 `spring-boot-maven-plugin` 即可打成可执行 jar，启动类放在模块对应的包下（如 `com.wxy.gateway`）。

需要对外提供接口的服务按服务目录拆：目录 `服务名/api` 与 `服务名/biz`，artifactId 为 `服务名-api`、`服务名-biz`，`biz` 依赖 `api`（工程内模块没登记在父 pom，所以依赖时手动写 `<version>${project.version}</version>`）。

## 构建

```bash
mvn -DskipTests clean install
```

> 父工程以 `import` 方式导入同一次构建中的 `dependencies`，所以必须从根目录执行（一起构建），不要单独只构建子模块。

建议每次加完模块后跑一次 `mvn -DskipTests clean install`；只写 pom、不用任何托管依赖时，不会暴露版本问题，等写业务代码才会报错。
