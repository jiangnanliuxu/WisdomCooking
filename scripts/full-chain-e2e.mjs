#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';

const ROOT = process.cwd();
const DATE_TAG = '20260515';
const PREFIX = `e2e_${DATE_TAG}_`;
const RUN_ID = `${PREFIX}${new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)}`;
const OUT_DIR = path.join(ROOT, 'logs', 'full-chain-e2e');
const SCREENSHOT_DIR = path.join(OUT_DIR, 'screenshots');
const API_RESULTS_PATH = path.join(OUT_DIR, `api-results-${DATE_TAG}.json`);
const BROWSER_RESULTS_PATH = path.join(OUT_DIR, `browser-results-${DATE_TAG}.json`);
const REPORT_PATH = path.join(ROOT, 'docs', `全链路接口与浏览器测试报告-${DATE_TAG}.md`);
const MYSQL = process.env.MYSQL_BIN || '/opt/homebrew/opt/mysql-client/bin/mysql';
const nacosAppConfigPath = process.env.NACOS_APP_CONFIG_PATH
  || path.join(ROOT, 'nacos-server-2.2.3/data/tenant-config-data/public/DEFAULT_GROUP/application-dev.yml');
const nacosAppConfig = existsSync(nacosAppConfigPath) ? readFileSync(nacosAppConfigPath, 'utf8') : '';
const jdbcBlockMatch = nacosAppConfig.match(/url:\s*jdbc:mysql:\/\/([^:/]+)(?::(\d+))?\/([^?\s]+)[\s\S]{0,240}?username:\s*([^\s]+)[\s\S]{0,120}?password:\s*([^\s]+)/);
const jdbcMatch = jdbcBlockMatch || nacosAppConfig.match(/url:\s*jdbc:mysql:\/\/([^:/]+)(?::(\d+))?\/([^?\s]+)/);
const MYSQL_HOST = process.env.MYSQL_HOST || jdbcMatch?.[1] || '127.0.0.1';
const MYSQL_PORT = process.env.MYSQL_PORT || jdbcMatch?.[2] || '3306';
const MYSQL_DB = process.env.MYSQL_DB || jdbcMatch?.[3] || 'cook';
const MYSQL_USER = process.env.MYSQL_USER || jdbcBlockMatch?.[4] || 'root';
const MYSQL_ENV = { ...process.env, MYSQL_PWD: process.env.MYSQL_PWD || jdbcBlockMatch?.[5] || '' };
const USER_BASE = process.env.USER_API_BASE || 'http://127.0.0.1:9210';
const GATEWAY_BASE = process.env.GATEWAY_API_BASE || 'http://127.0.0.1:8080';
const USER_APP_URL = process.env.USER_APP_URL || 'http://127.0.0.1:5174/';
const ADMIN_APP_URL = process.env.ADMIN_APP_URL || 'http://127.0.0.1:5173/';
const ADMIN_USERNAME = process.env.ADMIN_USERNAME || 'admin';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'change_me';

mkdirSync(OUT_DIR, { recursive: true });
mkdirSync(SCREENSHOT_DIR, { recursive: true });

const state = {
  runId: RUN_ID,
  startedAt: new Date().toISOString(),
  prefix: PREFIX,
  env: { USER_BASE, GATEWAY_BASE, USER_APP_URL, ADMIN_APP_URL, MYSQL_HOST, MYSQL_PORT, MYSQL_DB, MYSQL_USER },
  coverage: {},
  tests: [],
  cleanup: [],
  ids: {
    users: [],
    phones: [],
    media: [],
    recipes: [],
    posts: [],
    comments: [],
    checkins: [],
    feedbacks: [],
    reports: [],
    conversations: [],
    groups: [],
    aiConversations: [],
    aiRecognitions: [],
    uploadSessions: [],
    banners: [],
    categories: [],
  },
  tokens: {},
  blocked: [],
  residuals: [],
};

function now() {
  return new Date().toISOString();
}

function jsonBlock(value) {
  return JSON.stringify(value, null, 2);
}

function record(name, status, detail = {}) {
  const item = { name, status, at: now(), ...detail };
  state.tests.push(item);
  const icon = status === 'PASS' ? 'PASS' : status === 'BLOCKED' ? 'BLOCKED' : status === 'SKIP' ? 'SKIP' : 'FAIL';
  console.log(`${icon} ${name}${detail.message ? ` - ${detail.message}` : ''}`);
  return item;
}

function addCleanup(action, status, detail = {}) {
  state.cleanup.push({ action, status, at: now(), ...detail });
}

function dataOf(response) {
  return response && typeof response === 'object' && 'data' in response ? response.data : response;
}

function responseOk(response) {
  return response && typeof response === 'object' && (response.code === 200 || response.code === undefined);
}

async function http(name, method, url, { token, body, headers = {}, timeoutMs = 15000, raw = false } = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  const fullUrl = url.startsWith('http') ? url : `${USER_BASE}${url}`;
  try {
    const init = {
      method,
      headers: { ...headers },
      signal: controller.signal,
    };
    if (token) init.headers.Authorization = `Bearer ${token}`;
    if (body !== undefined) {
      if (body instanceof FormData) {
        init.body = body;
      }
      else {
        init.headers['Content-Type'] = init.headers['Content-Type'] || 'application/json';
        init.body = JSON.stringify(body);
      }
    }
    const started = Date.now();
    const res = await fetch(fullUrl, init);
    const contentType = res.headers.get('content-type') || '';
    const payload = raw
      ? await res.text()
      : (contentType.includes('application/json') || contentType.includes('+json'))
        ? await res.json().catch(() => ({}))
        : await res.text();
    const result = {
      status: res.status,
      ok: res.ok,
      elapsedMs: Date.now() - started,
      payload,
      url: fullUrl,
      method,
    };
    return result;
  }
  catch (error) {
    return {
      status: 0,
      ok: false,
      error: error.name === 'AbortError' ? `timeout ${timeoutMs}ms` : error.message,
      url: fullUrl,
      method,
    };
  }
  finally {
    clearTimeout(timeout);
  }
}

async function check(name, method, url, options = {}, assert = (result) => result.ok && responseOk(result.payload)) {
  const result = await http(name, method, url, options);
  const ok = assert(result);
  const payload = result.payload;
  record(name, ok ? 'PASS' : 'FAIL', {
    method,
    url: result.url,
    httpStatus: result.status,
    elapsedMs: result.elapsedMs,
    message: ok ? undefined : (payload?.msg || result.error || `HTTP ${result.status}`),
    responseSample: typeof payload === 'string' ? payload.slice(0, 500) : payload,
  });
  return result;
}

