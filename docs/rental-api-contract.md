# rental 服务接口契约稿

本文是 rental 服务的实现依据：接口清单、每个接口的入参/返回字段、落地清单与规矩。
实现代码前先读 `AGENTS.md`，再读本文；**不要读 `D:\interview\zza-rental`**（旧服务接口把一堆东西塞进一个返回体，照抄会重犯）。

## 0. 硬性约束

- 一个接口只做一件事；列表只返回列表展示需要的字段；**详情的子资源（图片、房间、租约）各自独立接口**，不打包进一个大对象。
- 禁止返回 PO/实体；请求用 `XxxReqVO`，响应用 `XxxRespVO`，跨服务传输才叫 `XxxDTO`。
- Controller 只做校验与调用 Service；不写业务、不拼错误响应。
- 禁止 `BeanUtil`/`BeanUtils`，对象转换用 MapStruct（`XxxConvert`）；JSON 用 Fastjson2。
- 注入统一 `@Resource`；写库方法加 `@Transactional(rollbackFor = Exception.class)`。
- Mapper 接口禁止注解 SQL，SQL 写在 `src/main/resources/mapper/XxxMapper.xml`。
- 结构模板直接照 infra：`UserAdminController` + `InfraUserService(Impl)` + `po/mapper/convert` 的写法。

## 1. 落地清单

| 项 | 内容 |
| --- | --- |
| 模块 | `rental/rental-api`（包 `com.wxy.rental.api`）、`rental/rental-biz`（包 `com.wxy.rental.biz`），目录名与 artifactId 一致 |
| 聚合与 BOM | 父 `pom.xml` 的 `<modules>` 加 `rental`；`rental/pom.xml` 聚合两子模块；`dependencies/pom.xml` 的 BOM 登记 `rental-api`、`rental-biz`（不写 `<version>`） |
| rental-api | 对外发布模块，本期只放 `package-info.java` 与 `RentalApiConstant.SERVICE_NAME = "rental"`，**不声明任何依赖**：没有 Feign 客户端、没有跨服务 DTO，用不到 `common-core`（对照 infra-api：它引 `common-core`/`openfeign`/`validation`/`sentinel` 是因为真发布了客户端）。将来要发布 `XxxClient`/`XxxDTO` 时再照 infra-api 的 pom 补 |
| rental-biz 依赖 | 照 infra-biz 的 pom 抄：`rental-api`、`common-core`、`common-webmvc`、`common-mybatis`、`common-redis`、`common-security`、`infra-api`、`nacos-discovery`、`actuator`、`lombok`、`mapstruct`、`spring-boot-starter-test`(test)；`rental-api` 本期没有要实现的服务间接口，引它是按仓库依赖方向 `biz → api` 先把链路搭好；不需要 `common-storage`、`spring-security-crypto`；打 `spring-boot-maven-plugin` |
| 启动类 | `RentalApplication`（`com.wxy.rental.biz`），加 `@EnableFeignClients(basePackages = "com.wxy.infra.api.client")`，组件扫描要覆盖 `com.wxy.infra.api`（降级工厂是 `@Component`，扫不到会启动失败） |
| 鉴权 | 不写校验代码：依赖 `common-security` 的 `DefaultTokenValidator` + `DefaultPermissionChecker`，它们经 Feign 回源 infra |
| 服务名与端口 | `spring.application.name: rental`（注释指向 `RentalApiConstant.SERVICE_NAME`）；端口 `8083`（infra 8082） |
| 网关路由 | `gateway` 的 `application.yml` 加一条：`id: rental-route`、`uri: lb://rental`、`Path=/api/rental/**`、`StripPrefix=2` |
| 配置文件 | 三份 yml 抄 infra（`application.yml` / `-dev` / `-prod`）：datasource 指向 `zza` 库、Redis、Nacos、`zza.security`、`feign.sentinel.enabled: true`、`mybatis-plus` 逻辑删除、knife4j |
| 端前缀 | **不用自己写**：`common-webmvc` 的 `WebMvcConfig#configurePathMatch` 已按包名自动拼（`.controller.admin` → `/admin-api`，`.controller.app` → `/app-api`），Controller 上只写业务路径 |
| 建表脚本 | 已就绪：`sql/rental.sql`（8 张表 + 字典种子） |

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
- 字典编码、区域 ID、文件 ID 只存编码/ID；字典中文名、文件预签名地址由前端按编码/ID 向 infra 取（见第 5 节）。
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
| `POST /delete` | `id`（`@RequestParam`） | `Result<Void>` 逻辑删除 |
| `GET /getById` | `id` | `Result<ApartmentRespVO>` |
| `POST /page` | `ApartmentPageReqVO` | `Result<PageRespVO<ApartmentPageItemRespVO>>` |
| `POST /updatePublishStatus` | `id`、`publishStatus` | `Result<Void>` |
| `GET /listSimple` | 无 | `Result<List<ApartmentSimpleRespVO>>` 房间表单的公寓下拉 |

