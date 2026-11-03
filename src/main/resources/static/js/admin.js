(function () {
  "use strict";

  const L = window.Lib;
  if (!L.requireAdmin()) return;

  const $ = (id) => document.getElementById(id);

  const META = {
    dashboard: ["数据概览", "图书馆运行状况一目了然"],
    books: ["图书管理", "馆藏图书的增删改查与上下架"],
    categories: ["分类管理", "维护图书分类体系"],
    borrows: ["借阅管理", "全馆借阅记录与归还操作"],
    users: ["读者管理", "账号状态、借阅情况与密码管理"]
  };

  const state = {
    panel: "dashboard",
    categories: [],
    books: { page: 1, size: 10, keyword: "", categoryId: "", status: "" },
    borrows: { page: 1, size: 10, keyword: "", status: "" },
    users: { page: 1, size: 10, keyword: "", role: "", status: "" },
    bookIndex: {},
    categoryIndex: {},
    editingBookId: null,
    editingCategoryId: null,
    resetUserId: null
  };

  init();

  function init() {
    const user = L.getUser() || {};
    $("sideAvatar").textContent = L.initials(user.nickname || user.username || "管");
    $("sideName").textContent = user.nickname || user.username || "管理员";
    $("todayBadge").textContent = new Date().toLocaleDateString("zh-CN", {
      month: "long", day: "numeric", weekday: "long"
    });

    bindNav();
    bindToolbars();
    bindForms();
    bindGlobal();

    const initial = (location.hash || "").replace("#", "");
    switchPanel(META[initial] ? initial : "dashboard", true);
    window.addEventListener("hashchange", () => {
      const p = (location.hash || "").replace("#", "");
      if (META[p] && p !== state.panel) switchPanel(p, true);
    });
  }

  /* ---------------- 导航与布局 ---------------- */

  function bindNav() {
    document.querySelectorAll(".side-link[data-panel]").forEach((btn) => {
      btn.addEventListener("click", () => switchPanel(btn.dataset.panel));
    });
    $("sidebarToggle").addEventListener("click", () => $("sidebar").classList.toggle("open"));
    document.addEventListener("click", (e) => {
      if (window.innerWidth > 860) return;
      const sidebar = $("sidebar");
      if (!sidebar.classList.contains("open")) return;
      if (sidebar.contains(e.target) || $("sidebarToggle").contains(e.target)) return;
      sidebar.classList.remove("open");
    });
    $("sideLogout").addEventListener("click", logout);
  }

  function switchPanel(panel, silentHash) {
    if (!META[panel]) return;
    state.panel = panel;
    $("panelTitle").textContent = META[panel][0];
    $("panelSub").textContent = META[panel][1];
    document.querySelectorAll(".side-link[data-panel]").forEach((b) => {
      b.classList.toggle("active", b.dataset.panel === panel);
    });
    document.querySelectorAll(".admin-panel").forEach((p) => {
      p.classList.toggle("active", p.id === "panel-" + panel);
    });
    if (!silentHash) history.replaceState(null, "", "#" + panel);
    $("sidebar").classList.remove("open");
    loadCurrent();
  }

  function loadCurrent() {
    if (state.panel === "dashboard") loadDashboard();
    else if (state.panel === "books") { ensureCategories(); loadBooks(); }
    else if (state.panel === "categories") loadCategoriesTable();
    else if (state.panel === "borrows") loadBorrows();
    else if (state.panel === "users") loadUsers();
  }

  async function logout() {
    try {
      await L.api("/api/auth/logout", { method: "POST", silent: true });
    } catch (e) { /* 忽略网络异常，本地清理即可 */ }
    L.clearAuth();
    location.href = "login.html";
  }

  /* ---------------- 工具栏 ---------------- */

  function bindToolbars() {
    $("refreshBtn").addEventListener("click", () => {
      loadCurrent();
      L.toast("数据已刷新");
    });

    $("bookKeyword").addEventListener("input", L.debounce((e) => {
      state.books.keyword = e.target.value.trim();
      state.books.page = 1;
      loadBooks();
    }, 400));
    $("bookKeyword").addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        state.books.keyword = e.target.value.trim();
        state.books.page = 1;
        loadBooks();
      }
    });
    $("bookCategory").addEventListener("change", (e) => {
      state.books.categoryId = e.target.value;
      state.books.page = 1;
      loadBooks();
    });
    $("bookStatus").addEventListener("change", (e) => {
      state.books.status = e.target.value;
      state.books.page = 1;
      loadBooks();
    });
    $("addBookBtn").addEventListener("click", () => openBookModal(null));

    $("addCategoryBtn").addEventListener("click", () => openCategoryModal(null));

    $("borrowKeyword").addEventListener("input", L.debounce((e) => {
      state.borrows.keyword = e.target.value.trim();
      state.borrows.page = 1;
      loadBorrows();
    }, 400));
    $("borrowStatus").addEventListener("change", (e) => {
      state.borrows.status = e.target.value;
      state.borrows.page = 1;
      loadBorrows();
    });

    $("userKeyword").addEventListener("input", L.debounce((e) => {
      state.users.keyword = e.target.value.trim();
      state.users.page = 1;
      loadUsers();
    }, 400));
    $("userRole").addEventListener("change", (e) => {
      state.users.role = e.target.value;
      state.users.page = 1;
      loadUsers();
    });
    $("userStatus").addEventListener("change", (e) => {
      state.users.status = e.target.value;
      state.users.page = 1;
      loadUsers();
    });

    $("bookRows").addEventListener("click", onBookAction);
    $("categoryRows").addEventListener("click", onCategoryAction);
    $("borrowRows").addEventListener("click", onBorrowAction);
    $("userRows").addEventListener("click", onUserAction);
  }

  function bindGlobal() {
    document.querySelectorAll("[data-close]").forEach((btn) => {
      btn.addEventListener("click", () => btn.closest(".modal-mask").classList.remove("show"));
    });
    document.querySelectorAll(".modal-mask").forEach((mask) => {
      mask.addEventListener("click", (e) => {
        if (e.target === mask) mask.classList.remove("show");
      });
    });
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        document.querySelectorAll(".modal-mask.show").forEach((m) => m.classList.remove("show"));
      }
    });
  }

  function bindForms() {
    $("bookForm").addEventListener("submit", saveBook);
    $("categoryForm").addEventListener("submit", saveCategory);
    $("resetPasswordForm").addEventListener("submit", submitResetPassword);
  }

  /* ---------------- 数据概览 ---------------- */

  async function loadDashboard() {
    try {
      const res = await L.api("/api/admin/stats");
      const d = res.data || {};
      const totalTrend = (d.trend || []).reduce((sum, t) => sum + Number(t.count || 0), 0);
      $("todayBadge").textContent = `今日借出 ${d.todayBorrows || 0} · 归还 ${d.todayReturns || 0}`;
      $("trendTotal").textContent = `7 日共 ${totalTrend} 册`;

      $("dashStats").innerHTML = [
        { label: "馆藏图书", value: d.totalBooks || 0, foot: `共 ${d.totalCopies || 0} 册 · 可借 ${d.availableCopies || 0} 册`, cls: "" },
        { label: "当前在借", value: d.borrowingCount || 0, foot: `今日新增借出 ${d.todayBorrows || 0} 册`, cls: "blue" },
        { label: "逾期未还", value: d.overdueCount || 0, foot: d.overdueCount > 0 ? "需要及时联系读者催还" : "暂无逾期，状态良好", cls: "red" },
        { label: "注册读者", value: d.readerCount || 0, foot: `今日归还 ${d.todayReturns || 0} 册`, cls: "bronze" }
      ].map((s) => `
        <div class="stat-card ${s.cls}">
          <div class="stat-label">${s.label}</div>
          <div class="stat-value">${s.value}</div>
          <div class="stat-foot">${s.foot}</div>
        </div>`).join("");

      renderTrend(d.trend || []);
      renderCategoryStats(d.categoryStats || []);
      renderHot(d.hotBooks || []);
    } catch (e) {
      $("dashStats").innerHTML = `<div class="card card-pad muted">数据加载失败，请点击右上角刷新重试</div>`;
    }
  }

  function renderTrend(trend) {
    const counts = trend.map((t) => Number(t.count || 0));
    const max = Math.max(1, ...counts);
    $("trendChart").innerHTML = trend.map((t, i) => {
      const c = counts[i];
      const h = c === 0 ? 4 : Math.max(9, Math.round((c / max) * 100));
      return `<div class="chart-col">
        <div class="chart-bar" style="height:${h}%" title="${t.date} 借出 ${c} 册">
          <span class="val">${c}</span>
        </div>
        <div class="chart-label">${t.date}</div>
      </div>`;
    }).join("");
  }

  function renderCategoryStats(list) {
    const max = Math.max(1, ...list.map((c) => Number(c.count || 0)));
    $("catStats").innerHTML = list.length ? list.map((c) => {
      const n = Number(c.count || 0);
      return `<div class="cat-row">
        <span class="name" title="${L.escapeHtml(c.name)}">${L.escapeHtml(c.name)}</span>
        <span class="track"><i style="width:${Math.round((n / max) * 100)}%"></i></span>
        <span class="val">${n}</span>
      </div>`;
    }).join("") : `<div class="empty" style="padding:30px 0"><p class="muted">暂无分类数据</p></div>`;
  }

  function renderHot(list) {
    if (!list.length) {
      $("dashHot").innerHTML = `<div class="empty" style="padding:36px 0"><p class="muted">还没有借阅记录，排行榜将在读者借书后生成</p></div>`;
      return;
    }
    const max = Math.max(1, ...list.map((b) => Number(b.borrowCount || 0)));
    $("dashHot").innerHTML = list.map((b, i) => `
      <div class="rank-item">
        <span class="rank-no">${String(i + 1).padStart(2, "0")}</span>
        <div class="rank-main">
          <div class="rank-title">${L.escapeHtml(b.title)}</div>
          <div class="rank-meta">${L.escapeHtml(b.author || "佚名")}${b.categoryName ? " · " + L.escapeHtml(b.categoryName) : ""}</div>
        </div>
        <div class="rank-bar"><i style="width:${Math.round((Number(b.borrowCount || 0) / max) * 100)}%"></i></div>
        <div class="rank-count"><b>${b.borrowCount || 0}</b> 次</div>
      </div>`).join("");
  }

  /* ---------------- 分类 ---------------- */

  async function ensureCategories(force) {
    if (state.categories.length && !force) return;
    try {
      const res = await L.api("/api/categories", { auth: false, silent: true });
      state.categories = res.data || [];
      fillCategorySelects();
    } catch (e) {
      state.categories = [];
    }
  }

  function fillCategorySelects() {
    const options = state.categories.map((c) =>
      `<option value="${c.id}">${L.escapeHtml(c.name)}</option>`).join("");
    const filter = $("bookCategory");
    const keepFilter = filter.value;
    filter.innerHTML = `<option value="">全部分类</option>` + options;
    filter.value = keepFilter;

    const formSel = $("fCategory");
    const keepForm = formSel.value;
    formSel.innerHTML = `<option value="">请选择分类</option>` + options;
    formSel.value = keepForm;
  }

  async function loadCategoriesTable() {
    try {
      const res = await L.api("/api/categories", { auth: false });
      state.categories = res.data || [];
      fillCategorySelects();
      state.categoryIndex = {};
      $("categoryRows").innerHTML = state.categories.length ? state.categories.map((c) => {
        state.categoryIndex[c.id] = c;
        return `<tr>
          <td><b style="font-family:var(--font-serif)">${L.escapeHtml(c.name)}</b></td>
          <td class="muted">${c.sort == null ? 0 : c.sort}</td>
          <td><span class="badge ${c.bookCount > 0 ? "green" : "gray"} plain">${c.bookCount || 0} 种</span></td>
          <td class="muted" style="max-width:340px">${L.escapeHtml(c.description || "—")}</td>
          <td class="right"><div class="table-actions">
            <button class="btn btn-xs btn-ghost" type="button" data-act="edit-category" data-id="${c.id}">编辑</button>
            <button class="btn btn-xs btn-danger-ghost" type="button" data-act="del-category" data-id="${c.id}">删除</button>
          </div></td>
        </tr>`;
      }).join("") : emptyRow(5, "还没有分类，先新增一个吧");
    } catch (e) {
      $("categoryRows").innerHTML = emptyRow(5, "分类加载失败");
    }
  }

  function openCategoryModal(category) {
    state.editingCategoryId = category ? category.id : null;
    $("categoryModalTitle").textContent = category ? "编辑分类" : "新增分类";
    $("categoryForm").reset();
    $("categoryError").textContent = "";
    if (category) {
      $("cName").value = category.name || "";
      $("cSort").value = category.sort == null ? 0 : category.sort;
      $("cDesc").value = category.description || "";
    } else {
      $("cSort").value = 0;
    }
    L.openModal("categoryModal");
    setTimeout(() => $("cName").focus(), 50);
  }

  async function saveCategory(e) {
    e.preventDefault();
    const btn = $("saveCategoryBtn");
    const name = $("cName").value.trim();
    const error = $("categoryError");
    error.textContent = "";

    if (!name) { error.textContent = "请填写分类名称"; return; }
    if (name.length > 20) { error.textContent = "分类名称不能超过 20 个字符"; return; }

    const body = {
      name: name,
      sort: Number($("cSort").value || 0),
      description: $("cDesc").value.trim() || null
    };

    L.setButtonLoading(btn, true, "保存中…");
    try {
      const res = state.editingCategoryId
        ? await L.api(`/api/admin/categories/${state.editingCategoryId}`, { method: "PUT", body, silent: true })
        : await L.api("/api/admin/categories", { method: "POST", body, silent: true });
      L.toast(res.message || "分类已保存");
      L.closeModal("categoryModal");
      state.categories = [];
      await ensureCategories(true);
      await loadCategoriesTable();
    } catch (err) {
      error.textContent = err.message || "保存失败";
    } finally {
      L.setButtonLoading(btn, false);
    }
  }

  async function onCategoryAction(e) {
    const btn = e.target.closest("button[data-act]");
    if (!btn) return;
    const category = state.categoryIndex[Number(btn.dataset.id)];
    if (!category) return;

    if (btn.dataset.act === "edit-category") {
      openCategoryModal(category);
      return;
    }

    const ok = await L.confirmDialog({
      title: "删除分类",
      message: `确定删除分类「${category.name}」吗？分类下仍有图书时将无法删除。`,
      confirmText: "删除",
      danger: true
    });
    if (!ok) return;
    try {
      const res = await L.api(`/api/admin/categories/${category.id}`, { method: "DELETE", silent: true });
      L.toast(res.message || "分类已删除");
      state.categories = [];
      await ensureCategories(true);
      await loadCategoriesTable();
    } catch (err) {
      L.toast(err.message || "删除失败", "error");
    }
  }

  /* ---------------- 图书 ---------------- */

  async function loadBooks() {
    const s = state.books;
    const params = { page: s.page, size: s.size };
    if (s.keyword) params.keyword = s.keyword;
    if (s.categoryId) params.categoryId = s.categoryId;
    if (s.status !== "") params.status = s.status;

    try {
      const res = await L.api("/api/admin/books" + query(params));
      const data = res.data || {};
      const records = data.records || [];
      state.bookIndex = {};
      $("bookRows").innerHTML = records.length ? records.map((b) => {
        state.bookIndex[b.id] = b;
        return `<tr>
          <td>
            <div class="book-cell">
              ${L.bookCover(b.title, "xs")}
              <div><b>${L.escapeHtml(b.title)}</b><span>${L.escapeHtml(b.author || "佚名")} · ${L.escapeHtml(b.isbn)}</span></div>
            </div>
          </td>
          <td><span class="tag">${L.escapeHtml(b.categoryName || "未分类")}</span></td>
          <td><b style="font-family:var(--font-serif)">${b.availableCopies}</b><span class="muted"> / ${b.totalCopies}</span></td>
          <td class="muted">${b.borrowCount || 0}</td>
          <td class="muted">${L.escapeHtml(b.location || "—")}</td>
          <td>${b.status === 1
            ? `<span class="badge green">在架</span>`
            : `<span class="badge gray">已下架</span>`}</td>
          <td class="right"><div class="table-actions">
            <button class="btn btn-xs btn-ghost" type="button" data-act="edit-book" data-id="${b.id}">编辑</button>
            <button class="btn btn-xs btn-ghost" type="button" data-act="toggle-book" data-id="${b.id}">${b.status === 1 ? "下架" : "上架"}</button>
            <button class="btn btn-xs btn-danger-ghost" type="button" data-act="del-book" data-id="${b.id}">删除</button>
          </div></td>
        </tr>`;
      }).join("") : emptyRow(7, "没有找到符合条件的图书");

      L.renderPagination($("bookPagination"), data.page, data.pages, (p) => {
        state.books.page = p;
        loadBooks();
      });
    } catch (e) {
      $("bookRows").innerHTML = emptyRow(7, "图书加载失败");
    }
  }

  function openBookModal(book) {
    state.editingBookId = book ? book.id : null;
    $("bookModalTitle").textContent = book ? "编辑图书" : "新增图书";
    $("bookForm").reset();
    $("bookError").textContent = "";
    fillCategorySelects();

    if (book) {
      $("fTitle").value = book.title || "";
      $("fIsbn").value = book.isbn || "";
      $("fAuthor").value = book.author || "";
      $("fPublisher").value = book.publisher || "";
      $("fCategory").value = book.categoryId == null ? "" : String(book.categoryId);
      $("fTotal").value = book.totalCopies;
      $("fLocation").value = book.location || "";
      $("fStatus").value = String(book.status == null ? 1 : book.status);
      $("fDesc").value = book.description || "";
    } else {
      $("fStatus").value = "1";
      $("fCategory").value = "";
    }
    L.openModal("bookModal");
    setTimeout(() => $("fTitle").focus(), 50);
  }

  async function saveBook(e) {
    e.preventDefault();
    const btn = $("saveBookBtn");
    const error = $("bookError");
    error.textContent = "";

    const body = {
      isbn: $("fIsbn").value.trim(),
      title: $("fTitle").value.trim(),
      author: $("fAuthor").value.trim(),
      publisher: $("fPublisher").value.trim() || null,
      categoryId: $("fCategory").value ? Number($("fCategory").value) : null,
      totalCopies: Number($("fTotal").value || 0),
      location: $("fLocation").value.trim() || null,
      status: Number($("fStatus").value),
      description: $("fDesc").value.trim() || null
    };

    if (!body.title) { error.textContent = "请填写书名"; return; }
    if (!/^[0-9Xx-]{10,20}$/.test(body.isbn)) { error.textContent = "ISBN 需为 10-20 位数字、X 或连字符"; return; }
    if (!body.author) { error.textContent = "请填写作者"; return; }
    if (!body.categoryId) { error.textContent = "请选择图书分类"; return; }
    if (!body.totalCopies || body.totalCopies < 1 || body.totalCopies > 999) {
      error.textContent = "馆藏册数需在 1-999 之间";
      return;
    }

    L.setButtonLoading(btn, true, "保存中…");
    try {
      const res = state.editingBookId
        ? await L.api(`/api/admin/books/${state.editingBookId}`, { method: "PUT", body, silent: true })
        : await L.api("/api/admin/books", { method: "POST", body, silent: true });
      L.toast(res.message || "图书已保存");
      L.closeModal("bookModal");
      state.categories = [];
      await ensureCategories(true);
      await loadBooks();
    } catch (err) {
      error.textContent = err.message || "保存失败";
    } finally {
      L.setButtonLoading(btn, false);
    }
  }

  async function onBookAction(e) {
    const btn = e.target.closest("button[data-act]");
    if (!btn) return;
    const book = state.bookIndex[Number(btn.dataset.id)];
    if (!book) return;
    const act = btn.dataset.act;

    if (act === "edit-book") {
      openBookModal(book);
      return;
    }

    if (act === "toggle-book") {
      const next = book.status === 1 ? 0 : 1;
      const ok = await L.confirmDialog({
        title: next === 0 ? "下架图书" : "上架图书",
        message: next === 0
          ? `下架《${book.title}》后，读者将无法检索和借阅该书，已借出的记录不受影响。`
          : `确定将《${book.title}》重新上架吗？`,
        confirmText: next === 0 ? "下架" : "上架",
        danger: next === 0
      });
      if (!ok) return;
      try {
        const res = await L.api(`/api/admin/books/${book.id}/status`, { method: "PUT", body: { status: next }, silent: true });
        L.toast(res.message || "状态已更新");
        await loadBooks();
      } catch (err) {
        L.toast(err.message || "操作失败", "error");
      }
      return;
    }

    if (act === "del-book") {
      const ok = await L.confirmDialog({
        title: "删除图书",
        message: `确定删除《${book.title}》吗？存在未归还记录时系统会阻止删除。`,
        confirmText: "删除",
        danger: true
      });
      if (!ok) return;
      try {
        const res = await L.api(`/api/admin/books/${book.id}`, { method: "DELETE", silent: true });
        L.toast(res.message || "图书已删除");
        state.categories = [];
        await ensureCategories(true);
        await loadBooks();
      } catch (err) {
        L.toast(err.message || "删除失败", "error");
      }
    }
  }

  /* ---------------- 借阅 ---------------- */

  async function loadBorrows() {
    const s = state.borrows;
    const params = { page: s.page, size: s.size };
    if (s.keyword) params.keyword = s.keyword;
    if (s.status !== "") params.status = s.status;

    try {
      const res = await L.api("/api/admin/borrows" + query(params));
      const data = res.data || {};
      const records = data.records || [];
      $("borrowTotal").textContent = `共 ${data.total || 0} 条记录`;
      $("borrowRows").innerHTML = records.length ? records.map((r) => {
        const st = L.borrowStatus(r);
        const remain = r.status === 1
          ? `<span class="muted">已归还 ${r.returnTime ? L.formatDate(r.returnTime) : ""}</span>`
          : (Number(r.daysLeft) < 0
            ? `<span style="color:var(--red);font-weight:600">逾期 ${r.overdueDays} 天</span>`
            : `<span class="muted">剩余 ${r.daysLeft} 天</span>`);
        return `<tr>
          <td>
            <div class="book-cell" style="min-width:150px">
              <div class="avatar" style="width:30px;height:30px;font-size:12px">${L.initials(r.nickname || r.username)}</div>
              <div><b>${L.escapeHtml(r.nickname || r.username)}</b><span>@${L.escapeHtml(r.username)}</span></div>
            </div>
          </td>
          <td>
            <div class="book-cell" style="min-width:190px">
              <div><b>${L.escapeHtml(r.bookTitle)}</b><span>${L.escapeHtml(r.isbn || "")}</span></div>
            </div>
          </td>
          <td class="muted">${L.formatDateTime(r.borrowTime)}</td>
          <td class="muted">${L.formatDateTime(r.dueTime)}</td>
          <td class="muted">${r.renewCount || 0}</td>
          <td><span class="badge ${st.cls}">${st.label}</span><div style="font-size:11px;margin-top:3px">${remain}</div></td>
          <td class="right"><div class="table-actions">
            ${r.status !== 1
              ? `<button class="btn btn-xs btn-bronze" type="button" data-act="return" data-id="${r.id}">办理归还</button>`
              : `<span class="muted" style="font-size:12px">—</span>`}
          </div></td>
        </tr>`;
      }).join("") : emptyRow(7, "没有找到符合条件的借阅记录");

      L.renderPagination($("borrowPagination"), data.page, data.pages, (p) => {
        state.borrows.page = p;
        loadBorrows();
      });
    } catch (e) {
      $("borrowRows").innerHTML = emptyRow(7, "借阅记录加载失败");
    }
  }

  async function onBorrowAction(e) {
    const btn = e.target.closest("button[data-act='return']");
    if (!btn) return;
    const id = Number(btn.dataset.id);
    const ok = await L.confirmDialog({
      title: "办理归还",
      message: "确认读者已将图书归还到馆？归还后库存会立即恢复。",
      confirmText: "确认归还"
    });
    if (!ok) return;
    try {
      const res = await L.api(`/api/admin/borrows/${id}/return`, { method: "PUT", silent: true });
      L.toast(res.message || "归还成功");
      await loadBorrows();
    } catch (err) {
      L.toast(err.message || "归还失败", "error");
    }
  }

  /* ---------------- 读者 ---------------- */

  async function loadUsers() {
    const s = state.users;
    const params = { page: s.page, size: s.size };
    if (s.keyword) params.keyword = s.keyword;
    if (s.role) params.role = s.role;
    if (s.status !== "") params.status = s.status;

    try {
      const res = await L.api("/api/admin/users" + query(params));
      const data = res.data || {};
      const records = data.records || [];
      $("userTotal").textContent = `共 ${data.total || 0} 位用户`;
      $("userRows").innerHTML = records.length ? records.map((u) => `
        <tr>
          <td>
            <div class="book-cell" style="min-width:160px">
              <div class="avatar ${u.role === "ADMIN" ? "admin" : ""}">${L.initials(u.nickname || u.username)}</div>
              <div><b>${L.escapeHtml(u.nickname || u.username)}</b><span>@${L.escapeHtml(u.username)}</span></div>
            </div>
          </td>
          <td><span class="badge ${u.role === "ADMIN" ? "blue" : "green"} plain">${u.role === "ADMIN" ? "管理员" : "读者"}</span></td>
          <td class="muted">${L.escapeHtml(u.phone || "—")}</td>
          <td>${u.borrowingCount || 0} 册${u.overdueCount > 0 ? ` <span class="badge red plain" style="margin-left:4px">逾期 ${u.overdueCount}</span>` : ""}</td>
          <td>${u.status === 1 ? `<span class="badge green">正常</span>` : `<span class="badge red">已禁用</span>`}</td>
          <td class="muted">${L.formatDate(u.createTime)}</td>
          <td class="right"><div class="table-actions">
            <button class="btn btn-xs btn-ghost" type="button" data-act="toggle-user" data-id="${u.id}">${u.status === 1 ? "禁用" : "启用"}</button>
            <button class="btn btn-xs btn-ghost" type="button" data-act="reset-pwd" data-id="${u.id}">重置密码</button>
          </div></td>
        </tr>`).join("") : emptyRow(7, "没有找到符合条件的用户");

      state.userIndex = {};
      records.forEach((u) => { state.userIndex[u.id] = u; });

      L.renderPagination($("userPagination"), data.page, data.pages, (p) => {
        state.users.page = p;
        loadUsers();
      });
    } catch (e) {
      $("userRows").innerHTML = emptyRow(7, "用户加载失败");
    }
  }

  async function onUserAction(e) {
    const btn = e.target.closest("button[data-act]");
    if (!btn) return;
    const user = (state.userIndex || {})[Number(btn.dataset.id)];
    if (!user) return;

    if (btn.dataset.act === "toggle-user") {
      const disable = user.status === 1;
      const ok = await L.confirmDialog({
        title: disable ? "禁用账号" : "启用账号",
        message: disable
          ? `禁用「${user.nickname || user.username}」后，该账号将无法登录，已登录的会话立即失效。`
          : `确定恢复「${user.nickname || user.username}」的登录权限吗？`,
        confirmText: disable ? "禁用" : "启用",
        danger: disable
      });
      if (!ok) return;
      try {
        const res = await L.api(`/api/admin/users/${user.id}/status`, {
          method: "PUT", body: { status: disable ? 0 : 1 }, silent: true
        });
        L.toast(res.message || "状态已更新");
        await loadUsers();
      } catch (err) {
        L.toast(err.message || "操作失败", "error");
      }
      return;
    }

    if (btn.dataset.act === "reset-pwd") {
      state.resetUserId = user.id;
      $("resetPasswordForm").reset();
      $("resetError").textContent = "";
      $("resetTargetName").textContent = user.nickname || user.username;
      L.openModal("resetPasswordModal");
      setTimeout(() => $("rNewPassword").focus(), 50);
    }
  }

  async function submitResetPassword(e) {
    e.preventDefault();
    const btn = $("resetPasswordBtn");
    const password = $("rNewPassword").value;
    const error = $("resetError");
    error.textContent = "";
    if (!password || password.length < 6 || password.length > 20) {
      error.textContent = "新密码长度需为 6-20 位";
      return;
    }
    L.setButtonLoading(btn, true, "提交中…");
    try {
      const res = await L.api(`/api/admin/users/${state.resetUserId}/reset-password`, {
        method: "PUT", body: { newPassword: password }, silent: true
      });
      L.toast(res.message || "密码已重置");
      L.closeModal("resetPasswordModal");
      await loadUsers();
    } catch (err) {
      error.textContent = err.message || "重置失败";
    } finally {
      L.setButtonLoading(btn, false);
    }
  }

  /* ---------------- 工具 ---------------- */

  function query(params) {
    const parts = Object.keys(params)
      .filter((k) => params[k] !== "" && params[k] != null)
      .map((k) => encodeURIComponent(k) + "=" + encodeURIComponent(params[k]));
    return parts.length ? "?" + parts.join("&") : "";
  }

  function emptyRow(cols, text) {
    return `<tr><td colspan="${cols}">${L.emptyState(text, "换个筛选条件试试", window.ICONS.search)}</td></tr>`;
  }
})();
