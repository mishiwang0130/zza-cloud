# rental 服务接口契约稿

本文是 rental 服务的实现依据：接口清单、每个接口的入参/返回字段、落地清单与规矩。
实现代码前先读 `AGENTS.md`，再读本文；**不要读 `D:\interview\zza-rental`**（旧服务接口把一堆东西塞进一个返回体，照抄会重犯）。

## 0. 硬性约束

- **查询接口只返这个页面真正要的字段**：列表只返列表展示需要的字段，详情只返详情页要展示的字段；不要把无关实体的明细顺手带出去，也不要用一条 SQL join 七八张表把嵌套对象整块返回——需要多表数据就在 Service 层按需组装。
- **但一次功能就一个接口，不要盲目拆分**：同一次功能里的连带动作直接放在这个接口里——例如 App 房间详情接口本身就负责「查详情 + 异步补写浏览记录（MQ）」，不要为此再给前端一个写浏览记录的接口；图片、标签、配套、费用项这些是房源表单的一部分，直接跟 `create`/`update` 一起提交，详情页要展示的图片直接放在详情返回里。只有本身就是独立动作的才单独出接口（发布状态、状态流转）。
- 禁止返回 PO/实体；请求用 `XxxReqVO`，响应用 `XxxRespVO`，跨服务传输才叫 `XxxDTO`。
- Controller 只做校验与调用 Service；不写业务、不拼错误响应。
- 禁止 `BeanUtil`/`BeanUtils`，对象转换用 MapStruct（`XxxConvert`）；JSON 用 Fastjson2。
- 注入统一 `@Resource`；写库方法加 `@Transactional(rollbackFor = Exception.class)`。
- Mapper 接口禁止注解 SQL，SQL 写在 `src/main/resources/mapper/XxxMapper.xml`。
- 结构模板直接照 infra：`UserAdminController` + `InfraUserService(Impl)` + `po/mapper/convert` 的写法。
- 凡是「等 infra 就绪才能补」的地方，代码里统一留 `// TODO wxy <详细内容>`：写清楚等哪个接口或哪张表、就绪后怎么补，不要静默返回空值，也不要写假的兜底实现。

## 1. 落地清单

| 项 | 内容 |
| --- | --- |
| 模块 | `rental/rental-api`（包 `com.wxy.rental.api`）、`rental/rental-biz`（包 `com.wxy.rental.biz`），目录名与 artifactId 一致 |
| 聚合与 BOM | 父 `pom.xml` 的 `<modules>` 加 `rental`；`rental/pom.xml` 聚合两子模块；`dependencies/pom.xml` 的 BOM 登记 `rental-api`、`rental-biz`（不写 `<version>`） |
| rental-api | 对外发布模块，本期只放 `package-info.java` 与 `RentalApiConstant.SERVICE_NAME = "rental"`，**不声明任何依赖**：没有 Feign 客户端、没有跨服务 DTO，用不到 `common-core`（对照 infra-api：它引 `common-core`/`openfeign`/`validation`/`sentinel` 是因为真发布了客户端）。将来要发布 `XxxClient`/`XxxDTO` 时再照 infra-api 的 pom 补 |
| rental-biz 依赖 | 照 infra-biz 的 pom 抄：`rental-api`、`common-core`、`common-webmvc`、`common-mybatis`、`common-redis`、`common-security`、`common-mq`、`rocketmq-spring-boot-starter`（浏览记录走 MQ）、`infra-api`、`nacos-discovery`、`actuator`、`lombok`、`mapstruct`、`spring-boot-starter-test`(test)；`rental-api` 本期没有要实现的服务间接口，引它是按仓库依赖方向 `biz → api` 先把链路搭好；不需要 `common-storage`、`spring-security-crypto`；打 `spring-boot-maven-plugin` |
| MQ | 浏览记录异步落库：`rental-biz` 配 `rocketmq.name-server`（各环境地址，放 `application-{dev,prod}.yml`）与 `rocketmq.producer.group`（与地址无关，放 `application.yml` 共用）；**两个属性必须同时存在**，否则 `RocketMQTemplate` Bean 不会创建，发消息静默跳过；topic/tag 常量放 `RentalMqConstant`（`CommonMqConstant.PREFIX + "-rental"`）；包结构 `mq/message`（`RentalBrowseHistoryMsg`）、`mq/producer`（`RentalBrowseHistoryProducer`）、`mq/consumer`（`RentalBrowseHistoryConsumer`，`@RocketMQMessageListener`）；消息体用 Fastjson2 转成 JSON 字符串收发，不用默认的 Jackson 转换器 |
| 启动类 | `RentalApplication`（`com.wxy.rental.biz`），加 `@EnableFeignClients(basePackages = "com.wxy.infra.api.client")`，组件扫描要覆盖 `com.wxy.infra.api`（降级工厂是 `@Component`，扫不到会启动失败） |
| 鉴权 | 不写校验代码：依赖 `common-security` 的 `DefaultTokenValidator` + `DefaultPermissionChecker`，它们经 Feign 回源 infra |
| 服务名与端口 | `spring.application.name: rental`（注释指向 `RentalApiConstant.SERVICE_NAME`）；端口 `8083`（infra 8082） |
| 网关路由 | 已就绪，不用改：`gateway` 的 `application.yml` 里已有 `rental-route`（`uri: lb://rental`、`Path=/api/rental/**`、`StripPrefix=2`），服务只要用 `spring.application.name: rental` 注册到 Nacos，`/api/rental/**` 就能路由过来 |
| 配置文件 | 三份 yml 抄 infra（`application.yml` / `-dev` / `-prod`）：datasource 指向 `zza` 库、Redis、Nacos、`zza.security`、`feign.sentinel.enabled: true`、`mybatis-plus` 逻辑删除、knife4j |
| 端前缀 | **不用自己写**：`common-webmvc` 的 `WebMvcConfig#configurePathMatch` 已按包名自动拼（`.controller.admin` → `/admin-api`，`.controller.app` → `/app-api`），Controller 上只写业务路径 |
| 建表脚本 | 已就绪：`sql/rental.sql`（8 张表 + 字典种子）；建表总脚本只追加不修改，后续变更单独出脚本（如 `sql/rental-view-appointment-user-id.sql`，在执行完 `rental.sql` 后执行） |

