param(
    [string]$OutputPath = (Join-Path $PSScriptRoot "..\docs\医疗小智-面试项目介绍.pptx")
)

$ErrorActionPreference = "Stop"

function Get-Rgb([int]$R, [int]$G, [int]$B) {
    return $R + 256 * $G + 65536 * $B
}

$Color = @{
    Ink       = Get-Rgb 20 31 48
    Muted     = Get-Rgb 91 105 124
    White     = Get-Rgb 255 255 255
    Canvas    = Get-Rgb 246 248 251
    Teal      = Get-Rgb 10 125 116
    TealDark  = Get-Rgb 7 91 85
    TealSoft  = Get-Rgb 226 244 241
    Blue      = Get-Rgb 54 99 216
    BlueSoft  = Get-Rgb 232 238 255
    Amber     = Get-Rgb 224 145 35
    AmberSoft = Get-Rgb 255 245 224
    Red       = Get-Rgb 205 65 69
    RedSoft   = Get-Rgb 255 235 236
    Green     = Get-Rgb 50 153 99
    Line      = Get-Rgb 216 223 232
    Dark      = Get-Rgb 12 40 52
}

$script:Ppt = New-Object -ComObject PowerPoint.Application
$script:Ppt.Visible = -1
$script:Deck = $script:Ppt.Presentations.Add()
$script:Deck.PageSetup.SlideWidth = 960
$script:Deck.PageSetup.SlideHeight = 540
$script:SlideNo = 0

function Add-Slide([int]$Background = $Color.Canvas) {
    $script:SlideNo++
    $slide = $script:Deck.Slides.Add($script:SlideNo, 12)
    $slide.FollowMasterBackground = 0
    $slide.Background.Fill.Solid()
    $slide.Background.Fill.ForeColor.RGB = $Background
    return $slide
}

function Add-Text {
    param(
        $Slide,
        [string]$Text,
        [double]$Left,
        [double]$Top,
        [double]$Width,
        [double]$Height,
        [double]$Size = 20,
        [int]$ColorValue = $Color.Ink,
        [bool]$Bold = $false,
        [int]$Align = 1,
        [string]$Font = "Microsoft YaHei",
        [int]$Vertical = 1
    )
    $shape = $Slide.Shapes.AddTextbox(1, $Left, $Top, $Width, $Height)
    $shape.TextFrame.MarginLeft = 0
    $shape.TextFrame.MarginRight = 0
    $shape.TextFrame.MarginTop = 0
    $shape.TextFrame.MarginBottom = 0
    $shape.TextFrame.WordWrap = -1
    $shape.TextFrame.VerticalAnchor = $Vertical
    $range = $shape.TextFrame.TextRange
    $range.Text = $Text
    $range.Font.Name = $Font
    $range.Font.NameFarEast = $Font
    $range.Font.Size = $Size
    $range.Font.Bold = $(if ($Bold) { -1 } else { 0 })
    $range.Font.Color.RGB = $ColorValue
    $range.ParagraphFormat.Alignment = $Align
    return $shape
}

function Add-Box {
    param(
        $Slide,
        [double]$Left,
        [double]$Top,
        [double]$Width,
        [double]$Height,
        [int]$Fill,
        [int]$Border = $Color.Line,
        [double]$RadiusType = 5
    )
    $shape = $Slide.Shapes.AddShape($RadiusType, $Left, $Top, $Width, $Height)
    $shape.Fill.Solid()
    $shape.Fill.ForeColor.RGB = $Fill
    $shape.Line.ForeColor.RGB = $Border
    $shape.Line.Weight = 1
    return $shape
}