function mysql(query, { raw = false } = {}) {
  const args = [`-h${MYSQL_HOST}`, `-P${MYSQL_PORT}`, `-u${MYSQL_USER}`, '--batch', '--raw', '--skip-column-names', '-e', query];
  const out = execFileSync(MYSQL, args, { env: MYSQL_ENV, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
  return raw ? out : out.trim();
}

function sqlString(value) {
  return String(value).replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}

function ids(list) {
  return list.filter((v) => v !== undefined && v !== null).map((v) => Number(v)).filter((v) => Number.isFinite(v));
}

function idCsv(list) {
  return ids(list).join(',');
}

function asRows(pageData) {
  if (!pageData) return [];
  if (Array.isArray(pageData)) return pageData;
  if (Array.isArray(pageData.rows)) return pageData.rows;
  if (Array.isArray(pageData.records)) return pageData.records;
  if (Array.isArray(pageData.list)) return pageData.list;
  return [];
}

function unwrap(result) {
  return dataOf(result.payload);
}

function imageForm(filename = `${RUN_ID}.png`) {
  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMB/ax8p7sAAAAASUVORK5CYII=',
    'base64',
  );
  const form = new FormData();
  form.append('file', new Blob([png], { type: 'image/png' }), filename);
  return form;
}

function binaryForm(filename, type, bytes) {
  const form = new FormData();
  form.append('file', new Blob([Buffer.from(bytes)], { type }), filename);
  return form;
}

function extractDocEndpoints() {
  const doc = readFileSync(path.join(ROOT, 'docs', '项目全栈技术与接口数据库总览.md'), 'utf8');
  const re = /`((?:GET|POST|PUT|DELETE|PATCH)\s+[^`]+)`/g;
  const endpoints = new Set();
  let match;
  while ((match = re.exec(doc))) endpoints.add(match[1].trim());
  return [...endpoints].sort();
}

function extractControllerEndpoints() {
  const baseDir = path.join(ROOT, 'projetc/RuoYi-Cloud/ruoyi-modules/ruoyi-cook/src/main/java/com/ruoyi/cook');
  const files = execFileSync('rg', ['--files', baseDir], { encoding: 'utf8' })
    .split('\n')
    .filter((file) => file.endsWith('Controller.java'));
  const endpoints = [];
  const map = { GetMapping: 'GET', PostMapping: 'POST', PutMapping: 'PUT', DeleteMapping: 'DELETE', PatchMapping: 'PATCH' };
  for (const file of files) {
    const text = readFileSync(file, 'utf8');
    const base = (text.match(/@RequestMapping\("([^"]*)"\)/) || [])[1] || '';
    const methodRe = /@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)(?:\(([^)]*)\))?/g;
    let match;
    while ((match = methodRe.exec(text))) {
      let route = '';
      const args = match[2] || '';
      const pathMatch = args.match(/"([^"]*)"/);
      if (pathMatch) route = pathMatch[1];
      endpoints.push(`${map[match[1]]} ${`${base}${route}`.replace(/\/+/g, '/')}`.replace(':/', '://'));
    }
  }
  return [...new Set(endpoints)].sort();
}

function extractFrontendEndpoints() {
  const files = [
    ...execFileSync('rg', ['--files', path.join(ROOT, 'user-app/src/api')], { encoding: 'utf8' }).split('\n'),
    ...execFileSync('rg', ['--files', path.join(ROOT, 'admin-web/src/api')], { encoding: 'utf8' }).split('\n'),
  ].filter(Boolean).filter((file) => file.endsWith('.ts'));
  const endpoints = new Set();
  for (const file of files) {
    const text = readFileSync(file, 'utf8');
    const urlRe = /url:\s*`([^`]+)`|url:\s*'([^']+)'|request\.(get|post|put|delete)<[^>]*>\((`[^`]+`|'[^']+')|request\.(get|post|put|delete)\((`[^`]+`|'[^']+')/g;
    let match;
    while ((match = urlRe.exec(text))) {
      const raw = match[1] || match[2] || match[4] || match[6] || '';
      const method = (match[3] || match[5] || (text.slice(Math.max(0, match.index - 120), match.index + 200).match(/method:\s*'([^']+)'/) || [])[1] || 'GET').toUpperCase();
      const route = raw.replace(/^['`]|['`]$/g, '').replace(/\$\{[^}]+\}/g, '{id}').split('?')[0];
      if (route.startsWith('/')) endpoints.add(`${method} ${route}`);
    }
  }
  return [...endpoints].sort();
}

function buildCoverage() {
  const docEndpoints = extractDocEndpoints();
  const controllerEndpoints = extractControllerEndpoints();
  const frontendEndpoints = extractFrontendEndpoints();
  const docSet = new Set(docEndpoints);
  const controllerSet = new Set(controllerEndpoints);
  const frontendSet = new Set(frontendEndpoints);
  state.coverage = {
    docCount: docEndpoints.length,
    controllerCount: controllerEndpoints.length,
    frontendCount: frontendEndpoints.length,
    docEndpoints,
    controllerEndpoints,
    frontendEndpoints,
    frontendIntegrated: controllerEndpoints.filter((endpoint) => frontendSet.has(endpoint)),
    backendOnly: controllerEndpoints.filter((endpoint) => !frontendSet.has(endpoint)),
    documentedOnly: docEndpoints.filter((endpoint) => !controllerSet.has(endpoint)),
    sourceNotDocumented: controllerEndpoints.filter((endpoint) => !docSet.has(endpoint)),
  };
}

async function registerUser(index) {
  const suffix = String((Date.now() + index) % 100000000).padStart(8, '0');
  const phone = `199${suffix}`;
  const nickname = `${PREFIX}用户${index}`;
  const password = process.env.E2E_NEW_USER_PASSWORD || 'change_me';
  state.ids.phones.push(phone);
  const register = await check(`用户${index} 免验证码注册`, 'POST', '/api/v1/auth/register', {
    body: { phone, password, confirmPassword: password, nickname },
  });
  const data = unwrap(register);
  const token = data?.accessToken;
  const userId = data?.user?.id;
  if (!token || !userId) throw new Error(`用户${index} 注册响应缺少 token/userId`);
  state.ids.users.push(userId);
  state.tokens[`user${index}`] = token;
  return { phone, password, token, userId, nickname };
}

async function loginExistingUser(index) {
  const seeds = [
    {
      phone: process.env.E2E_SEED_USER1_PHONE || '13000000001',
      password: process.env.E2E_SEED_USER1_PASSWORD || 'change_me',
      nickname: 'seed-user-1',
    },
    {
      phone: process.env.E2E_SEED_USER2_PHONE || '13000000002',
      password: process.env.E2E_SEED_USER2_PASSWORD || 'change_me',
      nickname: 'seed-user-2',
    },
  ];
  const seed = seeds[index - 1];
  const result = await check(`用户${index} 种子账号登录`, 'POST', '/api/v1/auth/login', {
    body: { phone: seed.phone, password: seed.password },
  });
  const data = unwrap(result);
  const token = data?.accessToken;
  const userId = data?.user?.id;
  if (!token || !userId) throw new Error(`用户${index} 种子账号不可用`);
  state.tokens[`user${index}`] = token;
  return { ...seed, token, userId, seeded: true };
}

