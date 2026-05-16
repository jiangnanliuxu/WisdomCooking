# RuoYi-Cloud 后端微服务一键打包与启动说明

## 1. 适用范围

本文档对应两个入口：

```bash
bash scripts/start-all.sh
bash release/ruoyi-cloud-backend-package/start-all.sh
```

该脚本只负责 `RuoYi-Cloud` 后端可执行微服务的打包、启动、停止、状态查看和日志查看，不包含以下内容：

- 不启动 `admin-web`
- 不启动 `user-app`
- 不自动拉起 `Nacos`
- 不自动拉起 `Sentinel`
- 不自动拉起 `Redis`

本次覆盖的后端微服务如下：

- `ruoyi-system`
- `ruoyi-gen`
- `ruoyi-job`
- `ruoyi-file`
- `ruoyi-cook`
- `ruoyi-monitor`
- `ruoyi-auth`
- `ruoyi-gateway`

以下模块只参与 Maven 依赖构建，不单独启动：

- `ruoyi-api`
- `ruoyi-common`
- `ruoyi-modules`
- `ruoyi-visual`

## 1.1 发布目录说明

执行下面命令后：

```bash
bash scripts/start-all.sh build
```

脚本会自动生成一个发布目录：

```bash
release/ruoyi-cloud-backend-package/
```

该目录下会统一放置以下内容，便于管理和发布：

- 8 个后端可执行 jar
- `start-all.sh` 启动脚本
- 本说明文档
- `logs/` 日志目录

也就是说，后续如果你要打包给别人使用，优先交付整个 `release/ruoyi-cloud-backend-package/` 目录即可。

## 2. 前置条件

执行脚本前，以下基础依赖必须已经在本机启动完成：

- `Nacos`：`127.0.0.1:8848`
- `Sentinel`：`127.0.0.1:8718`
- `Redis`：`127.0.0.1:6379`

脚本会在启动前主动检测以上端口；如果任一未就绪，会直接报错退出，不会继续启动 jar。

默认环境参数如下：

```bash
NACOS_ADDR=127.0.0.1:8848
NACOS_NAMESPACE=public
NACOS_USERNAME=371
NACOS_PASSWORD=123456
SENTINEL_DASHBOARD=127.0.0.1:8718
REDIS_ADDR=127.0.0.1:6379
```

## 3. 打包命令

脚本内置的全量打包命令如下：

```bash
mvn -pl ruoyi-gateway,ruoyi-auth,ruoyi-visual/ruoyi-monitor,ruoyi-modules/ruoyi-system,ruoyi-modules/ruoyi-gen,ruoyi-modules/ruoyi-job,ruoyi-modules/ruoyi-file,ruoyi-modules/ruoyi-cook -am -DskipTests package
```

如果你只想执行打包，不启动服务，使用：

```bash
bash scripts/start-all.sh build
```

执行完成后，除了源码目录下的 `target/` 产物外，还会把可发布内容同步整理到：

```bash
release/ruoyi-cloud-backend-package/
```

## 4. 常用命令

### 4.1 启动全部后端微服务

```bash
bash scripts/start-all.sh start
bash release/ruoyi-cloud-backend-package/start-all.sh start
```

### 4.2 重启全部后端微服务

```bash
bash scripts/start-all.sh restart
bash release/ruoyi-cloud-backend-package/start-all.sh restart
```

### 4.3 停止全部后端微服务

```bash
bash scripts/start-all.sh stop
bash release/ruoyi-cloud-backend-package/start-all.sh stop
```

### 4.4 查看当前状态

```bash
bash scripts/start-all.sh status
bash release/ruoyi-cloud-backend-package/start-all.sh status
```

### 4.5 查看某个服务日志

```bash
bash scripts/start-all.sh logs ruoyi-cook
bash scripts/start-all.sh logs ruoyi-gateway
bash release/ruoyi-cloud-backend-package/start-all.sh logs ruoyi-cook
```

### 4.6 跳过重新打包

如果你确认 jar 已经是最新的，可以跳过打包阶段：

```bash
bash scripts/start-all.sh start --skip-build
bash scripts/start-all.sh restart --skip-build
```

发布目录下的脚本不包含打包能力，因此不需要 `--skip-build`。

## 5. 微服务默认端口与健康检查地址

