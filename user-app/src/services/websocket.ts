import { Client } from '@stomp/stompjs'

let client: Client | null = null

function resolveWebSocketUrl() {
  // #ifdef H5
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}${import.meta.env.VITE_WS_PATH || '/ws'}`
  // #endif
  // #ifndef H5
  return ''
  // #endif
}

export function connectConversation(conversationId: number, onMessage: (body: unknown) => void) {
  // #ifdef H5
  if (!client) {
    client = new Client({
      brokerURL: resolveWebSocketUrl(),
      reconnectDelay: 4000,
      debug: () => undefined,
    })
  }

  if (!client.connected) {
    client.activate()
  }

  const subscribe = () => {
    client?.subscribe(`/topic/conversations/${conversationId}`, (message) => {
      try {
        onMessage(JSON.parse(message.body))
      }
      catch {
        onMessage(message.body)
      }
    })
  }

  if (client.connected) {
    subscribe()
  }
  else {
    client.onConnect = () => subscribe()
  }
  // #endif
}
