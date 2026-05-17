"""Kubernetes tools with secure kubectl commands.

SECURITY: All kubectl commands are validated and use safe subprocess execution.
"""

from typing import List, Optional, Set
import subprocess
import shlex
from pydantic import BaseModel
from langchain_core.tools import BaseTool, tool


class K8sCommandInput(BaseModel):
    command: str = ""
    namespace: str = "default"
    resource_type: str = "pods"
    resource_name: Optional[str] = None
    extra_args: str = ""


# Security: Whitelist of allowed kubectl subcommands
ALLOWED_K8S_SUBCOMMANDS: Set[str] = {
    "get", "describe", "logs", "top", "events", "explain",
    "api-resources", "api-versions", "cluster-info",
}

# Security: Whitelist of allowed resource types
ALLOWED_RESOURCE_TYPES: Set[str] = {
    "pods", "pod", "po",
    "services", "service", "svc",
    "deployments", "deployment", "deploy",
    "replicasets", "replicaset", "rs",
    "statefulsets", "statefulset", "sts",
    "daemonsets", "daemonset", "ds",
    "configmaps", "configmap", "cm",
    "secrets", "secret",
    "ingresses", "ingress", "ing",
    "persistentvolumeclaims", "persistentvolumeclaim", "pvc",
    "persistentvolumes", "persistentvolume", "pv",
    "namespaces", "namespace", "ns",
    "nodes", "node", "no",
    "events", "ev",
    "endpoints", "endpoint", "ep",
    "serviceaccounts", "serviceaccount", "sa",
    "roles", "role",
    "rolebindings", "rolebinding",
    "clusterroles", "clusterrole",
    "clusterrolebindings", "clusterrolebinding",
    "networkpolicies", "networkpolicy",
    "resourcequotas", "resourcequota",
    "limitranges", "limitrange",
    "hpa", "horizontalpodautoscalers",
    "cronjobs", "cronjob", "cj",
    "jobs", "job",
}

# Security: Dangerous patterns
DANGEROUS_K8S_PATTERNS: List[str] = [
    "&&", "||", ";", "|", ">", ">>", "<", "$(", "`",
    "exec", "attach", "cp", "port-forward", "proxy",
    "rollout undo", "delete", "apply -f", "replace",
]


def _validate_kubectl_args(command: str, resource_type: str = "", resource_name: str = "") -> tuple[bool, Optional[str]]:
    """Validate kubectl arguments against whitelists."""
    cmd_lower = command.lower()
    
    # Check for dangerous patterns
    for pattern in DANGEROUS_K8S_PATTERNS:
        if pattern.lower() in cmd_lower:
            return False, f"Dangerous pattern '{pattern}' not allowed"
    
    # Validate resource type if provided
    if resource_type:
        # Normalize resource type
        normalized = resource_type.lower().rstrip('s')
        if normalized not in ALLOWED_RESOURCE_TYPES and resource_type.lower() not in ALLOWED_RESOURCE_TYPES:
            return False, f"Resource type '{resource_type}' not in allowed list"
    
    return True, None


def _run_kubectl(args: List[str]) -> str:
    """Execute kubectl command with safe subprocess execution.
    
    Args:
        args: List of kubectl arguments (e.g., ["get", "pods", "-n", "default"])
    """
    if not args:
        return "Error: No kubectl arguments provided"
    
    # Validate subcommand
    subcommand = args[0] if args else ""
    if subcommand not in ALLOWED_K8S_SUBCOMMANDS:
        return f"Error: kubectl subcommand '{subcommand}' not in allowed list: {sorted(ALLOWED_K8S_SUBCOMMANDS)}"
    
    # Build command: kubectl + args
    cmd = ["kubectl"] + args
    
    try:
        # Use shell=False for safe execution
        result = subprocess.run(
            cmd,
            shell=False,
            capture_output=True,
            text=True,
            timeout=30,
        )
        
        # Combine stdout and stderr
        output = result.stdout if result.stdout else result.stderr
        if result.returncode != 0 and not output:
            output = f"Command failed with return code {result.returncode}"
        
        return output.strip()
        
    except subprocess.TimeoutExpired:
        return "Error: kubectl command timed out after 30 seconds"
    except FileNotFoundError:
        return "Error: kubectl command not found. Is kubectl installed?"
    except PermissionError:
        return "Error: Permission denied. Check RBAC permissions."
    except Exception as e:
        return f"Error: {str(e)}"


