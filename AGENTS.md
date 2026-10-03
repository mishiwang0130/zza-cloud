# zza-cloud 仓库开发规范

本文件只写**已经确定的技术事实**与**编码规范**，不规定业务设计；业务相关的类名、错误码、端口、接口形态等由你自己决定，不要照搬其他项目的设计。未写到的细节参照《阿里巴巴Java开发手册》执行。

## 技术栈与版本

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17 | 编译 `release=17` |
| Maven | 3.9+ | 多模块聚合工程 |
| Spring Boot | 3.3.5 | 父工程继承 `spring-boot-starter-parent` |
| Spring Cloud | 2023.0.3 | 官方组件：Gateway、OpenFeign、LoadBalancer、Bus、Resilience4j |
| Spring Cloud Alibaba | 2023.0.3.2 | Nacos、Sentinel、Seata、RocketMQ |
| Lombok | 1.18.34 | 简化样板代码，scope 为 `provided` |
| 其他 | MyBatis-Plus 3.5.7、Druid 1.2.23、MySQL 8、Hutool 5.8.32、Fastjson2 2.0.53、JJWT 0.12.6、MinIO 8.5.12、Knife4j 4.5.0、MapStruct 1.6.3、RocketMQ Spring 2.3.1 | 均在 `dependencies` 中管理 |

所有版本只在 `dependencies/pom.xml` 里定义，其他位置一律不写版本号。

## 项目结构与包名约定

```
zza-cloud              父工程 com.wxy:zza-cloud:1.0.0-SNAPSHOT（pom）
├── dependencies       依赖管理 BOM com.wxy:dependencies（pom，无代码）
└── common              公共能力聚合 com.wxy:common（pom，自己不放代码）
    ├── common-core       com.wxy:common-core       → com.wxy.common.core
    ├── common-webmvc     com.wxy:common-webmvc     → com.wxy.common.webmvc
    ├── common-webflux    com.wxy:common-webflux    → com.wxy.common.webflux
    ├── common-redis      com.wxy:common-redis      → com.wxy.common.redis
    ├── common-mybatis    com.wxy:common-mybatis    → com.wxy.common.mybatis
    ├── common-security   com.wxy:common-security   → com.wxy.common.security
    ├── common-storage    com.wxy:common-storage    → com.wxy.common.storage
    ├── common-mq         com.wxy:common-mq         → com.wxy.common.mq
    └── common-feign      com.wxy:common-feign      → com.wxy.common.feign
```

将来新增业务服务时（以 `user` 为例）：

```
user/             服务聚合 com.wxy:user（pom）
├── user-api/     对外发布 com.wxy:user-api → 包名 com.wxy.user.api
└── user-biz/     服务实现 com.wxy:user-biz → 包名 com.wxy.user.biz
```

- groupId 统一 `com.wxy`。
- 包名 = `com.wxy` + artifactId（连字符换成点）：`common-core` → `com.wxy.common.core`，`user-api` → `com.wxy.user.api`。
- 不需要对外提供接口的服务（例如网关）不拆 api/biz，单模块即可，artifactId 就是服务名，包名同理。

## 依赖管理规范

这是本仓库最容易踩坑的地方，务必遵守：

- **所有版本都只在 `dependencies/pom.xml` 里定义，模块声明依赖时一律不写 `<version>`**：第三方依赖由导入的 BOM 管，工程内模块（`common-*`、`xxx-api`）也登记在这个 BOM 里（照芋道的做法）；新增一个工程内模块时，记得同步在 BOM 里补一行登记。
- 子模块 `<parent>` 指向 `com.wxy:zza-cloud` 即可继承父工程 import 进来的依赖管理，模块自己**不需要**写 `<dependencyManagement>`。
- 版本管理和依赖传递是两条独立的链路：版本管理只通过 parent 继承（`parent` → `zza-cloud` → `import dependencies`），不会跟着 `<dependencies>` 传递。服务依赖 `common` 只是为了复用公共代码，`common` 自己也不需要引用 `dependencies`。
- **禁止把 `dependencies` 写进 `<dependencies>`**：它是 `<packaging>pom</packaging>`，当依赖引用会直接报错。
- `common-*` 都是 jar 模块，它们声明的 compile 依赖会隐式传递给引用方：所以每个子模块只声明自己这个能力必需的依赖（`common-webmvc` 才引 web，`common-redis` 才引 Redis，`common-mybatis` 才引 JDBC），`common-core` 保持零外部依赖。
- **禁止在 `dependencies` 里重复声明已被 BOM 托管的依赖**，尤其不能“声明了却不写 `<version>`”。dependencyManagement 是“先声明者优先”，这种空声明会把 BOM 的版本顶掉，子模块会报 `'dependencies.dependency.version' ... is missing`。引入新组件时优先只加 BOM 导入，BOM 没覆盖的才写明确版本。
- 父工程以 `import` 方式引入 `dependencies`，所以**父 pom 与 `dependencies` 必须能被解析到**（在同一次 reactor 里，或已 `install`/发布到仓库）。改动这两个文件后要在根目录重新构建一次，否则子模块会读到仓库里的旧版本；只改业务代码时可以只构建单个模块。

