"""Pipeline tools for AI Infrastructure Agents.

This module provides tools for orchestrating ML/DevOps pipelines and workflow automation.
"""

from datetime import datetime
from typing import Any, Dict, List, Optional

from langchain_core.tools import BaseTool
from pydantic import BaseModel, Field

from services.ai_agents.infrastructure.schemas import (
    PipelineDefinition,
    PipelineExecution,
    PipelineRun,
    PipelineStatus,
    PipelineStep,
)
from services.ai_agents.infrastructure.adapters.pipeline_adapter import PipelineAdapter as DomainPipelineAdapter


# ============================================================================
# Tool Input Schemas
# ============================================================================


class CreatePipelineInput(BaseModel):
    """Input schema for creating a pipeline."""
    name: str = Field(..., description="Pipeline name")
    description: Optional[str] = Field(default=None)
    steps: List[PipelineStep] = Field(..., description="Pipeline steps")
    trigger: Optional[str] = Field(default=None)
    timeout_seconds: int = Field(default=3600)


class AddPipelineStepInput(BaseModel):
    """Input schema for adding a step."""
    pipeline_name: str = Field(..., description="Pipeline name")
    step: PipelineStep = Field(..., description="Step to add")


class RunPipelineInput(BaseModel):
    """Input schema for running a pipeline."""
    pipeline_name: str = Field(..., description="Pipeline name")
    parameters: Optional[Dict[str, Any]] = Field(default=None)


class GetPipelineRunInput(BaseModel):
    """Input schema for getting a run."""
    run_id: str = Field(..., description="Run ID")


class CancelPipelineRunInput(BaseModel):
    """Input schema for canceling a run."""
    run_id: str = Field(..., description="Run ID")


# ============================================================================
# Pipeline Adapter Wrapper (translates between domain and infrastructure schemas)
# ============================================================================