`ApartmentCreateReqVO`：`name`（必填）、`introduction`、`districtId`（必填）、`addressDetail`、`phone`、`minLeaseMonths`、`depositMonths`、`paymentMethod`、`labelCodes:List<String>`、`facilityCodes:List<String>`、`feeItemIds:List<Long>`。
Service 侧把 `labelCodes`/`facilityCodes` 用逗号拼成 `label_codes`/`facility_codes` 落库；`feeItemIds` 覆盖写 `rental_apartment_fee`（先按公寓 ID 物理删除再批量插入）。

`ApartmentRespVO`：`id`、`name`、`introduction`、`districtId`、`districtName`、`addressDetail`、`phone`、`minLeaseMonths`、`depositMonths`、`paymentMethod`、`paymentMethodName`、`publishStatus`、`labelCodes:List<DictItemVO>`、`facilityCodes:List<DictItemVO>`、`feeItems:List<FeeItemSimpleRespVO>`、`createTime`、`updateTime`。
`DictItemVO` = `label` + `value`（字典中文名由 Service 查缓存后回填，前端不用再查）。**不含房间列表、不含图片、不含租约**。

`ApartmentPageReqVO`（继承 `PageReqVO`）：`name`（模糊）、`districtId`、`cityId`、`paymentMethod`、`publishStatus`。
`ApartmentPageItemRespVO`：`id`、`name`、`districtId`、`districtName`、`addressDetail`、`minLeaseMonths`、`depositMonths`、`paymentMethod`、`publishStatus`、`roomCount`、`vacantRoomCount`。
`roomCount`/`vacantRoomCount` 是本列表唯一的聚合字段（首页看空置用），SQL 写在 Mapper XML；**房间明细走房间接口**。`cityId` 筛选依赖 infra 区域接口把市展开为区县 ID 列表（见第 5 节），infra 未就绪前先只支持 `districtId`。

#### 房间 `/room`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /create` | `RoomCreateReqVO` | `Result<Long>` |
| `POST /update` | `RoomUpdateReqVO` = `id` + 下面字段 | `Result<Void>` |
| `POST /delete` | `id` | `Result<Void>` |
| `GET /getById` | `id` | `Result<RoomRespVO>` |
| `POST /page` | `RoomPageReqVO` | `Result<PageRespVO<RoomPageItemRespVO>>` |
| `POST /updatePublishStatus` | `id`、`publishStatus` | `Result<Void>` |
| `GET /listSimpleByApartment` | `apartmentId` | `Result<List<RoomSimpleRespVO>>` 租约选房下拉 |

`RoomCreateReqVO`：`apartmentId`（必填）、`roomNumber`（必填）、`rent`（必填）、`area`、`roomCount`、`orientation`、`floorNo`、`labelCodes:List<String>`、`facilityCodes:List<String>`。
`RoomRespVO`：上面全部 + `apartmentName`、`labelCodes:List<DictItemVO>`、`facilityCodes:List<DictItemVO>`、`publishStatus`、`createTime`、`updateTime`。
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
| `GET /getById` | `id` | `Result<FeeItemRespVO>` |
| `GET /list` | 无 | `Result<List<FeeItemRespVO>>` |

`FeeItemRespVO`：`id`、`name`、`amount`、`unit`。费用项数量少，用列表接口不分页；同名校验在 Service，报错用 `FEE_ITEM_NAME_EXISTS`。

#### 租约 `/lease`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /create` | `LeaseCreateReqVO` | `Result<Long>` |
| `POST /update` | `LeaseUpdateReqVO` | `Result<Void>` |
| `POST /delete` | `id` | `Result<Void>` |
| `GET /getById` | `id` | `Result<LeaseRespVO>` |
| `POST /page` | `LeasePageReqVO` | `Result<PageRespVO<LeasePageItemRespVO>>` |
| `POST /updateStatus` | `id`、`status` | `Result<Void>` |