包结构（按层，端分层在外、`admin`/`app` 在内）：

```
com.wxy.rental.biz
├── controller/admin        ApartmentAdminController、RoomAdminController …
├── controller/app          ApartmentAppController、RoomAppController …
├── vo/admin                XxxCreateReqVO / XxxRespVO / XxxPageReqVO
├── vo/app                  同上，用户端专用
├── service + service/impl  两端共用业务
├── mapper + resources/mapper/*.xml
├── po                      RentalApartment、RentalRoom …
├── convert                 XxxConvert（MapStruct）
├── enums                   RentalPaymentMethodEnum、RentalLeaseStatusEnum …
├── constant                RentalErrorConstant、RentalPermissionConstant、RentalRedisKeyConstant
├── job                     RentalLeaseExpireJob
└── util                    RentalRedisKeyUtil
```

## 2. 公共规矩

- 响应统一 `Result<T>`；业务失败 `throw new BizException(RentalErrorConstant.XXX)`（服务位固定 03，清单见第 6 节）。
- 分页：入参 `XxxPageReqVO extends PageReqVO`（`pageNum`/`pageSize`），出参 `Result<PageRespVO<XxxRespVO>>`，转换用 `PageUtil.toPage(reqVO)` 与 `PageUtil.of(page)`，不要自己造分页结构。
- 小配置表（费用项）用 `GET /list` 返回 `List<XxxRespVO>`，不强行分页（与 infra 的 `/dict-type/list` 一致）。
- 路径「资源 + 动作」camelCase：`/apartment/create`、`/apartment/getById`；同一路径不靠 HTTP 方法区分增删改查。
- 管理端写接口一律 `@PostMapping` + `@Validated @RequestBody`；查询详情用 `@GetMapping` + `@RequestParam`。
- 权限：方法上加 `@RequiresPermission(RentalPermissionConstant.XXX)`，perm 串固定 `rental:<资源>:<动作>`，与 `infra_menu.perms` 一致；菜单与按钮种子（`infra_menu` id 从 25 起 + `infra_role_menu` 授权给超管角色 1）由本服务提供，追加到 `sql/rental.sql`。
- 取当前登录用户用 `UserContextHolder`（`com.wxy.common.core.context`），App 端接口的 `userId` 一律从上下文取，**禁止由前端传入**。
- 字典编码、区域 ID、文件 ID 只存编码/ID；字典中文名、区域名称、文件预签名地址由 Service 通过 infra 的接口回填（见第 5 节），前端拿到的就是可直接展示的数据。
- 图片不落访问地址，只存 `fileId`；上传复用 infra 的 `POST /api/infra/admin-api/file/upload`。

## 3. 枚举与状态（代码侧）