服务模块 pom 模板：

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

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

## 模块划分：api / biz / common

- 业务服务用嵌套目录拆成两个模块：服务目录下的子目录名带服务名前缀并与 artifactId 一致，例如 `user/user-api` → `user-api`、`user/user-biz` → `user-biz`（目录与 artifactId 都带前缀：不带前缀的话多个服务的 `api`、`biz` 会撞名，单看目录也分不清属于哪个服务）。
  - `api`：对外发布的内容，包含 DTO、Feign 客户端接口、对外常量等，供其他服务依赖；
  - `biz`：服务实现，包含 Controller、Service、Mapper、启动类与配置文件，打成可执行 jar 独立部署。
- 依赖方向 `biz` → `api` → `common-core`；其他服务只允许依赖你的 `api`，禁止依赖别人的 `biz`。
- 工程内模块（`common-*`、`xxx-api`）统一登记在 `dependencies` 的 BOM 里，引用时不写版本；新增模块时在 BOM 补一行即可。
- `common-*` 只放跨服务通用的内容：通用异常、工具类、常量、统一响应对象等。服务自己的业务异常类和业务工具类放在自己的模块里，不要往 common 里堆。
- 不需要对外提供接口的服务（例如网关）不拆 api/biz，单模块即可。

## common 子模块划分规则

`common` 下的子模块按**能力与依赖边界**拆，不按业务拆。满足下面任意一条，就单独开一个 `common-xxx`：

1. 需要引入独立的第三方依赖，且不是所有服务都要用（Redis、MyBatis、MinIO、MQ…）；
2. 只有某类服务会引用，或者引错了会出问题（例如 Servlet 栈的 `common-webmvc` 引到 WebFlux 网关里会冲突）；
3. 有独立的自动配置，需要能按服务启用/关闭（不引就不生效）；
4. 代码量已经大到可以独立演进与测试。

反过来，下面这些**不要**单独开模块，放 `common-core`：纯 POJO（`Result`、错误码、分页类）、零外部依赖的工具类、公共常量与枚举——所有服务都要用，拆开只会让依赖更啰嗦。

每个 `common-*` 模块都必须满足：

- 只依赖 `common-core`，模块之间不互相依赖（保持星形依赖，避免网状）；
- 引用时不写版本（已在 `dependencies` BOM 里登记）；
- 命名 `common-<能力>`，包名自然是 `com.wxy.common.<能力>`；
- 一句话能说清职责，说不清就是拆错了。

已建：`common-core`、`common-webmvc`、`common-webflux`、`common-redis`、`common-mybatis`、
`common-security`、`common-storage`、`common-mq`、`common-feign`。
后续按需：`common-log`（操作日志、traceId）。

各模块的引用方与依赖代价：

| 模块 | 谁引 | 带进来的东西 |
| --- | --- | --- |
| `common-core` | 所有服务（含网关） | 无 compile 依赖（Lombok 为 provided，不传递） |
| `common-webmvc` | 业务服务 | spring-webmvc、校验、Knife4j |
| `common-webflux` | 只有网关 | spring-webflux |
| `common-redis` | 用 Redis 的服务 | spring-data-redis、Fastjson2 |
| `common-mybatis` | 连库的服务 | MyBatis-Plus、Druid、MySQL 驱动 |
| `common-security` | 需要签发/解析凭证的服务 | JJWT、spring-boot-starter |
| `common-storage` | 用对象存储的服务 | MinIO 客户端 |
| `common-mq` | 收发消息的服务 | 无（只有常量） |
| `common-feign` | 调用其他服务的 biz | OpenFeign |