function Add-Card {
    param(
        $Slide,
        [string]$Title,
        [string]$Body,
        [double]$Left,
        [double]$Top,
        [double]$Width,
        [double]$Height,
        [int]$Accent = $Color.Teal,
        [int]$Fill = $Color.White
    )
    $null = Add-Box $Slide $Left $Top $Width $Height $Fill $Color.Line
    $bar = $Slide.Shapes.AddShape(1, $Left, $Top, 6, $Height)
    $bar.Fill.Solid(); $bar.Fill.ForeColor.RGB = $Accent; $bar.Line.Visible = 0
    $null = Add-Text $Slide $Title ($Left + 20) ($Top + 15) ($Width - 35) 28 18 $Color.Ink $true
    $null = Add-Text $Slide $Body ($Left + 20) ($Top + 52) ($Width - 35) ($Height - 64) 13 $Color.Muted $false
}

function Add-Header($Slide, [string]$Index, [string]$Title, [string]$SubTitle = "") {
    $null = Add-Text $Slide $Index 42 26 80 22 12 $Color.Teal $true
    $null = Add-Text $Slide $Title 42 52 840 40 27 $Color.Ink $true
    if ($SubTitle) {
        $null = Add-Text $Slide $SubTitle 42 93 840 24 12 $Color.Muted $false
    }
    $line = $Slide.Shapes.AddLine(42, 122, 918, 122)
    $line.Line.ForeColor.RGB = $Color.Line
}

function Add-Footer($Slide) {
    $null = Add-Text $Slide "医疗小智 · Multi-Agent Medical Assistant" 42 512 500 14 9 $Color.Muted $false
    $null = Add-Text $Slide ([string]$script:SlideNo) 890 512 28 14 9 $Color.Muted $true 3
}

function Add-Arrow($Slide, [double]$X1, [double]$Y1, [double]$X2, [double]$Y2, [int]$ArrowColor = $Color.Teal) {
    $line = $Slide.Shapes.AddLine($X1, $Y1, $X2, $Y2)
    $line.Line.ForeColor.RGB = $ArrowColor
    $line.Line.Weight = 2
    $line.Line.EndArrowheadStyle = 3
    return $line
}

function Add-Step {
    param($Slide, [string]$Number, [string]$Title, [string]$Body, [double]$Left, [double]$Top, [double]$Width, [int]$Accent = $Color.Teal)
    $null = Add-Box $Slide $Left $Top $Width 96 $Color.White $Color.Line
    $circle = $Slide.Shapes.AddShape(9, ($Left + 12), ($Top + 13), 30, 30)
    $circle.Fill.Solid(); $circle.Fill.ForeColor.RGB = $Accent; $circle.Line.Visible = 0
    $null = Add-Text $Slide $Number ($Left + 12) ($Top + 18) 30 18 12 $Color.White $true 2
    $null = Add-Text $Slide $Title ($Left + 52) ($Top + 13) ($Width - 64) 24 15 $Color.Ink $true
    $null = Add-Text $Slide $Body ($Left + 15) ($Top + 52) ($Width - 30) 32 11 $Color.Muted $false
}

# Slide 1
$slide = Add-Slide $Color.Dark
$accent = $slide.Shapes.AddShape(1, 0, 0, 960, 10)
$accent.Fill.Solid(); $accent.Fill.ForeColor.RGB = $Color.Teal; $accent.Line.Visible = 0
$pill = Add-Box $slide 54 64 210 34 $Color.Teal $Color.Teal 5
$null = Add-Text $slide "INTERVIEW PROJECT" 70 72 180 18 11 $Color.White $true 2
$null = Add-Text $slide "医疗小智" 54 142 560 74 42 $Color.White $true
$null = Add-Text $slide "面向医院业务的多智能体医疗助手" 57 222 600 38 24 (Get-Rgb 192 228 223) $false
$null = Add-Text $slide "让大模型负责理解，让代码负责约束和执行" 57 290 610 30 17 $Color.White $false
$null = Add-Text $slide "Spring Boot  ·  LangChain4j  ·  RAG  ·  Tool Calling  ·  Multimodal" 57 356 720 24 13 (Get-Rgb 166 184 195) $false
$badge = Add-Box $slide 743 110 150 150 $Color.TealDark $Color.Teal 5
$null = Add-Text $slide "AI" 743 137 150 58 40 $Color.White $true 2
$null = Add-Text $slide "医疗服务编排" 743 203 150 24 13 (Get-Rgb 192 228 223) $true 2
$null = Add-Text $slide "项目架构 / 技术难点 / 工程实践" 57 468 520 22 12 (Get-Rgb 166 184 195) $false

