# 简历项目成稿

以下内容按“一页简历、每个项目 3-4 条要点”的长度整理。项目中的具体准确率、延迟和吞吐量只有在你实际重新测量后才能替换为数字。

## 项目一：医疗小智——基于多智能体的企业级医疗助手

**项目描述：** 面向医院业务场景的智能服务平台，整合分诊建议、排班与预约、院内医学知识问答、互联网实时搜索及医学报告分析。

**技术栈：** Java 17、Spring Boot、LangChain4j、MySQL、MongoDB、Elasticsearch、DashScope、Tavily、PDFBox、PaddleOCR、SSE。

**简历要点：**

- 设计 Leader + Hospital/RAG/General Worker 多智能体架构，采用“确定性规则优先 + LLM 兜底”路由；通过能力边界、交接协议和统一结果整合，支持单轮多意图任务的拆分、串联、失败重试和最终答复。
- 将医院预约流程拆解为日期解析、排班/号源查询、个人信息收集、重复预约校验和事务写入等可验证工具链；使用 MySQL 约束业务状态，避免模型猜测日期、号源或用户身份。
- 构建 Elasticsearch 医学知识库，支持关键词、向量和混合检索并返回引用来源；通过独立 General Worker 接入 Tavily，处理路线、天气、新闻等时效性问题。
- 实现账户、登录、多会话和用户级数据隔离，使用 MongoDB 分层保存主会话与 Worker 私有记忆；支持 TXT/PDF/PNG/JPG 医学报告的文本提取、OCR、结构化和指标解释，并加入文件校验、日志脱敏和失败降级。

**项目难点：** 多 Agent 状态交接与内部轨迹隔离；LLM 输出与数据库真实状态的一致性；相对日期、号源和预约冲突的确定性校验；多模态文件处理与外部服务超时降级。

## 项目二：GRPO-Zero——从零实现可验证奖励驱动的数学推理强化学习

**项目描述：** 基于 Qwen2 风格 Decoder-only Transformer，从模型前向、KV Cache、轨迹采样到 GRPO 策略更新，构建 Countdown 数学推理任务的强化学习训练框架。

**技术栈：** Python、PyTorch、CUDA/BF16、TensorBoard、自定义 Tokenizer、Transformer、GRPO。

**简历要点：**

- 从零实现 Qwen2 风格 Transformer 核心组件，包括 RMSNorm、RoPE、Grouped-Query Attention、SwiGLU FeedForward、权重加载和 tied embedding 输出层。
- 实现带 KV Cache 的批量自回归 rollout：首轮计算完整 prompt 的 Key/Value，后续仅追加新 token，在保持完整上下文的同时降低重复计算开销。
- 针对 Countdown 任务设计可验证奖励函数，分别计算 `<think>/<answer>` 格式奖励、数字使用约束和表达式求值正确性；避免依赖人工偏好模型，直接用程序验证最终答案。
- 实现 GRPO 训练闭环：同一问题采样多条回答、组内奖励归一化得到优势、按回答 token mask 计算策略梯度、梯度裁剪、熵监控、TensorBoard 记录和定期 checkpoint 保存。

**项目难点：** rollout 阶段的变长序列和 KV Cache 管理；只对生成答案部分计算策略梯度；组内奖励归一化与可验证奖励设计；训练显存、BF16 和批量采样之间的平衡。

## 使用提醒

- `chapter11` 的简历版本不要写“准确率提升 X%”或“训练耗时 X 小时”，除非你在本地补齐数据、模型、训练日志并实际测量。
- 面试展示时优先讲清楚 `rollout -> reward -> group normalization -> policy update` 四步闭环，并现场解释为什么需要 KV Cache 和答案 token mask。
- 医疗小智项目适合突出系统设计和工程落地；GRPO-Zero 适合突出模型训练、强化学习原理和 PyTorch 底层实现，两个项目形成互补。
