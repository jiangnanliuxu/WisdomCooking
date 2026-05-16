#!/usr/bin/env node
'use strict'

const { spawn, execFile } = require('node:child_process')
const fs = require('node:fs')
const fsp = require('node:fs/promises')
const http = require('node:http')
const path = require('node:path')
const { URL } = require('node:url')

const rootDir = path.resolve(__dirname, '..', '..')
const publicDir = path.join(__dirname, 'public')
const startScript = path.join(rootDir, 'scripts', 'start-all.sh')
const logDir = process.env.LOG_DIR || path.join(rootDir, 'logs', 'startup')
const host = process.env.CONTROL_PANEL_HOST || '127.0.0.1'
const port = Number(process.env.CONTROL_PANEL_PORT || 8787)

const serviceLogs = new Map([
  ['ruoyi-system', 'ruoyi-system.log'],
  ['ruoyi-cook', 'ruoyi-cook.log'],
  ['ruoyi-auth', 'ruoyi-auth.log'],
  ['ruoyi-gateway', 'ruoyi-gateway.log'],
  ['admin-web', 'admin-web.log'],
  ['user-app', 'user-app.log'],
])

const mimeTypes = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
}

let activeCommand = null
let nextEventId = 1
const eventBuffer = []
const clients = new Set()

function nowText() {
  return new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

function pushEvent(type, payload) {
  const event = {
    id: nextEventId++,
    type,
    time: nowText(),
    payload,
  }
  eventBuffer.push(event)
  if (eventBuffer.length > 500) {
    eventBuffer.shift()
  }
  const encoded = `id: ${event.id}\nevent: ${type}\ndata: ${JSON.stringify(event)}\n\n`
  for (const client of clients) {
    client.write(encoded)
  }
}

function sendJson(res, statusCode, data) {
  const body = JSON.stringify(data)
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Cache-Control': 'no-store',
  })
  res.end(body)
}

function sendText(res, statusCode, text) {
  res.writeHead(statusCode, {
    'Content-Type': 'text/plain; charset=utf-8',
    'Cache-Control': 'no-store',
  })
  res.end(text)
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = ''
    req.on('data', chunk => {
      body += chunk
      if (body.length > 64 * 1024) {
        req.destroy()
        reject(new Error('Request body is too large'))
      }
    })
    req.on('end', () => {
      if (!body) {
        resolve({})
        return
      }
      try {
        resolve(JSON.parse(body))
      } catch (error) {
        reject(error)
      }
    })
    req.on('error', reject)
  })
}

function safeEnv() {
  const env = { ...process.env }
  delete env.http_proxy
  delete env.https_proxy
  delete env.all_proxy
  delete env.HTTP_PROXY
  delete env.HTTPS_PROXY
  delete env.ALL_PROXY
  delete env.JAVA_TOOL_OPTIONS
  delete env.JDK_JAVA_OPTIONS
  delete env._JAVA_OPTIONS
  env.NO_PROXY = env.NO_PROXY || '127.0.0.1,localhost,::1'
  env.no_proxy = env.no_proxy || '127.0.0.1,localhost,::1'
  env.FORCE_COLOR = '0'
  return env
}

function parseStatusOutput(output) {
  const services = []
  for (const line of output.split(/\r?\n/)) {
    const match = line.match(/^(\S+)\s+screen=(\S+)\s+port=(\S+)\s+http=(\S+)/)
    if (!match) {
      continue
    }
    const [, name, screen, portState, http] = match
    services.push({
      name,
      screen,
      port: portState,
      http,
      ok: screen === 'running' && portState === 'listening' && http === 'ok',
    })
  }
  return services
}

function getStatus() {
  return new Promise(resolve => {
    execFile('bash', [startScript, 'status'], {
      cwd: rootDir,
      env: safeEnv(),
      timeout: 12000,
      maxBuffer: 1024 * 1024,
    }, (error, stdout, stderr) => {
      resolve({
        ok: !error,
        services: parseStatusOutput(stdout),
        output: stdout,
        error: stderr || (error ? error.message : ''),
      })
    })
  })
}

function runAction(action, skipBuild) {
  const allowed = new Set(['start', 'restart', 'stop', 'status'])
  if (!allowed.has(action)) {
    return { ok: false, status: 400, message: `Unsupported action: ${action}` }
  }
  if (activeCommand) {
    return {
      ok: false,
      status: 409,
      message: `${activeCommand.action} is still running. Wait for it to finish before starting another command.`,
    }
  }

  const args = [startScript, action]
  if ((action === 'start' || action === 'restart') && skipBuild) {
    args.push('--skip-build')
  }

  const command = {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    action,
    startedAt: new Date().toISOString(),
    args,
  }
  activeCommand = command
  pushEvent('command-start', command)

  const child = spawn('bash', args, {
    cwd: rootDir,
    env: safeEnv(),
    stdio: ['ignore', 'pipe', 'pipe'],
  })

  child.stdout.on('data', chunk => {
    pushEvent('command-output', {
      commandId: command.id,
      stream: 'stdout',
      text: chunk.toString(),
    })
  })
  child.stderr.on('data', chunk => {
    pushEvent('command-output', {
      commandId: command.id,
      stream: 'stderr',
      text: chunk.toString(),
    })
  })
  child.on('error', error => {
    pushEvent('command-error', {
      commandId: command.id,
      message: error.message,
    })
  })
  child.on('close', code => {
    pushEvent('command-exit', {
      commandId: command.id,
      action,
      code,
    })
    activeCommand = null
    getStatus().then(status => pushEvent('status', status))
  })

  return { ok: true, status: 202, command }
}

