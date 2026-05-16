'use strict'

const statusGrid = document.querySelector('#statusGrid')
const commandConsole = document.querySelector('#commandConsole')
const errorConsole = document.querySelector('#errorConsole')
const skipBuild = document.querySelector('#skipBuild')
const busyBadge = document.querySelector('#busyBadge')
const lastUpdated = document.querySelector('#lastUpdated')
const refreshStatusButton = document.querySelector('#refreshStatus')
const refreshLogsButton = document.querySelector('#refreshLogs')
const clearConsoleButton = document.querySelector('#clearConsole')
const logService = document.querySelector('#logService')
const errorsOnly = document.querySelector('#errorsOnly')
const commandButtons = Array.from(document.querySelectorAll('[data-action]'))

let busy = false
let autoStatusTimer = null

const serviceLabels = {
  'ruoyi-system': '系统服务',
  'ruoyi-cook': '业务服务',
  'ruoyi-auth': '认证服务',
  'ruoyi-gateway': '网关服务',
  'admin-web': '管理端',
  'user-app': '用户端',
}

function appendConsole(text, className = '') {
  const span = document.createElement('span')
  if (className) {
    span.className = className
  }
  span.textContent = text
  commandConsole.append(span)
  commandConsole.scrollTop = commandConsole.scrollHeight
}

function setBusy(nextBusy, label = '') {
  busy = nextBusy
  busyBadge.textContent = nextBusy ? label || '执行中' : '空闲'
  busyBadge.className = nextBusy ? 'badge badge--busy' : 'badge badge--idle'
  commandButtons.forEach(button => {
    button.disabled = nextBusy
  })
}

function statusClass(service) {
  if (service.ok) {
    return 'module module--ok'
  }
  if (service.http === 'fail' || service.port === 'closed') {
    return 'module module--fail'
  }
  return 'module'
}

function renderStatus(services) {
  if (!services.length) {
    statusGrid.innerHTML = '<div class="module module--fail"><div class="module__name">没有状态数据</div><div class="module__meta">请检查启动脚本输出。</div></div>'
    return
  }

  statusGrid.innerHTML = services.map(service => `
    <article class="${statusClass(service)}">
      <div class="module__head">
        <div>
          <div class="module__name">${service.name}</div>
          <div class="module__meta">${serviceLabels[service.name] || service.name}</div>
        </div>
        <span class="dot" aria-hidden="true"></span>
      </div>
      <div class="module__meta">
        <span>screen <strong>${service.screen}</strong></span>
        <span>port <strong>${service.port}</strong></span>
        <span>http <strong>${service.http}</strong></span>
      </div>
    </article>
  `).join('')
}

async function refreshStatus() {
  const response = await fetch('/api/status', { cache: 'no-store' })
  const data = await response.json()
  if (!response.ok) {
    appendConsole(`[状态检查失败] ${data.error || data.message || response.statusText}\n`, 'line--stderr')
    return
  }
  renderStatus(data.services || [])
  setBusy(Boolean(data.activeCommand), data.activeCommand ? `${data.activeCommand.action} 执行中` : '')
  lastUpdated.textContent = `状态刷新：${new Date().toLocaleTimeString('zh-CN', { hour12: false })}`
}

async function refreshLogs() {
  const params = new URLSearchParams({
    service: logService.value,
    errorsOnly: String(errorsOnly.checked),
  })
  const response = await fetch(`/api/logs?${params.toString()}`, { cache: 'no-store' })
  const data = await response.json()
  if (!response.ok) {
    errorConsole.textContent = data.message || '日志读取失败'
    return
  }
  errorConsole.textContent = data.text || '(没有日志内容)'
  errorConsole.scrollTop = errorConsole.scrollHeight
}

async function runAction(action) {
  if (busy) {
    return
  }
  const skipBuildArg = (action === 'start' || action === 'restart') && skipBuild.checked ? ' --skip-build' : ''
  appendConsole(`\n$ bash scripts/start-all.sh ${action}${skipBuildArg}\n`, 'line--event')
  setBusy(true, `${action} 执行中`)

  const response = await fetch('/api/action', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      action,
      skipBuild: skipBuild.checked,
    }),
  })
  const data = await response.json()
  if (!response.ok) {
    appendConsole(`[拒绝执行] ${data.message || response.statusText}\n`, 'line--stderr')
    setBusy(Boolean(data.activeCommand), data.activeCommand ? `${data.activeCommand.action} 执行中` : '')
  }
}

function connectStream() {
  const events = new EventSource('/api/stream')

  events.addEventListener('hello', event => {
    const data = JSON.parse(event.data)
    if (Array.isArray(data.events)) {
      for (const item of data.events.slice(-80)) {
        handleEvent(item)
      }
    }
    setBusy(Boolean(data.activeCommand), data.activeCommand ? `${data.activeCommand.action} 执行中` : '')
  })

  for (const type of ['server-ready', 'command-start', 'command-output', 'command-error', 'command-exit', 'status']) {
    events.addEventListener(type, event => {
      handleEvent(JSON.parse(event.data))
    })
  }

  events.onerror = () => {
    appendConsole('[控制台连接中断，浏览器会自动重连]\n', 'line--stderr')
  }
}

function handleEvent(event) {
  if (!event || !event.type) {
    return
  }

  if (event.type === 'server-ready') {
    appendConsole(`[${event.time}] 控制台服务已就绪\n`, 'line--success')
    return
  }

  if (event.type === 'command-start') {
    appendConsole(`[${event.time}] 开始执行 ${event.payload.action}\n`, 'line--event')
    setBusy(true, `${event.payload.action} 执行中`)
    return
  }

  if (event.type === 'command-output') {
    appendConsole(event.payload.text, event.payload.stream === 'stderr' ? 'line--stderr' : '')
    return
  }

  if (event.type === 'command-error') {
    appendConsole(`[${event.time}] ${event.payload.message}\n`, 'line--stderr')
    return
  }

  if (event.type === 'command-exit') {
    const ok = event.payload.code === 0
    appendConsole(`[${event.time}] ${event.payload.action} 结束，退出码 ${event.payload.code}\n`, ok ? 'line--success' : 'line--stderr')
    setBusy(false)
    refreshLogs()
    return
  }

  if (event.type === 'status') {
    renderStatus(event.payload.services || [])
    lastUpdated.textContent = `状态刷新：${new Date().toLocaleTimeString('zh-CN', { hour12: false })}`
  }
}

commandButtons.forEach(button => {
  button.addEventListener('click', () => runAction(button.dataset.action))
})

refreshStatusButton.addEventListener('click', refreshStatus)
refreshLogsButton.addEventListener('click', refreshLogs)
logService.addEventListener('change', refreshLogs)
errorsOnly.addEventListener('change', refreshLogs)
clearConsoleButton.addEventListener('click', () => {
  commandConsole.textContent = ''
})

connectStream()
refreshStatus()
refreshLogs()
autoStatusTimer = window.setInterval(refreshStatus, 5000)

window.addEventListener('beforeunload', () => {
  if (autoStatusTimer) {
    window.clearInterval(autoStatusTimer)
  }
})
