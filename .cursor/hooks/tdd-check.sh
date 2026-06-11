#!/bin/bash
# TDD Compliance Checker
# Warns if implementation files exist without corresponding tests

input=$(cat)
file_path=$(echo "$input" | jq -r '.file_path // empty')

# Only check Java/TypeScript implementation files
if [[ ! "$file_path" =~ \.(java|ts)$ ]]; then
    echo '{"context": "TDD check skipped - not a Java/TypeScript file"}'
    exit 0
fi

# Check if this is a test file (skip if so)
if [[ "$file_path" =~ (Test|Tests|test|spec)\.(java|ts)$ ]]; then
    echo '{"context": "TDD check skipped - this is a test file"}'
    exit 0
fi

# Get the directory and filename
dir=$(dirname "$file_path")
filename=$(basename "$file_path" | sed 's/\.[^.]*$//')
ext="${filename##*.}"
base="${filename%.*}"

# Common test directories
test_dirs=("test" "tests" "src/test" "src/test/java" "src/test/kotlin" "spec")

# Check for corresponding test file
found_test=""
for test_dir in "${test_dirs[@]}"; do
    # Check various test naming patterns
    patterns=(
        "${base}Test.${ext}"
        "${base}Tests.${ext}"
        "${base}.spec.${ext}"
        "${base}.test.${ext}"
        "${base}Spec.${ext}"
    )
    
    for pattern in "${patterns[@]}"; do
        candidate="${dir/src\/test\/java/test}/${test_dir}/${pattern}"
        candidate="${candidate/src\/main\/java/test}"
        if [ -f "$candidate" ]; then
            found_test="$candidate"
            break 2
        fi
    done
done

# Also check parallel test directories
if [ -z "$found_test" ]; then
    # Common parallel test locations
    if [[ "$dir" == *"src/main"* ]]; then
        test_base="${dir/src\/main/test}"
        for pattern in "${patterns[@]}"; do
            candidate="${test_base}/${base}.${ext}"
            candidate="${candidate}.test.${ext}"
            if [ -f "$candidate" ]; then
                found_test="$candidate"
                break
            fi
            # Try Test suffix
            candidate="${test_base}/${base}Test.${ext}"
            if [ -f "$candidate" ]; then
                found_test="$candidate"
                break
            fi
        done
    fi
fi

if [ -n "$found_test" ]; then
    echo "{\"context\": \"TDD check: Test file found at ${found_test}\"}"
else
    # No test found - this is a warning for new implementation
    # This is informational, not blocking
    echo "{\"context\": \"TDD warning: No corresponding test file found for ${file_path}. Consider writing tests first (TDD workflow)\"}"
fi

exit 0