async function tailFile(filePath, maxBytes = 160 * 1024) {
  const stat = await fsp.stat(filePath)
  const start = Math.max(0, stat.size - maxBytes)
  const length = stat.size - start
  const handle = await fsp.open(filePath, 'r')
  try {
    const buffer = Buffer.alloc(length)
    await handle.read(buffer, 0, length, start)
    return buffer.toString('utf8')
  } finally {
    await handle.close()
  }
}

function filterErrors(text) {
  const errorPattern = /(error|exception|failed|timeout|refused|denied|报错|错误|异常|失败)/i
  return text
    .split(/\r?\n/)
    .filter(line => errorPattern.test(line))
    .join('\n')
}

async function readLogs(service, errorsOnly) {
  const selected = service === 'all'
    ? Array.from(serviceLogs.entries())
    : serviceLogs.has(service)
      ? [[service, serviceLogs.get(service)]]
      : []

  if (selected.length === 0) {
    return { ok: false, status: 400, message: `Unknown service: ${service}` }
  }

  const chunks = []
  for (const [name, filename] of selected) {
    const filePath = path.join(logDir, filename)
    if (!fs.existsSync(filePath)) {
      chunks.push(`===== ${name} =====\nLog file does not exist: ${filePath}`)
      continue
    }
    const text = await tailFile(filePath)
    const body = errorsOnly ? filterErrors(text) : text
    chunks.push(`===== ${name} =====\n${body || '(no matching log lines)'}`)
  }

  return {
    ok: true,
    status: 200,
    service,
    errorsOnly,
    logDir,
    text: chunks.join('\n\n'),
  }
}

function serveStatic(req, res, pathname) {
  const requested = pathname === '/' ? '/index.html' : pathname
  const decoded = decodeURIComponent(requested)
  const filePath = path.normalize(path.join(publicDir, decoded))
  if (!filePath.startsWith(publicDir)) {
    sendText(res, 403, 'Forbidden')
    return
  }

  fs.readFile(filePath, (error, data) => {
    if (error) {
      sendText(res, 404, 'Not found')
      return
    }
    const ext = path.extname(filePath)
    res.writeHead(200, {
      'Content-Type': mimeTypes[ext] || 'application/octet-stream',
      'Cache-Control': 'no-store',
    })
    res.end(data)
  })
}

const server = http.createServer(async (req, res) => {
  const requestUrl = new URL(req.url, `http://${host}:${port}`)
  const pathname = requestUrl.pathname

  try {
    if (req.method === 'GET' && pathname === '/api/stream') {
      res.writeHead(200, {
        'Content-Type': 'text/event-stream; charset=utf-8',
        'Cache-Control': 'no-cache, no-transform',
        Connection: 'keep-alive',
        'X-Accel-Buffering': 'no',
      })
      clients.add(res)
      res.write(`event: hello\ndata: ${JSON.stringify({ events: eventBuffer, activeCommand })}\n\n`)
      req.on('close', () => clients.delete(res))
      return
    }

    if (req.method === 'GET' && pathname === '/api/status') {
      const status = await getStatus()
      sendJson(res, status.ok ? 200 : 500, {
        ...status,
        activeCommand,
      })
      return
    }

    if (req.method === 'POST' && pathname === '/api/action') {
      const body = await readBody(req)
      const result = runAction(String(body.action || ''), Boolean(body.skipBuild))
      sendJson(res, result.status, result.ok ? result : { ok: false, message: result.message, activeCommand })
      return
    }

    if (req.method === 'GET' && pathname === '/api/logs') {
      const service = requestUrl.searchParams.get('service') || 'all'
      const errorsOnly = requestUrl.searchParams.get('errorsOnly') !== 'false'
      const logs = await readLogs(service, errorsOnly)
      sendJson(res, logs.status, logs)
      return
    }

    if (req.method === 'GET') {
      serveStatic(req, res, pathname)
      return
    }

    sendText(res, 405, 'Method not allowed')
  } catch (error) {
    sendJson(res, 500, {
      ok: false,
      message: error.message,
    })
  }
})

server.listen(port, host, () => {
  const url = `http://${host}:${port}/`
  console.log(`智慧食刻控制台已启动: ${url}`)
  console.log(`工作目录: ${rootDir}`)
  console.log(`日志目录: ${logDir}`)
  pushEvent('server-ready', { url, rootDir, logDir })
})