## 包组织与类命名

模块内按层分包，包名全小写，同类代码集中放同一包，类名带固定后缀：

| 包 | 放什么 | 类名规则 | 放在哪个模块 |
| --- | --- | --- | --- |
| `controller/` | 接口入口，只做参数校验和调用 Service | `XxxController` | `biz` |
| `vo/` | 接口的请求与返回实体 | 请求 `XxxReqVO`、返回 `XxxRespVO` | `biz` |
| `dto/` | 微服务之间调用用的对象 | `XxxDTO` | `api` |
| `client/` | 调用其他服务的 Feign 接口 | `XxxClient` | `api` |
| `config/` | 配置类 | `XxxConfig` | `biz` |
| `job/` | 定时任务 | `XxxJob` | `biz` |
| `service/` | 业务逻辑接口，实现放 `service/impl/` | `XxxService`、`XxxServiceImpl` | `biz` |
| `mapper/` | 数据库访问接口，SQL 写在 `resources/mapper/XxxMapper.xml` | `XxxMapper` | `biz` |
| `po/` | 数据库实体，与表一一对应 | 表 `sys_user` → `SysUser` | `biz` |
| `convert/` | 对象转换（MapStruct） | `XxxConvert` | 按需 |
| `util/` | 通用工具类 | `XxxUtil` | 按需 |

- 完整包名 = 模块包 + 层包，例如 `com.wxy.zza.biz.controller`、`com.wxy.zza.api.dto`。
- `dto` 只放跨服务传输的对象，服务对外接口的请求与返回用 `vo`，两者不要混用：其他服务依赖 `xxx-api` 拿到 `XxxDTO`，前端调接口拿到 `XxxRespVO`。
- `controller` 只做参数校验和调用 Service，不写业务逻辑。
- `config` 包里只放 `XxxConfig` 配置类，不要把工具类、常量塞进来。
- 业务服务的 `biz` 模块要区分管理后台与用户端，**层包在外、`admin`/`app` 在内**（与芋道一致，不是 `admin/controller` 那种倒过来）：`controller/admin`、`controller/app`，`vo` 同样按端分。与端无关的 `mapper`、`po`、`convert`、`util` 放在 `biz` 根下共用：

```
com.wxy.infra.biz
├── controller
│   ├── admin         UserAdminController
│   └── app           UserAppController
├── vo
│   ├── admin         管理后台的 XxxReqVO / XxxRespVO
│   └── app           用户端的 XxxReqVO / XxxRespVO
├── service           业务逻辑（两端共用，只在某一端用的方法写在对应端包下）
├── mapper            UserMapper（两端共用）
├── po                User（两端共用）
└── convert           UserConvert（两端共用）
```

- 公共类的落点（包名 = 模块包 + 层包）：
  - `common-core`：`com.wxy.common.core.result`（`Result`、`ErrorCode`、`CommonErrorConstant`）、`com.wxy.common.core.exception`（`BizException`）、`com.wxy.common.core.vo`（`PageReqVO`、`PageRespVO`）、`com.wxy.common.core.constant`、`com.wxy.common.core.util`；
  - `common-webmvc`：`com.wxy.common.webmvc.exception`（全局异常处理器）、`com.wxy.common.webmvc.config`（端前缀等 WebMvc 配置）；
  - `common-redis`：`com.wxy.common.redis.util`（`RedisUtil`、`TokenCacheKeyUtil`）、`com.wxy.common.redis.constant`（`CommonRedisKeyConstant`）、`com.wxy.common.redis.config`（`RedisConfig`）、`com.wxy.common.redis.bo`（`TokenCacheBO`）、`com.wxy.common.redis.security`（`CacheFirstTokenValidator`）；
  - `common-mybatis`：`com.wxy.common.mybatis.config`（`MybatisPlusConfig`）、`com.wxy.common.mybatis.po`（`BasePO`）、`com.wxy.common.mybatis.handler`（`AuditMetaObjectHandler`）、`com.wxy.common.mybatis.util`（`PageUtil`）；
  - `common-security`：`com.wxy.common.security.util`（`JwtUtil`）、`com.wxy.common.security.config`（`JwtProperties`、`SecurityConfig`）、`com.wxy.common.security.constant`（`TokenConstant`）；
  - `common-webflux`：`com.wxy.common.webflux.handler`（`GlobalWebExceptionHandler`）、`com.wxy.common.webflux.config`（`WebFluxConfig`）；
  - `common-storage`：`com.wxy.common.storage.util`（`MinioUtil`）、`com.wxy.common.storage.config`（`MinioProperties`、`MinioConfig`）；
  - `common-mq`：`com.wxy.common.mq.constant`（`CommonMqConstant`）；
  - `common-feign`：`com.wxy.common.feign.interceptor`（`UserContextFeignInterceptor`）、`com.wxy.common.feign.decoder`（`FeignErrorDecoder`）、`com.wxy.common.feign.config`（`FeignConfig`）。

