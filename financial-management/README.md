# Financial Management System

## 项目概述

Financial Management System是一个用于管理个人金融投资组合的系统，支持资产的添加、编辑、删除，投资组合的管理，市场数据的获取，以及投资表现的分析。

## 技术栈

- **后端**：Spring Boot 4.0.5
- **数据库**：H2 Database（开发环境）
- **API文档**：Swagger/OpenAPI
- **构建工具**：Maven

## 核心功能

### 1. 资产管理
- 添加新资产
- 编辑已有资产
- 删除资产
- 按类型筛选资产

### 2. 投资组合管理
- 创建投资组合
- 查看投资组合详情
- 添加资产到投资组合
- 从投资组合中移除资产
- 计算投资组合总价值

### 3. 市场数据查询
- 获取实时市场价格（模拟数据）
- 更新资产价格
- 批量更新所有资产价格

### 4. API文档
- 通过Swagger UI查看API文档
- 支持API测试和调试

## 项目结构

```
financial-management/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── reserio/
│   │   │           └── financialmanagement/
│   │   │               ├── config/         # 配置类
│   │   │               ├── controller/      # 控制器
│   │   │               ├── dto/             # 数据传输对象
│   │   │               ├── model/           # 数据模型
│   │   │               ├── repository/      # 数据访问层
│   │   │               ├── service/         # 业务逻辑层
│   │   │               └── FinancialManagementApplication.java  # 应用入口
│   │   └── resources/
│   │       └── application.properties      # 配置文件
│   └── test/
│       └── java/
│           └── com/
│               └── reserio/
│                   └── financialmanagement/
│                       ├── service/         # 服务测试
│                       └── FinancialManagementApplicationTests.java  # 应用测试
├── pom.xml                                 # Maven配置文件
└── README.md                               # 项目文档
```

## 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.6+

### 2. 构建项目

```bash
mvn clean install
```

### 3. 运行项目

```bash
mvn spring-boot:run
```

### 4. 访问API文档

启动项目后，访问以下地址查看API文档：

```
http://localhost:8080/swagger-ui.html
```

### 5. 访问H2数据库控制台

```
http://localhost:8080/h2-console
```

- **JDBC URL**: jdbc:h2:mem:testdb
- **Username**: sa
- **Password**: （空）

## API端点

### 资产管理
- `GET /api/assets` - 获取所有资产
- `GET /api/assets/{id}` - 获取单个资产
- `POST /api/assets` - 创建资产
- `PUT /api/assets/{id}` - 更新资产
- `DELETE /api/assets/{id}` - 删除资产
- `GET /api/assets/type/{type}` - 按类型获取资产

### 投资组合管理
- `GET /api/portfolios` - 获取所有投资组合
- `GET /api/portfolios/{id}` - 获取单个投资组合
- `POST /api/portfolios` - 创建投资组合
- `PUT /api/portfolios/{id}` - 更新投资组合
- `DELETE /api/portfolios/{id}` - 删除投资组合
- `POST /api/portfolios/{portfolioId}/assets/{assetId}` - 添加资产到投资组合
- `DELETE /api/portfolios/{portfolioId}/assets/{assetId}` - 从投资组合中移除资产

### 市场数据管理
- `GET /api/market-data/{ticker}` - 获取单个资产的市场价格
- `POST /api/market-data/update` - 批量更新市场数据
- `POST /api/market-data/update/{assetId}` - 更新单个资产的市场价格

## 示例请求

### 创建资产

```json
POST /api/assets
Content-Type: application/json

{
  "ticker": "AAPL",
  "name": "Apple Inc.",
  "type": "stock",
  "quantity": 10,
  "buyPrice": 150.0,
  "currentPrice": 180.25
}
```

### 创建投资组合

```json
POST /api/portfolios
Content-Type: application/json

{
  "name": "My Portfolio",
  "description": "My investment portfolio"
}
```

### 添加资产到投资组合

```
POST /api/portfolios/1/assets/1
```

## 注意事项

- 本系统使用H2内存数据库，重启应用后数据会丢失
- 市场数据为模拟数据，实际应用中需要集成真实的市场数据API
- 系统目前只支持基本的资产管理和投资组合管理功能，后续可以扩展更多分析和决策支持功能

## 后续扩展

- 集成真实的市场数据API（如Yahoo Finance API）
- 添加用户认证和授权
- 实现更复杂的投资分析功能
- 支持多用户管理
- 添加前端界面
- 实现数据持久化存储

## 许可证

Apache 2.0