| 服务 | 端口 | 健康检查 |
| --- | --- | --- |
| `ruoyi-monitor` | `9100` | `http://127.0.0.1:9100/actuator/health` |
| `ruoyi-auth` | `9200` | `http://127.0.0.1:9200/actuator/health` |
| `ruoyi-system` | `9201` | `http://127.0.0.1:9201/actuator/health` |
| `ruoyi-gen` | `9202` | `http://127.0.0.1:9202/actuator/health` |
| `ruoyi-job` | `9203` | `http://127.0.0.1:9203/actuator/health` |
| `ruoyi-cook` | `9210` | `http://127.0.0.1:9210/actuator/health` |
| `ruoyi-file` | `9300` | `http://127.0.0.1:9300/actuator/health` |
| `ruoyi-gateway` | `8080` | `http://127.0.0.1:8080/actuator/health` |

## 6. 日志与 screen 会话

源码目录脚本默认把日志写入：

```bash
logs/startup/
```

发布目录脚本默认把日志写入：

```bash
release/ruoyi-cloud-backend-package/logs/
```

对应日志文件示例：

- `logs/startup/ruoyi-system.log`
- `logs/startup/ruoyi-gen.log`
- `logs/startup/ruoyi-job.log`
- `logs/startup/ruoyi-file.log`
- `logs/startup/ruoyi-cook.log`
- `logs/startup/ruoyi-monitor.log`
- `logs/startup/ruoyi-auth.log`
- `logs/startup/ruoyi-gateway.log`

对应 `screen` 会话名与服务名保持一致，例如：

- `ruoyi-system`
- `ruoyi-gen`
- `ruoyi-job`
- `ruoyi-file`
- `ruoyi-cook`
- `ruoyi-monitor`
- `ruoyi-auth`
- `ruoyi-gateway`

## 7. 标准执行流程

推荐按以下顺序执行：

### 第一步：停止当前项目旧进程

```bash
bash scripts/start-all.sh stop
```

### 第二步：全量打包并启动

```bash
bash scripts/start-all.sh start
```

如果只想重启并跳过打包：

```bash
bash scripts/start-all.sh restart --skip-build
```

如果是从发布目录直接运行，标准流程是：

```bash
bash release/ruoyi-cloud-backend-package/start-all.sh stop
bash release/ruoyi-cloud-backend-package/start-all.sh start
bash release/ruoyi-cloud-backend-package/start-all.sh status
```

### 第三步：查看状态

```bash
bash scripts/start-all.sh status
```

### 第四步：逐个检查健康接口

```bash
curl http://127.0.0.1:9100/actuator/health
curl http://127.0.0.1:9200/actuator/health
curl http://127.0.0.1:9201/actuator/health
curl http://127.0.0.1:9202/actuator/health
curl http://127.0.0.1:9203/actuator/health
curl http://127.0.0.1:9210/actuator/health
curl http://127.0.0.1:9300/actuator/health
curl http://127.0.0.1:8080/actuator/health
```

## 8. 常见失败排查

### 8.1 基础依赖未启动

如果脚本直接报以下类似错误：

```bash
ERROR: Nacos is not listening on 127.0.0.1:8848. Start it first.
ERROR: Sentinel is not listening on 127.0.0.1:8718. Start it first.
ERROR: Redis is not listening on 127.0.0.1:6379. Start it first.
```

说明不是微服务 jar 本身有问题，而是外部依赖没准备好。先把对应依赖拉起来，再重新执行脚本。

### 8.2 端口被占用

如果脚本提示某个端口已被占用，会打印当前监听进程。先确认是不是本项目旧进程没有停干净，或是否有其他程序占用了相同端口。

### 8.3 Jar 缺失

如果报：

```bash
Missing jar: ...
```

说明打包未成功，或者使用了 `--skip-build` 但目标 jar 还不存在。去掉 `--skip-build` 重新执行。

### 8.4 某个服务健康检查失败

先看对应日志，例如：

```bash
bash scripts/start-all.sh logs ruoyi-cook
```

或者直接打开：

```bash
logs/startup/ruoyi-cook.log
release/ruoyi-cloud-backend-package/logs/ruoyi-cook.log
```

重点排查：

- Nacos 注册/配置连接失败
- Redis 连接失败
- 数据库连接失败
- 端口冲突
- 配置中心缺配置项

## 9. 本次验证口径

本次脚本交付后的验证口径如下：

- `stop` 能关闭本项目相关微服务进程
- `build` 能打出 8 个可执行服务的目标 jar
- `build` 能同步产出 `release/ruoyi-cloud-backend-package/` 发布目录
- `start` 仅在 `Nacos + Sentinel + Redis` 已就绪时继续执行
- `start/restart` 能拉起 8 个后端服务
- `status` 能输出基础依赖状态和 8 个微服务运行状态
- `logs <service>` 能查看对应服务日志
- 发布目录下能同时看到 jar、启动脚本和说明文档
- 文档中的命令、端口、日志路径与脚本实际行为保持一致
