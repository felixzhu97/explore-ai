"""Domain services for pipeline operations.

This module contains the pure domain logic for pipeline orchestration,
separated from infrastructure concerns.
"""

from typing import Any, Dict, List, Optional, Set

from services.ai_agents.domain.values import (
    PipelineDefinition,
    PipelineStep,
)


class PipelineValidationService:
    """Domain service for validating pipeline configurations.
    
    Contains pure business logic for pipeline validation.
    """
    
    @staticmethod
    def validate_steps(steps: List[PipelineStep]) -> Dict[str, Any]:
        """Validate pipeline steps for correctness.
        
        Args:
            steps: Steps to validate.
            
        Returns:
            Validation result with any errors.
        """
        errors = []
        warnings = []
        
        if not steps:
            errors.append("Pipeline must have at least one step")
            return {"valid": False, "errors": errors, "warnings": warnings}
        
        step_names = {s.name for s in steps}
        step_names_seen: Set[str] = set()
        
        for i, step in enumerate(steps):
            if step.name in step_names_seen:
                errors.append(f"Duplicate step name: '{step.name}'")
            step_names_seen.add(step.name)
            
            for dep in step.depends_on:
                if dep not in step_names:
                    errors.append(
                        f"Step '{step.name}' depends on unknown step '{dep}'"
                    )
            
            if step.timeout_seconds and step.timeout_seconds <= 0:
                warnings.append(
                    f"Step '{step.name}' has invalid timeout, using default"
                )
            
            if step.retry_policy and step.retry_policy.get("max_retries", 0) < 0:
                warnings.append(
                    f"Step '{step.name}' has invalid retry count, ignoring"
                )
        
        has_start = any(not s.depends_on for s in steps)
        if not has_start:
            errors.append("Pipeline has no starting step (no step without dependencies)")
        
        return {
            "valid": len(errors) == 0,
            "errors": errors,
            "warnings": warnings
        }
    
    @staticmethod
    def validate_dag(steps: List[PipelineStep]) -> Dict[str, Any]:
        """Validate that steps form a valid DAG (no cycles).
        
        Args:
            steps: Steps to validate.
            
        Returns:
            Validation result with cycle info if any.
        """
        step_map = {s.name: s for s in steps}
        
        visited: Set[str] = set()
        rec_stack: Set[str] = set()
        
        def has_cycle(name: str, path: List[str]) -> Optional[List[str]]:
            visited.add(name)
            rec_stack.add(name)
            path.append(name)
            
            step = step_map.get(name)
            if step:
                for dep in step.depends_on:
                    if dep not in visited:
                        cycle = has_cycle(dep, path.copy())
                        if cycle:
                            return cycle
                    elif dep in rec_stack:
                        cycle_start = path.index(dep)
                        return path[cycle_start:] + [dep]
            
            rec_stack.remove(name)
            return None
        
        for step in steps:
            if step.name not in visited:
                cycle = has_cycle(step.name, [])
                if cycle:
                    return {
                        "valid": False,
                        "has_cycle": True,
                        "cycle": cycle,
                        "error": f"Circular dependency detected: {' -> '.join(cycle)}"
                    }
        
        return {"valid": True, "has_cycle": False}
    
    @staticmethod
    def get_execution_order(steps: List[PipelineStep]) -> List[str]:
        """Compute topological order for pipeline execution.
        
        Args:
            steps: Steps to order.
            
        Returns:
            List of step names in execution order.
        """
        step_map = {s.name: s for s in steps}
        in_degree = {s.name: len(s.depends_on) for s in steps}
        queue = [name for name, degree in in_degree.items() if degree == 0]
        result = []
        
        while queue:
            current = queue.pop(0)
            result.append(current)
            
            for step in steps:
                if current in step.depends_on:
                    in_degree[step.name] -= 1
                    if in_degree[step.name] == 0:
                        queue.append(step.name)
        
        return result


class PipelineExecutionService:
    """Domain service for pipeline execution logic.
    
    Contains pure business logic for pipeline execution.
    """
    
    @staticmethod
    def get_next_runnable_steps(
        completed: Set[str],
        pending_steps: List[PipelineStep]
    ) -> List[PipelineStep]:
        """Get steps that are ready to run.
        
        Args:
            completed: Names of completed steps.
            pending_steps: All pending steps.
            
        Returns:
            Steps that can be executed next.
        """
        runnable = []
        
        for step in pending_steps:
            if step.name in completed:
                continue
            
            deps_met = all(dep in completed for dep in step.depends_on)
            if deps_met:
                runnable.append(step)
        
        return runnable
    
    @staticmethod
    def can_retry_step(
        step: PipelineStep,
        current_attempts: int
    ) -> bool:
        """Check if a step can be retried.
        
        Args:
            step: The step to check.
            current_attempts: Current attempt count.
            
        Returns:
            True if step can be retried.
        """
        if not step.retry_policy:
            return False
        return current_attempts < step.retry_policy.max_retries
    
    @staticmethod
    def should_retry_on_error(error: Exception, retry_config: Any) -> bool:
        """Determine if an error should trigger a retry.
        
        Args:
            error: The error that occurred.
            retry_config: Retry policy configuration.
            
        Returns:
            True if should retry.
        """
        if not retry_config:
            return False
        
        retry_on = retry_config.retry_on or []
        error_type = type(error).__name__
        
        if "*" in retry_on or error_type in retry_on:
            return True
        
        return False
