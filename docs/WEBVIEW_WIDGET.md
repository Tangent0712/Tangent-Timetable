# WebView 小组件 / 全屏应用

> 更新日期：2026-09-26

「大切课程表」支持在 iPad 桌面小组件或 Android 上以 **WebView** 访问只读视图。

**iPad 与 Android 采用同一套方案**：由一个 WebView 宿主加载只读展示页 `/widget`，展示页再通过
**RESTful 接口**获取数据。宿主可以是：

- **iPad**：支持加载网页的桌面小组件（WebView 组件）；
- **Android**：全屏应用，或支持加载网页的桌面小组件。

本质都是「用 WebView 打开一个网页」，因此**无需维护任何原生客户端**：一份数据、一份 UI，
Web 与各 WebView 宿主共用同一套实现。

---

## 1. 整体链路

```
WebView 宿主
  ├─ iPad：桌面小组件（WebView 组件）
  └─ Android：全屏应用 / 桌面小组件（WebView）
        │  加载  https://<站点>/widget?key=<APIKEY>&scheduleId=<ID>
        ▼
前端展示页  /widget  (WidgetPreviewPage.tsx，公开只读，无需登录)
        │  fetch  每 60 秒轮询 + 页面可见时刷新
        ▼
后端接口  GET /api/widget/overview/{key}?scheduleId=<ID>
        │  实时聚合
        ▼
紧凑 JSON（今日课程 / 考试 / 待办 / 周次）
```

---

## 2. 展示页 `/widget`

- 路由：`/widget`，在 `App.tsx` 中注册，**早于登录判断**，公开访问、不依赖登录态。
- 参数（URL Query）：
  | 参数 | 必填 | 说明 |
  |------|------|------|
  | `key` | 是 | API Key，用于鉴权；缺省时回退到本地已存 Key 或演示 Key |
  | `scheduleId` | 否 | 指定课表；不传则默认该账号第一个课表 |
- 行为：每 60 秒轮询一次；`visibilitychange` / 窗口 `focus` 时立即刷新。
- 样式：暗色只读双栏布局（左：课程与考试；右：待办与 DDL 倒计时），样式在 `index.css` 的 `.widget-*`。
- 设置页「小组件 / WebView 网页地址」卡片可一键复制带 `key` 与当前课表的完整地址。

---

## 3. 后端接口

```
GET /api/widget/overview/{key}?scheduleId={id}
```

- 鉴权：Key 通过 **URL 路径** 传入，`WidgetController` 内手动校验
  （`apiKeyMapper.selectById`）；该路径已在 `WebMvcConfig` 加入拦截器白名单。
- 响应头：`Cache-Control: no-store`；`Content-Type: application/json;charset=utf-8`。
- 响应结构（`ApiResponse`）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "title": "大切课程表",
    "week": "第3周",
    "today": "2026-09-26 周六",
    "updatedAt": "12:30",
    "todayCourses": [
      { "name": "数据结构", "time": "10:40-11:25", "location": "教3-201",
        "teacher": "张老师", "period": "第3-4节", "status": "进行中",
        "date": "09-26", "dow": "周六", "week": 3, "sortKey": 3 }
    ],
    "exams": [
      { "name": "CET-6", "time": "09:00-11:20", "location": "教1-101",
        "date": "09-26", "dow": "周六", "week": 3, "status": "今天", "sortKey": 1 }
    ],
    "todayRemaining": 2,
    "todos": [
      { "title": "高数作业", "ddlText": "今天 23:00",
        "ddlRemain": "还有 10 小时", "overdue": false }
    ],
    "todoSummary": "3 项待办",
    "empty": false
  }
}
```

- 数据为空时返回空数组，`empty=true`，展示页显示「今天没有课 / 暂无待办」，不报错。

---

## 4. 配置

### 4.1 通用步骤

1. 在 Web 端「设置 → 小组件 / WebView 网页地址」选择课表，点击**一键复制**，得到形如
   `https://<站点>/widget?key=xxx&scheduleId=1` 的地址。
2. 在宿主（iPad 小组件 / Android 全屏应用）中新建 WebView，把该地址填入。
3. WebView 即加载该只读页面，展示今日课程、考试与待办。

> 该地址包含你的 API Key，仅供本人使用，请勿公开分享。

### 4.2 iPad

使用支持加载网页（WebView）的桌面小组件 App，新建一个中号（4×2）小组件，填入上述地址。

### 4.3 Android

使用全屏应用，或支持加载网页（WebView）的桌面小组件，填入上述地址即可。
Android 端不需要单独的课程表/待办原生实现——它只是这个只读页面的一个 WebView 容器。

---

## 5. 安全注意事项

- **Key 出现在 URL 中**：会进入浏览器/代理访问日志与历史记录，属于已知风险。
  建议仅在个人设备使用；后续可考虑改用短期 token。
- 接口只返回展示所需的汇总字段，不返回敏感明细。
- `/api/widget/**` 位于鉴权拦截器白名单，鉴权由 Controller 手动完成。

---

## 6. 验收清单

- [ ] `curl '<站点>/api/widget/overview/<key>'` 能直接拿到 JSON（不依赖 JS）。
- [ ] 返回 `charset=utf-8`，中文正常。
- [ ] 响应体精简，字段简短，适合小组件展示。
- [ ] 无效 Key 返回 401；空数据不报错。
- [ ] 展示页每 60 秒自动刷新，页面重新可见时立即刷新。
- [ ] 宿主（iPad 小组件 / Android 全屏）中暗色布局正常，无横向溢出。