| 枚举 | 取值 |
| --- | --- |
| `RentalPaymentMethodEnum` | 1 月付、2 季付、3 半年付、4 年付（对应 `rental_apartment.payment_method`） |
| `RentalPublishStatusEnum` | 0 未发布、1 已发布 |
| `RentalLeaseStatusEnum` | 1 签约待确认、2 已签约、3 已取消、4 已到期、5 退租待确认、6 已退租、7 续约待确认 |
| `RentalLeaseSourceTypeEnum` | 1 新签、2 续约 |
| `RentalAppointmentStatusEnum` | 1 待看房、2 已取消、3 已看房 |
| `RentalImageItemTypeEnum` | 1 公寓、2 房间 |

租约状态流转（`/lease/updateStatus` 只允许下表迁移，其他一律报错）：

| 当前 | 允许改为 |
| --- | --- |
| 1 签约待确认 | 2 已签约、3 已取消 |
| 2 已签约 | 4 已到期、5 退租待确认、7 续约待确认 |
| 5 退租待确认 | 2 已签约（驳回退租）、6 已退租 |
| 7 续约待确认 | 2 已签约（续约完成或驳回） |
| 3 已取消 / 4 已到期 / 6 已退租 | 终态，不允许再流转 |

`RentalLeaseExpireJob`：每天扫描「已签约且 `lease_end_date` 早于今天」的租约置为 4 已到期（定时任务类命名 `XxxJob`，放 `job` 包）。

## 4. 接口清单

### 4.1 管理后台（`/admin-api/...`）

#### 公寓 `/apartment`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /create` | `ApartmentCreateReqVO` | `Result<Long>` 新 ID |
| `POST /update` | `ApartmentUpdateReqVO` = `id` + 下面全部字段 | `Result<Void>` |
| `GET /getById` | `id` | `Result<ApartmentRespVO>` |
| `POST /page` | `ApartmentPageReqVO` | `Result<PageRespVO<ApartmentPageItemRespVO>>` |
| `POST /updatePublishStatus` | `id`、`publishStatus` | `Result<Void>` |
| `GET /listSimple` | 无 | `Result<List<ApartmentSimpleRespVO>>` 房间表单的公寓下拉 |

`ApartmentCreateReqVO`：`name`（必填）、`introduction`、`districtId`（必填）、`addressDetail`、`phone`、`minLeaseMonths`、`depositMonths`、`paymentMethod`、`labelCodes:List<String>`、`facilityCodes:List<String>`、`feeItemIds:List<Long>`、`images:List<ImageItemReqVO>`。
一次表单提交就落全：`labelCodes`/`facilityCodes` 逗号拼成 `label_codes`/`facility_codes` 写主表，`feeItemIds` 覆盖写 `rental_apartment_fee`，`images` 覆盖写 `rental_image`（关联表都是先按公寓 ID 物理删除再批量插入），全在一个事务里；前端不需要再调"保存图片"之类的第二个接口。
公寓不提供删除接口，下架即"不再对外展示"：`/updatePublishStatus` 在下架前校验公寓下还有没有已发布的房间，有则报 `APARTMENT_HAS_ROOM`。

`ApartmentRespVO`：`id`、`name`、`introduction`、`districtId`、`districtName`、`addressDetail`、`phone`、`minLeaseMonths`、`depositMonths`、`paymentMethod`、`paymentMethodName`、`publishStatus`、`labelCodes:List<DictItemVO>`、`facilityCodes:List<DictItemVO>`、`feeItems:List<FeeItemSimpleRespVO>`、`images:List<ImageRespVO>`、`createTime`、`updateTime`。
`DictItemVO` = `label` + `value`（字典中文名由 Service 查缓存后回填，前端不用再查）。详情带图片是因为详情页要展示；**不含房间列表、不含租约**——房间与租约是各自独立的功能，不由公寓详情顺带返回。

`ApartmentPageReqVO`（继承 `PageReqVO`）：`name`（模糊）、`districtId`、`cityId`、`paymentMethod`、`publishStatus`。
`ApartmentPageItemRespVO`：`id`、`name`、`districtId`、`districtName`、`addressDetail`、`minLeaseMonths`、`depositMonths`、`paymentMethod`、`publishStatus`、`roomCount`、`vacantRoomCount`。
`roomCount`/`vacantRoomCount` 是本列表唯一的聚合字段（首页看空置用），SQL 写在 Mapper XML；**房间明细走房间接口**。`cityId` 筛选依赖 infra 区域接口把市展开为区县 ID 列表（见第 5 节），infra 未就绪前先只支持 `districtId`，代码里留 `// TODO wxy 等 infra 提供区域内接口后支持按 cityId 筛选`。