class PipelineAdapter:
    """Adapter for Pipeline operations.
    
    This is a wrapper around the domain PipelineAdapter that translates between
    infrastructure schemas (PipelineDefinition, PipelineStep) and domain types.
    """
    
    def __init__(self):
        """Initialize the Pipeline adapter."""
        self._domain_adapter = DomainPipelineAdapter()
        self._pipelines: Dict[str, Dict[str, Any]] = {}
        self._runs: Dict[str, Dict[str, Any]] = {}
    
    def create_pipeline(
        self,
        name: str,
        steps: List[PipelineStep],
        description: Optional[str] = None,
        trigger: Optional[str] = None,
        timeout_seconds: int = 3600
    ) -> Dict[str, Any]:
        """Create a new pipeline."""
        try:
            steps_data = [s.model_dump() for s in steps]
            config = {
                "steps": steps_data,
                "description": description,
                "trigger": trigger,
                "timeout_seconds": timeout_seconds
            }
            self._domain_adapter.create_pipeline(name, config)
            
            self._pipelines[name] = {
                "name": name,
                "description": description,
                "steps": steps,
                "trigger": trigger,
                "timeout_seconds": timeout_seconds
            }
            return {"success": True, "pipeline": name, "steps": len(steps)}
        except ValueError as e:
            return {"success": False, "error": str(e)}
    
    def _validate_steps(self, steps: List[PipelineStep]) -> None:
        """Validate pipeline steps."""
        step_names = {s.name for s in steps}
        for step in steps:
            for dep in step.depends_on:
                if dep not in step_names:
                    raise ValueError(f"Step '{step.name}' depends on unknown step '{dep}'")
    
    def get_pipeline(self, name: str) -> Optional[PipelineDefinition]:
        """Get a pipeline."""
        if name not in self._pipelines:
            return None
        data = self._pipelines[name]
        return PipelineDefinition.model_validate(data)
    
    def list_pipelines(self) -> List[PipelineDefinition]:
        """List all pipelines."""
        return [PipelineDefinition.model_validate(p) for p in self._pipelines.values()]
    
    def add_step(self, pipeline_name: str, step: PipelineStep) -> Dict[str, Any]:
        """Add a step to a pipeline."""
        if pipeline_name not in self._pipelines:
            return {"success": False, "error": f"Pipeline '{pipeline_name}' not found"}
        
        self._pipelines[pipeline_name]["steps"].append(step)
        
        step_data = step.model_dump()
        self._domain_adapter.add_step(pipeline_name, step_data)
        
        return {"success": True, "pipeline": pipeline_name, "step": step.name}
    
    def run_pipeline(
        self,
        pipeline_name: str,
        parameters: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Run a pipeline."""
        if pipeline_name not in self._pipelines:
            return {"success": False, "error": f"Pipeline '{pipeline_name}' not found"}
        
        run_result = self._domain_adapter.run_pipeline(pipeline_name, parameters)
        
        run = PipelineRun(
            run_id=run_result.get("run_id", ""),
            pipeline_name=pipeline_name,
            status=PipelineStatus.SUCCEEDED,
            started_at=datetime.now(),
            completed_at=datetime.now()
        )
        self._runs[run.run_id] = run.model_dump()
        
        return {
            "success": True,
            "run_id": run.run_id,
            "status": PipelineStatus.SUCCEEDED.value,
            "pipeline": pipeline_name
        }
    
    def get_run(self, run_id: str) -> Optional[PipelineRun]:
        """Get a pipeline run."""
        if run_id not in self._runs:
            return None
        return PipelineRun.model_validate(self._runs[run_id])
    
    def list_runs(
        self,
        pipeline_name: Optional[str] = None,
        status: Optional[PipelineStatus] = None
    ) -> List[PipelineRun]:
        """List pipeline runs."""
        runs = list(self._runs.values())
        if pipeline_name:
            runs = [r for r in runs if r.get("pipeline_name") == pipeline_name]
        if status:
            runs = [r for r in runs if r.get("status") == status.value]
        return [PipelineRun.model_validate(r) for r in sorted(runs, key=lambda r: r.get("started_at", ""), reverse=True)]
    
    def cancel_run(self, run_id: str) -> Dict[str, Any]:
        """Cancel a pipeline run."""
        if run_id not in self._runs:
            return {"success": False, "error": f"Run '{run_id}' not found"}
        
        run = self._runs[run_id]
        if run.get("status") in (PipelineStatus.SUCCEEDED.value, PipelineStatus.FAILED.value, PipelineStatus.CANCELLED.value):
            return {"success": False, "error": f"Run '{run_id}' is already finished"}
        
        run["status"] = PipelineStatus.CANCELLED.value
        
        return {"success": True, "run_id": run_id, "status": PipelineStatus.CANCELLED.value}
    
    def delete_pipeline(self, name: str) -> Dict[str, Any]:
        """Delete a pipeline."""
        if name not in self._pipelines:
            return {"success": False, "error": f"Pipeline '{name}' not found"}
        
        active_runs = [r for r in self._runs.values() if r.get("pipeline_name") == name and r.get("status") == PipelineStatus.RUNNING.value]
        if active_runs:
            return {"success": False, "error": f"Pipeline '{name}' has active runs"}
        
        del self._pipelines[name]
        return {"success": True, "deleted": name}
    
    def _validate_steps(self, steps: List[PipelineStep]) -> None:
        """Validate pipeline steps."""
        step_names = {s.name for s in steps}
        for step in steps:
            for dep in step.depends_on:
                if dep not in step_names:
                    raise ValueError(f"Step '{step.name}' depends on unknown step '{dep}'")
    
    def get_pipeline(self, name: str) -> Optional[PipelineDefinition]:
        """Get a pipeline."""
        return self._pipelines.get(name)
    
    def list_pipelines(self) -> List[PipelineDefinition]:
        """List all pipelines."""
        return list(self._pipelines.values())
    
    def add_step(self, pipeline_name: str, step: PipelineStep) -> Dict[str, Any]:
        """Add a step to a pipeline."""
        if pipeline_name not in self._pipelines:
            return {"success": False, "error": f"Pipeline '{pipeline_name}' not found"}
        
        self._pipelines[pipeline_name].steps.append(step)
        self._domain_adapter.add_step(pipeline_name, step)
        
        return {"success": True, "pipeline": pipeline_name, "step": step.name}
    
    def run_pipeline(
        self,
        pipeline_name: str,
        parameters: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Run a pipeline."""
        if pipeline_name not in self._pipelines:
            return {"success": False, "error": f"Pipeline '{pipeline_name}' not found"}
        
        run_id = self._domain_adapter.run_pipeline(pipeline_name, parameters)
        
        run = PipelineRun(
            run_id=run_id.get("run_id", ""),
            pipeline_name=pipeline_name,
            status=PipelineStatus.SUCCEEDED,
            started_at=datetime.now(),
            completed_at=datetime.now()
        )
        self._runs[run.run_id] = run
        
        return {
            "success": True,
            "run_id": run.run_id,
            "status": PipelineStatus.SUCCEEDED.value,
            "pipeline": pipeline_name
        }
    
    def get_run(self, run_id: str) -> Optional[PipelineRun]:
        """Get a pipeline run."""
        return self._runs.get(run_id)
    
    def list_runs(
        self,
        pipeline_name: Optional[str] = None,
        status: Optional[PipelineStatus] = None
    ) -> List[PipelineRun]:
        """List pipeline runs."""
        runs = list(self._runs.values())
        if pipeline_name:
            runs = [r for r in runs if r.pipeline_name == pipeline_name]
        if status:
            runs = [r for r in runs if r.status == status]
        return sorted(runs, key=lambda r: r.started_at or datetime.min, reverse=True)
    
    def cancel_run(self, run_id: str) -> Dict[str, Any]:
        """Cancel a pipeline run."""
        if run_id not in self._runs:
            return {"success": False, "error": f"Run '{run_id}' not found"}
        
        run = self._runs[run_id]
        if run.status in (PipelineStatus.SUCCEEDED, PipelineStatus.FAILED, PipelineStatus.CANCELLED):
            return {"success": False, "error": f"Run '{run_id}' is already finished"}
        
        run.status = PipelineStatus.CANCELLED
        run.completed_at = datetime.now()
        
        return {"success": True, "run_id": run_id, "status": PipelineStatus.CANCELLED.value}
    
    def delete_pipeline(self, name: str) -> Dict[str, Any]:
        """Delete a pipeline."""
        if name not in self._pipelines:
            return {"success": False, "error": f"Pipeline '{name}' not found"}
        
        active_runs = [r for r in self._runs.values() if r.pipeline_name == name and r.status == PipelineStatus.RUNNING]
        if active_runs:
            return {"success": False, "error": f"Pipeline '{name}' has active runs"}
        
        del self._pipelines[name]
        return {"success": True, "deleted": name}


# Global adapter instance
_pipeline_adapter: Optional[PipelineAdapter] = None


def get_pipeline_adapter() -> PipelineAdapter:
    """Get the global Pipeline adapter instance."""
    global _pipeline_adapter
    if _pipeline_adapter is None:
        _pipeline_adapter = PipelineAdapter()
    return _pipeline_adapter


# ============================================================================
# Pipeline Tools
# ============================================================================


def _create_pipeline_tool() -> BaseTool:
    """Create the create pipeline tool."""
    def create_pipeline(
        name: str,
        steps: List[Dict[str, Any]],
        description: Optional[str] = None,
        trigger: Optional[str] = None,
        timeout_seconds: int = 3600
    ) -> str:
        """Create a new pipeline.
        
        Args:
            name: Pipeline name.
            steps: Pipeline steps.
            description: Optional description.
            trigger: Trigger configuration.
            timeout_seconds: Pipeline timeout.
            
        Returns:
            Creation result.
        """
        adapter = get_pipeline_adapter()
        
        pipeline_steps = [
            PipelineStep(
                name=s["name"],
                type=s["type"],
                config=s.get("config", {}),
                depends_on=s.get("depends_on", []),
                retry=s.get("retry", 0),
                timeout_seconds=s.get("timeout_seconds", 300)
            )
            for s in steps
        ]
        
        try:
            result = adapter.create_pipeline(
                name=name,
                steps=pipeline_steps,
                description=description,
                trigger=trigger,
                timeout_seconds=timeout_seconds
            )
        except ValueError as e:
            return f"Validation error: {str(e)}"
        
        if result["success"]:
            return f"""Successfully created pipeline '{name}'
Steps: {result['steps']}
Timeout: {timeout_seconds}s
Trigger: {trigger or 'manual'}"""
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="create_pipeline",
        description="Create a new pipeline. Use this to define multi-step workflows for data processing or ML operations.",
        args_schema=CreatePipelineInput,
        func=create_pipeline
    )


