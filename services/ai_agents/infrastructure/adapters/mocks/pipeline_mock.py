"""Mock implementations for pipeline operations.

This module contains mock/stub implementations for testing and development.
"""

from typing import Any, Dict, List, Optional
import uuid
from datetime import datetime

from services.ai_agents.domain.repositories.pipeline_repository import (
    PipelineInfo,
    PipelineRepository,
)
from services.ai_agents.domain.values import (
    PipelineDefinition,
    PipelineRun,
    PipelineStatus,
    PipelineStep,
)
from services.ai_agents.domain.services.pipeline_service import PipelineValidationService


class PipelineMockStore:
    """Mock data store for pipeline operations."""
    
    def __init__(self):
        self._pipelines: Dict[str, PipelineDefinition] = {}
        self._runs: Dict[str, PipelineRun] = {}
        self._executions: Dict[str, List[Dict[str, Any]]] = {}


class MockPipelineAdapter(PipelineRepository):
    """Adapter for Pipeline operations (mock implementation).
    
    Uses domain services for validation logic.
    """
    
    def __init__(self):
        self._store = PipelineMockStore()
    
    def create_pipeline(
        self,
        name: str,
        config: Dict[str, Any]
    ) -> PipelineInfo:
        steps = config.get("steps", [])
        validation = PipelineValidationService.validate_steps(steps)
        if not validation["valid"]:
            raise ValueError(f"Invalid pipeline steps: {validation['errors']}")
        
        if name in self._store._pipelines:
            raise ValueError(f"Pipeline '{name}' already exists")
        
        pipeline = PipelineDefinition(
            name=name,
            description=config.get("description"),
            steps=steps,
            trigger=config.get("trigger"),
            timeout_seconds=config.get("timeout_seconds", 3600)
        )
        
        self._store._pipelines[name] = pipeline
        self._store._executions[name] = []
        
        return PipelineInfo(
            name=name,
            status="created",
            config=config
        )
    
    def get_pipeline(self, name: str) -> PipelineInfo:
        pipeline = self._store._pipelines.get(name)
        if not pipeline:
            raise KeyError(f"Pipeline '{name}' not found")
        
        return PipelineInfo(
            name=pipeline.name,
            status="active",
            config={
                "description": pipeline.description,
                "steps": pipeline.steps,
                "trigger": pipeline.trigger,
                "timeout_seconds": pipeline.timeout_seconds
            }
        )
    
    def trigger_pipeline(
        self,
        name: str,
        params: Dict[str, Any]
    ) -> str:
        pipeline = self._store._pipelines.get(name)
        if not pipeline:
            raise KeyError(f"Pipeline '{name}' not found")
        
        run_id = str(uuid.uuid4())
        
        run = PipelineRun(
            run_id=run_id,
            pipeline_name=name,
            status=PipelineStatus.PENDING,
            started_at=datetime.now(),
            completed_at=None,
            step_results={},
            error=None
        )
        
        self._store._runs[run_id] = run
        self._store._executions[name].append({"run_id": run_id, "params": params})
        
        return run_id
    
    def get_pipeline_status(self, name: str) -> str:
        pipeline = self._store._pipelines.get(name)
        if not pipeline:
            raise KeyError(f"Pipeline '{name}' not found")
        return "active"
    
    def list_pipeline_runs(self, name: str) -> List[Dict[str, Any]]:
        if name not in self._store._executions:
            return []
        
        runs = []
        for exec_data in self._store._executions[name]:
            run_id = exec_data["run_id"]
            run = self._store._runs.get(run_id)
            if run:
                runs.append({
                    "run_id": run.run_id,
                    "pipeline_name": run.pipeline_name,
                    "status": run.status.value,
                    "started_at": run.started_at.isoformat() if run.started_at else None,
                    "completed_at": run.completed_at.isoformat() if run.completed_at else None,
                    "step_results": run.step_results,
                    "error": run.error,
                    "is_terminal": run.is_terminal,
                    "duration_seconds": run.duration_seconds
                })
        return runs