#### 房间 `/room`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /create` | `RoomCreateReqVO` | `Result<Long>` |
| `POST /update` | `RoomUpdateReqVO` = `id` + 下面字段 | `Result<Void>` |
| `GET /getById` | `id` | `Result<RoomRespVO>` |
| `POST /page` | `RoomPageReqVO` | `Result<PageRespVO<RoomPageItemRespVO>>` |
| `POST /updatePublishStatus` | `id`、`publishStatus` | `Result<Void>` |
| `GET /listSimpleByApartment` | `apartmentId` | `Result<List<RoomSimpleRespVO>>` 租约选房下拉 |

`RoomCreateReqVO`：`apartmentId`（必填）、`roomNumber`（必填）、`rent`（必填）、`area`、`roomCount`、`orientation`、`floorNo`、`labelCodes:List<String>`、`facilityCodes:List<String>`、`images:List<ImageItemReqVO>`；和公寓一样一次提交落全，`images` 覆盖写 `rental_image`。
房间不提供删除接口：`/updatePublishStatus` 下架前校验房间有没有生效中的租约（状态 1/2/5），有则报 `ROOM_HAS_LEASE`。
`RoomRespVO`：上面全部 + `apartmentName`、`labelCodes:List<DictItemVO>`、`facilityCodes:List<DictItemVO>`、`images:List<ImageRespVO>`、`publishStatus`、`createTime`、`updateTime`。
`RoomPageReqVO`：`apartmentId`、`roomNumber`（模糊）、`publishStatus`、`minRent`、`maxRent`、`vacantOnly`。
`RoomPageItemRespVO`：`id`、`apartmentId`、`apartmentName`、`roomNumber`、`rent`、`area`、`roomCount`、`orientation`、`orientationName`、`floorNo`、`publishStatus`、`checkInStatus`。
`checkInStatus`（0 空置、1 在租）由租约表 `status in (1,2,5)` 派生，属列表必需字段，允许带上；不要顺带返回租约详情。
`RoomSimpleRespVO`：`id`、`roomNumber`、`rent`、`publishStatus`。

#### 费用项 `/fee-item`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /create` | `FeeItemCreateReqVO`（`name`、`amount`、`unit`） | `Result<Long>` |
| `POST /update` | `FeeItemUpdateReqVO`（`id` + 上面字段） | `Result<Void>` |
| `POST /delete` | `id` | `Result<Void>` |
| `GET /list` | 无 | `Result<List<FeeItemRespVO>>` |

`FeeItemRespVO`：`id`、`name`、`amount`、`unit`。费用项字段少，`/list` 已返回全部字段，编辑弹窗直接用列表行数据，**不单独出 `/getById`**；同名校验在 Service，报错用 `FEE_ITEM_NAME_EXISTS`。

#### 租约 `/lease`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /create` | `LeaseCreateReqVO` | `Result<Long>` |
| `POST /update` | `LeaseUpdateReqVO` | `Result<Void>` |
| `GET /getById` | `id` | `Result<LeaseRespVO>` |
| `POST /page` | `LeasePageReqVO` | `Result<PageRespVO<LeasePageItemRespVO>>` |
| `POST /updateStatus` | `id`、`status` | `Result<Void>` |

`LeaseCreateReqVO`：`userId`（必填，App 用户 ID）、`apartmentId`（必填）、`roomId`（必填）、`contractFileId`（可空，0 表示尚未上传）、`leaseStartDate`（必填）、`leaseEndDate`（必填）、`rent`（必填）、`deposit`（可空，不传按 `rent × 公寓 depositMonths` 计算）、`sourceType`、`remark`。承租人由后台从 App 用户列表里选（`POST /api/infra/admin-api/app-user/page`），不用手填 ID。
创建校验：房间存在且 `apartmentId` 与房间一致；`leaseEndDate` 晚于 `leaseStartDate`；房间没有处于 1/2/5 状态的租约，否则报 `ROOM_LEASE_EXISTS`。
`LeaseUpdateReqVO`：`id` + `contractFileId`、`leaseStartDate`、`leaseEndDate`、`rent`、`deposit`、`remark`；状态为 3/4/6 时不允许改条款（只能改合同文件与备注）。
租约不提供删除接口：要作废就 `updateStatus` 置为 3 已取消（合同不物理删、不逻辑删）。
`updateStatus` 按第 3 节流转表校验，非法迁移报 `LEASE_STATUS_TRANSITION_INVALID`。