- 每个 `common-*` 的自动配置都注册在 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，
  所以业务服务不需要把 `com.wxy.common` 加进 `@SpringBootApplication` 的扫描范围；服务想覆盖默认实现时，声明同类型 Bean 即可。

## 对象与 JSON 转换规范

- **禁止使用 `cn.hutool.core.bean.BeanUtil`、`org.springframework.beans.BeanUtils` 这类反射拷贝工具**：字段改名、类型变化、嵌套对象时容易静默丢字段，排查成本高。
- 对象转换统一用 MapStruct：转换接口命名 `XxxConvert`，用 `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)` 声明，交由 Spring 注入使用。注意 MapStruct 的注解是 `org.mapstruct.Mapper`，和 MyBatis 的 `org.apache.ibatis.annotations.Mapper` 同名不同包，别导错。
- 字段很少或需要特殊处理时，手写 setter 或用构造方法也可以，但同样禁止绕回 `BeanUtil`/`BeanUtils`。
- 转换要处理 null 入参：MapStruct 会自动生成判空，手写转换同样要判空。
- MapStruct 的注解处理器由父工程的 `annotationProcessorPaths` 统一提供，版本同样取自 `dependencies`（父工程不重复写版本），模块只需声明 `org.mapstruct:mapstruct` 依赖。注意该路径一旦指定，classpath 上的处理器不再生效——若某模块要用 `spring-boot-configuration-processor` 之类的处理器，需要把它加到父工程的 `annotationProcessorPaths` 中。
- **JSON 序列化与反序列化统一用 Fastjson2**（`com.alibaba.fastjson2:fastjson2`，版本由 `dependencies` 统一管理，模块里不写版本）：`JSON.toJSONString(obj)`、`JSON.parseObject(str, Xxx.class)`、`JSON.parseArray(str, Xxx.class)`。
- 禁止在业务代码里混用其他 JSON 库：Jackson 的 `ObjectMapper`、Hutool 的 `JSONUtil`、fastjson 1.x（`com.alibaba.fastjson`）都不用。
- 禁止手拼 JSON 字符串：先把数据组装成对象，再交给 Fastjson2 序列化。
- 需要保留 null 字段、时间格式、大数字等特殊处理时，统一用 Fastjson2 的 `JSONWriter.Feature`、`JSONReader.Feature` 配置，不要各写各的。
- 注意边界：HTTP 接口的请求体/响应体默认由 Spring Boot 自带的 Jackson 通过 `HttpMessageConverter` 处理；本条约束的是业务代码里主动做的 JSON 转换（Redis 值、MQ 消息体、调用第三方接口、日志打点等）。如果要把 HTTP 层也换成 Fastjson2，需要额外配置消息转换器并在本节补充说明。

## 错误码规范

错误码固定 **10 位数字**，按 `PSSMMMEEEE` 分段：项目位 1 位 + 服务位 2 位 + 模块位 3 位 + 错误位 4 位。

| 段 | 位数 | 含义 |
| --- | --- | --- |
| `P` | 1 | 项目位，本项目固定为 `1`，表示"这就是本项目的异常"；其他项目依次用 `2`、`3`…，这样多个项目的错误码放进同一份日志、监控或错误码文档里也不会冲突 |
| `SS` | 2 | 微服务位，已固定：`00` common、`01` 网关、`02` infra（基础服务）、`03` zza、`04` 智能客服 |
| `MMM` | 3 | 服务内模块位，由各服务自行分配，例如 infra 的 user 是 `001`、权限是 `002` |
| `EEEE` | 4 | 模块内具体错误位，从 `0001` 起递增，例如 infra 的「用户不存在」是 `1_02_001_0001` |

