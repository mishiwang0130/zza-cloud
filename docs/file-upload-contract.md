# infra 文件上传接口契约（含大文件分片上传）

本文是 `zza-admin`、`zza-app` 接入 infra 文件上传的依据。实现代码前先读 `AGENTS.md`，再看本文。

## 1. 为什么要有分片上传

原来的 `POST /file/upload` 是「一次请求把整个文件传上来」：

- 服务端容器 `spring.servlet.multipart.max-file-size` 是 10MB，超过直接被容器拦掉（HTTP 400 + `1_02_005_0002`）；
- 就算把上限调大，单个请求的体积、耗时、内存占用都会跟着涨，弱网下必然超时，重传还得整文件重来。

对象存储内部虽然也是分片写，但那条链路是「服务端 → 对象存储」，解决不了「浏览器 → 服务端」。
所以大文件改成：**客户端切片 → 服务端逐片转发 → 服务端合并**。小文件保持原样，不改任何现成调用。

## 2. 什么时候用哪条链路

| 文件大小 | 走哪条 | 说明 |
| --- | --- | --- |
| ≤ 10MB | `POST /file/upload`（原接口不动） | 图片、头像等，前端零改动 |
| > 10MB | `POST /fileChunk/init` → `upload` → `complete` | 单文件上限 200MB，超限在 init 直接拒绝 |

10MB 是前后端约定的**阈值常量**（服务端配置项 `zza.infra.file.multipart-threshold`，默认 10MB）；
分片大小不是常量，**必须用 init 返回的 `chunkSize`**（默认 5MiB），客户端不要写死：
对象存储要求除最后一片外每片不小于 5MiB，由服务端统一决定才不会再合并时失败。

## 3. 接口清单

管理后台与用户端各一份，端前缀由服务按包名自动拼：

| 用途 | 管理后台 | 用户端 |
| --- | --- | --- |
| 初始化 | `POST /api/infra/admin-api/fileChunk/init` | `POST /api/infra/app-api/fileChunk/init` |
| 上传分片 | `POST /api/infra/admin-api/fileChunk/upload` | `POST /api/infra/app-api/fileChunk/upload` |
| 合并 | `POST /api/infra/admin-api/fileChunk/complete` | `POST /api/infra/app-api/fileChunk/complete` |
| 取消 | `POST /api/infra/admin-api/fileChunk/abort` | `POST /api/infra/app-api/fileChunk/abort` |

四个接口都要求登录（`Authorization: Bearer <accessToken>`），不挂菜单按钮权限。
一个分片上传会话只能被「创建它的用户 + 同一个端」使用，uploadId 泄露给别人也用不了。

### 3.1 初始化

`POST /fileChunk/init`，`Content-Type: application/json`

```json
{
  "fileName": "租赁合同.pdf",
  "fileSize": 12582912,
  "contentType": "application/pdf",
  "fileMd5": "0f343b0931126a20f133d67c2b018a3b"
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `fileName` | 是 | 原始文件名，最长 255，仅用于展示与落库，不参与对象名生成 |
| `fileSize` | 是 | 文件总字节数，必须 > 0 且 ≤ 200MB |
| `contentType` | 否 | MIME，最长 128；建议带上，否则下载时是 `application/octet-stream` |
| `fileMd5` | 是 | 文件 MD5（32 位十六进制），**只用于断点续传定位同一个文件**，不做秒传复用 |

返回：

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "uploadId": "6f1d0a6d-1f2c-4c3f-9c2f-1a2b3c4d5e6f",
    "chunkSize": 5242880,
    "totalChunks": 3,
    "uploadedPartNumbers": [1, 2]
  }
}
```

- `chunkSize`：分片大小（字节），除最后一片外每片都按它切；
- `totalChunks`：总分片数，服务端按 `fileSize` 与 `chunkSize` 算出，客户端不要自己算；
- `uploadedPartNumbers`：**已经传完的分片序号（升序）**。续传时跳过这些片即可；新会话是空数组。

### 3.2 上传分片

`POST /fileChunk/upload`，`Content-Type: multipart/form-data`

| 表单字段 | 说明 |
| --- | --- |
| `uploadId` | init 返回的会话 ID |
| `partNumber` | 分片序号，从 **1** 开始 |
| `file` | 该分片的二进制内容 |

返回：

```json
{ "code": 200, "msg": "成功", "data": { "partNumber": 1, "etag": "\"a1b2c3\"" } }
```

约定：

- 分片大小必须等于 `chunkSize`，最后一片是余数（1 ~ `chunkSize` 字节）；
- 非最后一片不能小于 5MiB，否则报 `1_02_005_0006`；
- **同一个 `partNumber` 重复上传会覆盖之前的分片**，所以失败重试是安全的，不需要先清理；
- 每传一片服务端会把会话续期一次，长时间上传不会中途过期。

### 3.3 合并

`POST /fileChunk/complete`，`Content-Type: application/json`

```json
{ "uploadId": "6f1d0a6d-1f2c-4c3f-9c2f-1a2b3c4d5e6f" }
```