`LeaseRespVO`：`id`、`userId`、`userNickname`、`userMobile`、`apartmentId`、`apartmentName`、`roomId`、`roomNumber`、`contractFileId`、`leaseStartDate`、`leaseEndDate`、`rent`、`deposit`、`status`、`statusName`、`sourceType`、`remark`、`createTime`、`updateTime`。**不含租客实名信息（实名只在合同文件里）、不含付款方式快照**。
`LeasePageReqVO`：`userId`、`apartmentId`、`roomId`、`status`、`sourceType`、`leaseEndDateStart`、`leaseEndDateEnd`。
`LeasePageItemRespVO`：`id`、`userId`、`userNickname`、`userMobile`、`apartmentId`、`apartmentName`、`roomId`、`roomNumber`、`leaseStartDate`、`leaseEndDate`、`rent`、`deposit`、`status`、`statusName`、`sourceType`、`createTime`。
`userNickname`、`userMobile` 要展示给运营：租约表只存 `userId`，返回前由 `RentalAppUserService` 收集本页用户 ID 批量调 infra 的 `GET /internal-api/app-user/listByIds` 回填（见第 5 节第 5 项）；用户已删除或查不到时为 null。

#### 看房预约 `/view-appointment`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /page` | `ViewAppointmentPageReqVO` | `Result<PageRespVO<ViewAppointmentRespVO>>` |
| `POST /updateStatus` | `id`、`status` | `Result<Void>` |

`ViewAppointmentPageReqVO`：`userId`、`apartmentId`、`status`、`appointmentTimeStart`、`appointmentTimeEnd`。**没有姓名/手机模糊筛选**：这两项不落在预约表里，按它们过滤要先反查用户表，而 infra 只提供按 ID 批量查，所以只能按 `userId` 精确定位。
`ViewAppointmentRespVO`：`id`、`userId`、`userNickname`、`userMobile`、`apartmentId`、`apartmentName`、`appointmentTime`、`status`、`statusName`、`remark`、`createTime`。`/page` 返回的就是列表与详情弹窗要的全部字段，**不单独出 `/getById`**；`userNickname`、`userMobile` 按本页 `userId` 批量调 infra 的 `GET /internal-api/app-user/listByIds` 回填（见第 5 节第 5 项），用户已删除或查不到时为 null。
管理端只允许 1 待看房 → 3 已看房 / 2 已取消，其他迁移报错。

#### 房源图片（不单独出接口）

图片是房源表单的一部分：上传走 infra 的 `POST /api/infra/admin-api/file/upload` 拿到 `fileId`，前端本地回显，点保存时**和标签、配套、费用项一起随 `/apartment/create|update`、`/room/create|update` 提交**（旧版也是这么做的：图片跟着 saveOrUpdate 一起提交）。Service 在保存主表的同一个事务里覆盖写 `rental_image`。

`ImageItemReqVO`（公寓/房间的新增、修改入参里）：`fileId`、`sort`。
`ImageRespVO`（公寓/房间详情返回里）：`id`、`fileId`、`sort`、`url`（按 `fileId` 向 infra 文件接口取预签名地址后回填，见第 5 节）；两端各一份（`vo/admin/ImageRespVO`、`vo/app/ImageRespVO`），字段相同。

### 4.2 用户端（`/app-api/...`）

登录、注册、刷新令牌由 infra 提供（待 infra 补齐）；App 端需要身份的接口（预约、我的浏览）从 `UserContextHolder` 取 `userId`，公寓/房间的查询接口用 `@PermitAll`（`jakarta.annotation.security.PermitAll`）允许未登录浏览，拿不到 `userId` 时不做浏览记录，也不影响返回。

#### 公寓 `/apartment`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /page` | `AppApartmentPageReqVO` | `Result<PageRespVO<AppApartmentItemRespVO>>` |
| `GET /getById` | `id` | `Result<AppApartmentRespVO>` |

`AppApartmentPageReqVO`：`districtId`、`cityId`、`minRent`、`maxRent`、`paymentMethod`、`minLeaseMonths`、`roomCount`、`minArea`、`maxArea`、`labelCodes:List<String>`、`keyword`、`sortType`。只查 `publish_status = 1` 的公寓；租金/面积/室数条件落到房间表上做 `exists` 过滤。`keyword` 按公寓名称模糊匹配（空白视为不过滤）；`sortType`：0 综合（默认，保持按 id 倒序）、1 月租金从低到高、2 月租金从高到低、3 最新上架（`create_time` 倒序），非法值按 0，按月租金排序时取该公寓已发布房间的最低价，无已发布房间的公寓排在最后。
`AppApartmentItemRespVO`：`id`、`name`、`districtId`、`districtName`、`addressDetail`、`minLeaseMonths`、`depositMonths`、`paymentMethod`、`paymentMethodName`、`minRent`、`coverFileId`、`coverFileUrl`、`labelCodes:List<DictItemVO>`、`facilityCodes:List<DictItemVO>`。`coverFileUrl` 由 Service 按本页 `coverFileId` 一次批量调 infra `file/listByIds` 换取，无图或文件查不到时为 null，列表不做逐条 RPC。
`AppApartmentRespVO`：上面全部 + `introduction`、`phone`、`feeItems:List<FeeItemSimpleRespVO>`、`images:List<ImageRespVO>`（详情页要展示，直接跟着详情返回）。

