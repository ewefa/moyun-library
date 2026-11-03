(function () {
  'use strict';
  const { api, setAuth, getUser, toast } = window.Lib;

  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');
  const tabLogin = document.getElementById('tabLogin');
  const tabRegister = document.getElementById('tabRegister');
  const authTitle = document.getElementById('authTitle');
  const authSub = document.getElementById('authSub');
  const loginError = document.getElementById('loginError');
  const registerError = document.getElementById('registerError');

  // 已登录则直接跳转
  const existing = getUser();
  if (existing && window.Lib.getToken()) {
    const redirect = window.Lib.qs('redirect');
    location.replace(redirect || (existing.role === 'ADMIN' ? 'admin.html' : 'index.html'));
    return;
  }

  function switchTab(mode) {
    const login = mode === 'login';
    tabLogin.classList.toggle('active', login);
    tabRegister.classList.toggle('active', !login);
    loginForm.style.display = login ? '' : 'none';
    registerForm.style.display = login ? 'none' : '';
    authTitle.textContent = login ? '欢迎回来' : '加入墨韵图书馆';
    authSub.textContent = login ? '登录后即可借阅图书、查看借阅记录' : '注册一个读者账号，开启你的阅读清单';
    document.getElementById('demoAccounts').style.display = login ? '' : 'none';
    if (!login) document.getElementById('regUsername').focus();
  }
  tabLogin.addEventListener('click', function () { switchTab('login'); });
  tabRegister.addEventListener('click', function () { switchTab('register'); });

  document.querySelectorAll('#demoAccounts .tag').forEach(function (tag) {
    tag.addEventListener('click', function () {
      document.getElementById('loginUsername').value = tag.dataset.user;
      document.getElementById('loginPassword').value = tag.dataset.pass;
      loginError.textContent = '';
      document.getElementById('loginPassword').focus();
    });
  });

  function setLoading(btn, loading, text) {
    if (loading) {
      btn.dataset.label = btn.textContent;
      btn.textContent = text || '请稍候…';
      btn.disabled = true;
    } else {
      btn.textContent = btn.dataset.label || '登录';
      btn.disabled = false;
    }
  }

  function afterLogin(data) {
    setAuth(data.token, data.user);
    toast('欢迎回来，' + (data.user.nickname || data.user.username));
    const redirect = window.Lib.qs('redirect');
    setTimeout(function () {
      location.href = redirect || (data.user.role === 'ADMIN' ? 'admin.html' : 'index.html');
    }, 350);
  }

  loginForm.addEventListener('submit', async function (e) {
    e.preventDefault();
    loginError.textContent = '';
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;
    if (!username || !password) {
      loginError.textContent = '请输入用户名和密码';
      return;
    }
    const btn = document.getElementById('loginBtn');
    setLoading(btn, true, '登录中…');
    try {
      const res = await api('/api/auth/login', {
        method: 'POST', auth: false, silent: true,
        body: { username: username, password: password }
      });
      afterLogin(res.data);
    } catch (err) {
      loginError.textContent = err.message || '登录失败';
      setLoading(btn, false);
    }
  });

  registerForm.addEventListener('submit', async function (e) {
    e.preventDefault();
    registerError.textContent = '';
    const username = document.getElementById('regUsername').value.trim();
    const nickname = document.getElementById('regNickname').value.trim();
    const phone = document.getElementById('regPhone').value.trim();
    const password = document.getElementById('regPassword').value;
    const confirm = document.getElementById('regConfirm').value;

    if (!/^[A-Za-z0-9_]{4,20}$/.test(username)) {
      registerError.textContent = '用户名需为 4-20 位字母、数字或下划线';
      return;
    }
    if (!nickname) { registerError.textContent = '请输入昵称'; return; }
    if (phone && !/^1[3-9]\d{9}$/.test(phone)) { registerError.textContent = '手机号格式不正确'; return; }
    if (password.length < 6 || password.length > 20) { registerError.textContent = '密码长度需为 6-20 位'; return; }
    if (password !== confirm) { registerError.textContent = '两次输入的密码不一致'; return; }

    const btn = document.getElementById('registerBtn');
    setLoading(btn, true, '注册中…');
    try {
      const res = await api('/api/auth/register', {
        method: 'POST', auth: false, silent: true,
        body: { username: username, nickname: nickname, phone: phone, password: password }
      });
      afterLogin(res.data);
    } catch (err) {
      registerError.textContent = err.message || '注册失败';
      setLoading(btn, false);
    }
  });
})();
