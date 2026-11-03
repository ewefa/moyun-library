(function () {
  'use strict';
  const Lib = window.Lib;
  const { api, mountNav, bookCover, escapeHtml, emptyState, renderPagination, openModal, closeModal, toast, debounce, getUser } = Lib;

  mountNav('books');
  document.getElementById('searchIcon').outerHTML = window.ICONS.search;

  const state = {
    page: Number(Lib.qs('page')) || 1,
    size: 12,
    keyword: Lib.qs('keyword') || '',
    categoryId: Lib.qs('categoryId') || '',
    sort: Lib.qs('sort') || 'new',
    total: 0,
    pages: 0,
    books: []
  };
  let detailBook = null;

  const grid = document.getElementById('bookGrid');
  const pagination = document.getElementById('pagination');
  const keywordInput = document.getElementById('keyword');
  const categorySelect = document.getElementById('categoryId');
  const sortSelect = document.getElementById('sort');
  const loginTip = document.getElementById('loginTip');

  keywordInput.value = state.keyword;
  sortSelect.value = state.sort;
  if (!getUser()) loginTip.style.display = '';

  function syncUrl() {
    Lib.setQs('keyword', state.keyword);
    Lib.setQs('categoryId', state.categoryId);
    Lib.setQs('sort', state.sort === 'new' ? '' : state.sort);
    Lib.setQs('page', state.page > 1 ? state.page : '');
  }

  function cardHtml(b) {
    const canBorrow = b.availableCopies > 0;
    return '<article class="book-card" data-id="' + b.id + '" tabindex="0">' +
      '<div class="book-top">' + bookCover(b.title) +
        '<div class="book-info"><div class="book-cat">' + escapeHtml(b.categoryName || '未分类') + '</div>' +
        '<div class="book-title">' + escapeHtml(b.title) + '</div>' +
        '<div class="book-author">' + escapeHtml(b.author || '佚名') + '</div>' +
        '<div style="margin-top:7px"><span class="badge ' + (canBorrow ? 'green' : 'gray') + '">' + (canBorrow ? '可借阅' : '已借完') + '</span></div>' +
        '</div></div>' +
      '<div class="book-desc">' + escapeHtml(b.description || '这本书还没有简介，等你来翻阅。') + '</div>' +
      '<div class="book-bottom">' +
        '<div class="book-stock ' + (canBorrow ? '' : 'none') + '">可借 <b>' + b.availableCopies + '</b> / ' + b.totalCopies + ' 册</div>' +
        '<button class="btn btn-xs ' + (canBorrow ? '' : 'btn-ghost') + '" type="button" data-borrow="' + b.id + '" ' + (canBorrow ? '' : 'disabled') + '>' + (canBorrow ? '借阅' : '暂无库存') + '</button>' +
      '</div></article>';
  }

  async function loadCategories() {
    try {
      const res = await api('/api/categories', { auth: false, silent: true });
      const options = res.data.map(function (c) {
        return '<option value="' + c.id + '">' + escapeHtml(c.name) + ' (' + c.bookCount + ')</option>';
      }).join('');
      categorySelect.innerHTML = '<option value="">全部分类</option>' + options;
      if (state.categoryId) categorySelect.value = state.categoryId;
    } catch (e) { /* 分类加载失败不阻断图书列表 */ }
  }

  async function loadBooks() {
    grid.innerHTML = '<div class="card" style="height:220px;grid-column:1/-1"><div class="skeleton" style="height:100%;border-radius:14px"></div></div>';
    try {
      const params = new URLSearchParams();
      params.set('page', state.page);
      params.set('size', state.size);
      params.set('sort', state.sort);
      if (state.keyword) params.set('keyword', state.keyword);
      if (state.categoryId) params.set('categoryId', state.categoryId);

      const res = await api('/api/books?' + params.toString(), { auth: false, silent: true });
      const data = res.data;
      state.books = data.records || [];
      state.total = data.total;
      state.pages = data.pages;
      document.getElementById('resultCount').textContent = data.total;

      if (!state.books.length) {
        grid.innerHTML = '<div style="grid-column:1/-1">' +
          emptyState('没有找到相关图书', '换个关键词，或清空筛选条件再试试', window.ICONS.search) + '</div>';
      } else {
        grid.innerHTML = state.books.map(cardHtml).join('');
      }
      renderPagination(pagination, state.page, state.pages, function (p) {
        state.page = p;
        syncUrl();
        loadBooks();
        window.scrollTo({ top: 200, behavior: 'smooth' });
      });
      syncUrl();
    } catch (e) {
      grid.innerHTML = '<div style="grid-column:1/-1">' + emptyState('图书加载失败', '请确认后端服务已启动后重试', window.ICONS.warn) + '</div>';
    }
  }

  function search() {
    state.keyword = keywordInput.value.trim();
    state.categoryId = categorySelect.value;
    state.sort = sortSelect.value;
    state.page = 1;
    loadBooks();
  }

  keywordInput.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') { e.preventDefault(); search(); }
  });
  keywordInput.addEventListener('input', debounce(function () {
    state.keyword = keywordInput.value.trim();
    state.page = 1;
    loadBooks();
  }, 450));
  categorySelect.addEventListener('change', search);
  sortSelect.addEventListener('change', search);
  document.getElementById('resetBtn').addEventListener('click', function () {
    keywordInput.value = '';
    categorySelect.value = '';
    sortSelect.value = 'new';
    search();
  });

  /* ---------- 详情 ---------- */
  function renderDetail(b) {
    const canBorrow = b.availableCopies > 0;
    document.getElementById('detailBody').innerHTML =
      '<div style="display:flex;gap:22px;align-items:flex-start">' +
        bookCover(b.title, 'lg') +
        '<div style="flex:1;min-width:0">' +
          '<div class="book-cat">' + escapeHtml(b.categoryName || '未分类') + '</div>' +
          '<h2 style="font-size:24px;color:var(--green-deep);margin-top:4px">' + escapeHtml(b.title) + '</h2>' +
          '<div class="muted" style="font-size:13px;margin-top:6px">' + escapeHtml(b.author || '佚名') +
            (b.publisher ? ' · ' + escapeHtml(b.publisher) : '') + '</div>' +
          '<div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:14px">' +
            '<span class="badge ' + (canBorrow ? 'green' : 'gray') + '">' + (canBorrow ? '可借 ' + b.availableCopies + ' 册' : '暂时借完') + '</span>' +
            '<span class="badge bronze plain">累计借阅 ' + (b.borrowCount || 0) + ' 次</span>' +
            '<span class="badge blue plain">馆藏 ' + b.totalCopies + ' 册</span>' +
          '</div>' +
        '</div>' +
      '</div>' +
      '<div style="margin-top:22px;padding-top:18px;border-top:1px dashed var(--line)">' +
        '<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px 24px;font-size:12.5px">' +
          '<div><span class="muted">ISBN：</span>' + escapeHtml(b.isbn) + '</div>' +
          '<div><span class="muted">馆藏位置：</span>' + escapeHtml(b.location || '—') + '</div>' +
          '<div><span class="muted">上架状态：</span>' + (b.status === 1 ? '在架' : '已下架') + '</div>' +
          '<div><span class="muted">入库时间：</span>' + Lib.formatDate(b.createTime) + '</div>' +
        '</div>' +
        '<p style="margin-top:16px;font-size:13px;color:var(--ink-soft);line-height:1.9">' +
          escapeHtml(b.description || '这本书还没有简介，等你来翻阅。') + '</p>' +
      '</div>';
    const btn = document.getElementById('detailBorrowBtn');
    btn.disabled = !canBorrow;
    btn.textContent = canBorrow ? '借阅这本书' : '暂无库存';
    detailBook = b;
  }

  async function openDetail(id) {
    try {
      const res = await api('/api/books/' + id, { auth: false, silent: true });
      renderDetail(res.data);
      openModal('detailModal');
      Lib.setQs('id', String(id));
    } catch (e) {
      toast(e.message || '图书不存在', 'error');
      Lib.setQs('id', '');
    }
  }

  grid.addEventListener('click', function (e) {
    const borrowBtn = e.target.closest('[data-borrow]');
    if (borrowBtn) {
      e.preventDefault();
      e.stopPropagation();
      borrowBook(Number(borrowBtn.dataset.borrow), borrowBtn);
      return;
    }
    const card = e.target.closest('.book-card');
    if (card) openDetail(Number(card.dataset.id));
  });
  grid.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') {
      const card = e.target.closest('.book-card');
      if (card) openDetail(Number(card.dataset.id));
    }
  });

  document.querySelectorAll('[data-close]').forEach(function (b) {
    b.addEventListener('click', function () { closeModal(b.dataset.close); });
  });
  document.querySelectorAll('.modal-mask').forEach(function (mask) {
    mask.addEventListener('click', function (e) { if (e.target === mask) closeModal(mask.id); });
  });

  async function borrowBook(bookId, btn) {
    if (!Lib.getToken() || !getUser()) {
      location.href = 'login.html?redirect=' + encodeURIComponent('books.html?id=' + bookId);
      return;
    }
    Lib.setButtonLoading(btn, true, '借阅中…');
    try {
      const res = await api('/api/borrows', { method: 'POST', body: { bookId: bookId } });
      toast(res.message || '借阅成功');
      closeModal('detailModal');
      Lib.setQs('id', '');
      loadBooks();
    } catch (e) {
      if (e.code === 400 && e.message && e.message.indexOf('逾期') >= 0) {
        setTimeout(function () { location.href = 'my.html?status=2'; }, 1200);
      }
    } finally {
      Lib.setButtonLoading(btn, false);
    }
  }

  document.getElementById('detailBorrowBtn').addEventListener('click', function () {
    if (!detailBook) return;
    borrowBook(detailBook.id, this);
  });

  loadCategories();
  loadBooks().then(function () {
    const id = Lib.qs('id');
    if (id) openDetail(Number(id));
  });
})();
