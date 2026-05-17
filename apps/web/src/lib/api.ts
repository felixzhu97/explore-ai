const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8001'

export interface ToolResultData {
  id: string
  tool: string
  input: Record<string, unknown>
  output: unknown
  status: 'success' | 'error'
  timestamp: Date
}

export interface ChatResponse {
  content?: string
  toolResult?: ToolResultData
  done?: boolean
}

export async function chatWithAgent(
  agent: string,
  message: string,
  onChunk?: (chunk: string) => void,
  onToolResult?: (result: ToolResultData) => void
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
    throw new Error('Response body not available')
  }

  const decoder = new TextDecoder()
  let buffer = ''
  let currentEvent = ''
  let toolBuffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    const chunk = decoder.decode(value, { stream: true })
    buffer += chunk
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      if (line.startsWith('event: ')) {
        currentEvent = line.slice(7).trim()
        if (currentEvent === 'tool') {
          toolBuffer = ''
        }
      } else if (line.startsWith('data: ')) {
        const data = line.slice(6)

        if (currentEvent === 'tool') {
          toolBuffer += data
        } else if (currentEvent === 'tool_result') {
          try {
            const toolResult: ToolResultData = JSON.parse(toolBuffer)
            onToolResult?.(toolResult)
          } catch (e) {
            console.error('Failed to parse tool result:', e)
          }
          toolBuffer = ''
          currentEvent = ''
        } else if (data === '[DONE]') {
          break
        } else {
          onChunk?.(data)
        }
      } else if (line.trim() === '') {
        currentEvent = ''
      }
    }
  }
}

export async function fetchAgentStatus(): Promise<Record<string, { status: string; tools: string[] }>> {
  const response = await fetch(`${API_BASE_URL}/agent/status`)
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  return response.json()
}

export async function invokeTool(
  agent: string,
  toolName: string,
  toolInput: Record<string, unknown>
): Promise<unknown> {
  const response = await fetch(`${API_BASE_URL}/agent/tool`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      agent,
      tool: toolName,
      input: toolInput,
    }),
  })

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  const data = await response.json()
  return data.output
}