# Slide 2
$slide = Add-Slide
Add-Header $slide "01" "项目背景：医疗问答不只是回答问题" "目标是把大模型接入真实医院业务，而不是封装一次聊天 API"
Add-Card $slide "业务执行" "查询医生排班与剩余号源`n预约、查询和取消挂号" 42 150 270 125 $Color.Teal
Add-Card $slide "知识服务" "医院与科室知识问答`n医学科普与引用来源" 345 150 270 125 $Color.Blue
Add-Card $slide "实时与多模态" "路线、天气、新闻`n医学报告 OCR 与结构化" 648 150 270 125 $Color.Amber
$null = Add-Box $slide 42 307 876 146 $Color.Dark $Color.Dark
$null = Add-Text $slide "核心挑战" 66 328 150 24 16 $Color.White $true
$null = Add-Text $slide "不同请求需要不同数据源、工具和权限；模型输出必须与数据库真实状态一致；多轮对话还要保证用户、会话与内部轨迹隔离。" 66 369 810 58 18 (Get-Rgb 220 232 238) $false
Add-Footer $slide

# Slide 3
$slide = Add-Slide
Add-Header $slide "02" "核心能力总览"
Add-Card $slide "多智能体协作" "Leader + 3 Worker`n规则优先、模型兜底" 42 150 200 124 $Color.Teal
Add-Card $slide "可执行工具链" "号源、预约、取消`n日期与身份校验" 258 150 200 124 $Color.Blue
Add-Card $slide "医学 RAG" "关键词 + 向量检索`n结果携带引用" 474 150 200 124 $Color.Green
Add-Card $slide "医学报告" "TXT/PDF/图像`nOCR、结构化、解释" 690 150 228 124 $Color.Amber
Add-Card $slide "账户与会话" "注册、登录、注销`n一个用户多个会话" 42 302 273 124 $Color.Blue
Add-Card $slide "分层记忆" "主会话与 Worker 私有记忆`n工具轨迹不进入前端" 344 302 273 124 $Color.Teal
Add-Card $slide "实时信息" "Tavily 搜索`n路线、天气和新闻" 646 302 272 124 $Color.Red
Add-Footer $slide

# Slide 4
$slide = Add-Slide
Add-Header $slide "03" "系统总体架构" "请求从认证入口进入统一编排层，再访问受限工具和数据源"
$null = Add-Box $slide 52 157 135 58 $Color.Dark $Color.Dark
$null = Add-Text $slide "Web 前端" 52 176 135 22 16 $Color.White $true 2
$null = Add-Arrow $slide 187 186 240 186
$null = Add-Box $slide 240 146 160 80 $Color.White $Color.Teal
$null = Add-Text $slide "Spring Boot API" 252 162 136 23 16 $Color.Ink $true 2
$null = Add-Text $slide "认证 / 会话 / SSE / 文件" 252 193 136 18 10 $Color.Muted $false 2
$null = Add-Arrow $slide 400 186 453 186
$null = Add-Box $slide 453 140 192 92 $Color.TealSoft $Color.Teal
$null = Add-Text $slide "MultiAgentOrchestrator" 465 158 168 23 15 $Color.TealDark $true 2
$null = Add-Text $slide "规则路由 + Leader 编排" 465 193 168 18 11 $Color.Muted $false 2
$null = Add-Arrow $slide 645 186 698 186
$null = Add-Box $slide 698 146 190 80 $Color.BlueSoft $Color.Blue
$null = Add-Text $slide "Leader Agent" 710 161 166 23 16 $Color.Blue $true 2
$null = Add-Text $slide "交接 / 拆分 / 最终整合" 710 193 166 18 10 $Color.Muted $false 2
$workers = @(
    @{ X = 152; Title = "Hospital Worker"; Body = "预约工具 / 日期工具"; Accent = $Color.Teal },
    @{ X = 386; Title = "RAG Worker"; Body = "Elasticsearch 知识库"; Accent = $Color.Green },
    @{ X = 620; Title = "General Worker"; Body = "Tavily 实时搜索"; Accent = $Color.Amber }
)
foreach ($worker in $workers) {
    $null = Add-Box $slide $worker.X 330 188 82 $Color.White $worker.Accent
    $null = Add-Text $slide $worker.Title ($worker.X + 10) 346 168 22 14 $Color.Ink $true 2
    $null = Add-Text $slide $worker.Body ($worker.X + 10) 379 168 18 10 $Color.Muted $false 2
    $null = Add-Arrow $slide 548 232 ($worker.X + 94) 326 $worker.Accent
}
$null = Add-Text $slide "MySQL  ·  MongoDB  ·  Elasticsearch  ·  外部模型/OCR/搜索服务" 178 451 604 24 13 $Color.Muted $true 2
Add-Footer $slide