- 服务位只有上表这 5 个，已固定，不新增、不复用。
- 10 位数字是一个整体，`1_02_001_0001` 只是便于阅读的写法，下划线不是内容的一部分。
- 同一个错误在所有环境、所有接口下都用同一个错误码；一个错误码只对应一种含义，禁止复用或一码多义。
- 模块位与错误位在服务内递增分配，不回填已删除的号段。
- 错误码常量类的命名与位置见「接口响应与异常规范」（`CommonErrorConstant` / `<服务名>ErrorConstant`），禁止在业务代码里直接写数字字面量。

**类型用 `int` 即可（已实测）**：代码里的 `code` 字段用 `int`。项目位固定为 `1`，10 位码的理论最大值是 `1_99_999_9999` = `1999999999`，小于 `Integer.MAX_VALUE` = `2147483647`；服务位实际只用到 `00`~`04`，真实上限更低（`1_04_999_9999` = `1049999999`）。这些值都是合法的十进制 int 字面量，能正常编译，不需要 `long`，也不需要字符串补零。

**项目位不要用 `0`**：写成 `0` 就退化成 9 位，而且数字字面量以 `0` 开头会被 Java 当成**八进制**（`020010001` 实际等于 `4198401`，编译还不报错，属于静默出错）。用 `1` 开头天生没有这个问题，也不需要字符串补零。

## 接口响应与异常规范

- 所有接口统一返回 `Result<T>`，字段固定 `code`、`msg`、`data`（定义在 `common`）。
- 成功和业务异常统一返回 HTTP 200，失败语义全部由 `code` 表达；禁止用 HTTP 400、403 表示业务失败。
- 只有鉴权失败返回 HTTP 401（未登录、token 失效）；参数校验失败返回 400；路由资源不存在返回 404；未捕获的系统异常返回 500。
- 业务错误统一 `throw new BizException(...)` 抛出，Controller 不得自行拼装错误响应。
- 自定义异常统一叫 `BizException`（放 `common`），**所有服务共用同一个异常类**，构造时传入错误码常量；服务之间的差别只在错误码——公共错误码用 `CommonErrorConstant`，业务错误码用各自的 `<服务名>ErrorConstant`（如 `InfraErrorConstant`），不要为每个服务再造一个异常类。
- 全局异常处理统一放 `common`，负责把 `BizException` 与其他异常转成 `Result`，各服务不重复实现。
- 错误码常量的类名与位置：
  - 公共错误码：`CommonErrorConstant`，放 `common`，所有服务共用（参数错误、未登录、系统异常等）；
  - 业务错误码：`<服务名>ErrorConstant`，放各服务自己的模块，例如 zza 服务的 `ZzaErrorConstant`、infra 服务的 `InfraErrorConstant`。
- 错误码常量的类型统一为 `ErrorCode`（错误码 + 提示信息），业务代码只引用常量，禁止直接写数字。

## 接口设计规范

- 路径用「资源 + 动作」的写法（参考 RuoYi）：`/user/getById`、`/user/list`、`/user/create`、`/user/update`、`/user/delete`。
- 不使用「同一个路径靠 HTTP 方法区分增删查改」的 REST 写法（`GET/POST/PUT/DELETE /user` 那种），路径必须自解释。
- 路径用 camelCase，与 Java 方法名风格一致；一个接口只做一件事。
- 业务服务的接口按端分开实现：管理后台放 `admin` 包、用户端放 `app` 包，同名业务两端各写一份 Controller，例如 `UserAdminController#getById` 与 `UserAppController#getById`。
- 端前缀固定为 `/admin-api`（管理后台）与 `/app-api`（用户端），由服务里的配置类按包名自动拼接：`admin` 包 → `/admin-api`，`app` 包 → `/app-api`；Controller 上只写业务路径（`@RequestMapping("/user")`），**前缀不手写**。
- 端前缀的实现方式（代码待补）：前缀与包名规则放配置里（`/admin-api` ↔ `**.controller.admin.**`、`/app-api` ↔ `**.controller.app.**`），再用 `WebMvcConfigurer#configurePathMatch` + `addPathPrefix` 按 Controller 所在包名匹配自动加。芋道就是这么做的（`WebProperties` 默认 `adminApi = /admin-api + **.controller.admin.**`、`appApi = /app-api + **.controller.app.**`）。**这套配置还没写，写第一个接口前要先补上。**
- 参数校验统一用 `@Validated`。

