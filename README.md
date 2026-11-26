# 主要结构
0x9c6af4ade3a7f52d9f402d9066c6993021f834b452aa9836b0d0d956de223aef
```text
lease-proj
├── pom.xml                     # 根聚合父工程，统一管理依赖与版本
├── docs                        # 学习笔记与资料
│   ├── api
│   ├── database
│   ├── idea
│   ├── images
│   ├── linux
│   ├── minio
│   ├── mysql
│   └── mybatis-plus
├── hello-mp                    # MyBatis-Plus 练习模块
│   ├── pom.xml
│   └── src
├── hello-minio                 # MinIO 对象存储练习模块
│   ├── pom.xml
│   └── src
└── hello-knife4j               # Knife4j 接口文档练习模块
    ├── pom.xml
    └── src
```

## 生成或导入基础代码
在完成上述配置后，就可以使用一些逆向工具自动生成基础代码了（如，实体类，mapper,service等），在使用
Mybatis-Plus作为存储层框架时，使用Mybatis X 插件。

导入的代码和目标位置:

| 导入代码 | 模块 | 报名、路径 | 说明 |
|---------|-----|-----------|-----|
|实体类 | model | org.twiglau.lease.model.entity | 与数据库表一一对应 |
|枚举类 | model | org.twiglau.lease.model.enums | 实体类中的某些状态字段，使用枚举类型 |
|mapper接口 | web-admin | org.twiglau.lease.web.admin.mapper | 略 |
|mapper xml | web-admin | src/main/resources/mapper | 略 |
|service | web-admin | org.twiglau.lease.web.admin.service | 略 |
|serviceImpl | web-admin | org.twiglau.lease.web.admin.service.impl | 略 |

### 重点关注项:

- 实体类的公共字段（例如 id, create_time,update_time,is_deleted）抽取到一个基类，进行统一管理，然后让各实体类继承该基类。
- 实体类中的状态字段(例如 status) 或类型字段(例如 type),全部使用枚举类型。
  > 1. 状态（类型）字段，通常用一个数字表示一个状态（类型）。例如： 
  > 2. 订单状态： 1：待支付，2：待发货，3：待收货，4：已收货，5：已完结
  > 3. 通常情况下这种字段在数据库中使用int类型，只保存一个数字即可。若实体类中对应的字段也用数字类型。