# Slide 5
$slide = Add-Slide
Add-Header $slide "04" "多智能体职责边界" "Worker 只访问被授权的工具；Leader 不直接执行业务工具"
$cols = @(42, 224, 478, 734)
$widths = @(182, 254, 256, 184)
$headers = @("组件", "负责内容", "可访问能力", "输出去向")
for ($i = 0; $i -lt 4; $i++) {
    $null = Add-Box $slide $cols[$i] 148 $widths[$i] 42 $Color.Dark $Color.Dark 1
    $null = Add-Text $slide $headers[$i] ($cols[$i] + 8) 160 ($widths[$i] - 16) 18 12 $Color.White $true 2
}
$rows = @(
    @("Leader", "路由、任务交接、整合", "主会话与 Worker 结果", "前端最终答复"),
    @("Hospital", "分诊、排班、预约", "AppointmentTools / DateTools", "返回 Leader"),
    @("RAG", "医院、科室、医学知识", "Elasticsearch 检索上下文", "返回 Leader"),
    @("General", "路线、天气、实时信息", "WebSearchTool", "返回 Leader")
)
for ($r = 0; $r -lt $rows.Count; $r++) {
    $top = 190 + $r * 61
    $fill = $(if ($r % 2 -eq 0) { $Color.White } else { Get-Rgb 241 245 248 })
    for ($c = 0; $c -lt 4; $c++) {
        $null = Add-Box $slide $cols[$c] $top $widths[$c] 61 $fill $Color.Line 1
        $null = Add-Text $slide $rows[$r][$c] ($cols[$c] + 10) ($top + 15) ($widths[$c] - 20) 32 11 $Color.Ink ($c -eq 0) 2  "Microsoft YaHei" 3
    }
}
$null = Add-Text $slide "边界原则：模型负责理解和表达，工具负责读取事实与执行操作。" 42 459 876 24 15 $Color.TealDark $true 2
Add-Footer $slide

# Slide 6
$slide = Add-Slide
Add-Header $slide "05" "路由与多任务处理" "将可确定的问题交给代码，把不确定性留给模型"
Add-Step $slide "1" "解析当前消息" "保留当前指令，读取必要会话上下文" 42 162 165 $Color.Teal
Add-Step $slide "2" "确定性路由" "优先识别预约、RAG、实时搜索关键词" 220 162 165 $Color.Blue
Add-Step $slide "3" "Leader 兜底" "仅处理规则无法判断的复杂意图" 398 162 165 $Color.Amber
Add-Step $slide "4" "Worker 执行" "调用受限工具并返回结构化状态" 576 162 165 $Color.Green
Add-Step $slide "5" "统一答复" "Leader 汇总结果，只输出一次回复" 754 162 164 $Color.Red
for ($x = 207; $x -lt 754; $x += 178) { $null = Add-Arrow $slide $x 210 ($x + 13) 210 $Color.Muted }
$null = Add-Box $slide 42 306 876 135 $Color.Dark $Color.Dark
$null = Add-Text $slide "示例：查神经内科近期排班，再规划从北京大学到医院的路线" 66 329 828 25 16 $Color.White $true
$null = Add-Text $slide "Hospital 查询排班  +  General 查询路线  ->  Leader 合并为一条连贯答复" 66 374 828 27 16 (Get-Rgb 191 227 222) $false
Add-Footer $slide

