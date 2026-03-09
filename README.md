# WTrip —— 旅行记账 Android App

WTrip 是一款面向个人旅行者的 Android 原生记账应用，支持多行程管理、消费记录、分类统计与数据可视化。

---

## 功能介绍

- **行程管理**：新建、编辑、删除旅行行程，支持设置目的地与天数
- **消费记录**：按交通、饮食、住宿、体验四大类别录入消费，支持金额、支付方式、预定平台、简评、照片等字段
- **分类统计**：实时汇总各类别花费总额，直观展示每个行程的消费构成
- **数据可视化**：集成饼图展示各支付方式的消费占比
- **图片支持**：支持为消费记录附加照片，提供图片裁剪与预览功能

---

## 技术栈

| 技术 | 用途 |
|------|------|
| **Kotlin** | 项目主语言，全程使用 Kotlin 编写 |
| **Android SDK** | 原生 Android 开发（minSdk 24，targetSdk 36） |
| **Room Database** | 本地数据持久化，包含 Entity、DAO、Database、数据库版本迁移（Migration） |
| **Kotlin Coroutines + Flow** | 异步操作与响应式数据流，实现 UI 自动刷新 |
| **Repository 模式** | 数据访问层封装，解耦业务逻辑与数据库操作 |
| **Material Design 3** | 使用 MaterialToolbar、MaterialCardView、MaterialButton、TextInputLayout 等组件构建现代化 UI |
| **RecyclerView** | 列表与网格布局的自定义 Adapter 实现 |
| **MPAndroidChart** | 集成第三方图表库，实现饼图数据可视化 |
| **UCrop** | 集成图片裁剪库，处理用户上传的照片 |
| **KSP（Kotlin Symbol Processing）** | 配合 Room 进行注解处理，替代传统 kapt |
| **Activity Result API** | 使用 `registerForActivityResult` 处理页面跳转与返回结果 |
| **Gradle Kotlin DSL** | 使用 `build.gradle.kts` 进行构建配置管理 |

---

## 项目结构

```
app/src/main/java/com/baguetteui/wtrip/
├── data/
│   └── TripRepository.kt       # 数据仓库层
├── db/
│   ├── AppDatabase.kt          # Room 数据库，含 Migration
│   ├── ExpenseDao.kt           # 消费记录 DAO
│   ├── TripDao.kt              # 行程 DAO
│   └── ...                     # Entity、统计数据类
├── MainActivity.kt             # 行程列表主页
├── TripDetailActivity.kt       # 行程详情 + 统计图表
├── ExpenseDetailActivity.kt    # 消费记录详情/编辑
├── CategoryExpenseListActivity.kt  # 分类消费列表
├── PhotoPreviewActivity.kt     # 图片预览
└── ...                         # Adapter、数据模型、工具类
```

---

## 核心技术实践

- 使用 **Room + Flow** 实现数据库变更的实时监听，无需手动刷新列表
- 设计了完整的 **数据库版本迁移方案**（v1 → v6），保障用户数据在版本升级后不丢失
- 通过 **Repository 模式** 统一管理数据访问，Activity 仅负责 UI 逻辑
- 使用 **Kotlin Coroutines** 将数据库 I/O 切换到后台线程，保证主线程流畅
- 自定义 **RecyclerView Adapter** 支持点击与长按事件，实现行程和消费项的交互操作

---

## 开发环境

- Android Studio
- Kotlin 2.x
- Gradle 8.x（Kotlin DSL）
- 最低支持 Android 7.0（API 24）

---

## 作者

万远昊 · [WangYH183](https://github.com/WangYH183)
