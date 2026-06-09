"""AIOps tools for AI Infrastructure Agents."""

import random
import uuid
from datetime import datetime, timedelta
from typing import Any, Dict, List, Optional

from langchain_core.tools import tool


# ============================================================================
# Mock Data Store
# ============================================================================

_mock_incidents: List[Dict[str, Any]] = [
    {
        "id": "inc_001",
        "title": "High CPU Usage on api-server",
        "severity": "warning",
        "status": "open",
        "created_at": datetime.now() - timedelta(hours=2),
        "affected_systems": ["api-server", "load-balancer"],
    },
    {
        "id": "inc_002",
        "title": "Database connection timeout",
        "severity": "critical",
        "status": "investigating",
        "created_at": datetime.now() - timedelta(hours=1),
        "affected_systems": ["postgres-primary", "api-gateway"],
    },
]

_mock_services: Dict[str, str] = {
    "api-gateway": "healthy",
    "auth-service": "healthy",
    "user-service": "healthy",
    "order-service": "degraded",
    "payment-service": "healthy",
    "notification-service": "healthy",
}


# ============================================================================
# AIOps Tools
# ============================================================================


@tool("aiops_detect_anomaly")
def aiops_detect_anomaly(metric: str, time_range: str = "1h", sensitivity: float = 0.5) -> str:
    """Detect anomalies in a metric.
    
    Args:
        metric: Metric name
        time_range: Time range for analysis (5m, 15m, 1h, 6h, 24h)
        sensitivity: Detection sensitivity (0-1)
    """
    score = round(0.3 + (hash(metric) % 50) / 100, 4)
    threshold = round(0.7 - (sensitivity * 0.4), 4)
    is_anomaly = score > threshold
    
    explanations = [
        "Normal operation - no anomalies detected",
        "Slight deviation detected - monitoring",
        "Moderate anomaly - investigation recommended",
        "Significant anomaly - immediate attention required",
        "Critical anomaly - immediate action needed"
    ]
    
    explanation_idx = min(int((score - threshold) * 10) + 1, 4) if is_anomaly else 0
    
    return f"""Anomaly Detection Result for '{metric}'

Status: {'🚨 ANOMALY DETECTED' if is_anomaly else '✅ NORMAL'}
Score: {score:.4f}
Threshold: {threshold:.4f}
Sensitivity: {sensitivity}
Time Range: {time_range}
Explanation: {explanations[explanation_idx]}"""


@tool("aiops_list_incidents")
def aiops_list_incidents(status: Optional[str] = None, severity: Optional[str] = None) -> str:
    """List incidents.
    
    Args:
        status: Filter by status (open, investigating, resolved)
        severity: Filter by severity (critical, warning, info)
    """
    incidents = _mock_incidents
    
    if status:
        incidents = [i for i in incidents if i["status"] == status]
    
    if severity:
        incidents = [i for i in incidents if i["severity"] == severity]
    
    if not incidents:
        return "No incidents found matching the criteria."
    
    output = f"Incidents ({len(incidents)} shown):\n\n"
    
    for inc in incidents:
        severity_icon = {"critical": "🔴", "warning": "🟡", "info": "🔵"}.get(inc["severity"], "⚪")
        status_icon = {"open": "📋", "investigating": "🔍", "resolved": "✅"}.get(inc["status"], "❓")
        
        output += f"{severity_icon} {inc['title']}\n"
        output += f"   ID: {inc['id']}\n"
        output += f"   Status: {status_icon} {inc['status']}\n"
        output += f"   Severity: {inc['severity']}\n"
        output += f"   Affected: {', '.join(inc['affected_systems'])}\n"
        output += f"   Created: {inc['created_at'].strftime('%Y-%m-%d %H:%M')}\n\n"
    
    return output


@tool("aiops_create_incident")
def aiops_create_incident(title: str, severity: str, description: str, affected_systems: List[str]) -> str:
    """Create a new incident.
    
    Args:
        title: Incident title
        severity: Severity (critical, warning, info)
        description: Incident description
        affected_systems: List of affected systems
    """
    incident_id = f"inc_{uuid.uuid4().hex[:6]}"
    
    new_incident = {
        "id": incident_id,
        "title": title,
        "severity": severity,
        "status": "open",
        "description": description,
        "affected_systems": affected_systems,
        "created_at": datetime.now(),
        "timeline": [f"Created at {datetime.now().isoformat()}"]
    }
    
    _mock_incidents.append(new_incident)
    
    severity_icon = {"critical": "🔴", "warning": "🟡", "info": "🔵"}.get(severity, "⚪")
    
    return f"""Incident Created! {severity_icon}

ID: {incident_id}
Title: {title}
Severity: {severity}
Status: open
Description: {description}
Affected Systems: {', '.join(affected_systems)}"""