`LeaseCreateReqVO`：`userId`（必填，App 用户 ID）、`apartmentId`（必填）、`roomId`（必填）、`contractFileId`（可空，0 表示尚未上传）、`leaseStartDate`（必填）、`leaseEndDate`（必填）、`rent`（必填）、`deposit`（可空，不传按 `rent × 公寓 depositMonths` 计算）、`sourceType`、`remark`。
创建校验：房间存在且 `apartmentId` 与房间一致；`leaseEndDate` 晚于 `leaseStartDate`；房间没有处于 1/2/5 状态的租约，否则报 `ROOM_LEASE_EXISTS`。
`LeaseUpdateReqVO`：`id` + `contractFileId`、`leaseStartDate`、`leaseEndDate`、`rent`、`deposit`、`remark`；状态为 3/4/6 时不允许改条款（只能改合同文件与备注）。
`POST /delete` 仅允许状态 1/3 的租约，其他报 `LEASE_DELETE_FORBIDDEN`。
`updateStatus` 按第 3 节流转表校验，非法迁移报 `LEASE_STATUS_TRANSITION_INVALID`。

`LeaseRespVO`：`id`、`userId`、`apartmentId`、`apartmentName`、`roomId`、`roomNumber`、`contractFileId`、`leaseStartDate`、`leaseEndDate`、`rent`、`deposit`、`status`、`statusName`、`sourceType`、`remark`、`createTime`、`updateTime`。**不含租客实名信息（实名只在合同文件里）、不含付款方式快照**。
`LeasePageReqVO`：`userId`、`apartmentId`、`roomId`、`status`、`sourceType`、`leaseEndDateStart`、`leaseEndDateEnd`。
`LeasePageItemRespVO`：`id`、`userId`、`apartmentId`、`apartmentName`、`roomId`、`roomNumber`、`leaseStartDate`、`leaseEndDate`、`rent`、`deposit`、`status`、`statusName`、`sourceType`、`createTime`。

#### 看房预约 `/view-appointment`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /page` | `ViewAppointmentPageReqVO` | `Result<PageRespVO<ViewAppointmentRespVO>>` |
| `GET /getById` | `id` | `Result<ViewAppointmentRespVO>` |
| `POST /updateStatus` | `id`、`status` | `Result<Void>` |

`ViewAppointmentPageReqVO`：`userId`、`apartmentId`、`status`、`appointmentTimeStart`、`appointmentTimeEnd`、`name`（模糊）、`mobile`（模糊）。
`ViewAppointmentRespVO`：`id`、`userId`、`apartmentId`、`apartmentName`、`name`、`mobile`、`appointmentTime`、`status`、`statusName`、`remark`、`createTime`。
管理端只允许 1 待看房 → 3 已看房 / 2 已取消，其他迁移报错。

#### 浏览记录 `/room-browse`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /page` | `RoomBrowsePageReqVO` | `Result<PageRespVO<RoomBrowseRespVO>>` |

`RoomBrowsePageReqVO`：`userId`、`roomId`、`createTimeStart`、`createTimeEnd`（时间范围按 `create_time`，命名为起止时间而不是 `browseTime`）。
`RoomBrowseRespVO`：`id`、`userId`、`roomId`、`roomNumber`、`apartmentName`、`createTime`。浏览记录只读，不提供增删改（写入只在 App 端发生）。

#### 房源图片 `/image`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `GET /listByItem` | `itemType`、`itemId` | `Result<List<ImageRespVO>>` |
| `POST /saveBatch` | `ImageSaveBatchReqVO` | `Result<Void>` |
| `POST /delete` | `id` | `Result<Void>` |

`ImageSaveBatchReqVO`：`itemType`（1 公寓、2 房间）、`itemId`、`images:List<ImageItemReqVO{fileId, sort}>`；整体覆盖：先按 `(item_type, item_id)` 物理删除，再批量插入（逻辑删除会占唯一键，关联表一律物理删除）。
`ImageRespVO`：`id`、`fileId`、`sort`。**不返回 `name` 与访问地址**：文件名从 infra 文件接口取，地址按需重新签发（见第 5 节）。

### 4.2 用户端（`/app-api/...`）

登录、注册、刷新令牌由 infra 提供（待 infra 补齐）；本服务所有 App 端接口从 `UserContextHolder` 取 `userId`。

#### 公寓 `/apartment`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /page` | `AppApartmentPageReqVO` | `Result<PageRespVO<AppApartmentItemRespVO>>` |
| `GET /getById` | `id` | `Result<AppApartmentRespVO>` |

