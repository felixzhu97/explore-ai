"""Monitoring tools with mock Prometheus/Metrics operations."""

import random
from datetime import datetime, timedelta
from typing import Any, Dict, List, Optional

from langchain_core.tools import tool


# ============================================================================
# Mock Data Store
# ============================================================================

_mock_metrics: Dict[str, List[Dict[str, Any]]] = {
    "cpu_usage": [],
    "memory_usage": [],
    "disk_io": [],
    "network_in": [],
    "network_out": [],
    "request_count": [],
    "error_rate": [],
}

_mock_alerts: List[Dict[str, Any]] = [
    {
        "id": "alert_001",
        "name": "High CPU Usage",
        "severity": "warning",
        "metric": "cpu_usage",
        "threshold": 80,
        "current_value": 85.2,
        "status": "firing",
        "created_at": datetime.now() - timedelta(minutes=30),
    },
    {
        "id": "alert_002",
        "name": "Memory Usage Critical",
        "severity": "critical",
        "metric": "memory_usage",
        "threshold": 90,
        "current_value": 92.5,
        "status": "firing",
        "created_at": datetime.now() - timedelta(minutes=15),
    },
    {
        "id": "alert_003",
        "name": "Error Rate Spike",
        "severity": "warning",
        "metric": "error_rate",
        "threshold": 5,
        "current_value": 3.2,
        "status": "resolved",
        "created_at": datetime.now() - timedelta(hours=2),
    },
]

_mock_services: Dict[str, Dict[str, Any]] = {
    "api-gateway": {"status": "healthy", "uptime": "99.99%"},
    "auth-service": {"status": "healthy", "uptime": "99.95%"},
    "user-service": {"status": "healthy", "uptime": "99.98%"},
    "order-service": {"status": "degraded", "uptime": "99.50%"},
    "payment-service": {"status": "healthy", "uptime": "99.99%"},
    "notification-service": {"status": "healthy", "uptime": "99.90%"},
}


def _init_mock_metrics():
    """Initialize mock metrics data."""
    now = datetime.now()
    for metric_name in _mock_metrics.keys():
        if not _mock_metrics[metric_name]:
            _mock_metrics[metric_name] = [
                {
                    "timestamp": now - timedelta(minutes=i * 5),
                    "value": random.uniform(30, 90),
                }
                for i in range(12)
            ]

_init_mock_metrics()


# ============================================================================
# Monitoring Tools
# ============================================================================


@tool("query_metric")
def query_metric(metric: str, time_range: str = "1h", aggregation: str = "avg") -> str:
    """Query system metrics with aggregation.
    
    Args:
        metric: Metric name (cpu_usage, memory_usage, disk_io, network_in, network_out, request_count, error_rate)
        time_range: Time range (5m, 15m, 1h, 6h, 24h)
        aggregation: Aggregation method (avg, max, min, sum)
    """
    if metric not in _mock_metrics:
        return f"Unknown metric: {metric}. Available metrics: {', '.join(_mock_metrics.keys())}"
    
    data = _mock_metrics[metric]
    if not data:
        return f"No data available for metric: {metric}"
    
    time_map = {"5m": 1, "15m": 3, "1h": 12, "6h": 72, "24h": 288}
    num_points = time_map.get(time_range, 12)
    data = data[-num_points:]
    
    values = [d["value"] for d in data]
    
    if aggregation == "avg":
        result = sum(values) / len(values)
    elif aggregation == "max":
        result = max(values)
    elif aggregation == "min":
        result = min(values)
    elif aggregation == "sum":
        result = sum(values)
    else:
        result = values[-1] if values else 0
    
    if len(values) >= 2:
        trend = values[-1] - values[-2]
        trend_str = "↑" if trend > 0 else "↓" if trend < 0 else "→"
    else:
        trend_str = "→"
    
    return f"""Metric: {metric.upper()}
Time Range: {time_range}
Aggregation: {aggregation.upper()}
Current Value: {values[-1]:.2f}%
{trend_str} Trend: {trend:+.2f}% from last reading

Data Points: {len(data)}
Min: {min(values):.2f}% | Max: {max(values):.2f}% | Avg: {result:.2f}%"""


