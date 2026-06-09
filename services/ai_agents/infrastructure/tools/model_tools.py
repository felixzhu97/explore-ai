"""Model management tools with mock MLflow/Model Registry operations."""

import random
import uuid
from datetime import datetime, timedelta
from typing import Any, Dict, List, Optional

from langchain_core.tools import tool


# ============================================================================
# Mock Data Store
# ============================================================================

_mock_models: Dict[str, List[Dict[str, Any]]] = {
    "bert-sentiment": [
        {
            "version": "v1.0.0",
            "stage": "production",
            "created_at": datetime.now() - timedelta(days=30),
            "metrics": {"accuracy": 0.92, "f1": 0.91, "latency_ms": 45},
            "framework": "pytorch",
        },
        {
            "version": "v1.1.0",
            "stage": "staging",
            "created_at": datetime.now() - timedelta(days=5),
            "metrics": {"accuracy": 0.94, "f1": 0.93, "latency_ms": 42},
            "framework": "pytorch",
        },
    ],
    "gpt-text-classifier": [
        {
            "version": "v2.0.0",
            "stage": "production",
            "created_at": datetime.now() - timedelta(days=60),
            "metrics": {"accuracy": 0.88, "f1": 0.87, "latency_ms": 120},
            "framework": "tensorflow",
        },
    ],
    "resnet-image-classifier": [
        {
            "version": "v3.0.0",
            "stage": "production",
            "created_at": datetime.now() - timedelta(days=15),
            "metrics": {"accuracy": 0.96, "f1": 0.95, "latency_ms": 85},
            "framework": "pytorch",
        },
        {
            "version": "v3.1.0",
            "stage": "archived",
            "created_at": datetime.now() - timedelta(days=45),
            "metrics": {"accuracy": 0.94, "f1": 0.93, "latency_ms": 90},
            "framework": "pytorch",
        },
    ],
    "ner-extractor": [
        {
            "version": "v1.0.0",
            "stage": "staging",
            "created_at": datetime.now() - timedelta(days=3),
            "metrics": {"accuracy": 0.89, "f1": 0.88, "latency_ms": 35},
            "framework": "spacy",
        },
    ],
}

_mock_deployments: Dict[str, Dict[str, Any]] = {
    "bert-sentiment:v1.0.0": {
        "endpoint": "https://api.example.com/v1/models/sentiment",
        "replicas": 3,
        "status": "healthy",
        "uptime": "15 days",
    },
    "gpt-text-classifier:v2.0.0": {
        "endpoint": "https://api.example.com/v1/models/classifier",
        "replicas": 2,
        "status": "healthy",
        "uptime": "30 days",
    },
    "resnet-image-classifier:v3.0.0": {
        "endpoint": "https://api.example.com/v1/models/resnet",
        "replicas": 4,
        "status": "degraded",
        "uptime": "7 days",
    },
}


# ============================================================================
# Model Management Tools
# ============================================================================


@tool("list_models")
def list_models(stage: Optional[str] = None, framework: Optional[str] = None) -> str:
    """List all registered models.
    
    Args:
        stage: Filter by stage (production, staging, archived)
        framework: Filter by framework (pytorch, tensorflow, spacy)
    """
    results = []
    
    for model_name, versions in _mock_models.items():
        for v in versions:
            if stage and v["stage"] != stage:
                continue
            if framework and v.get("framework") != framework:
                continue
            
            results.append({
                "name": model_name,
                **v
            })
    
    if not results:
        return "No models found matching the criteria."
    
    output = f"Registered Models ({len(results)} shown):\n\n"
    
    for model in results:
        stage_icon = {
            "production": "🟢",
            "staging": "🟡",
            "archived": "⚪"
        }.get(model["stage"], "❓")
        
        output += f"{stage_icon} {model['name']}:{model['version']}\n"
        output += f"   Stage: {model['stage']}\n"
        output += f"   Framework: {model.get('framework', 'unknown')}\n"
        output += f"   Created: {model['created_at'].strftime('%Y-%m-%d')}\n"
        
        if model.get("metrics"):
            m = model["metrics"]
            output += f"   Metrics: accuracy={m.get('accuracy', 'N/A'):.2%}, f1={m.get('f1', 'N/A'):.2%}, latency={m.get('latency_ms', 'N/A')}ms\n"
        
        output += "\n"
    
    return output


@tool("get_model_info")
def get_model_info(model_name: str, version: Optional[str] = None) -> str:
    """Get detailed information about a model.
    
    Args:
        model_name: Model name
        version: Specific version (optional)
    """
    if model_name not in _mock_models:
        return f"Model '{model_name}' not found. Available: {', '.join(_mock_models.keys())}"
    
    versions = _mock_models[model_name]
    
    if version:
        versions = [v for v in versions if v["version"] == version]
        if not versions:
            return f"Version '{version}' not found for model '{model_name}'"
    
    output = f"Model: {model_name}\n"
    output += f"Total Versions: {len(_mock_models[model_name])}\n\n"
    
    for v in versions:
        stage_icon = {
            "production": "🟢",
            "staging": "🟡",
            "archived": "⚪"
        }.get(v["stage"], "❓")
        
        output += f"{stage_icon} Version: {v['version']}\n"
        output += f"   Stage: {v['stage']}\n"
        output += f"   Framework: {v.get('framework', 'unknown')}\n"
        output += f"   Created: {v['created_at'].strftime('%Y-%m-%d %H:%M')}\n"
        
        if v.get("metrics"):
            output += f"   Metrics:\n"
            for metric, value in v["metrics"].items():
                if "latency" in metric:
                    output += f"     - {metric}: {value}ms\n"
                else:
                    output += f"     - {metric}: {value:.2%}\n"
        
        deploy_key = f"{model_name}:{v['version']}"
        if deploy_key in _mock_deployments:
            d = _mock_deployments[deploy_key]
            status_icon = "✅" if d["status"] == "healthy" else "⚠️"
            output += f"   Deployment: {status_icon} {d['status']} ({d['replicas']} replicas)\n"
        
        output += "\n"
    
    return output