`AppApartmentPageReqVO`：`districtId`、`cityId`、`minRent`、`maxRent`、`paymentMethod`、`minLeaseMonths`、`roomCount`、`minArea`、`maxArea`、`labelCodes:List<String>`。只查 `publish_status = 1` 的公寓；租金/面积/室数条件落到房间表上做 `exists` 过滤。
`AppApartmentItemRespVO`：`id`、`name`、`districtId`、`districtName`、`addressDetail`、`minLeaseMonths`、`depositMonths`、`paymentMethod`、`paymentMethodName`、`minRent`、`coverFileId`、`labelCodes:List<DictItemVO>`、`facilityCodes:List<DictItemVO>`。
`AppApartmentRespVO`：上面全部 + `introduction`、`phone`、`feeItems:List<FeeItemSimpleRespVO>`；**图片单独调 `/image/listByItem`**。

#### 房间 `/room`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /page` | `AppRoomPageReqVO` | `Result<PageRespVO<AppRoomItemRespVO>>` |
| `GET /getById` | `id` | `Result<AppRoomRespVO>` |

`AppRoomPageReqVO`：`apartmentId`、`districtId`、`cityId`、`minRent`、`maxRent`、`roomCount`、`minArea`、`maxArea`、`orientation`、`labelCodes`、`facilityCodes`、`vacantOnly`。
`AppRoomItemRespVO`：`id`、`apartmentId`、`apartmentName`、`districtId`、`districtName`、`roomNumber`、`rent`、`area`、`roomCount`、`orientation`、`orientationName`、`floorNo`、`depositMonths`、`paymentMethod`、`minLeaseMonths`、`coverFileId`、`labelCodes`、`facilityCodes`。
`AppRoomRespVO`：上面全部 + 所属公寓精简信息（`apartmentId`、`apartmentName`、`addressDetail`、`phone`、`introduction`、`feeItems`）；图片单独调 `/image/listByItem`。

#### 看房预约 `/view-appointment`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /create` | `AppViewAppointmentCreateReqVO` | `Result<Long>` |
| `POST /page` | 继承 `PageReqVO`（无额外条件，只查自己） | `Result<PageRespVO<AppViewAppointmentRespVO>>` |
| `POST /cancel` | `id` | `Result<Void>` |

`AppViewAppointmentCreateReqVO`：`apartmentId`（必填）、`name`（必填）、`mobile`（必填）、`appointmentTime`（必填）、`remark`。`userId` 从上下文取，落库时姓名手机做快照。
`cancel` 只能取消自己的、状态为 1 待看房的预约，否则报 `APPOINTMENT_CANCEL_FORBIDDEN`。
`AppViewAppointmentRespVO`：`id`、`apartmentId`、`apartmentName`、`appointmentTime`、`status`、`statusName`、`remark`、`createTime`。

#### 浏览记录 `/room-browse`

| 接口 | 入参 | 返回 |
| --- | --- | --- |
| `POST /create` | `roomId` | `Result<Void>` |
| `POST /page` | 继承 `PageReqVO`（只查自己） | `Result<PageRespVO<AppRoomBrowseRespVO>>` |

`create` 语义：同一用户对同一房间只保留最新一条 —— 先按 `(user_id, room_id)` 物理删除旧行，再插入新行（表不会随浏览次数无限膨胀）。
`AppRoomBrowseRespVO`：`id`、`roomId`、`roomNumber`、`apartmentId`、`apartmentName`、`rent`、`coverFileId`、`createTime`；按 `create_time` 倒序。

## 5. 依赖 infra 的接口（待 infra 补齐）

现有可直接用：

- 文件上传：`POST /api/infra/admin-api/file/upload`（返回对象名与预签名地址）
- 后台字典：`GET /api/infra/admin-api/dict-data/listByType?dictType=`、`GET /api/infra/admin-api/dict-type/list`
- 后台区划：`GET /api/infra/admin-api/area/listChildren?parentId=`、`GET /api/infra/admin-api/area/listTree`

需要 infra 新增：