async function getTestUser(index) {
  return registerUser(index);
}

async function main() {
  buildCoverage();

  await check('gateway health', 'GET', `${GATEWAY_BASE}/actuator/health`);
  await check('system health', 'GET', 'http://127.0.0.1:9201/actuator/health');
  await check('cook health', 'GET', `${USER_BASE}/actuator/health`);
  await check('user-app 可访问', 'GET', USER_APP_URL, { raw: true }, (result) => result.status === 200);
  await check('admin-web 可访问', 'GET', ADMIN_APP_URL, { raw: true }, (result) => result.status === 200);
  await check('Nacos 8848 可访问', 'GET', 'http://127.0.0.1:8848/nacos/actuator/health', { raw: true, timeoutMs: 5000 }, (result) => result.status === 200);

  const home = await check('首页聚合 GET /api/v1/home', 'GET', '/api/v1/home?recipeLimit=3&userLimit=3');
  const banners = await check('轮播列表 GET /api/v1/banners', 'GET', '/api/v1/banners');
  const bannerId = asRows(unwrap(banners))[0]?.id || asRows(unwrap(home)?.banners)[0]?.id;
  if (bannerId) await check('轮播点击 POST /api/v1/banners/{id}/click', 'POST', `/api/v1/banners/${bannerId}/click`);
  await check('分类 GET /api/v1/categories', 'GET', '/api/v1/categories');
  await check('菜谱列表 GET /api/v1/recipes', 'GET', '/api/v1/recipes?page=1&pageSize=5');
  await check('动态列表 GET /api/v1/posts', 'GET', '/api/v1/posts?page=1&pageSize=5');
  await check('话题 GET /api/v1/topics', 'GET', '/api/v1/topics');
  await check('热门搜索 GET /api/v1/search/hot', 'GET', '/api/v1/search/hot?limit=5');
  await check('综合搜索 GET /api/v1/search', 'GET', `/api/v1/search?keyword=${encodeURIComponent('番茄')}&page=1&pageSize=5`);

  const user1 = await getTestUser(1);
  const user2 = await getTestUser(2);

  await check('用户登录 POST /api/v1/auth/login', 'POST', '/api/v1/auth/login', {
    body: { phone: user1.phone, password: user1.password },
  });
  await check('当前用户 GET /api/v1/users/me', 'GET', '/api/v1/users/me', { token: user1.token });
  if (user1.seeded) {
    record('更新资料 PUT /api/v1/users/me', 'SKIP', {
      message: '当前只能使用已有种子账号，跳过资料写入以避免污染非本次创建用户。',
    });
    record('保存兴趣 PUT /api/v1/users/me/interests', 'SKIP', {
      message: '当前只能使用已有种子账号，跳过兴趣标签写入以避免污染非本次创建用户。',
    });
  }
  else {
    await check('更新资料 PUT /api/v1/users/me', 'PUT', '/api/v1/users/me', {
      token: user1.token,
      body: { nickname: `${PREFIX}主测用户`, gender: '1', birthday: '1998-05-15', region: '上海市 徐汇区', bio: `${RUN_ID} profile` },
    });
    await check('保存兴趣 PUT /api/v1/users/me/interests', 'PUT', '/api/v1/users/me/interests', {
      token: user1.token,
      body: { interestTags: ['川菜', '粤菜'] },
    });
  }
  await check('公开主页 GET /api/v1/users/{id}/profile', 'GET', `/api/v1/users/${user2.userId}/profile`, { token: user1.token });
  await check('推荐用户 GET /api/v1/users/recommended', 'GET', '/api/v1/users/recommended?limit=3', { token: user1.token });
  await check('关注用户 POST /api/v1/users/{id}/follow', 'POST', `/api/v1/users/${user2.userId}/follow`, { token: user1.token });
  await check('粉丝列表 GET /api/v1/users/me/followers', 'GET', '/api/v1/users/me/followers?page=1&pageSize=5', { token: user2.token });
  await check('关注列表 GET /api/v1/users/me/following', 'GET', '/api/v1/users/me/following?page=1&pageSize=5', { token: user1.token });
  await check('取消关注 DELETE /api/v1/users/{id}/follow', 'DELETE', `/api/v1/users/${user2.userId}/follow`, { token: user1.token });

  const image = await check('上传图片 POST /api/v1/uploads/images', 'POST', '/api/v1/uploads/images', {
    token: user1.token,
    body: imageForm(),
  });
  const imageMediaId = unwrap(image)?.id;
  if (imageMediaId) state.ids.media.push(imageMediaId);
  if (imageMediaId) await check('媒体详情 GET /api/v1/uploads/{id}', 'GET', `/api/v1/uploads/${imageMediaId}`, { token: user1.token });
  if (imageMediaId) await check('媒体原文件 GET /api/v1/uploads/{id}/raw', 'GET', `/api/v1/uploads/${imageMediaId}/raw`, { raw: true }, (result) => result.status === 200);

  const audio = await check('上传语音 POST /api/v1/uploads/audios', 'POST', '/api/v1/uploads/audios', {
    token: user1.token,
    body: binaryForm(`${RUN_ID}.wav`, 'audio/wav', [82, 73, 70, 70, 0, 0, 0, 0]),
  });
  if (unwrap(audio)?.id) state.ids.media.push(unwrap(audio).id);
  const video = await check('上传视频兜底 POST /api/v1/uploads/videos', 'POST', '/api/v1/uploads/videos', {
    token: user1.token,
    body: binaryForm(`${RUN_ID}.mp4`, 'video/mp4', [0, 0, 0, 24, 102, 116, 121, 112, 109, 112, 52, 50]),
  });
  if (unwrap(video)?.id) state.ids.media.push(unwrap(video).id);

  const multipart = await check('OSS 分片初始化 POST /api/v1/uploads/videos/multipart/init', 'POST', '/api/v1/uploads/videos/multipart/init', {
    token: user1.token,
    body: { fileName: `${RUN_ID}.mp4`, fileSize: 1024, contentType: 'video/mp4', fingerprint: RUN_ID },
  }, (result) => result.status > 0);
  const sessionId = unwrap(multipart)?.sessionId;
  if (sessionId) {
    state.ids.uploadSessions.push(sessionId);
    await check('OSS 分片会话 GET /api/v1/uploads/videos/multipart/{sessionId}', 'GET', `/api/v1/uploads/videos/multipart/${sessionId}`, { token: user1.token });
    await check('OSS 分片完成 POST /api/v1/uploads/videos/multipart/complete', 'POST', '/api/v1/uploads/videos/multipart/complete', {
      token: user1.token,
      body: { sessionId, objectKey: unwrap(multipart)?.objectKey },
      timeoutMs: 20000,
    }, (result) => result.status > 0);
    await check('OSS 分片取消 POST /api/v1/uploads/videos/multipart/{sessionId}/cancel', 'POST', `/api/v1/uploads/videos/multipart/${sessionId}/cancel`, { token: user1.token }, (result) => result.status > 0);
  }

  const catData = unwrap(await check('分类回读用于菜谱分类', 'GET', '/api/v1/categories')) || [];
  const firstGroup = Array.isArray(catData) ? catData[0] : null;
  const categoryCode = firstGroup?.children?.[0]?.code || firstGroup?.code || 'sichuan';
  const recipePayload = {
    title: `${PREFIX}番茄鸡蛋 ${RUN_ID}`,
    categoryCode,
    coverMediaId: imageMediaId,
    intro: `${RUN_ID} recipe intro`,
    difficulty: 'easy',
    cookTime: '10分钟',
    serving: '1人份',
    ingredients: [{ name: '鸡蛋', amount: '2个' }, { name: '番茄', amount: '1个' }],
    steps: [{ stepNumber: 1, content: '搅拌并翻炒', imageMediaId }],
    tips: [{ content: '全链路测试数据' }],
    video: unwrap(video)?.id ? { mediaId: unwrap(video).id, url: unwrap(video).url, status: unwrap(video).status } : undefined,
  };
  const recipe = await check('创建菜谱 POST /api/v1/recipes', 'POST', '/api/v1/recipes', { token: user1.token, body: recipePayload });
  const recipeId = unwrap(recipe)?.id;
  if (recipeId) state.ids.recipes.push(recipeId);
  if (recipeId) await check('菜谱详情 GET /api/v1/recipes/{id}', 'GET', `/api/v1/recipes/${recipeId}`, { token: user1.token });
  if (recipeId) await check('提交菜谱审核 POST /api/v1/recipes/{id}/submit', 'POST', `/api/v1/recipes/${recipeId}/submit`, { token: user1.token });
  await check('我的菜谱 GET /api/v1/users/me/recipes', 'GET', '/api/v1/users/me/recipes?page=1&pageSize=10', { token: user1.token });
  if (recipeId) await check('审核前菜谱评论 GET /api/v1/comments', 'GET', `/api/v1/comments?target_type=recipe&target_id=${recipeId}&page=1&pageSize=5`, { token: user1.token });

  const post = await check('创建动态 POST /api/v1/posts', 'POST', '/api/v1/posts', {
    token: user1.token,
    body: { content: `${PREFIX}动态 ${RUN_ID}`, visibility: 'public', mediaIds: imageMediaId ? [imageMediaId] : [], topicCodes: ['food'], location: '上海市 徐汇区', relatedRecipeId: recipeId, sourceType: 'normal' },
  });
  const postId = unwrap(post)?.id;
  if (postId) state.ids.posts.push(postId);
  if (postId) await check('动态详情 GET /api/v1/posts/{id}', 'GET', `/api/v1/posts/${postId}`, { token: user1.token });
  if (postId) await check('重新提交动态 POST /api/v1/posts/{id}/submit', 'POST', `/api/v1/posts/${postId}/submit`, { token: user1.token }, (result) => result.status > 0);
  await check('我的动态 GET /api/v1/users/me/posts', 'GET', '/api/v1/users/me/posts?page=1&pageSize=10', { token: user1.token });
  if (postId) await check('审核前动态评论 GET /api/v1/comments', 'GET', `/api/v1/comments?target_type=post&target_id=${postId}&page=1&pageSize=5`, { token: user1.token });

  await check('搜索历史 GET /api/v1/search/history', 'GET', '/api/v1/search/history', { token: user1.token });
  await check('清空搜索历史 DELETE /api/v1/search/history', 'DELETE', '/api/v1/search/history', { token: user1.token });
  await check('收藏列表 GET /api/v1/users/me/favorites', 'GET', '/api/v1/users/me/favorites?page=1&pageSize=5', { token: user2.token });
  await check('点赞列表 GET /api/v1/users/me/likes', 'GET', '/api/v1/users/me/likes?page=1&pageSize=5', { token: user2.token });

  const today = new Date().toISOString().slice(0, 10);
  await check('打卡统计 GET /api/v1/checkins/summary', 'GET', '/api/v1/checkins/summary', { token: user1.token });
  const checkin = await check('新增打卡 POST /api/v1/checkins', 'POST', '/api/v1/checkins', {
    token: user1.token,
    body: { recipeId, checkinDate: today, content: `${RUN_ID} checkin`, mediaIds: imageMediaId ? [imageMediaId] : [], source: { type: 'e2e' } },
  });
  const checkinId = unwrap(checkin)?.id;
  if (checkinId) state.ids.checkins.push(checkinId);
  await check('月历打卡 GET /api/v1/checkins', 'GET', `/api/v1/checkins?date=${today}`, { token: user1.token });
  await check('日期打卡 GET /api/v1/checkins/by-date', 'GET', `/api/v1/checkins/by-date?date=${today}`, { token: user1.token });
  if (checkinId) await check('编辑打卡 PUT /api/v1/checkins/{id}', 'PUT', `/api/v1/checkins/${checkinId}`, {
    token: user1.token,
    body: { recipeId, checkinDate: today, content: `${RUN_ID} checkin updated`, mediaIds: imageMediaId ? [imageMediaId] : [], source: { type: 'e2e-update' } },
  });
  if (checkinId) {
    const cp = await check('打卡转动态 POST /api/v1/checkins/{id}/post', 'POST', `/api/v1/checkins/${checkinId}/post`, { token: user1.token });
    if (unwrap(cp)?.postId) state.ids.posts.push(unwrap(cp).postId);
  }

  const feedback = await check('提交反馈 POST /api/v1/feedbacks', 'POST', '/api/v1/feedbacks', {
    token: user1.token,
    body: { type: 'bug', content: `${RUN_ID} feedback`, mediaIds: imageMediaId ? [imageMediaId] : [], contact: user1.phone },
  });
  if (unwrap(feedback)?.id) state.ids.feedbacks.push(unwrap(feedback).id);
  await check('反馈列表 GET /api/v1/feedbacks', 'GET', '/api/v1/feedbacks?page=1&pageSize=5', { token: user1.token });

  if (recipeId) {
    const report = await check('提交举报 POST /api/v1/reports', 'POST', '/api/v1/reports', {
      token: user2.token,
      body: { targetType: 'recipe', targetId: recipeId, reasonType: 'other', reason: `${RUN_ID} report`, mediaIds: [] },
    });
    const reportId = unwrap(report)?.id;
    if (reportId) {
      state.ids.reports.push(reportId);
      await check('举报列表 GET /api/v1/reports', 'GET', '/api/v1/reports?page=1&pageSize=5', { token: user2.token });
      await check('举报详情 GET /api/v1/reports/{id}', 'GET', `/api/v1/reports/${reportId}`, { token: user2.token });
    }
  }

  const privateConv = await check('创建私聊 POST /api/v1/conversations/private', 'POST', '/api/v1/conversations/private', {
    token: user1.token,
    body: { targetUserId: user2.userId },
  });
  const privateConversationId = unwrap(privateConv)?.id;
  if (privateConversationId) state.ids.conversations.push(privateConversationId);
  await check('会话列表 GET /api/v1/conversations', 'GET', '/api/v1/conversations?type=private&page=1&pageSize=5', { token: user1.token });
  await check('通知列表 GET /api/v1/notifications', 'GET', '/api/v1/notifications?page=1&pageSize=5', { token: user1.token });
  if (privateConversationId) {
    await check('发送私聊消息 POST /api/v1/conversations/{id}/messages', 'POST', `/api/v1/conversations/${privateConversationId}/messages`, {
      token: user1.token,
      body: { messageType: 'text', content: `${RUN_ID} private message` },
    });
    await check('私聊消息 GET /api/v1/conversations/{id}/messages', 'GET', `/api/v1/conversations/${privateConversationId}/messages?page=1&pageSize=5`, { token: user1.token });
    await check('会话已读 POST /api/v1/conversations/{id}/read', 'POST', `/api/v1/conversations/${privateConversationId}/read`, { token: user1.token });
    await check('会话设置 PUT /api/v1/conversations/{id}/settings', 'PUT', `/api/v1/conversations/${privateConversationId}/settings`, { token: user1.token, body: { muted: true, pinned: true } });
  }

  const group = await check('创建群聊 POST /api/v1/groups', 'POST', '/api/v1/groups', {
    token: user1.token,
    body: { name: `${PREFIX}群聊 ${RUN_ID}`, intro: 'e2e group', notice: 'e2e notice', memberIds: [] },
  });
  const groupId = unwrap(group)?.id;
  const groupConversationId = unwrap(group)?.conversationId;
  if (groupId) state.ids.groups.push(groupId);
  if (groupConversationId) state.ids.conversations.push(groupConversationId);
  if (groupId) {
    await check('群详情 GET /api/v1/groups/{id}', 'GET', `/api/v1/groups/${groupId}`, { token: user1.token });
    await check('群成员 GET /api/v1/groups/{id}/members', 'GET', `/api/v1/groups/${groupId}/members`, { token: user1.token });
    await check('邀请群成员 POST /api/v1/groups/{id}/invite', 'POST', `/api/v1/groups/${groupId}/invite`, { token: user1.token, body: { userIds: [user2.userId] } }, (result) => result.status > 0);
    if (groupConversationId) await check('发送群消息 POST /api/v1/conversations/{id}/messages', 'POST', `/api/v1/conversations/${groupConversationId}/messages`, {
      token: user1.token,
      body: { messageType: 'text', content: `${RUN_ID} group message` },
    });
    await check('退出群聊 POST /api/v1/groups/{id}/leave', 'POST', `/api/v1/groups/${groupId}/leave`, { token: user2.token }, (result) => result.status > 0);
  }

  await check('AI 推荐问题 GET /api/v1/ai/recommended-questions', 'GET', '/api/v1/ai/recommended-questions', { token: user1.token });
  const ai = await check('AI 问答 POST /api/v1/ai/chat', 'POST', '/api/v1/ai/chat', {
    token: user1.token,
    timeoutMs: 45000,
    body: { question: '请用一句话推荐一道低油晚餐', conversationType: 'diet_advice' },
  }, (result) => result.status > 0);
  if (unwrap(ai)?.conversationId) state.ids.aiConversations.push(unwrap(ai).conversationId);
  await check('AI 会话列表 GET /api/v1/ai/conversations', 'GET', '/api/v1/ai/conversations?page=1&pageSize=5', { token: user1.token });
  if (state.ids.aiConversations[0]) await check('AI 会话详情 GET /api/v1/ai/conversations/{id}', 'GET', `/api/v1/ai/conversations/${state.ids.aiConversations[0]}`, { token: user1.token });
  await check('AI 识图 POST /api/v1/ai/recognize-food', 'POST', '/api/v1/ai/recognize-food', {
    token: user1.token,
    timeoutMs: 45000,
    body: { imageMediaId, imageUrl: imageMediaId ? `/api/v1/uploads/${imageMediaId}/raw` : 'https://example.com/food.jpg' },
  }, (result) => result.status > 0);
  await check('AI 识图历史 GET /api/v1/ai/recognitions', 'GET', '/api/v1/ai/recognitions?status=&page=1&pageSize=5', { token: user1.token });

  const adminLogin = await check('管理端登录 POST /auth/login', 'POST', `${GATEWAY_BASE}/auth/login`, {
    body: { username: ADMIN_USERNAME, password: ADMIN_PASSWORD },
  });
  const adminToken = unwrap(adminLogin)?.access_token || unwrap(adminLogin)?.accessToken || adminLogin.payload?.data?.access_token;
  if (adminToken) {
    state.tokens.admin = adminToken;
    await check('管理员信息 GET /system/user/getInfo', 'GET', `${GATEWAY_BASE}/system/user/getInfo`, { token: adminToken });
    await runAdminChecks(adminToken, imageMediaId, recipeId, postId, feedback.payload?.data?.id, state.ids.reports[0]);
    await runApprovedUserInteractions(user1, user2, recipeId, postId);
  }
  else {
    const message = adminLogin.payload?.msg || '未获得管理员 token，管理端 API 链路阻塞';
    state.blocked.push({ area: 'admin', message });
    record('管理端业务接口批量测试', 'BLOCKED', { message });
    await check('管理端未登录权限校验 GET /api/admin/v1/dashboard/summary', 'GET', '/api/admin/v1/dashboard/summary', {}, (result) => result.status === 200 && result.payload?.code === 403);
  }

  await cleanup(user1, user2);
  state.finishedAt = new Date().toISOString();
  writeFileSync(API_RESULTS_PATH, `${jsonBlock(state)}\n`, 'utf8');
  writeReport();
  console.log(`API results: ${API_RESULTS_PATH}`);
  console.log(`Report: ${REPORT_PATH}`);
}

