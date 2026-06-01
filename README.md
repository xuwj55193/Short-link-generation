# 短链接生成 + 访问统计系统

## 技术栈

- Java 21
- Spring Boot 3.2.0
- MyBatis Plus
- MySQL 8.0+
- Redis 7.0+
- RocketMQ 5.1.4

## 功能特性

1. **短链接生成** - 用户输入长URL生成唯一短链接
2. **短链接跳转** - 访问短链接302跳转到原链接
3. **访问统计** - 异步统计访问次数、IP、设备类型
4. **IP黑名单** - 拦截非法IP访问
5. **限流** - 单IP每分钟最多100次请求

## 技术点覆盖

### Spring Boot
- MVC：短链生成/跳转/统计接口
- DI：Service/DAO注入
- 事务：短链创建事务管理
- AOP：记录每次访问日志、耗时
- 拦截器：拦截非法IP、限流

### MySQL
- 短链表（id/long_url/short_code/user_id）
- 访问日志表
- short_code唯一索引、user_id索引

### Redis
- String：存短链→长链映射
- Hash：存访问统计
- Set：存黑名单IP
- 分布式锁：防止同一长链重复生成短链

### RocketMQ
- 生产者：访问短链时发送消息
- 消费者：异步记录访问日志、更新统计

## 快速开始

### 环境准备

1. 安装 MySQL 8.0+，创建数据库：
```sql
CREATE DATABASE example_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 安装 Redis 7.0+，确保运行在 localhost:6379

3. 安装 RocketMQ 5.1.4：
```bash
# 启动NameServer
nohup sh mqnamesrv &

# 启动Broker
nohup sh mqbroker -n localhost:9876 &
```

### 修改配置

编辑 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/example_db
    username: your_username
    password: your_password
```

### 运行项目

```bash
mvn spring-boot:run
```

## API接口

### 1. 生成短链接

**POST** `/api/shortlink/generate`

请求体：
```json
{
    "longUrl": "https://www.example.com/path/to/page"
}
```

响应：
```json
{
    "shortUrl": "http://localhost:8080/abc123",
    "shortCode": "abc123",
    "longUrl": "https://www.example.com/path/to/page"
}
```

### 2. 访问短链接

**GET** `/{shortCode}`

返回302跳转到原链接

### 3. 获取访问统计

**GET** `/api/shortlink/stats/{shortCode}`

响应：
```json
{
    "shortCode": "abc123",
    "accessCount": 150
}
```

## 项目结构

```
src/main/java/com/example/shortlink/
├── ShortLinkApplication.java    # 启动类
├── aspect/
│   └── AccessLogAspect.java     # AOP切面
├── config/
│   ├── RedisConfig.java         # Redis配置
│   └── WebMvcConfig.java        # Web配置
├── controller/
│   └── ShortLinkController.java # REST控制器
├── consumer/
│   └── AccessLogConsumer.java   # MQ消费者
├── entity/
│   ├── AccessLog.java           # 访问日志实体
│   └── ShortLink.java           # 短链接实体
├── interceptor/
│   └── AccessInterceptor.java   # 拦截器
├── mapper/
│   ├── AccessLogMapper.java     # 访问日志Mapper
│   └── ShortLinkMapper.java     # 短链接Mapper
└── service/
    ├── RedisService.java        # Redis服务接口
    ├── RocketMQProducerService.java  # MQ生产者接口
    ├── ShortLinkService.java    # 短链接服务接口
    └── impl/
        ├── RedisServiceImpl.java
        ├── RocketMQProducerServiceImpl.java
        └── ShortLinkServiceImpl.java
```