1. **App 端用户表 + 认证接口**：注册、登录、刷新令牌、查询当前用户（走 `infra_token.user_type = 2`）；rental 只引用 `userId`。
2. **字典读取（服务间/用户端）**：`GET /internal-api/dict-data/listByType`（或 app 端去权限版本），供 rental 回填 `DictItemVO` 中文名并做 Redis 缓存（key 走 `RentalRedisKeyConstant` + `RentalRedisKeyUtil`，一个 key 一个方法）。
3. **区域读取（服务间/用户端）**：`GET /internal-api/area/listChildren|listTree`，供 rental 把 `cityId` 展开为区县 ID 列表、App 端做三级联动。
4. **文件按 ID 查询**：`GET /internal-api/file/listByIds`（返回 `id`、`name`、`path` + 预签名地址），供 rental 把 `fileId` 换成展示地址（`rental_image.file_id`、`rental_lease.contract_file_id`）。
5. **App 用户批量查询**（可选）：管理端租约/预约列表要展示租客昵称手机时用；本期可以先只返回 `userId`。
6. **租客相关菜单权限**：`infra_menu` 加 rental 的目录/菜单/按钮（id 从 25 起，`perms` 用下面的常量值），`infra_role_menu` 授权给超管角色 1，追加到 `sql/rental.sql`。

`RentalPermissionConstant` 的 perm 串（与菜单按钮一一对应）：

```
rental:apartment:query / create / update / delete / update-publish-status
rental:room:query / create / update / delete / update-publish-status
rental:fee-item:query / create / update / delete
rental:lease:query / create / update / delete / update-status
rental:view-appointment:query / update-status
rental:room-browse:query
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
| 1_03_001_0002 | APARTMENT_HAS_ROOM | 公寓下还有房间，不能删除 |
| 1_03_002_0001 | ROOM_NOT_FOUND | 房间不存在 |
| 1_03_002_0002 | ROOM_NUMBER_EXISTS | 同一公寓下房间号已存在 |
| 1_03_002_0003 | ROOM_APARTMENT_MISMATCH | 房间不属于该公寓 |
| 1_03_002_0004 | ROOM_HAS_LEASE | 房间存在生效中的租约，不能删除或下架 |
| 1_03_003_0001 | FEE_ITEM_NOT_FOUND | 费用项不存在 |
| 1_03_003_0002 | FEE_ITEM_NAME_EXISTS | 费用项名称已存在 |
| 1_03_003_0003 | FEE_ITEM_IN_USE | 费用项已被公寓引用，不能删除 |
| 1_03_004_0001 | LEASE_NOT_FOUND | 租约不存在 |
| 1_03_004_0002 | ROOM_LEASE_EXISTS | 房间已有生效中的租约 |
| 1_03_004_0003 | LEASE_DATE_INVALID | 租约结束日期必须晚于开始日期 |
| 1_03_004_0004 | LEASE_STATUS_TRANSITION_INVALID | 租约状态不允许这样流转 |
| 1_03_004_0005 | LEASE_DELETE_FORBIDDEN | 该状态的租约不允许删除 |
| 1_03_004_0006 | LEASE_UPDATE_FORBIDDEN | 该状态的租约不允许修改条款 |
| 1_03_005_0001 | APPOINTMENT_NOT_FOUND | 预约记录不存在 |
| 1_03_005_0002 | APPOINTMENT_CANCEL_FORBIDDEN | 只能取消自己的待看房预约 |
| 1_03_005_0003 | APPOINTMENT_STATUS_TRANSITION_INVALID | 预约状态不允许这样流转 |
| 1_03_007_0001 | IMAGE_ITEM_TYPE_INVALID | 图片所属对象类型不合法 |
| 1_03_007_0002 | IMAGE_FILE_NOT_FOUND | 图片文件不存在 |

浏览记录（006）只读，没有业务错误码；参数校验、未登录、无权限、系统异常统一用 `CommonErrorConstant`。

## 7. 待你拍板的点

1. **浏览记录去重**：本文按「每个用户每个房间只留最新一条浏览时间」写；如果你要保留完整浏览流水，把 `create` 改成纯插入即可。
2. **管理端租约/预约列表是否展示租客昵称手机**：要展示就得等 infra 的 App 用户批量查询接口。
3. **押金是否允许后端自动算**：本文按「`deposit` 不传时按 `rent × depositMonths` 计算」写。

## 8. 验收

- 每个接口只做一件事：详情不含列表、列表不含明细，图片/房间/租约各自独立接口。
- 无 PO 泄漏：所有出参都是 `XxxRespVO`；跨服务对象才叫 DTO。
- 分页统一 `PageReqVO`/`PageRespVO`；路径全 camelCase「资源 + 动作」。
- 端前缀正确：admin 包下接口最终路径是 `/api/rental/admin-api/...`，app 包下是 `/api/rental/app-api/...`。
- 关联表覆盖写用物理删除；类型/唯一键与 `sql/rental.sql` 完全一致。
- 单测 mock Mapper、不依赖真实库/Redis/Nacos；`mvn -pl rental/rental-biz -am test` 通过，`mvn -DskipTests clean install` 全量通过。