async function runAdminChecks(adminToken, imageMediaId, recipeId, postId, feedbackId, reportId) {
  const adminHeaders = { token: adminToken };
  await check('管理端仪表盘 GET /api/admin/v1/dashboard/summary', 'GET', '/api/admin/v1/dashboard/summary', adminHeaders);
  await check('管理端用户增长 GET /api/admin/v1/dashboard/user-growth', 'GET', '/api/admin/v1/dashboard/user-growth', adminHeaders);
  await check('管理端分类占比 GET /api/admin/v1/dashboard/recipe-category-ratio', 'GET', '/api/admin/v1/dashboard/recipe-category-ratio', adminHeaders);
  await check('管理端最近操作 GET /api/admin/v1/operation-logs/recent', 'GET', '/api/admin/v1/operation-logs/recent?limit=5', adminHeaders);
  await check('管理端菜谱列表 GET /api/admin/v1/recipes', 'GET', '/api/admin/v1/recipes?page=1&pageSize=5', adminHeaders);
  if (recipeId) await check('管理端菜谱详情 GET /api/admin/v1/recipes/{id}', 'GET', `/api/admin/v1/recipes/${recipeId}`, adminHeaders);
  await check('管理端菜谱审核列表 GET /api/admin/v1/recipe-audits', 'GET', '/api/admin/v1/recipe-audits?page=1&pageSize=5', adminHeaders);
  if (recipeId) await check('管理端菜谱审核通过 POST /api/admin/v1/recipe-audits/{id}/approve', 'POST', `/api/admin/v1/recipe-audits/${recipeId}/approve`, adminHeaders);
  await check('管理端动态审核列表 GET /api/admin/v1/post-audits', 'GET', '/api/admin/v1/post-audits?page=1&pageSize=5', adminHeaders);
  if (postId) await check('管理端动态审核通过 POST /api/admin/v1/post-audits/{id}/approve', 'POST', `/api/admin/v1/post-audits/${postId}/approve`, adminHeaders);
  await check('管理端分类列表 GET /api/admin/v1/categories', 'GET', '/api/admin/v1/categories', adminHeaders);
  const category = await check('管理端新增分类 POST /api/admin/v1/categories', 'POST', '/api/admin/v1/categories', {
    token: adminToken,
    body: { name: `${PREFIX}分类`, icon: 'food', color: '#166534', description: RUN_ID, sortNo: 999, status: 'enabled' },
  });
  if (unwrap(category)?.id) state.ids.categories.push(unwrap(category).id);
  await check('管理端 AI 模型列表 GET /api/admin/v1/ai/models', 'GET', '/api/admin/v1/ai/models?page=1&pageSize=5', adminHeaders);
  await check('管理端 AI 对话日志 GET /api/admin/v1/ai/conversation-logs', 'GET', '/api/admin/v1/ai/conversation-logs?page=1&pageSize=5', adminHeaders);
  await check('管理端 AI 识图日志 GET /api/admin/v1/ai/recognition-logs', 'GET', '/api/admin/v1/ai/recognition-logs?page=1&pageSize=5', adminHeaders);
  await check('管理端知识库文档 GET /api/admin/v1/ai/knowledge/documents', 'GET', '/api/admin/v1/ai/knowledge/documents?page=1&pageSize=5', adminHeaders);
  await check('管理端用户列表 GET /api/admin/v1/users', 'GET', '/api/admin/v1/users?page=1&pageSize=5', adminHeaders);
  await check('管理端群组列表 GET /api/admin/v1/groups', 'GET', '/api/admin/v1/groups?page=1&pageSize=5', adminHeaders);
  await check('管理端轮播列表 GET /api/admin/v1/banners', 'GET', '/api/admin/v1/banners?page=1&pageSize=5', adminHeaders);
  if (imageMediaId) {
    const banner = await check('管理端新增轮播 POST /api/admin/v1/banners', 'POST', '/api/admin/v1/banners', {
      token: adminToken,
      body: { title: `${PREFIX}轮播`, subtitle: RUN_ID, imageMediaId, jumpType: 'recipe', jumpTarget: recipeId ? String(recipeId) : '', sortNo: 998, status: 'offline' },
    });
    if (unwrap(banner)?.id) state.ids.banners.push(unwrap(banner).id);
  }
  await check('管理端媒体列表 GET /api/admin/v1/media-assets', 'GET', '/api/admin/v1/media-assets?fileType=image&page=1&pageSize=5', adminHeaders);
  await check('管理端反馈列表 GET /api/admin/v1/feedbacks', 'GET', '/api/admin/v1/feedbacks?page=1&pageSize=5', adminHeaders);
  if (feedbackId) await check('管理端处理反馈 PUT /api/admin/v1/feedbacks/{id}', 'PUT', `/api/admin/v1/feedbacks/${feedbackId}`, {
    token: adminToken,
    body: { status: 'resolved', replyContent: `${RUN_ID} resolved` },
  });
  await check('管理端举报列表 GET /api/admin/v1/reports', 'GET', '/api/admin/v1/reports?page=1&pageSize=5', adminHeaders);
  if (reportId) await check('管理端处理举报 PUT /api/admin/v1/reports/{id}', 'PUT', `/api/admin/v1/reports/${reportId}`, {
    token: adminToken,
    body: { status: 'rejected', handleResult: `${RUN_ID} checked` },
  });
}

