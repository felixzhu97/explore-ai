"""System command execution tools with security hardening."""

from typing import List, Set, Optional
import subprocess
import shlex
from pydantic import BaseModel
from langchain_core.tools import BaseTool, tool


class CommandInput(BaseModel):
    command: str
    description: str = "Shell command to execute"


# Security: Whitelist of allowed commands
ALLOWED_COMMANDS: Set[str] = {
    "kubectl", "docker", "git", "ls", "ps", "grep", "cat", "echo",
    "pwd", "whoami", "df", "du", "top", "htop", "netstat", "curl", "wget",
    "npm", "node", "python", "python3", "pip", "pip3", "uv",
}

# Security: Dangerous patterns that should never be allowed
DANGEROUS_PATTERNS: List[str] = [
    "&&", "||", ";", "|", ">", ">>", "<", "2>", "&>", "&&", 
    "$(", "`", "${", "\\n", "\\r", "\n", "\r",
    "../", ".../", "~/", "/etc/", "/root/", "/var/log/",
    "ssh", "scp", "sftp", "ftp", "telnet", "nc", "netcat",
    "rm -rf", "dd", "mkfs", "fdisk", "umount",
    "chmod", "chown", "chgrp", "useradd", "userdel", "passwd",
    "curl -X", "wget -O", "--output-document", "--output",
    "base64 -d", "openssl", "eval", "exec",
]


def _validate_command(command: str) -> tuple[bool, Optional[str]]:
    """Validate command against whitelist and dangerous patterns.
    
    Returns:
        (is_valid, error_message)
    """
    if not command or not command.strip():
        return False, "Empty command not allowed"
    
    # Check for dangerous patterns first
    cmd_lower = command.lower()
    for pattern in DANGEROUS_PATTERNS:
        if pattern.lower() in cmd_lower:
            return False, f"Dangerous pattern '{pattern}' not allowed"
    
    # Parse command to get the base command
    try:
        parts = shlex.split(command)
        if not parts:
            return False, "Empty command"
        
        base_cmd = parts[0]
        
        # Check if base command is in whitelist
        if base_cmd not in ALLOWED_COMMANDS:
            return False, f"Command '{base_cmd}' not in allowed list: {sorted(ALLOWED_COMMANDS)}"
        
        return True, None
        
    except ValueError as e:
        return False, f"Invalid command syntax: {e}"


@tool("execute_command", args_schema=CommandInput)
def execute_command(command: str) -> str:
    """Execute a shell command and return the output.
    
    Use this tool to run system commands like kubectl, docker, git, etc.
    SECURITY: Only whitelisted commands are allowed.
    
    Examples:
    - kubectl get pods
    - docker ps
    - git status
    - ps aux
    """
    # Validate command before execution
    is_valid, error_msg = _validate_command(command)
    if not is_valid:
        return f"Error: {error_msg}"
    
    try:
        # Use shell=False with shlex.split for safe execution
        result = subprocess.run(
            shlex.split(command),
            shell=False,
            capture_output=True,
            text=True,
            timeout=30,
        )
        output = result.stdout if result.stdout else result.stderr
        if result.returncode != 0 and not output:
            output = f"Command failed with return code {result.returncode}"
        return output.strip()
    except subprocess.TimeoutExpired:
        return "Error: Command timed out after 30 seconds"
    except PermissionError:
        return "Error: Permission denied"
    except FileNotFoundError:
        return f"Error: Command not found"
    except Exception as e:
        return f"Error executing command: {str(e)}"


def get_system_tools() -> List[BaseTool]:
    """Get all system command tools."""
    return [execute_command]
