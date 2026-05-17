"""Shared authentication middleware for API Key validation.

This module provides authentication middleware and dependencies for securing API endpoints.
"""

import os
from typing import Optional, Set
from fastapi import HTTPException, Security, status
from fastapi.security import APIKeyHeader
from loguru import logger


# API Key configuration
API_KEY_NAME = os.getenv("API_KEY_HEADER_NAME", "X-API-Key")
API_KEYS: Set[str] = set()

# Load API keys from environment variable (comma-separated)
def load_api_keys() -> None:
    """Load API keys from environment variable."""
    global API_KEYS
    keys_env = os.getenv("API_KEYS", "")
    if keys_env:
        # Support comma-separated keys for multiple valid keys
        for key in keys_env.split(","):
            key = key.strip()
            if key:
                API_KEYS.add(key)
        if API_KEYS:
            logger.info(f"Loaded {len(API_KEYS)} API key(s) from environment")

# Initialize API keys on module import
load_api_keys()


class APIKeyAuth:
    """API Key authentication handler."""
    
    def __init__(self):
        self.api_key_header = APIKeyHeader(name=API_KEY_NAME, auto_error=False)
    
    async def __call__(
        self,
        api_key: Optional[str] = Security(api_key_header)
    ) -> Optional[str]:
        """Validate API key from request header.
        
        Returns the API key if valid, None if authentication is disabled.
        Raises HTTPException if authentication is required but key is invalid.
        """
        # Skip authentication if no API keys are configured (development mode)
        if not API_KEYS:
            logger.debug("API authentication disabled - no API keys configured")
            return None
        
        # Check if API key header is present
        if api_key is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={
                    "error": "missing_api_key",
                    "message": f"Missing API key. Include '{API_KEY_NAME}' header in your request.",
                }
            )
        
        # Validate API key
        if api_key not in API_KEYS:
            logger.warning(f"Invalid API key attempted: {api_key[:8]}...")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={
                    "error": "invalid_api_key",
                    "message": "Invalid API key.",
                }
            )
        
        return api_key


# Create singleton instance
api_key_auth = APIKeyAuth()


async def verify_api_key(
    api_key: Optional[str] = Security(api_key_auth)
) -> Optional[str]:
    """FastAPI dependency for API key verification.
    
    Usage:
        @app.post("/protected-endpoint")
        async def protected_endpoint(api_key: str = Depends(verify_api_key)):
            ...
    
    Returns the API key if valid, or None if authentication is disabled.
    """
    return api_key


def reload_api_keys() -> None:
    """Reload API keys from environment variable.
    
    Useful for hot-reloading configuration.
    """
    global API_KEYS
    API_KEYS.clear()
    load_api_keys()