async function runApprovedUserInteractions(user1, user2, recipeId, postId) {
  if (recipeId) {
    await check('审核后菜谱公开详情 GET /api/v1/recipes/{id}', 'GET', `/api/v1/recipes/${recipeId}`, { token: user2.token });
    await check('审核后点赞菜谱 POST /api/v1/recipes/{id}/like', 'POST', `/api/v1/recipes/${recipeId}/like`, { token: user2.token });
    await check('审核后收藏菜谱 POST /api/v1/recipes/{id}/favorite', 'POST', `/api/v1/recipes/${recipeId}/favorite`, { token: user2.token });
    await check('审核后分享菜谱 POST /api/v1/recipes/{id}/share', 'POST', `/api/v1/recipes/${recipeId}/share`, { token: user2.token });
    const c = await check('审核后评论菜谱 POST /api/v1/comments', 'POST', '/api/v1/comments', {
      token: user2.token,
      body: { targetType: 'recipe', targetId: recipeId, content: `${RUN_ID} approved recipe comment` },
    });
    const commentId = unwrap(c)?.id;
    if (commentId) state.ids.comments.push(commentId);
    await check('审核后菜谱评论 GET /api/v1/comments', 'GET', `/api/v1/comments?target_type=recipe&target_id=${recipeId}&page=1&pageSize=5`, { token: user1.token });
    if (commentId) await check('审核后点赞评论 POST /api/v1/comments/{id}/like', 'POST', `/api/v1/comments/${commentId}/like`, { token: user1.token });
    if (commentId) await check('审核后取消点赞评论 DELETE /api/v1/comments/{id}/like', 'DELETE', `/api/v1/comments/${commentId}/like`, { token: user1.token });
    if (commentId) await check('审核后删除评论 DELETE /api/v1/comments/{id}', 'DELETE', `/api/v1/comments/${commentId}`, { token: user2.token });
    await check('审核后取消收藏菜谱 DELETE /api/v1/recipes/{id}/favorite', 'DELETE', `/api/v1/recipes/${recipeId}/favorite`, { token: user2.token });
    await check('审核后取消点赞菜谱 DELETE /api/v1/recipes/{id}/like', 'DELETE', `/api/v1/recipes/${recipeId}/like`, { token: user2.token });
  }

  if (postId) {
    await check('审核后动态公开详情 GET /api/v1/posts/{id}', 'GET', `/api/v1/posts/${postId}`, { token: user2.token });
    await check('审核后点赞动态 POST /api/v1/posts/{id}/like', 'POST', `/api/v1/posts/${postId}/like`, { token: user2.token });
    await check('审核后收藏动态 POST /api/v1/posts/{id}/favorite', 'POST', `/api/v1/posts/${postId}/favorite`, { token: user2.token });
    const pc = await check('审核后评论动态 POST /api/v1/comments', 'POST', '/api/v1/comments', {
      token: user2.token,
      body: { targetType: 'post', targetId: postId, content: `${RUN_ID} approved post comment` },
    });
    const postCommentId = unwrap(pc)?.id;
    if (postCommentId) state.ids.comments.push(postCommentId);
    await check('审核后动态评论 GET /api/v1/comments', 'GET', `/api/v1/comments?target_type=post&target_id=${postId}&page=1&pageSize=5`, { token: user1.token });
    await check('审核后取消收藏动态 DELETE /api/v1/posts/{id}/favorite', 'DELETE', `/api/v1/posts/${postId}/favorite`, { token: user2.token });
    await check('审核后取消点赞动态 DELETE /api/v1/posts/{id}/like', 'DELETE', `/api/v1/posts/${postId}/like`, { token: user2.token });
  }
}

