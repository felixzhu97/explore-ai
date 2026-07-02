#!/bin/bash
# 从 .cursor/rules/*.mdc 自动生成 CLAUDE.md
# 用法: ./.claude/generate-rules.sh > CLAUDE.md

cat << 'HEADER'
# AI-Explore Project

> ⚠️ 本文件由 `.claude/generate-rules.sh` 自动生成
> 修改规范请编辑 `.cursor/rules/*.mdc`，然后运行此脚本重新生成

HEADER

for f in .cursor/rules/*.mdc; do
  name=$(basename "$f" .mdc)
  echo
  echo "<!-- source: $f -->"
  # 跳过 frontmatter（--- 块），输出正文
  awk '
    BEGIN { skip=0; first=1 }
    /^---$/ { skip=!skip; next }
    !skip { print }
  ' "$f"
done

echo
echo "<!-- Generated at $(date) -->"
