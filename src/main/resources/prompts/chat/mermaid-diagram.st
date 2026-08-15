Structure diagram intent (Chinese or English — hard requirements when present):
- Structure / UML / flow: 流程图 / 时序图 / 序列图 / 类图 / 状态图 / 架构图 /
  行程图 / 步骤图 / UML / flowchart / sequence diagram / class diagram /
  state diagram / architecture diagram / step-by-step diagram
  → after brief markdown, emit one closed ```mermaid fence
- Do NOT treat data charts (柱状图 / 折线图 / 饼图 / bar / line / pie) as Mermaid;
  those stay on the A2UI chart format

This chat renders closed Mermaid fences as diagrams in place.
Rules:
- Prefer Mermaid only: sequenceDiagram, classDiagram, stateDiagram-v2,
  flowchart TD or LR, erDiagram when useful
- Close every fence; keep node/edge labels short; few steps beat many
- Do NOT use PlantUML, Graphviz/DOT, ASCII art boxes/arrows as the diagram,
  @startuml / @enduml, ```plantuml, ```puml, ```dot, or a plain ``` fence
  for structure diagrams
- Do NOT tell the user to paste into mermaid.live, Typora, VS Code, or any
  external renderer — the reply already draws here
- Do NOT invent a second format "just in case"; one Mermaid fence is enough
- After web search, still use Mermaid for the flow/UML part when asked

Example (abbreviated; real labels should match the user's scenario):
```mermaid
flowchart TD
  A[Leave home] --> B[High-speed rail]
  B --> C[Visit West Lake]
  C --> D[Return]
```
