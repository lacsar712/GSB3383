<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>菜单 - 在线订餐系统</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
    <link href="${pageContext.request.contextPath}/assets/css/theme.css" rel="stylesheet" />
</head>
<body class="page-menu">
<%@ include file="fragments/nav.jspf" %>
<div class="container pb-5">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="mb-0">菜品菜单</h4>
    </div>
    <form id="searchForm" data-testid="menu-search-form" class="row g-2 mb-3">
        <div class="col-md-6">
            <input id="searchInput" data-testid="menu-search-input" class="form-control" placeholder="按菜品名称搜索" />
        </div>
        <div class="col-auto">
            <button data-testid="menu-search-submit" class="btn btn-primary" type="submit">搜索</button>
        </div>
        <div class="col-auto">
            <button data-testid="menu-search-clear" class="btn btn-outline-secondary" type="button" id="clearSearchBtn">清空</button>
        </div>
    </form>
    <div id="errorBox" data-testid="menu-error" class="alert alert-danger d-none"></div>
    <div id="successBox" data-testid="menu-success" class="alert alert-success d-none"></div>
    <div id="menuList" data-testid="menu-list" class="row g-3"></div>
</div>
<script>
    const ctx = '${pageContext.request.contextPath}';
    const errorBox = document.getElementById('errorBox');
    const successBox = document.getElementById('successBox');
    const menuList = document.getElementById('menuList');
    const searchInput = document.getElementById('searchInput');

    function resolveImageUrl(rawUrl) {
        const url = (rawUrl || '').trim();
        if (!url) {
            return '';
        }
        if (/^https?:\/\//i.test(url) || url.startsWith('data:') || url.startsWith('blob:')) {
            return url;
        }
        if (url.startsWith('/')) {
            return `\${ctx}\${url}`;
        }
        return `\${ctx}/\${url.replace(/^\.?\//, '')}`;
    }

    function showError(message) {
        successBox.classList.add('d-none');
        errorBox.textContent = message;
        errorBox.classList.remove('d-none');
    }

    function showSuccess(message) {
        errorBox.classList.add('d-none');
        successBox.textContent = message;
        successBox.classList.remove('d-none');
    }

    async function loadMenu() {
        errorBox.classList.add('d-none');
        const q = searchInput.value.trim();
        const query = q ? `?q=\${encodeURIComponent(q)}` : '';
        const response = await fetch(`\${ctx}/api/menu-items\${query}`, { credentials: 'same-origin' });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '加载菜单失败');
            return;
        }
        renderMenu(payload.data || []);
    }

    function renderMenu(items) {
        if (!items.length) {
            menuList.innerHTML = '<div class="col-12"><div class="alert alert-secondary">暂无符合条件的菜品</div></div>';
            return;
        }
        menuList.innerHTML = items.map((item) => {
            const disabledAttr = item.isAvailable ? '' : 'disabled';
            const buttonText = item.isAvailable ? '加入购物车' : '已下架';
            const imageUrl = resolveImageUrl(item.imageUrl);
            return `
            <div class="col-md-4" data-testid="menu-card-\${item.id}">
                <div class="card h-100 shadow-sm">
                    <img src="\${imageUrl}" data-testid="menu-image-\${item.id}" class="card-img-top" style="height: 180px; object-fit: cover;" alt="\${item.name}" />
                    <div class="card-body d-flex flex-column">
                        <h5 class="card-title" data-testid="menu-name-\${item.id}">\${item.name}</h5>
                        <p class="card-text text-muted" data-testid="menu-desc-\${item.id}">\${item.description}</p>
                        <p class="fw-bold mt-auto mb-2" data-testid="menu-price-\${item.id}">¥ \${Number(item.price).toFixed(2)}</p>
                        <button data-testid="menu-add-\${item.id}" class="btn btn-success btn-sm" \${disabledAttr}
                            onclick="addToCart(\${item.id})">
                            \${buttonText}
                        </button>
                    </div>
                </div>
            </div>
        `;
        }).join('');
    }

    async function addToCart(menuItemId) {
        const response = await fetch(`\${ctx}/api/cart/items`, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ menuItemId, quantity: 1 })
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '加入购物车失败');
            return;
        }
        showSuccess('已加入购物车');
    }

    document.getElementById('searchForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        await loadMenu();
    });

    document.getElementById('clearSearchBtn').addEventListener('click', async () => {
        searchInput.value = '';
        await loadMenu();
    });

    window.addToCart = addToCart;
    loadMenu();
</script>
</body>
</html>
