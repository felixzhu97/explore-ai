"""Pipeline Adapter - Anti-Corruption Layer for Pipeline operations.

This adapter implements the PipelineRepository interface and translates
domain concepts to external pipeline/orchestration system operations.
"""

import uuid
from datetime import datetime
from typing import Any, Dict, List, Optional

from services.ai_agents.domain.repositories.pipeline_repository import (
    PipelineInfo,
    PipelineRepository,
)
from services.ai_agents.domain.ports import PipelinePort
from services.ai_agents.domain.values import PipelineStatus


class PipelineAdapter(PipelineRepository):
    """Adapter for Pipeline operations implementing the PipelineRepository interface.

    This adapter translates domain operations to external pipeline/orchestration
    systems (e.g., Apache Airflow, Prefect, Metaflow), acting as an
    Anti-Corruption Layer (ACL) between the domain and infrastructure.
    """

    def __init__(self, pipeline_engine: Optional[PipelinePort] = None):
        """Initialize the Pipeline adapter.

        Args:
            pipeline_engine: External pipeline engine port for actual operations.
                              If None, uses in-memory storage.
        """
        self._pipeline_engine = pipeline_engine
        self._pipelines: Dict[str, Dict[str, Any]] = {}
        self._runs: Dict[str, Dict[str, Any]] = {}

    def create_pipeline(
        self,
        name: str,
        config: Dict[str, Any]
    ) -> PipelineInfo:
        """Create a new pipeline.

        Args:
            name: Pipeline name.
            config: Pipeline configuration containing steps, description, etc.

        Returns:
            PipelineInfo with pipeline metadata.

        Raises:
            ValueError: If pipeline already exists or config is invalid.
        """
        if name in self._pipelines:
            raise ValueError(f"Pipeline '{name}' already exists")

        steps = config.get("steps", [])
        if not steps:
            raise ValueError("Pipeline must have at least one step")

        pipeline_def = {
            "name": name,
            "description": config.get("description"),
            "steps": steps,
            "trigger": config.get("trigger"),
            "timeout_seconds": config.get("timeout_seconds", 3600),
            "status": "created",
            "created_at": datetime.now().isoformat()
        }

        self._pipelines[name] = pipeline_def

        if self._pipeline_engine is not None:
            self._pipeline_engine.create_pipeline(
                name=name,
                steps=steps,
                description=pipeline_def["description"],
                trigger=pipeline_def["trigger"],
                timeout_seconds=pipeline_def["timeout_seconds"]
            )

        return PipelineInfo(
            name=name,
            status="created",
            config=config
        )

    def get_pipeline(self, name: str) -> PipelineInfo:
        """Get pipeline details.

        Args:
            name: Pipeline name.

        Returns:
            PipelineInfo with pipeline metadata.

        Raises:
            ValueError: If pipeline not found.
        """
        if self._pipeline_engine is not None:
            pipeline = self._pipeline_engine.get_pipeline(name)
            if pipeline is not None:
                return PipelineInfo(
                    name=pipeline.name,
                    status="active",
                    config={
                        "description": pipeline.description,
                        "steps": len(pipeline.steps),
                        "trigger": pipeline.trigger,
                        "timeout_seconds": pipeline.timeout_seconds
                    }
                )

        pipeline = self._pipelines.get(name)
        if pipeline is None:
            raise ValueError(f"Pipeline '{name}' not found")

        return PipelineInfo(
            name=pipeline["name"],
            status=pipeline["status"],
            config={
                "description": pipeline.get("description"),
                "steps": len(pipeline["steps"]),
                "trigger": pipeline.get("trigger"),
                "timeout_seconds": pipeline.get("timeout_seconds", 3600)
            }
        )

    def list_pipelines(self) -> List[PipelineInfo]:
        """List all pipelines.

        Returns:
            List of PipelineInfo objects.
        """
        if self._pipeline_engine is not None:
            pipelines = self._pipeline_engine.list_pipelines()
            return [
                PipelineInfo(
                    name=p.name,
                    status="active",
                    config={
                        "description": p.description,
                        "steps": len(p.steps),
                        "trigger": p.trigger,
                        "timeout_seconds": p.timeout_seconds
                    }
                )
                for p in pipelines
            ]

        return [
            PipelineInfo(
                name=name,
                status=info["status"],
                config={
                    "description": info.get("description"),
                    "steps": len(info["steps"]),
                    "trigger": info.get("trigger"),
                    "timeout_seconds": info.get("timeout_seconds", 3600)
                }
            )
            for name, info in self._pipelines.items()
        ]

    def trigger_pipeline(
        self,
        name: str,
        params: Dict[str, Any]
    ) -> str:
        """Trigger a pipeline run.

        Args:
            name: Pipeline name.
            params: Run parameters.

        Returns:
            Run ID.

        Raises:
            ValueError: If pipeline not found.
        """
        if name not in self._pipelines and (self._pipeline_engine is None or self._pipeline_engine.get_pipeline(name) is None):
            raise ValueError(f"Pipeline '{name}' not found")

        run_id = f"run_{uuid.uuid4().hex[:12]}"

        run_def = {
            "run_id": run_id,
            "pipeline_name": name,
            "status": PipelineStatus.RUNNING.value,
            "started_at": datetime.now().isoformat(),
            "completed_at": None,
            "params": params,
            "step_results": {}
        }

        self._runs[run_id] = run_def

        if self._pipeline_engine is not None:
            result = self._pipeline_engine.run_pipeline(
                pipeline_name=name,
                parameters=params
            )
            if result.get("run_id"):
                run_def["run_id"] = result["run_id"]

        run_def["status"] = PipelineStatus.SUCCEEDED.value
        run_def["completed_at"] = datetime.now().isoformat()

        pipeline_steps = self._pipelines.get(name, {}).get("steps", [])
        run_def["step_results"] = {
            step["name"] if isinstance(step, dict) else step.name:
                {"status": "completed", "duration_seconds": 10}
            for step in pipeline_steps
        }

        return run_def["run_id"]

    def get_pipeline_status(self, name: str) -> str:
        """Get current pipeline status.

        Args:
            name: Pipeline name.

        Returns:
            Pipeline status string.
        """
        if name not in self._pipelines:
            return "not_found"

        runs = [r for r in self._runs.values() if r["pipeline_name"] == name]
        if not runs:
            return "idle"

        latest_run = max(runs, key=lambda r: r["started_at"])
        return latest_run["status"]

    def list_pipeline_runs(self, name: str) -> List[Dict[str, Any]]:
        """List all runs for a pipeline.

        Args:
            name: Pipeline name.

        Returns:
            List of run information dictionaries.
        """
        runs = [
            {
                "run_id": r["run_id"],
                "status": r["status"],
                "started_at": r["started_at"],
                "completed_at": r.get("completed_at"),
                "params": r.get("params", {})
            }
            for r in self._runs.values()
            if r["pipeline_name"] == name
        ]

        return sorted(runs, key=lambda r: r["started_at"], reverse=True)

    def add_step(self, pipeline_name: str, step) -> Dict[str, Any]:
        """Add a step to a pipeline.

        Args:
            pipeline_name: Pipeline name.
            step: Step to add (PipelineStep object or dict).

        Returns:
            Add result.
        """
        if pipeline_name not in self._pipelines:
            return {"success": False, "error": f"Pipeline '{pipeline_name}' not found"}

        step_dict = {
            "name": step.name if hasattr(step, "name") else step.get("name"),
            "type": step.type if hasattr(step, "type") else step.get("type"),
            "config": step.config if hasattr(step, "config") else step.get("config", {}),
            "depends_on": step.depends_on if hasattr(step, "depends_on") else step.get("depends_on", []),
            "retry": step.retry if hasattr(step, "retry") else step.get("retry", 0),
            "timeout_seconds": step.timeout_seconds if hasattr(step, "timeout_seconds") else step.get("timeout_seconds", 300),
        }
        self._pipelines[pipeline_name]["steps"].append(step_dict)

        return {
            "success": True,
            "pipeline": pipeline_name,
            "step": step_dict["name"]
        }

    def run_pipeline(
        self,
        pipeline_name: str,
        parameters: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Run a pipeline.

        Args:
            pipeline_name: Pipeline name.
            parameters: Run parameters.

        Returns:
            Run result.
        """
        if pipeline_name not in self._pipelines:
            return {"success": False, "error": f"Pipeline '{pipeline_name}' not found"}

        run_id = f"run_{uuid.uuid4().hex[:12]}"

        run_def = {
            "run_id": run_id,
            "pipeline_name": pipeline_name,
            "status": PipelineStatus.RUNNING.value,
            "started_at": datetime.now().isoformat(),
            "completed_at": None,
            "params": parameters or {},
            "step_results": {}
        }

        self._runs[run_id] = run_def

        run_def["status"] = PipelineStatus.SUCCEEDED.value
        run_def["completed_at"] = datetime.now().isoformat()

        pipeline_steps = self._pipelines.get(pipeline_name, {}).get("steps", [])
        run_def["step_results"] = {
            step["name"] if isinstance(step, dict) else step.name:
                {"status": "completed", "duration_seconds": 10}
            for step in pipeline_steps
        }

        return {
            "success": True,
            "run_id": run_id,
            "status": PipelineStatus.SUCCEEDED.value,
            "pipeline": pipeline_name
        }

    def get_run(self, run_id: str) -> Optional[Dict[str, Any]]:
        """Get a pipeline run.

        Args:
            run_id: Run ID.

        Returns:
            Run or None.
        """
        return self._runs.get(run_id)

    def list_runs(
        self,
        pipeline_name: Optional[str] = None,
        status: Optional[PipelineStatus] = None
    ) -> List[Dict[str, Any]]:
        """List pipeline runs.

        Args:
            pipeline_name: Optional pipeline filter.
            status: Optional status filter.

        Returns:
            List of runs.
        """
        runs = list(self._runs.values())

        if pipeline_name:
            runs = [r for r in runs if r["pipeline_name"] == pipeline_name]

        if status:
            runs = [r for r in runs if r["status"] == status.value]

        return sorted(runs, key=lambda r: r["started_at"], reverse=True)

    def cancel_run(self, run_id: str) -> Dict[str, Any]:
        """Cancel a pipeline run.

        Args:
            run_id: Run ID.

        Returns:
            Cancel result.
        """
        if run_id not in self._runs:
            return {"success": False, "error": f"Run '{run_id}' not found"}

        run = self._runs[run_id]

        if run["status"] in (PipelineStatus.SUCCEEDED.value, PipelineStatus.FAILED.value, PipelineStatus.CANCELLED.value):
            return {"success": False, "error": f"Run '{run_id}' is already finished"}

        run["status"] = PipelineStatus.CANCELLED.value
        run["completed_at"] = datetime.now().isoformat()

        return {
            "success": True,
            "run_id": run_id,
            "status": PipelineStatus.CANCELLED.value
        }

    def delete_pipeline(self, name: str) -> Dict[str, Any]:
        """Delete a pipeline.

        Args:
            name: Pipeline name.

        Returns:
            Delete result.
        """
        if name not in self._pipelines:
            return {"success": False, "error": f"Pipeline '{name}' not found"}

        active_runs = [
            r for r in self._runs.values()
            if r["pipeline_name"] == name and r["status"] == PipelineStatus.RUNNING.value
        ]

        if active_runs:
            return {
                "success": False,
                "error": f"Pipeline '{name}' has active runs"
            }

        del self._pipelines[name]

        return {"success": True, "deleted": name}
