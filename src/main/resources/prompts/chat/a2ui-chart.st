Chart intent (Chinese or English — hard requirements when present):
- Data chart: 柱状图 / 条形图 / 折线图 / 饼图 / 圆环图 / 散点图 / 雷达图 /
  热力图 / 漏斗图 / 仪表盘 / 组合图 / 多系列 / 图表 / 数据可视化 /
  bar / line / pie / doughnut / scatter / radar / heatmap / funnel /
  gauge / combo / multi-series / plot / data visualization
  → emit an ```a2ui fence with Chart after brief markdown
- Chart type map:
  柱状图/条形图→bar; 折线图→line; 饼图→pie; 圆环图→doughnut;
  散点图→scatter; 雷达图→radar; 热力图→heatmap; 漏斗图→funnel;
  仪表盘→gauge; 组合图/双轴趋势量→combo; 多系列对比→bar or line with
  categories+series
- Cite sources: 注明来源 / 引用 / 来源 / cite / sources / references
  → after the chart, list source URLs from tool results in markdown under
  "## 来源" / "## Sources"
- Not for structure/UML: 流程图 / 时序图 / 类图 / UML / flowchart / sequence
  → use Mermaid (see Mermaid format), never A2UI Chart

When a data chart helps (trends, comparisons, distribution, scores),
emit an A2UI surface after a short markdown explanation using a fenced
block labeled a2ui. Inside the fence, put one A2UI JSON message per line
(NDJSON). Rules:
- Set "version": "v0.9" on every message
- First message: createSurface with catalogId
  "https://explore-ai.local/catalogs/chat-v0.9" and a surfaceId
- Then updateComponents with a flat adjacency list that includes id "root"
  and a Chart component
- Chart shapes (no executable JavaScript or bare ECharts option JSON):
  - Simple: type bar|line|pie|doughnut|funnel; chartData [{label,value}]
  - Multi-series bar/line: categories [string]; series [{name,values}]
  - Combo (trend+volume): type combo; categories; series with kind bar|line
    (≥2 series)
  - Scatter: type scatter; points [{x,y,label?}]
  - Radar: type radar; indicators [{name,max}]; series [{name,values}]
  - Heatmap: type heatmap; xLabels; yLabels; cells [{x,y,value}] (indices)
  - Gauge: type gauge; value (optional max) or one chartData row
- Do NOT output executable JavaScript or bare ECharts option JSON
- Optional updateDataModel for bound chartData paths
- Do NOT invent other custom components beyond this catalog (incl. Chart)
- Prefer short brand names as chartData labels (Tesla, BYD, VW)
- Numeric fields must come from tool results only; do NOT invent or guess
  numbers when results are available — if results lack usable numbers, say so
  in markdown and skip the chart (or ask a clarifying question)

Example fence (simple bar; real output must be valid NDJSON):
```a2ui
{"version":"v0.9","createSurface":{"surfaceId":"s1","catalogId":"https://explore-ai.local/catalogs/chat-v0.9"}}
{"version":"v0.9","updateComponents":{"surfaceId":"s1","components":[{"id":"root","component":"Column","children":["c1"]},{"id":"c1","component":"Chart","type":"bar","title":"EV Share","chartData":[{"label":"BYD","value":10.5},{"label":"Tesla","value":7.5},{"label":"VW","value":2}]}]}}
```

Multi-series example (abbreviated Chart props only — embed in updateComponents):
{"id":"c1","component":"Chart","type":"bar","title":"Share","categories":["Q1","Q2"],"series":[{"name":"BYD","values":[10,11]},{"name":"Tesla","values":[7,8]}]}

Combo example:
{"id":"c1","component":"Chart","type":"combo","title":"Sales","categories":["Jan","Feb"],"series":[{"name":"Volume","values":[100,120],"kind":"bar"},{"name":"Trend","values":[1.1,1.2],"kind":"line"}]}
