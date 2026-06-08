# 物联网环境监测数据实时聚合与告警API服务

## 项目简介

基于 Java + Spring Boot 构建的物联网环境监测数据实时聚合与告警 API 服务。

## 功能特性

- **设备管理**：设备注册、状态管理、设备查询、心跳检测
- **数据接收**：支持 HTTP 和 MQTT 两种协议接入
- **数据聚合**：实时数据统计、分钟/小时/日维度聚合
- **告警管理**：告警规则配置、异常检测、多渠道通知
- **告警通知**：支持邮件、短信、Webhook 等通知方式

## 技术栈

- Java 17
- Spring Boot 3.2
- Spring Data JPA
- Spring Integration MQTT
- H2 Database (开发) / MySQL (生产)
- Lombok

## 项目结构

```
iot-monitor/
├── iot-common/              # 公共模块：实体、DTO、工具类
├── iot-device-management/   # 设备管理模块
├── iot-data-receiver/       # 数据接收模块（HTTP+MQTT）
├── iot-data-aggregation/    # 数据聚合与统计模块
├── iot-alert-management/    # 告警管理模块
└── iot-api/                 # API网关/启动模块
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 5.7+ (生产环境)
- MQTT Broker (可选，如 EMQX、Mosquitto)

### 本地运行

```bash
# 编译项目
mvn clean package -DskipTests

# 运行（使用H2内存数据库）
java -jar iot-api/target/iot-api-1.0.0.jar
```

服务启动后访问：
- API 地址：http://localhost:8080
- H2 控制台：http://localhost:8080/h2-console

### 生产部署

1. 准备 MySQL 数据库

```sql
CREATE DATABASE iot_monitor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改配置文件 `application-prod.yml`

3. 部署到 Linux 服务器

```bash
# 创建部署目录
mkdir -p /opt/iot-monitor/bin
mkdir -p /var/log/iot-monitor

# 复制文件
cp iot-api/target/iot-api-1.0.0.jar /opt/iot-monitor/
cp deploy/iot-monitor.sh /opt/iot-monitor/bin/
cp deploy/iot-monitor.service /etc/systemd/system/

# 设置权限
chmod +x /opt/iot-monitor/bin/iot-monitor.sh

# 启动服务
systemctl daemon-reload
systemctl start iot-monitor
systemctl enable iot-monitor
```

## API 接口

### 设备管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/devices | 创建设备 |
| GET | /api/devices/{id} | 查询设备详情 |
| GET | /api/devices | 设备列表 |
| PUT | /api/devices/{id} | 更新设备 |
| DELETE | /api/devices/{id} | 删除设备 |
| GET | /api/devices/statistics | 设备统计 |

### 数据接收

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/data/ingest | 单条数据上报 |
| POST | /api/data/ingest/batch | 批量数据上报 |
| POST | /api/data/ingest/{deviceCode}/{metric} | 简单数据上报 |

### 数据查询

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/data/history/{deviceCode} | 历史数据查询 |
| GET | /api/data/aggregated | 聚合数据查询 |
| GET | /api/data/realtime/{deviceCode}/{metric} | 实时统计 |
| GET | /api/data/latest/{deviceCode} | 最新数据 |

### 告警规则

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/alerts/rules | 创建告警规则 |
| GET | /api/alerts/rules/{id} | 查询规则详情 |
| GET | /api/alerts/rules | 规则列表 |
| PUT | /api/alerts/rules/{id} | 更新规则 |
| DELETE | /api/alerts/rules/{id} | 删除规则 |
| PUT | /api/alerts/rules/{id}/toggle | 启用/禁用规则 |

### 告警记录

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/alerts/records | 告警记录列表 |
| GET | /api/alerts/records/{id} | 告警详情 |
| PUT | /api/alerts/records/{id}/acknowledge | 确认告警 |
| PUT | /api/alerts/records/{id}/resolve | 处理告警 |
| PUT | /api/alerts/records/{id}/close | 关闭告警 |
| GET | /api/alerts/records/statistics | 告警统计 |

## MQTT 数据接入

Topic 格式：`iot/data/{deviceCode}`

Payload 格式（JSON）：

```json
{
  "temperature": 25.5,
  "humidity": 60.2,
  "pressure": 1013.25,
  "timestamp": "2024-01-01 12:00:00"
}
```

## 告警规则类型

- **阈值告警**：大于、小于、等于、范围内/外等比较
- **异常检测**：基于统计学的异常值检测
- **设备离线**：设备超时未上报数据
- **自定义**：支持自定义告警逻辑

## 配置说明

主要配置项：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| server.port | 8080 | 服务端口 |
| mqtt.enabled | false | 是否启用MQTT |
| mqtt.broker-url | tcp://localhost:1883 | MQTT Broker地址 |
| mqtt.topic | iot/data/# | 订阅主题 |

## License

MIT License