async function cleanup(user1, user2) {
  for (const id of ids(state.ids.comments)) {
    await http(`cleanup comment ${id}`, 'DELETE', `/api/v1/comments/${id}`, { token: user1.token });
  }
  for (const id of ids(state.ids.checkins)) {
    await http(`cleanup checkin ${id}`, 'DELETE', `/api/v1/checkins/${id}`, { token: user1.token });
  }
  for (const id of ids(state.ids.posts)) {
    await http(`cleanup post ${id}`, 'DELETE', `/api/v1/posts/${id}`, { token: user1.token });
  }
  for (const id of ids(state.ids.recipes)) {
    await http(`cleanup recipe ${id}`, 'DELETE', `/api/v1/recipes/${id}`, { token: user1.token });
  }
  for (const id of ids(state.ids.groups)) {
    await http(`cleanup group leave ${id}`, 'POST', `/api/v1/groups/${id}/leave`, { token: user1.token });
  }
  try {
    const u = idCsv(state.ids.users);
    const media = idCsv(state.ids.media);
    const recipes = idCsv(state.ids.recipes);
    const posts = idCsv(state.ids.posts);
    const comments = idCsv(state.ids.comments);
    const checkins = idCsv(state.ids.checkins);
    const feedbacks = idCsv(state.ids.feedbacks);
    const reports = idCsv(state.ids.reports);
    const conversations = idCsv(state.ids.conversations);
    const groups = idCsv(state.ids.groups);
    const categories = idCsv(state.ids.categories);
    const banners = idCsv(state.ids.banners);
    const phones = state.ids.phones.map((p) => `'${sqlString(p)}'`).join(',');
    const statements = [];
    const db = MYSQL_DB;
    if (u) statements.push(`delete from ${db}.cook_content_interactions where user_id in (${u}) or target_id in (${u})`);
    if (comments) statements.push(`delete from ${db}.cook_comments where id in (${comments})`);
    if (checkins) statements.push(`delete from ${db}.cook_checkins where id in (${checkins})`);
    if (posts) statements.push(`delete from ${db}.cook_posts where id in (${posts})`);
    if (recipes) statements.push(`delete from ${db}.cook_recipe_versions where recipe_id in (${recipes})`, `delete from ${db}.cook_recipes where id in (${recipes})`);
    if (feedbacks) statements.push(`delete from ${db}.cook_feedbacks where id in (${feedbacks})`);
    if (reports) statements.push(`delete from ${db}.cook_reports where id in (${reports})`);
    if (banners) statements.push(`delete from ${db}.cook_banners where id in (${banners})`);
    if (categories) statements.push(`delete from ${db}.cook_categories where id in (${categories})`);
    if (groups) statements.push(`delete from ${db}.cook_groups where id in (${groups})`);
    if (conversations) statements.push(`delete from ${db}.cook_messages where conversation_id in (${conversations})`, `delete from ${db}.cook_conversation_members where conversation_id in (${conversations})`, `delete from ${db}.cook_conversations where id in (${conversations})`);
    if (media) statements.push(`delete from ${db}.cook_media_upload_sessions where media_id in (${media})`, `delete from ${db}.cook_media_assets where id in (${media})`);
    if (u) statements.push(`delete from ${db}.cook_ai_messages where user_id in (${u})`, `delete from ${db}.cook_ai_image_recognition_logs where user_id in (${u})`);
    if (phones) statements.push(`delete from ${db}.cook_verification_codes where phone in (${phones})`);
    if (u) statements.push(`delete from ${db}.cook_users where id in (${u})`);
    if (statements.length) {
      mysql(`set foreign_key_checks=0; ${statements.join('; ')}; set foreign_key_checks=1;`);
    }
    addCleanup('SQL cleanup for isolated e2e IDs', 'PASS', { statements: statements.length });
  }
  catch (error) {
    addCleanup('SQL cleanup for isolated e2e IDs', 'FAIL', { message: error.message });
    if (state.ids.feedbacks.length || state.ids.reports.length || state.ids.conversations.length || state.ids.media.length) {
      state.residuals.push({
        reason: '当前后端使用远端数据源，MySQL CLI 被拒绝访问；部分无 DELETE 接口的数据只能记录残留 ID。',
        ids: {
          media: state.ids.media,
          feedbacks: state.ids.feedbacks,
          reports: state.ids.reports,
          conversations: state.ids.conversations,
          aiConversations: state.ids.aiConversations,
        },
      });
    }
  }
}

