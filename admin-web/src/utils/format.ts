export function formatDateTime(value?: string): string {
  if (!value) {
    return '-'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export function safeJson(input?: string): Record<string, unknown> {
  if (!input) {
    return {}
  }

  try {
    return JSON.parse(input) as Record<string, unknown>
  }
  catch {
    return {}
  }
}