@tool("k8s_list_pods")
def k8s_list_pods(namespace: str = "default") -> str:
    """List all pods in a Kubernetes namespace.
    
    Args:
        namespace: The Kubernetes namespace (default: default)
    """
    return _run_kubectl(["get", "pods", "-n", namespace, "-o", "wide"])


@tool("k8s_get_pod")
def k8s_get_pod(pod_name: str, namespace: str = "default") -> str:
    """Get detailed information about a specific pod.
    
    Args:
        pod_name: Name of the pod
        namespace: The Kubernetes namespace
    """
    return _run_kubectl(["get", "pod", pod_name, "-n", namespace, "-o", "yaml"])


@tool("k8s_describe_pod")
def k8s_describe_pod(pod_name: str, namespace: str = "default") -> str:
    """Get detailed description of a pod including events.
    
    Args:
        pod_name: Name of the pod
        namespace: The Kubernetes namespace
    """
    return _run_kubectl(["describe", "pod", pod_name, "-n", namespace])


@tool("k8s_get_pod_logs")
def k8s_get_pod_logs(pod_name: str, namespace: str = "default", lines: int = 50) -> str:
    """Get logs from a pod.
    
    Args:
        pod_name: Name of the pod
        namespace: The Kubernetes namespace
        lines: Number of log lines to retrieve
    """
    return _run_kubectl(["logs", pod_name, "-n", namespace, f"--tail={lines}"])


@tool("k8s_list_services")
def k8s_list_services(namespace: str = "default") -> str:
    """List all services in a Kubernetes namespace."""
    return _run_kubectl(["get", "svc", "-n", namespace])


@tool("k8s_list_deployments")
def k8s_list_deployments(namespace: str = "default") -> str:
    """List all deployments in a Kubernetes namespace."""
    return _run_kubectl(["get", "deployments", "-n", namespace])


@tool("k8s_scale_deployment")
def k8s_scale_deployment(deployment_name: str, replicas: int, namespace: str = "default") -> str:
    """Scale a deployment to a specific number of replicas.
    
    Args:
        deployment_name: Name of the deployment
        replicas: Number of replicas
        namespace: The Kubernetes namespace
    """
    # Validate arguments
    is_valid, error = _validate_kubectl_args("", "deployment", deployment_name)
    if not is_valid:
        return f"Error: {error}"
    
    if replicas < 0 or replicas > 100:
        return "Error: Replicas must be between 0 and 100"
    
    return _run_kubectl(["scale", "deployment", deployment_name, "-n", namespace, f"--replicas={replicas}"])


@tool("k8s_get_node_status")
def k8s_get_node_status() -> str:
    """Get status of all nodes in the cluster."""
    return _run_kubectl(["get", "nodes", "-o", "wide"])


@tool("k8s_get_events")
def k8s_get_events(namespace: str = "default") -> str:
    """Get recent events in a namespace."""
    return _run_kubectl(["get", "events", "-n", namespace, "--sort-by=.lastTimestamp"])


def get_all_k8s_tools() -> List[BaseTool]:
    """Get all Kubernetes tools."""
    return [
        k8s_list_pods,
        k8s_get_pod,
        k8s_describe_pod,
        k8s_get_pod_logs,
        k8s_list_services,
        k8s_list_deployments,
        k8s_scale_deployment,
        k8s_get_node_status,
        k8s_get_events,
    ]