def _create_run_pipeline_tool() -> BaseTool:
    """Create the run pipeline tool."""
    def run_pipeline(
        pipeline_name: str,
        parameters: Optional[Dict[str, Any]] = None
    ) -> str:
        """Run a pipeline.
        
        Args:
            pipeline_name: Pipeline name.
            parameters: Run parameters.
            
        Returns:
            Run result.
        """
        adapter = get_pipeline_adapter()
        result = adapter.run_pipeline(pipeline_name, parameters)
        
        if result["success"]:
            return f"""Pipeline run started!
Run ID: {result['run_id']}
Pipeline: {result['pipeline']}
Status: {result['status']}"""
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="run_pipeline",
        description="Run a pipeline. Use this to execute a pipeline and get the results.",
        args_schema=RunPipelineInput,
        func=run_pipeline
    )


def _create_list_pipelines_tool() -> BaseTool:
    """Create the list pipelines tool."""
    def list_pipelines() -> str:
        """List all pipelines.
        
        Returns:
            List of pipelines.
        """
        adapter = get_pipeline_adapter()
        pipelines = adapter.list_pipelines()
        
        if not pipelines:
            return "No pipelines found"
        
        output = f"Available Pipelines ({len(pipelines)}):\n\n"
        for p in pipelines:
            output += f"- {p.name}\n"
            if p.description:
                output += f"  {p.description}\n"
            output += f"  Steps: {len(p.steps)}\n"
            output += f"  Trigger: {p.trigger or 'manual'}\n"
        
        return output
    
    return BaseTool(
        name="list_pipelines",
        description="List all available pipelines. Use this to see what workflows are available.",
        args_schema={},
        func=list_pipelines
    )