@tool("list_alerts")
def list_alerts(status: Optional[str] = None, severity: Optional[str] = None) -> str:
    """List monitoring alerts.
    
    Args:
        status: Filter by status (firing, resolved, pending)
        severity: Filter by severity (critical, warning, info)
    """
    alerts = _mock_alerts
    
    if status:
        alerts = [a for a in alerts if a["status"] == status]
    
    if severity:
        alerts = [a for a in alerts if a["severity"] == severity]
    
    if not alerts:
        return "No alerts found matching the criteria."
    
    output = f"Alerts ({len(alerts)} shown):\n\n"
    
    for alert in alerts:
        severity_icon = {"critical": "🔴", "warning": "🟡", "info": "🔵"}.get(alert["severity"], "⚪")
        status_icon = {"firing": "🔥", "resolved": "✅", "pending": "⏳"}.get(alert["status"], "❓")
        
        output += f"{severity_icon} {alert['name']}\n"
        output += f"   ID: {alert['id']}\n"
        output += f"   Status: {status_icon} {alert['status']}\n"
        output += f"   Metric: {alert['metric']} = {alert['current_value']:.1f}% (threshold: {alert['threshold']})\n"
        output += f"   Created: {alert['created_at'].strftime('%Y-%m-%d %H:%M')}\n\n"
    
    return output


@tool("get_service_health")
def get_service_health(service: Optional[str] = None) -> str:
    """Get service health status.
    
    Args:
        service: Service name (optional, returns all if not specified)
    """
    if service:
        if service not in _mock_services:
            return f"Service '{service}' not found. Available: {', '.join(_mock_services.keys())}"
        
        info = _mock_services[service]
        status_icon = "✅" if info["status"] == "healthy" else "⚠️"
        return f"""Service: {service}
Status: {status_icon} {info['status']}
Uptime: {info['uptime']}"""
    
    output = f"""Service Health Overview ({len(_mock_services)} services)

"""
    healthy = sum(1 for s in _mock_services.values() if s["status"] == "healthy")
    degraded = sum(1 for s in _mock_services.values() if s["status"] == "degraded")
    
    output += f"Summary: {healthy} healthy, {degraded} degraded\n\n"
    output += "Services:\n"
    
    for name, info in _mock_services.items():
        status_icon = "✅" if info["status"] == "healthy" else "⚠️"
        output += f"  {status_icon} {name}: {info['status']} | Uptime: {info['uptime']}\n"
    
    return output


@tool("create_alert_rule")
def create_alert_rule(name: str, metric: str, threshold: float, severity: str = "warning") -> str:
    """Create a new alert rule.
    
    Args:
        name: Alert rule name
        metric: Metric to monitor
        threshold: Threshold value
        severity: Alert severity (critical, warning, info)
    """
    if metric not in _mock_metrics:
        return f"Unknown metric: {metric}. Available: {', '.join(_mock_metrics.keys())}"
    
    import uuid
    alert_id = f"alert_{uuid.uuid4().hex[:6]}"
    
    new_alert = {
        "id": alert_id,
        "name": name,
        "severity": severity,
        "metric": metric,
        "threshold": threshold,
        "current_value": random.uniform(50, 80),
        "status": "pending",
        "created_at": datetime.now(),
    }
    
    _mock_alerts.append(new_alert)
    
    severity_icon = {"critical": "🔴", "warning": "🟡", "info": "🔵"}.get(severity, "⚪")
    
    return f"""Alert Rule Created! {severity_icon}
ID: {alert_id}
Name: {name}
Metric: {metric}
Threshold: {threshold}
Severity: {severity}
Status: pending"""


@tool("get_dashboard_summary")
def get_dashboard_summary() -> str:
    """Get a summary of all monitoring data."""
    firing_alerts = sum(1 for a in _mock_alerts if a["status"] == "firing")
    critical_alerts = sum(1 for a in _mock_alerts if a["status"] == "firing" and a["severity"] == "critical")
    
    cpu = _mock_metrics["cpu_usage"][-1]["value"] if _mock_metrics["cpu_usage"] else 0
    memory = _mock_metrics["memory_usage"][-1]["value"] if _mock_metrics["memory_usage"] else 0
    error_rate = _mock_metrics["error_rate"][-1]["value"] if _mock_metrics["error_rate"] else 0
    
    healthy_services = sum(1 for s in _mock_services.values() if s["status"] == "healthy")
    total_services = len(_mock_services)
    
    return f"""📊 Monitoring Dashboard Summary

⏰ Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

🔥 Alerts:
   Total Firing: {firing_alerts}
   Critical: {critical_alerts}
   Resolved: {sum(1 for a in _mock_alerts if a['status'] == 'resolved')}

🖥️ Key Metrics:
   CPU Usage:    {cpu:.1f}%
   Memory Usage: {memory:.1f}%
   Error Rate:   {error_rate:.2f}%

🛠️ Services:
   Healthy: {healthy_services}/{total_services}

📈 Available Metrics: {', '.join(_mock_metrics.keys())}
"""


def get_all_monitoring_tools():
    """Get all monitoring tools."""
    return [
        query_metric,
        list_alerts,
        get_service_health,
        create_alert_rule,
        get_dashboard_summary,
    ]