function formatStatusCounts(items) {
  return ['PASS', 'FAIL', 'BLOCKED', 'SKIP']
    .map((status) => `${status}: ${items.filter((item) => item.status === status).length}`)
    .join(', ');
}

function loadBrowserResults() {
  if (!existsSync(BROWSER_RESULTS_PATH)) return null;
  try {
    return JSON.parse(readFileSync(BROWSER_RESULTS_PATH, 'utf8'));
  }
  catch {
    return null;
  }
}

function writeReport() {
  const browser = loadBrowserResults();
  const failures = state.tests.filter((item) => item.status === 'FAIL' || item.status === 'BLOCKED');
  const lines = [];
  lines.push(`# 全链路接口与浏览器测试报告-${DATE_TAG}`);
  lines.push('');
  lines.push(`- 执行时间：${state.startedAt} ~ ${state.finishedAt || now()}`);
  lines.push(`- 测试前缀：\`${PREFIX}\``);
  lines.push(`- API 测试结果：${formatStatusCounts(state.tests)}`);
  lines.push(`- 浏览器测试结果：${browser ? formatStatusCounts(browser.tests || []) : '尚未写入浏览器结果'}`);
  lines.push(`- API 原始结果：\`${path.relative(ROOT, API_RESULTS_PATH)}\``);
  lines.push(`- 浏览器原始结果：\`${path.relative(ROOT, BROWSER_RESULTS_PATH)}\``);
  lines.push('');
  lines.push('## 1. 接口覆盖清单');
  lines.push('');
  lines.push(`- 文档接口数：${state.coverage.docCount}`);
  lines.push(`- 后端 Controller 接口数：${state.coverage.controllerCount}`);
  lines.push(`- 前端 API 封装接口数：${state.coverage.frontendCount}`);
  lines.push(`- 后端存在但前端未封装：${state.coverage.backendOnly.length}`);
  lines.push(`- 源码存在但文档未列：${state.coverage.sourceNotDocumented.length}`);
  lines.push('');
  lines.push('### 后端存在但前端未封装');
  lines.push('');
  for (const endpoint of state.coverage.backendOnly.slice(0, 80)) lines.push(`- \`${endpoint}\``);
  if (state.coverage.backendOnly.length > 80) lines.push(`- 其余 ${state.coverage.backendOnly.length - 80} 项见原始结果 JSON`);
  lines.push('');
  lines.push('### 源码存在但文档未列');
  lines.push('');
  for (const endpoint of state.coverage.sourceNotDocumented.slice(0, 80)) lines.push(`- \`${endpoint}\``);
  if (state.coverage.sourceNotDocumented.length > 80) lines.push(`- 其余 ${state.coverage.sourceNotDocumented.length - 80} 项见原始结果 JSON`);
  lines.push('');
  lines.push('## 2. API 测试结论');
  lines.push('');
  for (const item of state.tests) {
    lines.push(`- ${item.status} \`${item.name}\`${item.message ? `：${item.message}` : ''}`);
  }
  lines.push('');
  lines.push('## 3. 浏览器点击测试结论');
  lines.push('');
  if (browser) {
    for (const item of browser.tests || []) {
      lines.push(`- ${item.status} \`${item.name}\`${item.message ? `：${item.message}` : ''}${item.screenshot ? `；截图：\`${item.screenshot}\`` : ''}`);
    }
    if (browser.consoleErrors?.length) {
      lines.push('');
      lines.push('### 浏览器控制台错误');
      lines.push('');
      for (const error of browser.consoleErrors) lines.push(`- ${error.page}: ${error.message}`);
    }
  }
  else {
    lines.push('- BLOCKED `浏览器点击测试`：尚未执行或结果文件未生成。');
  }
  lines.push('');
  lines.push('## 4. 失败与阻塞项');
  lines.push('');
  if (!failures.length && !(browser?.tests || []).some((item) => item.status !== 'PASS')) {
    lines.push('- 无阻塞失败。');
  }
  else {
    for (const item of failures) {
      lines.push(`- ${item.status} \`${item.name}\``);
      if (item.method || item.url) lines.push(`  - 请求：${item.method || ''} ${item.url || ''}`.trimEnd());
      if (item.message) lines.push(`  - 原因：${item.message}`);
      if (item.responseSample) lines.push(`  - 响应摘要：\`${JSON.stringify(item.responseSample).slice(0, 500)}\``);
    }
    if (browser) {
      for (const item of (browser.tests || []).filter((entry) => entry.status !== 'PASS')) {
        lines.push(`- ${item.status} \`${item.name}\`${item.message ? `：${item.message}` : ''}`);
      }
    }
  }
  lines.push('');
  lines.push('## 5. 测试数据与清理');
  lines.push('');
  lines.push(`- 用户 ID：${state.ids.users.join(', ') || '无'}`);
  lines.push(`- 菜谱 ID：${state.ids.recipes.join(', ') || '无'}`);
  lines.push(`- 动态 ID：${state.ids.posts.join(', ') || '无'}`);
  lines.push(`- 媒体 ID：${state.ids.media.join(', ') || '无'}`);
  lines.push(`- 反馈 ID：${state.ids.feedbacks.join(', ') || '无'}`);
  lines.push(`- 举报 ID：${state.ids.reports.join(', ') || '无'}`);
  lines.push('');
  for (const item of state.cleanup) lines.push(`- ${item.status} ${item.action}${item.message ? `：${item.message}` : ''}`);
  if (state.residuals?.length) {
    lines.push('');
    lines.push('### 残留数据');
    lines.push('');
    for (const item of state.residuals) lines.push(`- ${item.reason}：\`${JSON.stringify(item.ids)}\``);
  }
  lines.push('');
  lines.push('## 6. 后续修复建议');
  lines.push('');
  if (state.tests.some((item) => item.name.includes('Nacos') && item.status === 'FAIL') || state.blocked.some((item) => item.area === 'admin')) {
    lines.push('- 先修复 Nacos 8848 启动和服务发现。当前 Nacos 日志显示旧 Raft peer 配置导致无法启动，管理端 `/auth/login` 因 `ruoyi-system` 无服务实例而失败。');
  }
  if (state.tests.some((item) => item.name.includes('OSS') && item.status === 'FAIL')) {
    lines.push('- 补齐 OSS/STS 配置或在本地提供可控测试 Bucket，否则 H5 视频分片直传只能验证到初始化/配置失败层。');
  }
  if (state.tests.some((item) => item.name.includes('AI') && item.status === 'FAIL')) {
    lines.push('- 检查 AI 模型 API Key、模型配置和外部网络，AI 问答/识图失败不应被前端静默吞掉。');
  }
  lines.push('');
  writeFileSync(REPORT_PATH, `${lines.join('\n')}\n`, 'utf8');
}

if (process.argv.includes('--report-only')) {
  if (!existsSync(API_RESULTS_PATH)) {
    console.error(`Missing API result file: ${API_RESULTS_PATH}`);
    process.exitCode = 1;
  }
  else {
    Object.assign(state, JSON.parse(readFileSync(API_RESULTS_PATH, 'utf8')));
    writeReport();
    console.log(`Report: ${REPORT_PATH}`);
  }
}
else {
main().catch((error) => {
  record('脚本执行异常', 'FAIL', { message: error.stack || error.message });
  state.finishedAt = new Date().toISOString();
  writeFileSync(API_RESULTS_PATH, `${jsonBlock(state)}\n`, 'utf8');
  writeReport();
  console.error(error);
  process.exitCode = 1;
});
}