# Slide 7
$slide = Add-Slide
Add-Header $slide "06" "医院业务：把自然语言变成可验证工具链"
$steps = @(
    @{T="识别意图"; B="科室 / 医生"}, @{T="解析日期"; B="明天 / 未来三天"},
    @{T="查询号源"; B="排班 / 剩余数量"}, @{T="收集身份"; B="姓名 / 身份证 / 手机"},
    @{T="业务校验"; B="归属 / 冲突 / 状态"}, @{T="持久化"; B="MySQL 写入结果"}
)
for ($i = 0; $i -lt $steps.Count; $i++) {
    $x = 42 + $i * 147
    $null = Add-Box $slide $x 165 126 89 $Color.White $(if ($i % 2 -eq 0) {$Color.Teal} else {$Color.Blue})
    $null = Add-Text $slide ([string]($i + 1)) ($x + 10) 176 22 20 12 $(if ($i % 2 -eq 0) {$Color.Teal} else {$Color.Blue}) $true 2
    $null = Add-Text $slide $steps[$i].T ($x + 12) 203 102 22 14 $Color.Ink $true 2
    $null = Add-Text $slide $steps[$i].B ($x + 8) 230 110 17 9 $Color.Muted $false 2
    if ($i -lt $steps.Count - 1) { $null = Add-Arrow $slide ($x + 126) 210 ($x + 145) 210 $Color.Muted }
}
Add-Card $slide "关键约束" "未收集完整个人信息不能预约；相对日期必须通过 DateTools 计算；重复预约和号源扣减由数据库状态决定；失败时明确返回错误，不允许模型伪造成功结果。" 42 302 876 131 $Color.Red $Color.RedSoft
Add-Footer $slide

# Slide 8
$slide = Add-Slide
Add-Header $slide "07" "医学 RAG 与实时搜索：按数据时效拆分"
$null = Add-Box $slide 42 153 408 278 $Color.White $Color.Green
$null = Add-Text $slide "医学 RAG" 66 174 200 28 21 $Color.Green $true
$null = Add-Text $slide "院内审核资料" 66 220 160 20 13 $Color.Ink $true
$null = Add-Text $slide "解析与切分  ->  向量化  ->  Elasticsearch" 66 254 335 22 12 $Color.Muted $false
$null = Add-Text $slide "关键词 / 向量 / 混合检索" 66 304 335 22 16 $Color.Ink $true
$null = Add-Text $slide "答案携带引用来源，降低无依据回答风险" 66 350 335 46 13 $Color.Muted $false
$null = Add-Box $slide 510 153 408 278 $Color.White $Color.Amber
$null = Add-Text $slide "General Worker" 534 174 250 28 21 $Color.Amber $true
$null = Add-Text $slide "互联网实时信息" 534 220 180 20 13 $Color.Ink $true
$null = Add-Text $slide "路线 / 天气 / 新闻  ->  Tavily Search" 534 254 335 22 12 $Color.Muted $false
$null = Add-Text $slide "超时、空结果和异常降级" 534 304 335 22 16 $Color.Ink $true
$null = Add-Text $slide "避免使用模型静态知识回答正在变化的信息" 534 350 335 46 13 $Color.Muted $false
$null = Add-Text $slide "院内事实走 RAG，外部实时事实走搜索；两条链路互不混用。" 42 458 876 24 15 $Color.TealDark $true 2
Add-Footer $slide

