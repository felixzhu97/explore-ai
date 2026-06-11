#!/bin/bash
# Clean Architecture Compliance Checker
# Warns if domain layer has framework dependencies

input=$(cat)
file_path=$(echo "$input" | jq -r '.file_path // empty')

# Only check Java files
if [[ ! "$file_path" =~ \.(java|kt)$ ]]; then
    echo '{"context": "Clean Architecture check skipped - not a Java/Kotlin file"}'
    exit 0
fi

# Check if file is in domain layer
# Common domain paths
if [[ "$file_path" =~ (domain/Domain|domain/model|domain/entity|domain/vo|domain/valueobject|domain/service|domain/event|domain/aggregate) ]]; then
    # This is a domain layer file - check for forbidden imports
    
    # Read file content
    content=$(cat "$file_path" 2>/dev/null || echo "")
    
    violations=""
    
    # Check for Spring framework dependencies
    if echo "$content" | grep -q "import org.springframework"; then
        violations="${violations}Spring framework; "
    fi
    
    # Check for Jakarta/JEE dependencies
    if echo "$content" | grep -q "import jakarta.persistence"; then
        violations="${violations}Jakarta Persistence; "
    fi
    if echo "$content" | grep -q "import javax.persistence"; then
        violations="${violations}JPA annotations; "
    fi
    
    # Check for Hibernate specific annotations
    if echo "$content" | grep -q "@Entity\|@Table\|@Column\|@Id"; then
        violations="${violations}JPA/Hibernate annotations; "
    fi
    
    # Check for @Service, @Component, @Repository in domain
    if echo "$content" | grep -q "@Service\|@Component"; then
        violations="${violations}Spring annotations (@Service/@Component); "
    fi
    
    # Check for dependency injection in domain
    if echo "$content" | grep -q "@Autowired\|@Inject"; then
        violations="${violations}Dependency injection; "
    fi
    
    if [ -n "$violations" ]; then
        echo "{\"context\": \"Clean Architecture violation in ${file_path}: Domain layer should not depend on [${violations}] Move business logic to domain, infrastructure to infrastructure layer\"}"
        exit 0
    fi
fi

echo '{"context": "Clean Architecture check passed"}'
exit 0