def _create_get_pipeline_tool() -> BaseTool:
    """Create the get pipeline tool."""
    def get_pipeline(pipeline_name: str) -> str:
        """Get pipeline details.
        
        Args:
            pipeline_name: Pipeline name.
            
        Returns:
            Pipeline details.
        """
        adapter = get_pipeline_adapter()
        pipeline = adapter.get_pipeline(pipeline_name)
        
        if pipeline is None:
            return f"Pipeline '{pipeline_name}' not found"
        
        output = f"Pipeline: {pipeline.name}\n"
        if pipeline.description:
            output += f"Description: {pipeline.description}\n"
        output += f"Trigger: {pipeline.trigger or 'manual'}\n"
        output += f"Timeout: {pipeline.timeout_seconds}s\n\n"
        output += "Steps:\n"
        
        for i, step in enumerate(pipeline.steps, 1):
            output += f"  {i}. {step.name} [{step.type}]\n"
            if step.depends_on:
                output += f"     Depends on: {', '.join(step.depends_on)}\n"
            output += f"     Retry: {step.retry}, Timeout: {step.timeout_seconds}s\n"
        
        return output
    
    return BaseTool(
        name="get_pipeline",
        description="Get detailed information about a pipeline. Use this to see the pipeline structure and steps.",
        args_schema={
            "pipeline_name": str
        },
        func=get_pipeline
    )


def _create_get_run_status_tool() -> BaseTool:
    """Create the get run status tool."""
    def get_run_status(run_id: str) -> str:
        """Get pipeline run status.
        
        Args:
            run_id: Run ID.
            
        Returns:
            Run status.
        """
        adapter = get_pipeline_adapter()
        run = adapter.get_run(run_id)
        
        if run is None:
            return f"Run '{run_id}' not found"
        
        output = f"Run: {run.run_id}\n"
        output += f"Pipeline: {run.pipeline_name}\n"
        output += f"Status: {run.status.value}\n"
        
        if run.started_at:
            output += f"Started: {run.started_at.isoformat()}\n"
        if run.completed_at:
            output += f"Completed: {run.completed_at.isoformat()}\n"
        
        if run.step_results:
            output += "\nStep Results:\n"
            for step_name, result in run.step_results.items():
                output += f"  - {step_name}: {result.get('status', 'unknown')}\n"
        
        if run.error:
            output += f"\nError: {run.error}\n"
        
        return output
    
    return BaseTool(
        name="get_run_status",
        description="Get the status of a pipeline run. Use this to check progress and results of a running or completed pipeline.",
        args_schema=GetPipelineRunInput,
        func=get_run_status
    )