# Slide 9
$slide = Add-Slide
Add-Header $slide "08" "医学报告多模态处理" "支持 TXT、含文本层 PDF、PNG、JPG/JPEG"
$pipeline = @("上传", "校验", "文本提取 / OCR", "结构化", "指标解释")
for ($i = 0; $i -lt $pipeline.Count; $i++) {
    $x = 52 + $i * 177
    $fill = $(if ($i -eq 2) {$Color.BlueSoft} elseif ($i -eq 4) {$Color.TealSoft} else {$Color.White})
    $border = $(if ($i -eq 2) {$Color.Blue} elseif ($i -eq 4) {$Color.Teal} else {$Color.Line})
    $null = Add-Box $slide $x 177 145 72 $fill $border
    $null = Add-Text $slide $pipeline[$i] ($x + 8) 201 129 24 14 $Color.Ink $true 2
    if ($i -lt $pipeline.Count - 1) { $null = Add-Arrow $slide ($x + 145) 213 ($x + 173) 213 $Color.Muted }
}
Add-Card $slide "本地优先" "TXT 和含文本层 PDF 使用本地提取，减少外部依赖与成本。" 42 308 270 113 $Color.Green
Add-Card $slide "可配置 OCR" "扫描 PDF 和图片可使用 PaddleOCR 或 DashScope OCR。" 345 308 270 113 $Color.Blue
Add-Card $slide "安全边界" "私有目录存储、类型/大小校验；识别失败时不伪造报告内容。" 648 308 270 113 $Color.Red
Add-Footer $slide

# Slide 10
$slide = Add-Slide
Add-Header $slide "09" "账户、会话与记忆隔离"
$null = Add-Box $slide 52 160 200 246 $Color.Dark $Color.Dark
$null = Add-Text $slide "用户账户" 72 184 160 28 20 $Color.White $true 2
$null = Add-Text $slide "注册 / 登录 / 退出 / 注销" 72 231 160 38 12 (Get-Rgb 196 211 219) $false 2
$null = Add-Text $slide "一个用户可创建多个独立会话" 72 292 160 54 15 $Color.White $true 2
$null = Add-Text $slide "BCrypt 密码哈希" 72 365 160 20 11 (Get-Rgb 196 211 219) $false 2
$items = @(
    @{Y=160; T="MySQL"; B="账户、医生、排班、预约；通过 user_id 绑定归属"; C=$Color.Blue},
    @{Y=247; T="MongoDB 主会话"; B="只保存用户与助手最终可见内容"; C=$Color.Teal},
    @{Y=334; T="MongoDB 私有记忆"; B="三个 Worker 使用独立 memory ID 保存内部轨迹"; C=$Color.Amber}
)
foreach ($item in $items) {
    $null = Add-Box $slide 318 $item.Y 600 70 $Color.White $item.C
    $null = Add-Text $slide $item.T 338 ($item.Y + 13) 185 22 15 $item.C $true
    $null = Add-Text $slide $item.B 525 ($item.Y + 13) 370 42 12 $Color.Muted $false
    $null = Add-Arrow $slide 252 ($item.Y + 35) 316 ($item.Y + 35) $item.C
}
$null = Add-Text $slide "目标：A 用户不能看到 B 用户的会话、预约和文件；Worker 之间不能互读私有轨迹。" 52 451 866 24 14 $Color.TealDark $true 2
Add-Footer $slide

# Slide 11
$slide = Add-Slide
Add-Header $slide "10" "项目难点与解决方案"
Add-Card $slide "01  路由准确性" "确定性规则覆盖高置信场景；Leader 只兜底；提示词写明 Worker 工具清单与负向边界。" 42 150 421 126 $Color.Teal
Add-Card $slide "02  上下文与轨迹隔离" "主会话、交接信息和 Worker 私有记忆分层，防止内部分析或工具轨迹泄露到前端。" 497 150 421 126 $Color.Blue
Add-Card $slide "03  业务一致性" "日期、号源、身份和冲突由代码校验；数据库结果是唯一事实源，模型不得宣称未发生的操作。" 42 305 421 126 $Color.Red
Add-Card $slide "04  多命令请求" "Worker 不直接结束请求；所有结果回到 Leader，按完成、缺信息、失败状态继续调度并统一回复。" 497 305 421 126 $Color.Amber
Add-Footer $slide

