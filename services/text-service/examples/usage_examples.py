"""Usage examples for Text-to-Text service."""

# =============================================================================
# Python Client Examples
# =============================================================================

import requests
import json

BASE_URL = "http://localhost:8006"


def example_complete():
    """Simple text completion example."""
    response = requests.post(f"{BASE_URL}/api/text/complete", json={
        "prompt": "Explain the concept of recursion in programming:",
        "temperature": 0.7,
        "max_tokens": 500,
    })
    data = response.json()
    print(f"Response: {data['text']}")
    print(f"Provider: {data['provider']}, Model: {data['model']}")


def example_chat():
    """Multi-turn chat example."""
    session_id = "my-session-123"
    
    # First message
    response = requests.post(f"{BASE_URL}/api/text/chat", json={
        "messages": [{"role": "user", "content": "What is Python?"}],
        "session_id": session_id,
        "temperature": 0.7,
    })
    print(f"Assistant: {response.json()['text']}")
    
    # Second message (same session)
    response = requests.post(f"{BASE_URL}/api/text/chat", json={
        "messages": [{"role": "user", "content": "How do I install it?"}],
        "session_id": session_id,
        "temperature": 0.7,
    })
    print(f"Assistant: {response.json()['text']}")


def example_with_system_prompt():
    """Chat with custom system prompt."""
    response = requests.post(f"{BASE_URL}/api/text/chat", json={
        "messages": [{"role": "user", "content": "Explain quantum entanglement"}],
        "system_prompt": "You are a physics professor who explains complex concepts simply. Always give examples.",
        "temperature": 0.5,
        "max_tokens": 1000,
    })
    print(f"Response: {response.json()['text']}")


def example_streaming():
    """Streaming response example."""
    response = requests.post(
        f"{BASE_URL}/api/text/complete/stream",
        json={"prompt": "Write a short story about a robot:", "temperature": 0.8},
        stream=True
    )
    
    for line in response.iter_lines():
        if line:
            data = json.loads(line.decode('utf-8').replace('data: ', ''))
            if 'token' in data:
                print(data['token'], end='', flush=True)
    print()


def example_provider_switch():
    """Switch between providers."""
    providers = ["openai", "anthropic", "ollama"]
    
    for provider in providers:
        response = requests.post(f"{BASE_URL}/api/text/complete", json={
            "prompt": "Hello, say hi briefly.",
            "provider": provider,
            "temperature": 0.7,
        })
        if response.status_code == 200:
            data = response.json()
            print(f"{provider}: {data['text'][:50]}...")
        else:
            print(f"{provider}: Not available ({response.status_code})")


# =============================================================================
# JavaScript/TypeScript Client Examples
# =============================================================================

"""
// JavaScript: Text Completion
const response = await fetch('http://localhost:8006/api/text/complete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        prompt: 'What is machine learning?',
        temperature: 0.7,
        max_tokens: 500
    })
});
const data = await response.json();
console.log(data.text);

// JavaScript: Streaming Chat
const response = await fetch('http://localhost:8006/api/text/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        messages: [{ role: 'user', content: 'Hello!' }]
    })
});

const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    const text = decoder.decode(value);
    const lines = text.split('\\n');
    
    for (const line of lines) {
        if (line.startsWith('data: ')) {
            try {
                const data = JSON.parse(line.slice(6));
                if (data.token) process.stdout.write(data.token);
                if (data.error) console.error('Error:', data.error);
            } catch (e) {}
        }
    }
}
console.log();
"""

# =============================================================================
# cURL Examples
# =============================================================================

"""
# Text completion
curl -X POST http://localhost:8006/api/text/complete \\
    -H "Content-Type: application/json" \\
    -d '{
        "prompt": "Explain neural networks:",
        "temperature": 0.7
    }'

# Streaming completion
curl -X POST http://localhost:8006/api/text/complete/stream \\
    -H "Content-Type: application/json" \\
    -d '{"prompt": "Count to 5:"}'

# Chat with Anthropic
curl -X POST http://localhost:8006/api/text/chat \\
    -H "Content-Type: application/json" \\
    -d '{
        "messages": [{"role": "user", "content": "Hello"}],
        "provider": "anthropic",
        "model": "claude-sonnet-4-20250514"
    }'

# List available providers
curl http://localhost:8006/api/text/providers

# List models for a provider
curl http://localhost:8006/api/text/models?provider=openai
"""


if __name__ == "__main__":
    print("Text-to-Text Service Usage Examples")
    print("=" * 50)
    print("\nPython Examples:")
    print("- example_complete()")
    print("- example_chat()")
    print("- example_with_system_prompt()")
    print("- example_streaming()")
    print("- example_provider_switch()")
    print("\nRun individual examples by calling them directly.")
