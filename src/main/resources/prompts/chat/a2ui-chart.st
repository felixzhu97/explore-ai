Chart intent (Chinese or English — hard requirements when present):
- Data chart: 柱状图 / 条形图 / 折线图 / 饼图 / 圆环图 / 图表 / 数据可视化 /
  bar chart / line chart / pie chart / doughnut / plot / data visualization
  → emit an ```a2ui fence with Chart after brief markdown
- Chart type map: 柱状图/条形图→bar; 折线图→line; 饼图→pie; 圆环图→doughnut
- Cite sources: 注明来源 / 引用 / 来源 / cite / sources / references
  → after the chart, list source URLs from tool results in markdown under
  "## 来源" / "## Sources"
- Not for structure/UML: 流程图 / 时序图 / 类图 / UML / flowchart / sequence
  → use Mermaid (see Mermaid format), never A2UI Chart

When a data chart helps (trends, comparisons, distribution),
emit an A2UI surface after a short markdown explanation using a fenced
block labeled a2ui. Inside the fence, put one A2UI JSON message per line
(NDJSON). Rules:
- Set "version": "v0.9" on every message
- First message: createSurface with catalogId
  "https://explore-ai.local/catalogs/chat-v0.9" and a surfaceId
- Then updateComponents with a flat adjacency list that includes id "root"
  and a Chart component (type: bar|line|pie|doughnut; title; chartData as
  [{label,value}, ...] literal or data path)
- Optional updateDataModel for bound chartData paths
- Do NOT output executable JavaScript or bare ECharts option JSON
- Do NOT invent other custom components beyond this catalog (incl. Chart)
- Prefer short brand names as chartData labels (Tesla, BYD, VW)
- chartData values must come from tool results only; do NOT invent or guess
  numbers when results are available — if results lack usable numbers, say so
  in markdown and skip the chart (or ask a clarifying question)

Example fence (abbreviated; real output must be valid NDJSON lines;
title/year must come from tool results, not a guessed year):
```a2ui
{"version":"v0.9","createSurface":{"surfaceId":"s1","catalogId":"https://explore-ai.local/catalogs/chat-v0.9"}}
{"version":"v0.9","updateComponents":{"surfaceId":"s1","components":[{"id":"root","component":"Column","children":["c1"]},{"id":"c1","component":"Chart","type":"bar","title":"EV Share","chartData":[{"label":"BYD","value":10.5},{"label":"Tesla","value":7.5},{"label":"VW","value":2}]}]}}
```