def _create_list_runs_tool() -> BaseTool:
    """Create the list runs tool."""
    def list_runs(
        pipeline_name: Optional[str] = None,
        status: Optional[str] = None
    ) -> str:
        """List pipeline runs.
        
        Args:
            pipeline_name: Optional pipeline filter.
            status: Optional status filter.
            
        Returns:
            List of runs.
        """
        adapter = get_pipeline_adapter()
        
        status_enum = None
        if status:
            try:
                status_enum = PipelineStatus(status)
            except ValueError:
                return f"Invalid status: {status}"
        
        runs = adapter.list_runs(pipeline_name, status_enum)
        
        if not runs:
            return "No runs found"
        
        output = f"Pipeline Runs ({len(runs)} shown):\n\n"
        for run in runs[:20]:
            output += f"- {run.run_id}\n"
            output += f"  Pipeline: {run.pipeline_name}\n"
            output += f"  Status: {run.status.value}\n"
            if run.started_at:
                output += f"  Started: {run.started_at.strftime('%Y-%m-%d %H:%M')}\n"
        
        return output
    
    return BaseTool(
        name="list_runs",
        description="List pipeline runs. Use this to see recent runs and their status.",
        args_schema={
            "pipeline_name": Optional[str],
            "status": Optional[str]
        },
        func=list_runs
    )


def _create_cancel_run_tool() -> BaseTool:
    """Create the cancel run tool."""
    def cancel_run(run_id: str) -> str:
        """Cancel a pipeline run.
        
        Args:
            run_id: Run ID.
            
        Returns:
            Cancel result.
        """
        adapter = get_pipeline_adapter()
        result = adapter.cancel_run(run_id)
        
        if result["success"]:
            return f"Successfully cancelled run '{run_id}'"
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="cancel_run",
        description="Cancel a running pipeline. Use this to stop a pipeline execution.",
        args_schema=CancelPipelineRunInput,
        func=cancel_run
    )


def _create_add_pipeline_step_tool() -> BaseTool:
    """Create the add step tool."""
    def add_pipeline_step(
        pipeline_name: str,
        step_name: str,
        step_type: str,
        config: Optional[Dict[str, Any]] = None,
        depends_on: Optional[List[str]] = None,
        retry: int = 0,
        timeout_seconds: int = 300
    ) -> str:
        """Add a step to a pipeline.
        
        Args:
            pipeline_name: Pipeline name.
            step_name: Step name.
            step_type: Step type.
            config: Step configuration.
            depends_on: Dependencies.
            retry: Retry count.
            timeout_seconds: Step timeout.
            
        Returns:
            Add result.
        """
        adapter = get_pipeline_adapter()
        
        step = PipelineStep(
            name=step_name,
            type=step_type,
            config=config or {},
            depends_on=depends_on or [],
            retry=retry,
            timeout_seconds=timeout_seconds
        )
        
        result = adapter.add_step(pipeline_name, step)
        
        if result["success"]:
            return f"Successfully added step '{step_name}' to pipeline '{pipeline_name}'"
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="add_pipeline_step",
        description="Add a step to an existing pipeline. Use this to extend pipeline functionality.",
        args_schema=AddPipelineStepInput,
        func=add_pipeline_step
    )


def _create_delete_pipeline_tool() -> BaseTool:
    """Create the delete pipeline tool."""
    def delete_pipeline(pipeline_name: str) -> str:
        """Delete a pipeline.
        
        Args:
            pipeline_name: Pipeline name.
            
        Returns:
            Delete result.
        """
        adapter = get_pipeline_adapter()
        result = adapter.delete_pipeline(pipeline_name)
        
        if result["success"]:
            return f"Successfully deleted pipeline '{pipeline_name}'"
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="delete_pipeline",
        description="Delete a pipeline. Use this to remove unused or obsolete pipelines.",
        args_schema={
            "pipeline_name": str
        },
        func=delete_pipeline
    )


def get_all_pipeline_tools() -> List[BaseTool]:
    """Get all Pipeline tools.
    
    Returns:
        List of all Pipeline tools.
    """
    return [
        _create_pipeline_tool(),
        _create_run_pipeline_tool(),
        _create_list_pipelines_tool(),
        _create_get_pipeline_tool(),
        _create_get_run_status_tool(),
        _create_list_runs_tool(),
        _create_cancel_run_tool(),
        _create_add_pipeline_step_tool(),
        _create_delete_pipeline_tool(),
    ]