## 网关与路径前缀规范

对外完整路径 = **网关前缀 + 服务名 + 端前缀 + 接口路径**：

```
/api/infra/admin-api/user/getById
 │     │         │        └── 业务路径（资源 + 动作）
 │     │         └─────────── 端前缀：/admin-api 或 /app-api，服务里按包名自动加
 │     └───────────────────── 服务名：网关据此路由，例如 /api/infra/** → lb://infra
 └─────────────────────────── 网关前缀，前端只认 /api
```

- `/api` 是网关前缀，用于进入网关；`{服务名}` 用于转发，网关配 `StripPrefix=2` 去掉这两段后再转发。
- 服务最终收到的是 `/admin-api/user/getById`，与服务自身的端前缀一致；网关不改写业务路径。
- 路径里必须能看出服务名：否则多个服务都有 `/user` 这类同名资源时，网关无法按路径判断转发给谁，只能给每个服务写死一堆具体路径。
- 网关的路由规则按服务一条：`/api/{服务名}/**` → `lb://{服务名}`，新增服务时同步加一条路由配置。
- `{服务名}` 是服务在 Nacos 里的注册名，等于该服务 `biz` 模块的 `spring.application.name`（例如 `infra`、`zza`、`ai-agent`），**不带 `-biz` 后缀**；artifactId 仍然是 `infra-biz`，只是注册名不用模块名，否则网关按 `lb://{服务名}` 找不到实例。

## 分页规范

- 分页只维护一套公共类，都放 `common-core` 的 `com.wxy.common.core.vo`：
  - 查询类 `PageReqVO`：`pageNum`（从 1 开始，默认 1）、`pageSize`（默认 20）；
  - 返回类 `PageRespVO<T>`：`total`、`pageNum`、`pageSize`、`records`（当前页数据），提供静态方法 `of(total, pageNum, pageSize, records)`。
- 分页接口的入参统一用 `PageReqVO`，返回 `Result<PageRespVO<XxxRespVO>>`；各服务不得自己再造一套分页参数或返回结构。
- Service 层用 MyBatis-Plus 的 `Page<T>` 查库（分页插件统一在 `common` 里配置），返回前转成 `PageRespVO`。

## 编码与命名规范

- Java 使用 4 空格缩进、UTF-8 编码、JDK 17，使用 Lombok 简化样板代码。
- 类名 PascalCase，方法与字段 camelCase，常量 UPPER_SNAKE_CASE，包名全小写。
- 启动类命名为 `XxxApplication`。
- 数据库表名与字段名 snake_case，Java 字段 camelCase，依赖 MyBatis-Plus 的 `map-underscore-to-camel-case` 自动映射。
- 禁止魔法值；禁止用 `Executors` 创建线程池；`equals` 用常量或确定非空对象调用；POJO 布尔字段不加 `is` 前缀。

## 持久层与 SQL 规范

- **禁止在 `Mapper.java` 接口上用 `@Select`、`@Update`、`@Insert`、`@Delete` 以及 `@SelectProvider`、`@UpdateProvider`、`@InsertProvider`、`@DeleteProvider` 这类注解手写 SQL。**
- 所有 SQL 一律写在 XML 中，路径 `{模块}/src/main/resources/mapper/XxxMapper.xml`，`<mapper namespace>` 指向对应接口的全限定名，接口方法名与 XML 中的 `id` 一致。
- 允许使用的注解：`@Mapper`、`@Param`，以及 MyBatis-Plus 的 `@TableName`、`@TableId`、`@TableField`、`@TableLogic` 等映射类注解；它们不是手写 SQL。
- SQL 一律使用 `#{}` 预编译占位符，禁止用 `${}` 拼接外部输入。
- 动态标签（`<if>`、`<foreach>` 等）保持清晰缩进；改动 SQL 时同步更新接口上的 Javadoc。

## 数据库规范

- 表名 = 服务名 + `_` + 业务表名，用服务名做前缀：infra 服务的用户表 `infra_user`、zza 服务的订单表 `zza_order`；跨服务共用的系统表用 `sys_` 前缀。
- 字段名 snake_case，Java 字段 camelCase，靠 MyBatis-Plus 的 `map-underscore-to-camel-case` 自动映射。
- 每张表都要带公共字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键，自增 |
| `create_time` | `DATETIME` | 创建时间，`DEFAULT CURRENT_TIMESTAMP` |
| `create_by` | `BIGINT` | 创建人 ID，默认 `0`（0 表示系统或未登录） |
| `update_time` | `DATETIME` | 更新时间，`ON UPDATE CURRENT_TIMESTAMP` |
| `update_by` | `BIGINT` | 更新人 ID，默认 `0` |
| `is_delete` | `TINYINT` | 逻辑删除：0 未删除、1 已删除 |

