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
zza-cloud         父工程 com.wxy:zza-cloud:1.0.0-SNAPSHOT（pom）
├── dependencies   依赖管理 BOM com.wxy:dependencies（pom，无代码）
└── common        公共模块 com.wxy:common → 包名 com.wxy.common
```

将来新增业务服务时（以 `user` 为例）：

```
user/             服务聚合 com.wxy:user（pom）
├── api/          对外发布 com.wxy:user-api → 包名 com.wxy.user.api
└── biz/          服务实现 com.wxy:user-biz → 包名 com.wxy.user.biz
```

- groupId 统一 `com.wxy`。
- 包名 = `com.wxy` + artifactId（连字符换成点）：`common` → `com.wxy.common`，`user-api` → `com.wxy.user.api`。
- 不需要对外提供接口的服务（例如网关）不拆 api/biz，单模块即可，artifactId 就是服务名，包名同理。

## 依赖管理规范

这是本仓库最容易踩坑的地方，务必遵守：

- 第三方依赖的版本只在 `dependencies/pom.xml` 中定义，业务模块声明这些依赖时**不写 `<version>`**；工程内模块（`common`、`xxx-api`）不登记版本，谁用谁手动写 `<version>${project.version}</version>`。
- 子模块 `<parent>` 指向 `com.wxy:zza-cloud` 即可继承父工程 import 进来的依赖管理，模块自己**不需要**写 `<dependencyManagement>`。
- 版本管理和依赖传递是两条独立的链路：版本管理只通过 parent 继承（`parent` → `zza-cloud` → `import dependencies`），不会跟着 `<dependencies>` 传递。服务依赖 `common` 只是为了复用公共代码，`common` 自己也不需要引用 `dependencies`。
- **禁止把 `dependencies` 写进 `<dependencies>`**：它是 `<packaging>pom</packaging>`，当依赖引用会直接报错。
- `common` 是 jar 模块，它声明的 compile 依赖会隐式传递给所有依赖它的服务，因此 `common` 只声明自己真正需要的轻量依赖，`spring-boot-starter-web` 这类 starter 由各服务自行声明。
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
        <artifactId>common</artifactId>
        <version>${project.version}</version>
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

- 业务服务用嵌套目录拆成两个模块：服务目录下放 `api/` 与 `biz/`，artifactId 带服务名前缀，例如 `user/api` → `user-api`、`user/biz` → `user-biz`（不加前缀的话多个服务的 `api`、`biz` 会撞名）。
  - `api`：对外发布的内容，包含 DTO、Feign 客户端接口、对外常量等，供其他服务依赖；
  - `biz`：服务实现，包含 Controller、Service、Mapper、启动类与配置文件，打成可执行 jar 独立部署。
- 依赖方向 `biz` → `api` → `common`；其他服务只允许依赖你的 `api`，禁止依赖别人的 `biz`。
- 工程内模块（`common`、`xxx-api`）都不登记在父 pom 的 `dependencyManagement` 里，谁用谁在自己的依赖中手动写 `<version>${project.version}</version>`。
- `common` 只放跨服务通用的内容：通用异常、工具类、常量、统一响应对象等。服务自己的业务异常类和业务工具类放在自己的模块里，不要往 `common` 里堆。
- 不需要对外提供接口的服务（例如网关）不拆 api/biz，单模块即可。

## 对象转换规范

- **禁止使用 `cn.hutool.core.bean.BeanUtil`、`org.springframework.beans.BeanUtils` 这类反射拷贝工具**：字段改名、类型变化、嵌套对象时容易静默丢字段，排查成本高。
- 对象转换统一用 MapStruct：转换接口命名 `XxxConvert`，用 `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)` 声明，交由 Spring 注入使用。注意 MapStruct 的注解是 `org.mapstruct.Mapper`，和 MyBatis 的 `org.apache.ibatis.annotations.Mapper` 同名不同包，别导错。
- 字段很少或需要特殊处理时，手写 setter 或用构造方法也可以，但同样禁止绕回 `BeanUtil`/`BeanUtils`。
- 转换要处理 null 入参：MapStruct 会自动生成判空，手写转换同样要判空。
- MapStruct 的注解处理器由父工程的 `annotationProcessorPaths` 统一提供，版本同样取自 `dependencies`（父工程不重复写版本），模块只需声明 `org.mapstruct:mapstruct` 依赖。注意该路径一旦指定，classpath 上的处理器不再生效——若某模块要用 `spring-boot-configuration-processor` 之类的处理器，需要把它加到父工程的 `annotationProcessorPaths` 中。

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
- 错误码常量集中定义在各自模块的常量类里（common 的放 common，服务的放自己服务的模块），禁止在业务代码里直接写数字字面量。

**类型用 `int` 即可（已实测）**：代码里的 `code` 字段用 `int`。项目位固定为 `1`，10 位码的理论最大值是 `1_99_999_9999` = `1999999999`，小于 `Integer.MAX_VALUE` = `2147483647`；服务位实际只用到 `00`~`04`，真实上限更低（`1_04_999_9999` = `1049999999`）。这些值都是合法的十进制 int 字面量，能正常编译，不需要 `long`，也不需要字符串补零。

**项目位不要用 `0`**：写成 `0` 就退化成 9 位，而且数字字面量以 `0` 开头会被 Java 当成**八进制**（`020010001` 实际等于 `4198401`，编译还不报错，属于静默出错）。用 `1` 开头天生没有这个问题，也不需要字符串补零。

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
