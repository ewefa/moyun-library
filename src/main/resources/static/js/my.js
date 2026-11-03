(function () {
  'use strict';
  const Lib = window.Lib;
  const { api, mountNav, getUser, setAuth, clearAuth, escapeHtml, formatDateTime, formatDate,
          renderPagination, openModal, closeModal, confirmDialog, toast, emptyState, bookCover,
          borrowStatus } = Lib;
  const ICONS = window.ICONS;

  if (!Lib.requireLogin()) return;
  mountNav('my');

  const state = {
    status: Lib.qs('status') || '',
    page: Number(Lib.qs('page')) || 1,
    size: 8,
    summary: null
  };

  const listBox = document.getElementById('borrowList');
  const pagination = document.getElementById('pagination');

  function syncUrl() {
    Lib.setQs('status', state.status);
    Lib.setQs('page', state.page > 1 ? state.page : '');
  }

  function applyUser(user) {
    if (!user) return;
    document.getElementById('profileAvatar').textContent = Lib.initials(user.nickname || user.username);
    document.getElementById('profileName').textContent = user.nickname || user.username;
    document.getElementById('profileSub').textContent = '@' + user.username;
    document.getElementById('profileUsername').textContent = user.username;
    document.getElementById('profilePhone').textContent = user.phone || '未填写';
    document.getElementById('profileCreated').textContent = formatDate(user.createTime);
    document.getElementById('profileRole').textContent = user.role === 'ADMIN' ? '管理员' : '读者';
  }

  async function loadMe() {
    try {
      const res = await api('/api/auth/me', { silent: true });
      setAuth(Lib.getToken(), res.data);
      applyUser(res.data);
    } catch (e) {
      const cached = getUser();
      if (cached) applyUser(cached);
    }
  }

  async function loadSummary() {
    try {
      const res = await api('/api/borrows/summary', { silent: true });
      state.summary = res.data;
      const s = res.data;
      document.getElementById('sumBorrowing').textContent = s.borrowingCount;
      document.getElementById('sumLimit').textContent = '/ ' + s.maxBorrowing + ' 册';
      document.getElementById('sumOverdue').textContent = s.overdueCount;
      document.getElementById('sumHistory').textContent = s.historyCount;
      document.getElementById('ruleFoot').textContent = '借期 ' + s.loanDays + ' 天 · 可续借 ' + s.maxRenew + ' 次';
      document.getElementById('overdueFoot').textContent = s.overdueCount > 0 ? '请尽快归还，逾期期间不可借阅' : '暂无逾期，保持下去';
      const normal = Math.max(0, s.borrowingCount - s.overdueCount);
      document.getElementById('countAll').textContent = s.historyCount;
      document.getElementById('countActive').textContent = normal;
      document.getElementById('countOverdue').textContent = s.overdueCount;
      document.getElementById('countReturned').textContent = Math.max(0, s.historyCount - s.borrowingCount);
    } catch (e) { /* 概况失败不影响列表 */ }
  }

  function itemHtml(r) {
    const st = borrowStatus(r);
    const returned = r.status === 1;
    const overdue = !returned && (r.status === 2 || r.overdueDays > 0);
    const maxRenew = state.summary ? state.summary.maxRenew : 2;
    const canRenew = !returned && !overdue && (r.renewCount || 0) < maxRenew;
    const dueClass = returned ? '' : overdue ? 'danger' : (r.daysLeft <= 5 ? 'warn' : '');

    let actions = '';
    if (!returned) {
      if (canRenew) {
        actions += '<button class="btn btn-ghost btn-sm" type="button" data-renew="' + r.id + '">续借</button>';
      }
      actions += '<button class="btn btn-sm ' + (overdue ? 'btn-danger' : '') + '" type="button" data-return="' + r.id + '">归还</button>';
    } else {
      actions += '<span class="muted" style="font-size:12px;text-align:center">已归还<br>' + formatDate(r.returnTime) + '</span>';
    }

    let dueText;
    if (returned) dueText = '已归还 ' + formatDate(r.returnTime);
    else if (overdue) dueText = '已逾期 ' + r.overdueDays + ' 天';
    else dueText = '剩余 ' + r.daysLeft + ' 天';

    return '<div class="borrow-card">' +
      bookCover(r.bookTitle) +
      '<div class="borrow-main">' +
        '<div class="borrow-title">' + escapeHtml(r.bookTitle) + '<span class="badge ' + st.cls + '">' + st.label + '</span></div>' +
        '<div class="borrow-meta">' + escapeHtml(r.bookAuthor || '佚名') + ' · ISBN ' + escapeHtml(r.isbn || '—') +
          (r.renewCount > 0 ? ' · 已续借 ' + r.renewCount + ' 次' : '') + '</div>' +
        '<div class="borrow-dates">' +
          '<div>借出时间<b>' + formatDateTime(r.borrowTime) + '</b></div>' +
          '<div>应还时间<b class="' + dueClass + '">' + formatDate(r.dueTime) + '</b></div>' +
          '<div>状态<b class="' + dueClass + '">' + dueText + '</b></div>' +
        '</div>' +
      '</div>' +
      '<div class="borrow-actions">' + actions + '</div>' +
    '</div>';
  }

  async function loadList() {
    listBox.innerHTML = '<div style="padding:22px"><div class="skeleton" style="height:16px;width:50%;margin-bottom:14px"></div><div class="skeleton" style="height:16px;width:70%"></div></div>';
    try {
      const params = new URLSearchParams();
      params.set('page', state.page);
      params.set('size', state.size);
      if (state.status !== '') params.set('status', state.status);
      const res = await api('/api/borrows/my?' + params.toString(), { silent: true });
      const data = res.data;
      if (!data.records || !data.records.length) {
        listBox.innerHTML = emptyState(
          state.status === '' ? '还没有借阅记录' : '这个分类下暂无记录',
          state.status === '' ? '去馆藏里挑一本喜欢的书吧' : '切换其他标签看看',
          ICONS.book
        );
      } else {
        listBox.innerHTML = data.records.map(itemHtml).join('');
      }
      renderPagination(pagination, state.page, data.pages, function (p) {
        state.page = p;
        syncUrl();
        loadList();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      });
      syncUrl();
    } catch (e) {
      listBox.innerHTML = emptyState('借阅记录加载失败', '请稍后重试', ICONS.warn);
    }
  }

  function bindTabs() {
    document.querySelectorAll('#tabs .tab').forEach(function (tab) {
      tab.classList.toggle('active', tab.dataset.status === state.status);
      tab.addEventListener('click', function () {
        state.status = tab.dataset.status;
        state.page = 1;
        document.querySelectorAll('#tabs .tab').forEach(function (t) {
          t.classList.toggle('active', t === tab);
        });
        syncUrl();
        loadList();
      });
    });
  }

  listBox.addEventListener('click', async function (e) {
    const renewBtn = e.target.closest('[data-renew]');
    if (renewBtn) {
      const id = renewBtn.dataset.renew;
      Lib.setButtonLoading(renewBtn, true, '续借中…');
      try {
        const res = await api('/api/borrows/' + id + '/renew', { method: 'PUT' });
        toast(res.message || '续借成功');
        await loadSummary();
        await loadList();
      } catch (err) {
        Lib.setButtonLoading(renewBtn, false);
      }
      return;
    }
    const returnBtn = e.target.closest('[data-return]');
    if (returnBtn) {
      const ok = await confirmDialog({
        title: '确认归还',
        message: '归还后库存将立即恢复，其他读者可以借阅这本书。请确认你已经把书带回图书馆。',
        confirmText: '确认归还'
      });
      if (!ok) return;
      Lib.setButtonLoading(returnBtn, true, '归还中…');
      try {
        const res = await api('/api/borrows/' + returnBtn.dataset.return + '/return', { method: 'PUT' });
        toast(res.message || '归还成功');
        await loadSummary();
        await loadList();
      } catch (err) {
        Lib.setButtonLoading(returnBtn, false);
      }
    }
  });

  /* ---------- 资料与密码 ---------- */
  document.getElementById('editProfileBtn').addEventListener('click', function () {
    const user = getUser() || {};
    document.getElementById('editNickname').value = user.nickname || '';
    document.getElementById('editPhone').value = user.phone || '';
    document.getElementById('profileError').textContent = '';
    openModal('profileModal');
  });

  document.getElementById('profileForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    const nickname = document.getElementById('editNickname').value.trim();
    const phone = document.getElementById('editPhone').value.trim();
    const error = document.getElementById('profileError');
    if (!nickname) { error.textContent = '请输入昵称'; return; }
    if (phone && !/^1[3-9]\d{9}$/.test(phone)) { error.textContent = '手机号格式不正确'; return; }
    const btn = document.getElementById('saveProfileBtn');
    Lib.setButtonLoading(btn, true, '保存中…');
    try {
      const res = await api('/api/auth/profile', { method: 'PUT', body: { nickname: nickname, phone: phone } });
      setAuth(Lib.getToken(), res.data);
      closeModal('profileModal');
      toast('资料已更新');
      applyUser(res.data);
      mountNav('my');
    } catch (err) {
      error.textContent = err.message || '保存失败';
    } finally {
      Lib.setButtonLoading(btn, false);
    }
  });

  document.getElementById('changePasswordBtn').addEventListener('click', function () {
    document.getElementById('passwordForm').reset();
    document.getElementById('passwordError').textContent = '';
    openModal('passwordModal');
  });

  document.getElementById('passwordForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    const oldPassword = document.getElementById('oldPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirm = document.getElementById('confirmPassword').value;
    const error = document.getElementById('passwordError');
    error.textContent = '';
    if (newPassword.length < 6 || newPassword.length > 20) { error.textContent = '新密码长度需为 6-20 位'; return; }
    if (newPassword !== confirm) { error.textContent = '两次输入的新密码不一致'; return; }
    const btn = document.getElementById('savePasswordBtn');
    Lib.setButtonLoading(btn, true, '提交中…');
    try {
      const res = await api('/api/auth/password', { method: 'PUT', body: { oldPassword: oldPassword, newPassword: newPassword } });
      closeModal('passwordModal');
      toast(res.message || '密码已修改，请重新登录');
      clearAuth();
      setTimeout(function () { location.href = 'login.html'; }, 1200);
    } catch (err) {
      error.textContent = err.message || '修改失败';
      Lib.setButtonLoading(btn, false);
    }
  });

  document.getElementById('logoutBtn').addEventListener('click', async function () {
    try { await api('/api/auth/logout', { method: 'POST', silent: true }); } catch (e) { /* ignore */ }
    clearAuth();
    location.href = 'index.html';
  });

  document.querySelectorAll('[data-close]').forEach(function (b) {
    b.addEventListener('click', function () { closeModal(b.closest('.modal-mask').id); });
  });
  document.querySelectorAll('.modal-mask').forEach(function (mask) {
    mask.addEventListener('click', function (e) { if (e.target === mask) closeModal(mask.id); });
  });

  bindTabs();
  applyUser(getUser());
  loadMe();
  loadSummary().then(loadList);
})();