返回体与原来的 `POST /file/upload` 完全一致（`FileUploadRespVO`）：

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "fileId": 1024,
    "objectName": "admin/20261005/6f1d0a6d....pdf",
    "url": "http://minio/zza/admin/...&X-Amz-Expires=3600..."
  }
}
```

- 合并前服务端会核对「分片数量」与「分片总大小」，与 init 声明不一致就报错，不会合并出半个文件；
- 分片清单由服务端从对象存储读取，**客户端不需要提交 ETag 清单**；
- **幂等**：同一个 `uploadId` 重复调 complete，返回的是同一份结果，不会重复落库；网络抖动可以放心重试。

### 3.4 取消

`POST /fileChunk/abort`，请求体同 complete。用户放弃上传（关页面、点取消、切换文件）时调用，
服务端会取消对象存储里的分片上传并释放缓存；重复调用是安全的。

## 4. 前端实现建议

### 4.1 整体流程

```
选择文件
  ├─ size ≤ 10MB → POST /file/upload（老逻辑，原样保留）
  └─ size > 10MB → ① 计算 MD5（Web Worker）
                   ② POST /fileChunk/init    → 拿 uploadId / chunkSize / totalChunks / 已传分片
                   ③ 按 chunkSize 切片，跳过已传分片，并发 3 上传
                   ④ 全部分片成功 → POST /fileChunk/complete → 拿 fileId
                   ⑤ 任一步用户取消 → POST /fileChunk/abort
```

业务表只存 `fileId`（与现有约定一致），预签名 `url` 只用于即时回显。

### 4.2 几个容易踩的点

1. **MD5 要在 Web Worker 里算**（如 `spark-md5` 分块喂数据）。200MB 在主线程算会把页面卡死几十秒。
   算好后可缓存在本地（key 用 `文件名 + size + lastModified`），避免重选同一个文件再算一次。
2. **并发数建议 3**。更高并发对小带宽没有收益，反而容易出现单片超时；顺序上传则太慢。
3. **每片失败退避重试 3 次**（如 1s / 3s / 8s）。重试就是重发同一 `partNumber`，服务端会覆盖，不会出现重复分片。
4. **进度按分片算**：`已传片数 / totalChunks`（续传时把 `uploadedPartNumbers.length` 计入初始值），
   比单请求的 `loaded/total` 稳定得多；单片内部如需更细，可再用该片的 axios `onUploadProgress` 折算。
5. **超时必须单独放大**：两个前端现在的 axios 全局 `timeout` 是 **15 秒**，
   分片上传、complete、init 都要显式传 `timeout: 120000`（或按片大小放大），否则大文件必然被前端自己掐断。
6. **取消要调 abort**，否则对象存储里会留下未完成的分片（虽然服务端有兜底清理，但不要依赖它）。
7. **续传**：刷新页面后重新选同一个文件 → 重新算 MD5 → 调 init。
   同一个用户 + 同一个端 + 同一个 MD5 + 同一个大小会命中同一条会话，`uploadedPartNumbers` 就是已经传过的片。
   换用户、换端、换文件内容都会创建新会话，不会串到别人的上传上。

## 5. 错误码（infra 服务位 02，文件模块 005）

响应统一 `Result`：业务失败是 HTTP 200 + 非 200 的 `code`，前端按 `code` 分支处理即可。

| 错误码 | 常量 | 含义 | 处理建议 |
| --- | --- | --- | --- |
| 1_02_005_0001 | FILE_EMPTY | 上传文件（分片）不能为空 | 前端拦截 |
| 1_02_005_0002 | FILE_SIZE_EXCEEDED | 超过大小限制 | 提示上限，阻断上传 |
| 1_02_005_0003 | FILE_UPLOAD_ERROR | 文件上传失败 | 服务端异常，提示重试 |
| 1_02_005_0004 | FILE_CHUNK_SESSION_NOT_FOUND | 分片上传会话不存在或已过期 | 重新走 init（等价于从头/断点续传） |
| 1_02_005_0005 | FILE_CHUNK_NUMBER_INVALID | 分片序号不合法 | 检查切片逻辑 |
| 1_02_005_0006 | FILE_CHUNK_SIZE_INVALID | 分片大小不合法 | 检查是否按 `chunkSize` 切片 |
| 1_02_005_0007 | FILE_CHUNK_INCOMPLETE | 分片未全部上传 | 用 init 返回的已传分片补齐后再 complete |
| 1_02_005_0008 | FILE_CHUNK_UPLOAD_ERROR | 分片上传失败 | 重试该分片 |
| 1_02_005_0009 | FILE_CHUNK_COMPLETE_ERROR | 合并分片失败 | 重试 complete |
| 1_02_005_0010 | FILE_CHUNK_ABORT_ERROR | 取消上传失败 | 记录日志即可，服务端有兜底清理 |

请求体校验失败（HTTP 400）与未登录（HTTP 401）沿用全局约定，不在上表内。

## 6. 运维与部署

1. **反向代理**：如果网关前面还有 nginx/ALB，`client_max_body_size` 必须 ≥ 单片大小 + 表单开销
   （按 5MiB 片算，配 6m 即可），否则会在代理层被掐断。
2. **未完成分片清理**：客户端异常退出可能留下对象存储里的未完成分片。
   服务端已有两道防线——会话缓存 24 小时过期、abort 主动取消；此外建议在 MinIO 桶 `zza` 上配置
   生命周期规则 `AbortIncompleteMultipartUpload`（例如 7 天），由对象存储兜底清理长期未完成的分片。
3. **容量**：分片上传的最大文件 200MB，可用 `zza.infra.file.max-file-size` 调整（调大后注意分片数不超过 10000）。

## 7. 验收

- ≤ 10MB 文件走 `POST /file/upload` 的结果与改造前完全一致（`fileId`/`objectName`/`url` 含义不变）。
- > 10MB 文件走分片：init → 按 `chunkSize` 上传全部分片 → complete 返回 `fileId`；
  用返回的预签名地址下载并比对 MD5 与原文件一致。
- 传到一半刷新页面、重选同一文件：init 返回的 `uploadedPartNumbers` 包含已传分片，只补传缺的片即可完成。
- 同一个 `uploadId` 重复调 complete 返回同一 `fileId`，不会新增 `infra_file` 记录。
- 调用 abort 后对象存储里查不到该未完成分片上传。
- 声明 201MB 的文件在 init 阶段被拒（`1_02_005_0002`）。
