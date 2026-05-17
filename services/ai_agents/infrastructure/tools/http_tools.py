"""HTTP API call tools."""

from typing import List, Optional, Dict, Any
import json
import urllib.request
import urllib.error
from pydantic import BaseModel, Field
from langchain_core.tools import BaseTool, tool


class HttpRequestInput(BaseModel):
    url: str = Field(description="The URL to request")
    method: str = Field(default="GET", description="HTTP method (GET, POST, PUT, DELETE)")
    headers: Optional[Dict[str, str]] = Field(default=None, description="HTTP headers as JSON string")
    body: Optional[str] = Field(default=None, description="Request body as JSON string")


@tool("http_request", args_schema=HttpRequestInput)
def http_request(
    url: str,
    method: str = "GET",
    headers: Optional[Dict[str, str]] = None,
    body: Optional[str] = None
) -> str:
    """Make an HTTP request to an API endpoint.
    
    Use this tool to call REST APIs, webhooks, or any HTTP endpoints.
    
    Examples:
    - GET request: url="https://api.example.com/data"
    - POST with body: url="https://api.example.com/create", method="POST", body='{"name": "test"}'
    - With headers: headers='{"Authorization": "Bearer token"}'
    """
    try:
        req_headers = headers or {}
        req_headers.setdefault("Content-Type", "application/json")
        req_headers.setdefault("Accept", "application/json")
        
        req = urllib.request.Request(url, method=method.upper())
        for key, value in req_headers.items():
            req.add_header(key, value)
        
        if body and method.upper() in ["POST", "PUT", "PATCH"]:
            if isinstance(body, str):
                req.data = body.encode("utf-8")
            else:
                req.data = json.dumps(body).encode("utf-8")
        
        with urllib.request.urlopen(req, timeout=30) as response:
            response_body = response.read().decode("utf-8")
            try:
                # Try to parse as JSON for better formatting
                parsed = json.loads(response_body)
                return json.dumps(parsed, indent=2)
            except:
                return response_body or "OK"
                
    except urllib.error.HTTPError as e:
        error_body = e.read().decode("utf-8") if e.fp else str(e)
        try:
            parsed = json.loads(error_body)
            return f"HTTP {e.code}: {json.dumps(parsed, indent=2)}"
        except:
            return f"HTTP {e.code}: {error_body}"
    except urllib.error.URLError as e:
        return f"Connection error: {e.reason}"
    except Exception as e:
        return f"Error: {str(e)}"


def get_http_tools() -> List[BaseTool]:
    """Get all HTTP API tools."""
    return [http_request]
