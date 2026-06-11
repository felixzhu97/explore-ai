#!/bin/bash
# Test Naming Convention Checker
# Ensures test files follow naming conventions

input=$(cat)
file_path=$(echo "$input" | jq -r '.file_path // empty')

# Only check test files
if [[ ! "$file_path" =~ (Test|Tests|test|spec|Tests?\\.)\\.(java|ts|py)$ ]]; then
    echo '{"context": "Test naming check skipped - not a test file"}'
    exit 0
fi

filename=$(basename "$file_path")
dir=$(dirname "$file_path")

# Determine expected naming pattern based on language and framework
case "$file_path" in
    *.java)
        # Java: should end with Test or Tests
        if [[ "$filename" =~ ^(.*)Test(s)?\\.java$ ]]; then
            expected_suffix="Test or Tests"
            valid=true
        else
            valid=false
        fi
        ;;
    *.ts)
        # TypeScript: should end with .test.ts, .spec.ts, or Test.ts
        if [[ "$filename" =~ ^(.*)(\\.test|\\.spec|Test)\\.ts$ ]]; then
            valid=true
        else
            valid=false
        fi
        ;;
    *.py)
        # Python: should start with test_ or end with _test.py
        if [[ "$filename" =~ ^(test_.*|.*_test)\\.py$ ]]; then
            valid=true
        else
            valid=false
        fi
        ;;
    *)
        valid=true
        ;;
esac

if [ "$valid" = false ]; then
    echo "{\"context\": \"Test naming convention warning: ${filename} may not follow standard naming. Java: *Test.java, TypeScript: *.test.ts or *.spec.ts, Python: test_*.py\"}"
else
    echo '{"context": "Test naming check passed"}'
fi

exit 0