@tool("register_model")
def register_model(name: str, version: str, framework: str = "pytorch", description: Optional[str] = None) -> str:
    """Register a new model version.
    
    Args:
        name: Model name
        version: Model version
        framework: ML framework (pytorch, tensorflow, spacy)
        description: Model description
    """
    if name not in _mock_models:
        _mock_models[name] = []
    
    if any(v["version"] == version for v in _mock_models[name]):
        return f"Version '{version}' already exists for model '{name}'"
    
    new_version = {
        "version": version,
        "stage": "staging",
        "created_at": datetime.now(),
        "metrics": {
            "accuracy": round(random.uniform(0.85, 0.95), 2),
            "f1": round(random.uniform(0.84, 0.94), 2),
            "latency_ms": random.randint(30, 150),
        },
        "framework": framework,
        "description": description,
    }
    
    _mock_models[name].append(new_version)
    
    return f"""Model Registered Successfully! 🎉

Name: {name}
Version: {version}
Framework: {framework}
Stage: staging
Metrics:
  - Accuracy: {new_version['metrics']['accuracy']:.2%}
  - F1 Score: {new_version['metrics']['f1']:.2%}
  - Latency: {new_version['metrics']['latency_ms']}ms

Use deploy_model to promote to production."""


@tool("deploy_model")
def deploy_model(model_name: str, version: str, replicas: int = 1) -> str:
    """Deploy a model to production.
    
    Args:
        model_name: Model name
        version: Model version
        replicas: Number of replicas
    """
    if model_name not in _mock_models:
        return f"Model '{model_name}' not found"
    
    version_info = None
    for v in _mock_models[model_name]:
        if v["version"] == version:
            version_info = v
            break
    
    if not version_info:
        return f"Version '{version}' not found for model '{model_name}'"
    
    version_info["stage"] = "production"
    
    deploy_key = f"{model_name}:{version}"
    _mock_deployments[deploy_key] = {
        "endpoint": f"https://api.example.com/v1/models/{model_name}",
        "replicas": replicas,
        "status": "healthy",
        "uptime": "just now",
    }
    
    return f"""Model Deployed Successfully! 🚀

Model: {model_name}:{version}
Endpoint: {_mock_deployments[deploy_key]['endpoint']}
Replicas: {replicas}
Status: healthy

The model is now ready to serve predictions."""


@tool("compare_models")
def compare_models(model_a: str, model_b: str) -> str:
    """Compare two model versions.
    
    Args:
        model_a: First model (format: name:version)
        model_b: Second model (format: name:version)
    """
    def get_model_data(model_str: str):
        parts = model_str.split(":")
        name = parts[0]
        version = parts[1] if len(parts) > 1 else None
        
        if name not in _mock_models:
            return None, f"Model '{name}' not found"
        
        versions = _mock_models[name]
        if version:
            versions = [v for v in versions if v["version"] == version]
            if not versions:
                return None, f"Version '{version}' not found"
        
        return versions[0], None
    
    model_a_data, error_a = get_model_data(model_a)
    model_b_data, error_b = get_model_data(model_b)
    
    if error_a:
        return error_a
    if error_b:
        return error_b
    
    output = f"""Model Comparison: {model_a} vs {model_b}

"""
    
    output += f"{'Metric':<15} {'Model A':<15} {'Model B':<15} {'Winner':<10}\n"
    output += "-" * 55 + "\n"
    
    metrics = ["accuracy", "f1", "latency_ms"]
    metric_labels = {"accuracy": "Accuracy", "f1": "F1 Score", "latency_ms": "Latency (ms)"}
    
    for metric in metrics:
        val_a = model_a_data.get("metrics", {}).get(metric, 0)
        val_b = model_b_data.get("metrics", {}).get(metric, 0)
        
        if metric == "latency_ms":
            winner = "A" if val_a < val_b else "B" if val_b < val_a else "Tie"
        else:
            winner = "A" if val_a > val_b else "B" if val_b > val_a else "Tie"
        
        if metric == "latency_ms":
            output += f"{metric_labels[metric]:<15} {val_a:<15.1f} {val_b:<15.1f} {winner:<10}\n"
        else:
            output += f"{metric_labels[metric]:<15} {val_a:<15.2%} {val_b:<15.2%} {winner:<10}\n"
    
    output += f"\n{'Summary':<15}\n"
    output += "-" * 20 + "\n"
    output += f"Model A ({model_a}): {model_a_data['stage']}\n"
    output += f"Model B ({model_b}): {model_b_data['stage']}\n"
    
    return output


@tool("get_deployment_status")
def get_deployment_status() -> str:
    """Get status of all model deployments."""
    if not _mock_deployments:
        return "No active deployments."
    
    output = f"Model Deployments ({len(_mock_deployments)} active):\n\n"
    
    for deploy_key, info in _mock_deployments.items():
        status_icon = "✅" if info["status"] == "healthy" else "⚠️"
        
        output += f"{status_icon} {deploy_key}\n"
        output += f"   Endpoint: {info['endpoint']}\n"
        output += f"   Replicas: {info['replicas']}\n"
        output += f"   Status: {info['status']}\n"
        output += f"   Uptime: {info['uptime']}\n\n"
    
    return output


def get_all_model_tools():
    """Get all model management tools."""
    return [
        list_models,
        get_model_info,
        register_model,
        deploy_model,
        compare_models,
        get_deployment_status,
    ]