#### 房间 `/room`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /page` | `AppRoomPageReqVO` | `Result<PageRespVO<AppRoomItemRespVO>>` |
| `GET /getById` | `id` | `Result<AppRoomRespVO>` |

`AppRoomPageReqVO`：`apartmentId`、`districtId`、`cityId`、`minRent`、`maxRent`、`roomCount`、`minArea`、`maxArea`、`orientation`、`labelCodes`、`facilityCodes`、`vacantOnly`、`keyword`、`sortType`。`keyword` 按房间号或所属公寓名称模糊匹配（空白视为不过滤）；`sortType` 口径与公寓列表一致，按月租金排序时取房间自身租金。
`AppRoomItemRespVO`：`id`、`apartmentId`、`apartmentName`、`districtId`、`districtName`、`roomNumber`、`rent`、`area`、`roomCount`、`orientation`、`orientationName`、`floorNo`、`depositMonths`、`paymentMethod`、`minLeaseMonths`、`coverFileId`、`coverFileUrl`、`labelCodes`、`facilityCodes`。`coverFileUrl` 同样由 Service 按本页 `coverFileId` 一次批量换取，无图或文件查不到时为 null。
`AppRoomRespVO`：上面全部 + 所属公寓精简信息（`apartmentId`、`apartmentName`、`addressDetail`、`phone`、`introduction`、`feeItems`）+ `images:List<ImageRespVO>`。
**这个接口就是「一次功能一个接口」的例子**：它返回房间详情页要的全部数据，同时自己异步补写浏览记录（`userId` 存在时发 MQ 消息给 `RentalBrowseHistoryProducer`；同一房间已看过则刷新浏览时间；发消息失败只记日志），前端不需要再调写浏览记录的接口。

#### 看房预约 `/view-appointment`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /create` | `AppViewAppointmentCreateReqVO` | `Result<Long>` |
| `POST /page` | 继承 `PageReqVO`（无额外条件，只查自己） | `Result<PageRespVO<AppViewAppointmentRespVO>>` |
| `POST /cancel` | `id` | `Result<Void>` |

`AppViewAppointmentCreateReqVO`：`apartmentId`（必填）、`appointmentTime`（必填）、`remark`。`userId` 从上下文取；**不填姓名与手机号**，`rental_view_appointment` 只存 `user_id`，联系方式后台按 `userId` 查用户档案。
`create` 只能预约已发布公寓：未发布（含已下架）的公寓对 App 视为不存在，报 `APARTMENT_NOT_FOUND`。
`cancel` 只能取消自己的、状态为 1 待看房的预约，否则报 `APPOINTMENT_CANCEL_FORBIDDEN`。
`AppViewAppointmentRespVO`：`id`、`apartmentId`、`apartmentName`、`appointmentTime`、`status`、`statusName`、`remark`、`createTime`。

#### 浏览记录 `/room-browse`