@tool("aiops_get_system_health")
def aiops_get_system_health() -> str:
    """Get system health overview."""
    healthy = sum(1 for s in _mock_services.values() if s == "healthy")
    degraded = sum(1 for s in _mock_services.values() if s == "degraded")
    
    output = f"""System Health Overview

Overall Status: {'✅ Healthy' if degraded == 0 else '⚠️ Degraded'}
Services: {healthy} healthy, {degraded} degraded

Services:
"""
    for name, status in _mock_services.items():
        status_icon = "✅" if status == "healthy" else "⚠️"
        output += f"  {status_icon} {name}: {status}\n"
    
    return output


@tool("aiops_root_cause_analysis")
def aiops_root_cause_analysis(incident_id: str, affected_services: List[str] = None) -> str:
    """Perform root cause analysis for an incident.
    
    Args:
        incident_id: Incident ID
        affected_services: List of affected services
    """
    if affected_services is None:
        affected_services = ["api-gateway", "database"]
    
    root_causes = [
        "Database connection pool exhaustion",
        "Memory leak in application service",
        "Network latency spike due to infrastructure issue",
        "Configuration change caused cascade failure",
        "Increased traffic beyond capacity"
    ]
    
    recommendations = [
        "Scale up database connection pool",
        "Implement circuit breaker pattern",
        "Add horizontal pod autoscaling",
        "Review and optimize query performance",
        "Set up capacity planning alerts"
    ]
    
    root_cause = root_causes[hash(incident_id) % len(root_causes)]
    
    output = f"""Root Cause Analysis for Incident: {incident_id}

🔍 Analysis Result:

Root Cause: {root_cause}
Confidence: {round(0.75 + (hash(incident_id) % 20) / 100, 1):.0%}

Contributing Factors:
"""
    for i, factor in enumerate([
        "High memory usage detected 10 minutes before incident",
        "Connection pool at maximum capacity",
        "Multiple retries observed from upstream services",
        "Recent deployment of new configuration"
    ], 1):
        output += f"  {i}. {factor}\n"
    
    output += "\nRecommendations:\n"
    for i, rec in enumerate(recommendations[:3], 1):
        output += f"  {i}. {rec}\n"
    
    return output


@tool("aiops_search_logs")
def aiops_search_logs(query: str, time_range: str = "1h", limit: int = 20) -> str:
    """Search logs across services.
    
    Args:
        query: Search query
        time_range: Time range
        limit: Maximum results
    """
    mock_logs = [
        f"[{datetime.now() - timedelta(minutes=i)}] INFO service: Request processed successfully",
        f"[{datetime.now() - timedelta(minutes=i*2)}] WARN service: High latency detected: 250ms",
        f"[{datetime.now() - timedelta(minutes=i*3)}] ERROR service: Connection timeout to database",
    ]
    
    output = f"Log Search Results for '{query}'\n\n"
    output += f"Time Range: {time_range} | Showing: {limit} results\n\n"
    
    for i, log in enumerate(mock_logs[:limit], 1):
        level_icon = "ℹ️" if "INFO" in log else "⚠️" if "WARN" in log else "🔴"
        output += f"{i}. {level_icon} {log}\n"
    
    return output


@tool("aiops_acknowledge_alert")
def aiops_acknowledge_alert(alert_id: str, user: str) -> str:
    """Acknowledge an alert.
    
    Args:
        alert_id: Alert ID
        user: User acknowledging
    """
    return f"""Alert Acknowledged! ✅

Alert ID: {alert_id}
Acknowledged By: {user}
Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

You are now responsible for handling this alert."""


def get_all_aiops_tools():
    """Get all AIOps tools."""
    return [
        aiops_detect_anomaly,
        aiops_list_incidents,
        aiops_create_incident,
        aiops_get_system_health,
        aiops_root_cause_analysis,
        aiops_search_logs,
        aiops_acknowledge_alert,
    ]