- 索引命名：唯一索引 `uk_<表名>_<字段>`，普通索引 `idx_<表名>_<字段>`。
- 建表统一 `ENGINE = InnoDB`、`DEFAULT CHARSET = utf8mb4`、`COLLATE = utf8mb4_general_ci`；每个字段和表都要写 `COMMENT`。
- 逻辑删除字段固定用 `is_delete`，PO 上配 `@TableLogic`。
- 建表与初始化脚本集中放 `sql/` 目录，一个工程一份总脚本，新增业务表追加进去，不允许只改本地库不入脚本。

## 缓存与消息规范

Redis key 与 MQ 的 topic/tag 都用**三段前缀**拼接：`全局前缀 + 模块前缀 + 具体业务键`。

- **跨模块共享的有三样**：`RedisUtil`（读写工具）、全局前缀常量 `CommonRedisKeyConstant.PREFIX`，以及**平台凭证缓存**（`CommonRedisKeyConstant.TOKEN`/`REFRESH_TOKEN` + `TokenCacheBO` + `TokenCacheKeyUtil`，由签发凭证的服务写、其他服务读，未命中再回源）；除此之外 key 常量类与 key 拼接方法一个模块一份，各模块在自己的常量类里拼自己的模块前缀，common 目前不需要落 Redis 业务数据，所以只维护 `CommonRedisKeyConstant`（将来 common 自己要存 Redis 数据时，再按服务的做法建 `CommonRedisKeyUtil`，一个 key 一个方法）：

```java
// common-redis：全局前缀与 common 自己的模块前缀
CommonRedisKeyConstant.PREFIX = "zza:";
CommonRedisKeyConstant.COMMON = CommonRedisKeyConstant.PREFIX + "common:";   // zza:common:

// 服务模块（infra 为例，各服务自己维护）
InfraRedisKeyConstant.PREFIX = CommonRedisKeyConstant.PREFIX + "infra:";    // zza:infra:
```

- **一个 key 一个方法、方法内部自己拼**（例如 `InfraRedisKeyUtil.userTokenKey(userId)` 返回 `InfraRedisKeyConstant.PREFIX + "token:" + userId`），不要提供通用的 `buildKey(...)`：通用拼接把「key 长什么样」推给调用方，key 结构一改就要满仓库找调用点。
- 业务代码只引用常量，禁止硬编码字符串；Redis key 用 `:` 分隔。
- MQ 的 topic/tag 只能用字母、数字、`_`、`-`（RocketMQ 不允许 `:`），用 `-` 分隔，例如 `zza-infra-order-created`；模块前缀同样由服务自己的常量类拼：`InfraMqConstant.PREFIX = CommonMqConstant.PREFIX + "-infra"`。

Redis：

- `RedisUtil` 是所有模块共用的读写工具（按数据类型提供方法）；key 常量类与 key 拼接方法每个模块一份，common 维护的是 `CommonRedisKeyConstant`。业务代码不直接用 `RedisTemplate`，也不手写 key 字符串。
- 凭证缓存是唯一的跨模块共享 key：key 形如 `zza:token:{摘要}`（不带模块段，因为它不属于某一个服务），值为 `TokenCacheBO`；签发凭证的服务负责写入与失效，其他服务直读，未命中或已过期一律回源校验，禁止「缓存没查到就放行」。
- 客户端统一用 `StringRedisTemplate`，Redis 里存的是 **JSON 字符串**（由 Fastjson2 转换）：不配置 `RedisTemplate<String,Object>` 的默认类型序列化，也不用 JDK 序列化。这样 `redis-cli` 直接可读，也不会因为类名或字段变化就反序列化失败。
- `RedisUtil` 按数据类型提供方法：String（`set`、`setIfAbsent`、`get`、`increment`、`delete`、`expire`、`hasKey`、`scanKeys`）、Hash、List、Set、ZSet；键空间可控时用 `scanKeys`（底层 SCAN），禁止用 `KEYS`。
- 除确实不需要过期的 key（例如固定字典数据）外，**所有 key 都必须设置过期时间**；不需要过期的要在常量类里注明原因。
- 带过期时间的 String 写入固定为 `set(key, value, timeout, timeUnit)`：`timeout` 为 `long`，`timeUnit` 为 `java.util.concurrent.TimeUnit`。
- key 的序列化方式等公共配置统一放 `common`，各服务不重复配置。