没有写入接口：浏览记录由 App 房间详情接口异步发 MQ 消息触发（见上）。

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /page` | 继承 `PageReqVO`（只查自己） | `Result<PageRespVO<AppRoomBrowseRespVO>>` |

写入流程：房间详情 → `RentalBrowseHistoryProducer` 发 `RentalMqConstant` 里的 topic/tag（消息体 `RentalBrowseHistoryMsg`：`userId`、`roomId`、`browseTime`）→ `RentalBrowseHistoryConsumer` 交给 `RentalBrowseHistoryService` **按「用户 + 房间」去重**写入：已有记录就刷新浏览时间（浏览时间即 `create_time`），没有才插入，所以同一房间在「我的浏览」里只有一条、最近看的排最前；消费异常抛出让 RocketMQ 重试。
`AppRoomBrowseRespVO`：`id`、`roomId`、`roomNumber`、`apartmentId`、`apartmentName`、`rent`、`coverFileId`、`coverFileUrl`、`createTime`；按 `create_time` 倒序返回（同一房间只有一条，重复浏览刷新时间）。

### 4.3 用户端租约（后续窗口，本期不做）

旧版 App 有租约相关接口（我的租约、详情、用户确认签约 / 申请退租 / 申请续约），本服务也要有，但**依赖 infra 的 App 端用户表与登录**，本期不实现；等就绪后按下面的形状补（状态流转复用第 3 节的表，只是发起方从运营变成用户本人，且只允许操作自己的租约）：

| 接口 | 用途 |
| --- | --- |
| `POST /lease/page` | 我的租约列表（只查自己） |
| `GET /lease/getById` | 我的租约详情（含合同文件地址） |
| `POST /lease/confirm` | 确认签约：1 签约待确认 → 2 已签约 |
| `POST /lease/apply-withdraw` | 申请退租：2 已签约 → 5 退租待确认 |
| `POST /lease/apply-renew` | 申请续约：2 已签约 → 7 续约待确认 |

## 5. 依赖 infra 的接口（待 infra 补齐）

现有可直接用：

- 文件上传：`POST /api/infra/admin-api/file/upload`（返回 `fileId`、对象名与预签名地址，rental 图片按 `fileId` 引用）
- App 端文件上传：`POST /api/infra/app-api/file/upload`（返回体同上，供 app 用户上传头像等；只校验登录，不挂权限标识）
- 后台字典：`GET /api/infra/admin-api/dict-data/listByType?dictType=`、`GET /api/infra/admin-api/dict-type/list`
- 后台区划：`GET /api/infra/admin-api/area/listChildren?parentId=`、`GET /api/infra/admin-api/area/listTree`
- 后台 App 用户列表：`POST /api/infra/admin-api/app-user/page`（`keyword` 匹配昵称/手机号 + `status`，分页返回 `id`/`nickname`/`mobile`/`status`/`createTime`，权限 `infra:app-user:query`），租约表单的承租人选择器用它

需要 infra 新增：

1. **App 端用户表 + 认证接口**：注册、登录、刷新令牌、查询当前用户（走 `infra_token.user_type = 2`）；rental 只引用 `userId`。
2. **字典读取（服务间/用户端）**：`GET /internal-api/dict-data/listByType`（或 app 端去权限版本），供 rental 回填 `DictItemVO` 中文名并做 Redis 缓存（key 走 `RentalRedisKeyConstant` + `RentalRedisKeyUtil`，一个 key 一个方法）。
3. **区域读取（服务间/用户端）**：`GET /internal-api/area/listChildren|listTree`，供 rental 把 `cityId` 展开为区县 ID 列表、App 端做三级联动。
4. **文件按 ID 查询**：`GET /internal-api/file/listByIds`（返回 `id`、`name`、`path` + 预签名地址），供 rental 把 `fileId` 换成展示地址（`rental_image.file_id`、`rental_lease.contract_file_id`）。
5. **App 用户批量查询**：管理端租约、预约列表要展示租客昵称与手机，已就绪：`GET /internal-api/app-user/listByIds`（返回 `id`、`nickname`、`mobile`），rental 侧由 `RentalAppUserService` 封装后回填（见第 4.1 节租约、看房预约）。后台「App 用户」菜单与列表走的则是管理端 `POST /api/infra/admin-api/app-user/page`（同一个服务，不分页/分页两种用法）。
6. **租客相关菜单权限**：`infra_menu` 加 rental 的目录/菜单/按钮（id 从 25 起，`perms` 用下面的常量值），`infra_role_menu` 授权给超管角色 1，追加到 `sql/rental.sql`。

`RentalPermissionConstant` 的 perm 串（与菜单按钮一一对应）：

```
rental:apartment:query / create / update / delete / update-publish-status
rental:room:query / create / update / delete / update-publish-status
rental:fee-item:query / create / update / delete
rental:lease:query / create / update / delete / update-status
rental:view-appointment:query / update-status
```

## 6. 错误码（服务位 03）

服务位固定 `03 rental`（`AGENTS.md` 的服务位表已同步），不新增、不复用。错误码常量类为 `RentalErrorConstant`（`com.wxy.rental.biz.constant`），类型 `ErrorCode`，业务代码只引用常量、禁止数字字面量。

模块位分配：`000` 跨模块通用、`001` 公寓、`002` 房间、`003` 费用项、`004` 租约、`005` 预约、`006` 浏览、`007` 图片。

| 错误码 | 常量 | 含义 |
| --- | --- | --- |
| 1_03_000_0001 | DICT_CODE_INVALID | 字典编码不存在或已停用 |
| 1_03_000_0002 | AREA_NOT_FOUND | 行政区划不存在 |
| 1_03_000_0003 | REMOTE_SERVICE_ERROR | 依赖的基础服务调用失败 |
| 1_03_001_0001 | APARTMENT_NOT_FOUND | 公寓不存在 |
| 1_03_001_0002 | APARTMENT_HAS_ROOM | 公寓下还有已发布房间，不能下架 |
| 1_03_002_0001 | ROOM_NOT_FOUND | 房间不存在 |
| 1_03_002_0002 | ROOM_NUMBER_EXISTS | 同一公寓下房间号已存在 |
| 1_03_002_0003 | ROOM_APARTMENT_MISMATCH | 房间不属于该公寓 |
| 1_03_002_0004 | ROOM_HAS_LEASE | 房间存在生效中的租约，不能下架 |
| 1_03_003_0001 | FEE_ITEM_NOT_FOUND | 费用项不存在 |
| 1_03_003_0002 | FEE_ITEM_NAME_EXISTS | 费用项名称已存在 |
| 1_03_003_0003 | FEE_ITEM_IN_USE | 费用项已被公寓引用，不能删除 |
| 1_03_004_0001 | LEASE_NOT_FOUND | 租约不存在 |
| 1_03_004_0002 | ROOM_LEASE_EXISTS | 房间已有生效中的租约 |
| 1_03_004_0003 | LEASE_DATE_INVALID | 租约结束日期必须晚于开始日期 |
| 1_03_004_0004 | LEASE_STATUS_TRANSITION_INVALID | 租约状态不允许这样流转 |
| 1_03_004_0005 | LEASE_UPDATE_FORBIDDEN | 该状态的租约不允许修改条款 |
| 1_03_005_0001 | APPOINTMENT_NOT_FOUND | 预约记录不存在 |
| 1_03_005_0002 | APPOINTMENT_CANCEL_FORBIDDEN | 只能取消自己的待看房预约 |
| 1_03_005_0003 | APPOINTMENT_STATUS_TRANSITION_INVALID | 预约状态不允许这样流转 |
| 1_03_007_0001 | IMAGE_ITEM_TYPE_INVALID | 图片所属对象类型不合法 |
| 1_03_007_0002 | IMAGE_FILE_NOT_FOUND | 图片文件不存在 |

浏览记录（006）由 MQ 异步写入、只提供查询，没有业务错误码；**发消息失败与消费失败都不允许影响房间详情的返回**（发送失败记 warn；消费失败抛出让 RocketMQ 重试）。参数校验、未登录、无权限、系统异常统一用 `CommonErrorConstant`。

## 7. 验收

- 查询接口不返无关数据：列表不带明细，详情不带别的实体整块数据；同一次功能只出一个接口（App 房间详情内含异步浏览记录），只有本身就是独立动作的（发布状态、状态流转）才单独出接口。
- 表单类关联数据（标签、配套、费用项、图片）随 `/apartment|room/create|update` 一次提交，不额外拆保存接口。
- 不提供删除接口：公寓、房间靠 `/updatePublishStatus` 下架（分别校验 `APARTMENT_HAS_ROOM`、`ROOM_HAS_LEASE`），租约靠状态置为 3 已取消。
- 浏览记录链路：房间详情发 MQ → 消费端按「用户 + 房间」去重写入（有则刷新浏览时间、无则插入）；RocketMQ 不可用时详情接口照常返回，只记日志。
- 看房预约只存 `user_id`：`rental_view_appointment` 不含姓名/手机，App 提交也不传，后台列表按 `userId` 调 `GET /internal-api/app-user/listByIds` 回填 `userNickname`/`userMobile`。
- 租约与预约的后台列表/详情都按 `userId` 回填 `userNickname`/`userMobile`，没有遗留的 `// TODO wxy`。
- 押金：`deposit` 不传时按 `rent × 公寓 depositMonths` 计算。
- 无 PO 泄漏：所有出参都是 `XxxRespVO`；跨服务对象才叫 DTO。
- 分页统一 `PageReqVO`/`PageRespVO`；路径全 camelCase「资源 + 动作」。
- 端前缀正确：admin 包下接口最终路径是 `/api/rental/admin-api/...`，app 包下是 `/api/rental/app-api/...`。
- 关联表覆盖写用物理删除；类型/唯一键与 `sql/rental.sql` 完全一致。
- 单测 mock Mapper、不依赖真实库/Redis/Nacos；`mvn -pl rental/rental-biz -am test` 通过，`mvn -DskipTests clean install` 全量通过。
