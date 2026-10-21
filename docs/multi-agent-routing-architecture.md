# 多 Agent 路由架构

## 请求流程

```text
当前用户消息 + 主会话记忆（只读）
              |
              v
      确定性路由规则
        | 命中       | 未命中
        v            v
 HOSPITAL/RAG/GENERAL  Leader（只输出一个枚举值）
              \      /
               v    v
          选中的 Worker
               |
               v
          Worker 结果回传 Leader
```

Leader 负责初始路由和 Worker 结果汇总。每个 Worker 无论返回成功、需要用户信息、无结果、不支持还是错误，都必须先回到 Leader；Leader 判断当前消息是否还有未完成的子任务，必要时继续调用其他 Worker，最终只由 Leader 向前端输出答复。

## 记忆边界

- 主会话记忆由后端读取并传给 Leader 和当前 Worker，Agent 不写入。
- 后端在 Worker 返回后，统一追加用户消息和最终助手消息到主会话记忆。
- `conversation:<id>:worker:hospital`、`conversation:<id>:worker:rag`、`conversation:<id>:worker:general` 是三个隔离的私有记忆空间。
- 私有记忆不与其他 Worker 共享；主会话历史不会写入私有记忆。

## 测试

200 条公开用例见 [routing-test-cases-200.md](routing-test-cases-200.md)，测试实现见 [RoutingAccuracy200Test.java](../src/test/java/langchain4j/multiagent/RoutingAccuracy200Test.java)。
