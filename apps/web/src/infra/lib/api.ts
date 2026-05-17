const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export interface ChatRequest {
  agent: string
  message: string
  context?: Record<string, unknown>
}

export interface ChatResponse {
  success: boolean
  message: string
  data?: unknown
  error?: string
}

export interface ToolResultData {
  id: string
  toolCallId: string
  toolName: string
  success: boolean
  result?: unknown
  error?: string
}

type ChunkCallback = (chunk: string) => void
type ToolResultCallback = (result: ToolResultData) => void

export async function chatWithAgent(
  agent: string,
  message: string,
  onChunk?: ChunkCallback,
  onToolResult?: ToolResultCallback
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/agent/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      agent,
      message,
      context: {},
    }),
  })

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('Response body is not readable')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()

    if (done) {
      if (buffer) {
        try {
          const parsed = JSON.parse(buffer)
          if (parsed.tool_result) {
            onToolResult?.(parsed.tool_result)
          }
        } catch {
          onChunk?.(buffer)
        }
      }
      break
    }

    buffer += decoder.decode(value, { stream: true })

    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      if (!line.trim()) continue

      if (line.startsWith('data: ')) {
        const data = line.slice(6)
        if (data === '[DONE]') {
          return
        }
        try {
          const parsed = JSON.parse(data)
          if (parsed.tool_result) {
            onToolResult?.(parsed.tool_result)
          } else if (parsed.content) {
            onChunk?.(parsed.content)
          } else if (parsed.text) {
            onChunk?.(parsed.text)
          }
        } catch {
          onChunk?.(data)
        }
      } else {
        try {
          const parsed = JSON.parse(line)
          if (parsed.tool_result) {
            onToolResult?.(parsed.tool_result)
          } else if (parsed.content) {
            onChunk?.(parsed.content)
          }
        } catch {
          onChunk?.(line)
        }
      }
    }
  }
}

export async function getAgentStatus(agent: string): Promise<{
  status: 'online' | 'offline' | 'error'
  capabilities: string[]
  lastActive?: string
}> {
  const response = await fetch(`${API_BASE_URL}/agent/status/${agent}`)
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  return response.json()
}

export async function getChatHistory(sessionId: string): Promise<ChatResponse[]> {
  const response = await fetch(`${API_BASE_URL}/chat/history/${sessionId}`)
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  return response.json()
}

export async function clearChatHistory(sessionId: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/chat/history/${sessionId}`, {
    method: 'DELETE',
  })
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
}

export async function executeTool(
  agent: string,
  toolName: string,
  parameters: Record<string, unknown>
): Promise<ToolResultData> {
  const response = await fetch(`${API_BASE_URL}/agent/execute`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      agent,
      tool: toolName,
      parameters,
    }),
  })

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  return response.json()
}

export default {
  chatWithAgent,
  getAgentStatus,
  getChatHistory,
  clearChatHistory,
  executeTool,
}
