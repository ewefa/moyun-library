/* ============================================================
   墨韵图书馆 · 公共脚本
   ============================================================ */
(function () {
  'use strict';

  const TOKEN_KEY = 'library_token';
  const USER_KEY = 'library_user';

  const ICONS = {
    book: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15Z"/><path d="M4 17.5A2.5 2.5 0 0 1 6.5 15H20"/><path d="M8 6h8"/></svg>',
    search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.2-3.2"/></svg>',
    arrow: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg>',
    close: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M6 6l12 12M18 6 6 18"/></svg>',
    check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m4 12.5 5 5L20 6.5"/></svg>',
    warn: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3 2.5 20h19L12 3Z"/><path d="M12 9v5M12 17.2v.1"/></svg>',
    info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 7.8v.1"/></svg>',
    logout: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5M21 12H9"/></svg>',
    plus: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>',
    edit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>',
    trash: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v5M14 11v5"/></svg>',
    menu: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h16"/></svg>'
  };

  /* ---------- 存储与鉴权 ---------- */
  function getToken() { return localStorage.getItem(TOKEN_KEY) || ''; }
  function getUser() {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch (e) { return null; }
  }
  function setAuth(token, user) {
    localStorage.setItem(TOKEN_KEY, token || '');
    localStorage.setItem(USER_KEY, JSON.stringify(user || null));
  }
  function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  /* ---------- 请求封装 ---------- */
  async function api(path, options) {
    options = options || {};
    const method = options.method || 'GET';
    const auth = options.auth !== false;
    const headers = {};
    if (options.body !== undefined) headers['Content-Type'] = 'application/json; charset=utf-8';
    const token = getToken();
    if (auth && token) headers['Authorization'] = 'Bearer ' + token;

    let resp;
    try {
      resp = await fetch(path, {
        method,
        headers,
        body: options.body === undefined ? undefined : JSON.stringify(options.body)
      });
    } catch (e) {
      if (!options.silent) toast('无法连接服务器，请确认后端已启动', 'error');
      throw e;
    }

    let data;
    try { data = await resp.json(); } catch (e) {
      if (!options.silent) toast('服务器返回异常（HTTP ' + resp.status + '）', 'error');
      throw new Error('bad response');
    }

    if (data.code === 401) {
      clearAuth();
      if (auth) {
        const redirect = encodeURIComponent(location.pathname.split('/').pop() + location.search);
        location.href = 'login.html?redirect=' + redirect;
      }
      const err = new Error(data.message || '请先登录');
      err.code = 401;
      throw err;
    }
    if (data.code !== 200) {
      if (!options.silent) toast(data.message || '操作失败', 'error');
      const err = new Error(data.message || '操作失败');
      err.code = data.code;
      throw err;
    }
    return data;
  }

  /* ---------- 提示 ---------- */
  function toast(message, type) {
    let wrap = document.querySelector('.toast-wrap');
    if (!wrap) {
      wrap = document.createElement('div');
      wrap.className = 'toast-wrap';
      document.body.appendChild(wrap);
    }
    const el = document.createElement('div');
    el.className = 'toast ' + (type === 'error' ? 'error' : type === 'warn' ? 'warn' : '');
    const icon = type === 'error' ? ICONS.warn : type === 'warn' ? ICONS.warn : ICONS.check;
    el.innerHTML = '<span style="display:flex;width:16px;height:16px">' + icon + '</span><span></span>';
    el.lastChild.textContent = message;
    wrap.appendChild(el);
    setTimeout(function () {
      el.classList.add('out');
      setTimeout(function () { el.remove(); }, 220);
    }, 2600);
  }

  /* ---------- 弹窗 ---------- */
  function openModal(id) {
    const el = typeof id === 'string' ? document.getElementById(id) : id;
    if (!el) return;
    el.classList.add('show');
    document.body.classList.add('modal-open');
  }
  function closeModal(id) {
    const el = typeof id === 'string' ? document.getElementById(id) : id;
    if (!el) return;
    el.classList.remove('show');
    if (!document.querySelector('.modal-mask.show')) document.body.classList.remove('modal-open');
  }
  function confirmDialog(opts) {
    opts = opts || {};
    return new Promise(function (resolve) {
      const mask = document.createElement('div');
      mask.className = 'modal-mask show';
      mask.innerHTML =
        '<div class="modal" style="max-width:420px">' +
          '<div class="modal-head"><h3></h3>' +
            '<button class="modal-close" data-cancel type="button">' + ICONS.close + '</button></div>' +
          '<div class="modal-body"><p class="muted" style="font-size:13.5px;line-height:1.8"></p></div>' +
          '<div class="modal-foot">' +
            '<button class="btn btn-ghost" data-cancel type="button">取消</button>' +
            '<button class="btn ' + (opts.danger ? 'btn-danger' : '') + '" data-ok type="button"></button>' +
          '</div>' +
        '</div>';
      mask.querySelector('h3').textContent = opts.title || '确认操作';
      mask.querySelector('.modal-body p').textContent = opts.message || '';
      mask.querySelector('[data-ok]').textContent = opts.confirmText || (opts.danger ? '确认删除' : '确定');
      document.body.appendChild(mask);
      document.body.classList.add('modal-open');

      function done(value) {
        mask.remove();
        if (!document.querySelector('.modal-mask.show')) document.body.classList.remove('modal-open');
        resolve(value);
      }
      mask.querySelectorAll('[data-cancel]').forEach(function (b) {
        b.addEventListener('click', function () { done(false); });
      });
      mask.querySelector('[data-ok]').addEventListener('click', function () { done(true); });
      mask.addEventListener('click', function (e) { if (e.target === mask) done(false); });
    });
  }

  /* ---------- 工具 ---------- */
  function escapeHtml(str) {
    return String(str == null ? '' : str)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }
  function debounce(fn, wait) {
    let timer;
    return function () {
      const args = arguments, ctx = this;
      clearTimeout(timer);
      timer = setTimeout(function () { fn.apply(ctx, args); }, wait || 300);
    };
  }
  function formatDateTime(value) {
    if (!value) return '—';
    return String(value).replace('T', ' ').slice(0, 16);
  }
  function formatDate(value) {
    if (!value) return '—';
    return String(value).replace('T', ' ').slice(0, 10);
  }
  function initials(name) {
    const s = String(name || '读').trim();
    return s ? s.slice(0, 1) : '读';
  }
  function qs(name) {
    return new URLSearchParams(location.search).get(name);
  }
  function setQs(name, value) {
    const url = new URL(location.href);
    if (value === null || value === undefined || value === '') url.searchParams.delete(name);
    else url.searchParams.set(name, value);
    history.replaceState(null, '', url.toString());
  }

  const PALETTE = [
    ['#2d5545', '#14352a'], ['#7a4a35', '#4c2a1d'], ['#3d5470', '#22344a'],
    ['#6b5a3a', '#40341f'], ['#4c5b45', '#2a3525'], ['#6d3f4d', '#432331'],
    ['#455a64', '#26343b'], ['#8a6a3d', '#57401f'], ['#3f5b55', '#21342f'],
    ['#5b4a68', '#33293c']
  ];
  function paletteFor(text) {
    let hash = 0;
    const s = String(text || '');
    for (let i = 0; i < s.length; i++) hash = (hash * 31 + s.charCodeAt(i)) >>> 0;
    return PALETTE[hash % PALETTE.length];
  }
  function bookCover(title, cls) {
    const p = paletteFor(title);
    return '<div class="book-cover ' + (cls || '') + '" style="--c1:' + p[0] + ';--c2:' + p[1] + '">' +
      '<span>' + escapeHtml(String(title || '书').slice(0, 6)) + '</span></div>';
  }

  function borrowStatus(record) {
    if (record.status === 1) return { label: '已归还', cls: 'gray' };
    if (record.status === 2 || (record.status === 0 && record.overdueDays > 0)) {
      return { label: '已逾期', cls: 'red' };
    }
    return { label: '借阅中', cls: 'green' };
  }

  /* ---------- 导航 ---------- */
  function navHtml(active) {
    const user = getUser();
    const links = [
      { href: 'index.html', key: 'home', text: '首页' },
      { href: 'books.html', key: 'books', text: '馆藏图书' },
      { href: 'my.html', key: 'my', text: '我的借阅' }
    ];
    if (user && user.role === 'ADMIN') links.push({ href: 'admin.html', key: 'admin', text: '管理后台' });
    const linkHtml = links.map(function (l) {
      return '<a href="' + l.href + '" class="' + (active === l.key ? 'active' : '') + '">' + l.text + '</a>';
    }).join('');

    let right;
    if (user) {
      right =
        '<div class="user-chip" id="userChip">' +
          '<div class="avatar ' + (user.role === 'ADMIN' ? 'admin' : '') + '">' + escapeHtml(initials(user.nickname || user.username)) + '</div>' +
          '<div class="u-meta"><div class="u-name">' + escapeHtml(user.nickname || user.username) + '</div>' +
          '<div class="u-role">' + (user.role === 'ADMIN' ? '管理员' : '读者') + '</div></div>' +
          '<div class="user-menu" id="userMenu">' +
            '<a href="my.html">个人中心</a>' +
            (user.role === 'ADMIN' ? '<a href="admin.html">管理后台</a>' : '') +
            '<div class="divider"></div>' +
            '<button type="button" id="navLogout">' + ICONS.logout + '<span>退出登录</span></button>' +
          '</div>' +
        '</div>';
    } else {
      right = '<a href="login.html" class="btn btn-sm">登录 / 注册</a>';
    }

    return '<div class="nav-inner">' +
      '<a class="brand" href="index.html">' + ICONS.book + '<span>墨韵图书馆<small>MOYUN LIBRARY</small></span></a>' +
      '<nav class="nav-links">' + linkHtml + '</nav>' +
      '<div class="nav-right">' + right + '</div>' +
    '</div>';
  }

  function mountNav(active) {
    const el = document.getElementById('navbar');
    if (!el) return;
    el.className = 'nav';
    el.innerHTML = navHtml(active);
    const chip = document.getElementById('userChip');
    const menu = document.getElementById('userMenu');
    if (chip && menu) {
      chip.addEventListener('click', function (e) {
        if (e.target.closest('#navLogout')) return;
        menu.classList.toggle('show');
        e.stopPropagation();
      });
      document.addEventListener('click', function () { menu.classList.remove('show'); });
    }
    const logout = document.getElementById('navLogout');
    if (logout) {
      logout.addEventListener('click', async function () {
        try { await api('/api/auth/logout', { method: 'POST', silent: true }); } catch (e) { /* ignore */ }
        clearAuth();
        location.href = 'index.html';
      });
    }
  }

  /* ---------- 路由保护 ---------- */
  function requireLogin() {
    if (getToken() && getUser()) return true;
    const redirect = encodeURIComponent(location.pathname.split('/').pop() + location.search);
    location.href = 'login.html?redirect=' + redirect;
    return false;
  }
  function requireAdmin() {
    const user = getUser();
    if (!getToken() || !user) {
      const redirect = encodeURIComponent(location.pathname.split('/').pop() + location.search);
      location.href = 'login.html?redirect=' + redirect;
      return false;
    }
    if (user.role !== 'ADMIN') {
      location.href = 'index.html';
      return false;
    }
    return true;
  }

  /* ---------- 分页 ---------- */
  function renderPagination(container, page, pages, onChange) {
    if (!container) return;
    container.innerHTML = '';
    if (!pages || pages <= 1) return;
    const frag = document.createDocumentFragment();
    function add(label, target, opts) {
      opts = opts || {};
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'page-btn' + (opts.active ? ' active' : '');
      b.innerHTML = label;
      b.disabled = !!opts.disabled;
      if (!opts.disabled && !opts.active) b.addEventListener('click', function () { onChange(target); });
      frag.appendChild(b);
    }
    add('上一页', page - 1, { disabled: page <= 1 });
    const start = Math.max(1, Math.min(page - 2, pages - 4));
    const end = Math.min(pages, start + 4);
    for (let i = start; i <= end; i++) add(String(i), i, { active: i === page });
    add('下一页', page + 1, { disabled: page >= pages });
    const info = document.createElement('span');
    info.className = 'page-info';
    info.textContent = '共 ' + pages + ' 页';
    frag.appendChild(info);
    container.appendChild(frag);
  }

  function emptyState(title, desc, icon) {
    return '<div class="empty"><div class="empty-icon">' + (icon || ICONS.book) + '</div>' +
      '<h4>' + escapeHtml(title) + '</h4><p>' + escapeHtml(desc || '') + '</p></div>';
  }

  function setButtonLoading(btn, loading, loadingText) {
    if (!btn) return;
    if (loading) {
      btn.dataset.original = btn.innerHTML;
      btn.disabled = true;
      btn.innerHTML = '<span class="btn-spin"></span>' + (loadingText || '处理中…');
    } else {
      btn.disabled = false;
      if (btn.dataset.original) btn.innerHTML = btn.dataset.original;
    }
  }

  window.ICONS = ICONS;
  window.Lib = {
    getToken, getUser, setAuth, clearAuth, api, toast,
    openModal, closeModal, confirmDialog,
    escapeHtml, debounce, formatDateTime, formatDate, initials, qs, setQs,
    paletteFor, bookCover, borrowStatus,
    mountNav, requireLogin, requireAdmin, renderPagination, emptyState, setButtonLoading
  };
  window.api = api;
})();
