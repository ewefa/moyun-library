(function () {
  'use strict';
  const { api, mountNav, bookCover, escapeHtml, emptyState, toast } = window.Lib;

  mountNav('home');
  document.getElementById('heroArrow').innerHTML = window.ICONS.arrow;
  document.getElementById('moreArrow').innerHTML = window.ICONS.arrow;

  let started = false;
  function animateNumber(el, target, suffix) {
    if (!el) return;
    const end = Number(target) || 0;
    const start = performance.now();
    const duration = 700;
    function tick(now) {
      const p = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - p, 3);
      el.textContent = Math.round(end * eased).toLocaleString('zh-CN') + (suffix || '');
      if (p < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }

  function renderStats(stats) {
    const items = [
      { label: '馆藏图书', value: stats.totalBooks, unit: '种', cls: '', foot: '在架可借图书种类' },
      { label: '馆藏册数', value: stats.totalCopies, unit: '册', cls: 'bronze', foot: '含多副本馆藏' },
      { label: '当前在借', value: stats.borrowingCount, unit: '册', cls: 'blue', foot: '读者正在阅读中' },
      { label: '注册读者', value: stats.readerCount, unit: '人', cls: 'red', foot: '有效读者账号' }
    ];
    const box = document.getElementById('homeStats');
    box.innerHTML = items.map(function (it) {
      return '<div class="stat-card ' + it.cls + '">' +
        '<div class="stat-label">' + it.label + '</div>' +
        '<div class="stat-value"><span data-count="' + it.value + '">0</span><small>' + it.unit + '</small></div>' +
        '<div class="stat-foot">' + it.foot + '</div></div>';
    }).join('');
    box.querySelectorAll('[data-count]').forEach(function (el) {
      animateNumber(el, el.dataset.count);
    });
    animateNumber(document.getElementById('heroBooks'), stats.totalBooks);
    animateNumber(document.getElementById('heroCopies'), stats.totalCopies);
    animateNumber(document.getElementById('heroBorrowing'), stats.borrowingCount);
    animateNumber(document.getElementById('heroReaders'), stats.readerCount);
  }

  function renderRank(hotBooks) {
    const box = document.getElementById('rankList');
    if (!hotBooks || !hotBooks.length) {
      box.innerHTML = emptyState('暂无借阅记录', '等第一位读者来点亮榜单');
      return;
    }
    const max = Math.max.apply(null, hotBooks.map(function (b) { return b.borrowCount || 1; }));
    box.innerHTML = hotBooks.map(function (b, i) {
      const pct = Math.max(6, Math.round((b.borrowCount || 0) / max * 100));
      return '<a class="rank-item" href="books.html?id=' + b.bookId + '">' +
        '<div class="rank-no">' + String(i + 1).padStart(2, '0') + '</div>' +
        '<div class="rank-main"><div class="rank-title">' + escapeHtml(b.title) + '</div>' +
        '<div class="rank-meta">' + escapeHtml(b.author || '佚名') + (b.categoryName ? ' · ' + escapeHtml(b.categoryName) : '') + '</div></div>' +
        '<div class="rank-bar"><i style="width:' + pct + '%"></i></div>' +
        '<div class="rank-count"><b>' + (b.borrowCount || 0) + '</b> 次</div></a>';
    }).join('');
    document.getElementById('quoteRecommend').textContent =
      hotBooks[0].title + ' · ' + (hotBooks[0].author || '');
  }

  function renderNewBooks(books) {
    const box = document.getElementById('newBooks');
    if (!books || !books.length) {
      box.innerHTML = emptyState('暂无新书', '新书上架后会出现在这里');
      return;
    }
    box.innerHTML = books.slice(0, 8).map(function (b) {
      return '<a class="book-card" href="books.html?id=' + b.id + '">' +
        '<div class="book-top">' + bookCover(b.title) +
          '<div class="book-info"><div class="book-cat">' + escapeHtml(b.categoryName || '未分类') + '</div>' +
          '<div class="book-title">' + escapeHtml(b.title) + '</div>' +
          '<div class="book-author">' + escapeHtml(b.author || '佚名') + '</div></div></div>' +
        '<div class="book-desc">' + escapeHtml(b.description || '这本书还没有简介，等你来翻阅。') + '</div>' +
        '<div class="book-bottom"><div class="book-stock ' + (b.availableCopies > 0 ? '' : 'none') + '">可借 <b>' + b.availableCopies + '</b> / ' + b.totalCopies + ' 册</div>' +
        '<span class="badge ' + (b.availableCopies > 0 ? 'green' : 'gray') + '">' + (b.availableCopies > 0 ? '可借' : '已借完') + '</span></div></a>';
    }).join('');
  }

  async function load() {
    if (started) return;
    started = true;
    try {
      const res = await api('/api/home', { auth: false, silent: true });
      const data = res.data;
      renderStats(data.stats);
      renderRank(data.hotBooks);
      renderNewBooks(data.newBooks);
    } catch (e) {
      started = false;
      document.getElementById('homeStats').innerHTML = emptyState('数据加载失败', '请确认后端服务已启动后刷新页面', window.ICONS.warn);
      document.getElementById('rankList').innerHTML = emptyState('数据加载失败', '请稍后重试', window.ICONS.warn);
      document.getElementById('newBooks').innerHTML = emptyState('数据加载失败', '请稍后重试', window.ICONS.warn);
      toast('首页数据加载失败，请检查后端服务', 'error');
    }
  }

  load();
})();