# Slide 12
$slide = Add-Slide
Add-Header $slide "11" "工程化与质量保障"
$metrics = @(
    @{N="200"; T="路由测试用例"; C=$Color.Teal},
    @{N="3"; T="职责隔离 Worker"; C=$Color.Blue},
    @{N="4"; T="核心数据与存储边界"; C=$Color.Green},
    @{N="SSE"; T="流式用户交互"; C=$Color.Amber}
)
for ($i=0; $i -lt $metrics.Count; $i++) {
    $x=42+$i*220
    $null=Add-Box $slide $x 151 202 105 $Color.White $metrics[$i].C
    $null=Add-Text $slide $metrics[$i].N ($x+15) 168 172 38 28 $metrics[$i].C $true 2
    $null=Add-Text $slide $metrics[$i].T ($x+15) 218 172 18 11 $Color.Muted $false 2
}
$null = Add-Box $slide 42 292 876 143 $Color.Dark $Color.Dark
$null = Add-Text $slide "工程措施" 65 313 145 24 16 $Color.White $true
$null = Add-Text $slide "• REST API + SSE 流式响应      • readiness 配置检查`n• 文件类型、大小与私有路径控制    • 日志脱敏与用户数据隔离`n• 外部模型、OCR、搜索和检索服务可配置及失败降级" 65 353 806 66 14 (Get-Rgb 220 232 238) $false
Add-Footer $slide

# Slide 13
$slide = Add-Slide
Add-Header $slide "12" "面试现场演示建议" "用三段请求覆盖核心架构，不演示无关页面"
Add-Card $slide "场景 A｜医院业务" "“查未来三天神经内科号源，并预约明天上午刘文华医生。”`n展示日期工具、号源查询、身份收集和数据库写入。" 42 150 876 88 $Color.Teal
Add-Card $slide "场景 B｜医学 RAG" "“介绍一下你们医院的神经内科。”`n展示知识检索、来源引用，以及 RAG 与预约工具的职责边界。" 42 258 876 88 $Color.Green
Add-Card $slide "场景 C｜混合任务" "“查神经内科近期排班，再告诉我从北京大学怎么去医院。”`n展示任务拆分、Hospital/General 串联、Leader 交接和统一回复。" 42 366 876 88 $Color.Amber
Add-Footer $slide

# Slide 14
$slide = Add-Slide $Color.Dark
$null = Add-Text $slide "项目价值" 54 54 200 28 15 $Color.Teal $true
$null = Add-Text $slide "不只是调用大模型 API" 54 103 670 54 34 $Color.White $true
$null = Add-Text $slide "而是把不确定的模型能力，放进可验证、可隔离、可降级的业务系统。" 54 177 770 52 20 (Get-Rgb 196 221 224) $false
$keywords = @("Agent Orchestration", "Tool Calling", "RAG", "Memory", "Multimodal", "Security", "Evaluation")
$x = 54; $y = 279
foreach ($keyword in $keywords) {
    $w = 64 + $keyword.Length * 6
    if ($x + $w -gt 900) { $x = 54; $y += 54 }
    $null = Add-Box $slide $x $y $w 34 $Color.TealDark $Color.Teal
    $null = Add-Text $slide $keyword ($x + 7) ($y + 8) ($w - 14) 18 10 $Color.White $true 2
    $x += $w + 12
}
$null = Add-Text $slide "Q & A" 54 436 220 42 27 $Color.White $true
$null = Add-Text $slide "医疗小智 · Multi-Agent Medical Assistant" 555 458 345 18 11 (Get-Rgb 166 184 195) $false 3

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
$outputDir = Split-Path -Parent $resolvedOutput
if (-not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

try {
    $script:Deck.SaveAs($resolvedOutput, 24)
    Write-Output "Created: $resolvedOutput"
    Write-Output "Slides: $($script:Deck.Slides.Count)"
}
finally {
    $script:Deck.Close()
    $script:Ppt.Quit()
    [Runtime.InteropServices.Marshal]::ReleaseComObject($script:Deck) | Out-Null
    [Runtime.InteropServices.Marshal]::ReleaseComObject($script:Ppt) | Out-Null
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