MQ：

- **只提供常量类，不提供 util**：每个模块一份常量类，`CommonMqConstant` 只放全局前缀 `PREFIX` 与 common 自己的模块段 `COMMON`，服务在自己的常量类里拼自己的模块段并写全 topic / tag，生产者和消费者引用同一份常量。

## 注释规范

- 每个类和接口都必须写 Javadoc，说明职责与使用场景，并按《阿里巴巴Java开发手册》标注 `@author`、`@date`。
- 每个方法必须有注释说明用途；有参数、返回值或异常时补充 `@param`、`@return`、`@throws`。
- 每个字段必须有注释，说明含义、取值范围、单位或约束；PO 字段注明对应库表字段含义，DTO/VO 字段注明业务含义。
- 字段注释写在字段上方，统一 `/** ... */` 风格，禁止行尾 `//` 注释；类和方法禁止用 `//` 或 `/* */` 代替 Javadoc。
- 重点解释「为什么这么做」、业务规则、边界条件和特殊处理，不要逐行翻译代码。
- 复杂分支、循环、事务、缓存、并发前后必须注释说明意图。
- 修改代码同步更新注释，禁止保留被注释掉的废弃代码和无意义注释。

## Spring 注入与事务规范

- Bean 注入统一使用 `@Resource`（`jakarta.annotation.Resource`），禁止使用 `@Autowired`；字段名与 Bean 名保持一致。
- 涉及数据库写操作的方法必须加事务，并显式指定回滚范围：

```java
@Resource
private XxxMapper xxxMapper;

/**
 * 方法用途说明
 */
@Transactional(rollbackFor = Exception.class)
public void doSomething() {
    // 写库逻辑
}
```

- 事务注解只加在 Spring 管理的 public 方法上，不加在 Controller、Mapper 或私有方法上；类内自调用不会开启事务，需要拆到独立 Bean。
- 单条查询不加事务；需要一致性的多次查询可加 `@Transactional(readOnly = true)`。

## 配置与环境

- 配置按 Profile 隔离，禁止把数据库连接、密码、密钥写死在 `application.yml`。
- 密钥、口令在任何环境都不得写入 YAML 与代码，只能从环境变量读取。
- 本地私有覆盖配置放已忽略的 `application-*.local.yml`。
- 新增配置项要在各环境对应的配置文件中同步补齐。

## 构建与运行命令

```bash
mvn -DskipTests clean install            # 从根目录构建全部模块（改动 dependencies / 父 pom 后必须跑）
mvn -pl <模块名> -am -DskipTests install # 只构建指定模块及其在本工程内的依赖模块
mvn -pl <模块名> -DskipTests install     # 只构建指定模块，parent 与依赖从仓库解析
mvn -f <模块名>/pom.xml -DskipTests install  # 完全脱离聚合工程构建单个模块
mvn -pl <模块名> -am spring-boot:run     # 启动单个服务
mvn test                                 # 运行测试
```

## 测试规范

- 新增功能必须补测试，测试代码放 `{模块}/src/test/java`，包结构与主代码一致，类名以 `Test` 结尾。
- 测试不得依赖真实网络、数据库、Redis 等外部环境，外部依赖一律 mock。
- 提交前至少执行 `mvn test`。

## Git 提交规范

采用 Conventional Commits，格式 `<type>(<scope>): <描述>`：

- `type`：`feat`、`fix`、`refactor`、`docs`、`style`、`test`、`chore`、`perf`。
- `scope`：模块名，如 `common`、`dependencies`；影响多个模块时可省略。
- 描述动词开头、简洁明确，不超过 50 字，句末不加句号。
- 一次提交只做一件事，不把格式化、重构和功能混在一起。
- 禁止提交 `target/`、`.idea/`、任何密钥与本地私有配置。

## 安全

- 不提交密钥；敏感配置走环境变量。
- 日志中禁止打印密码、Token、身份证、手机号等敏感信息。
