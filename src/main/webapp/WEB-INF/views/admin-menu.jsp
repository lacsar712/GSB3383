<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>后台菜品管理 - 在线订餐系统</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
    <link href="${pageContext.request.contextPath}/assets/css/theme.css" rel="stylesheet" />
</head>
<body class="page-admin-menu">
<%@ include file="fragments/nav.jspf" %>
<div class="container pb-5">
    <h4 class="mb-3">后台菜品管理</h4>
    <div id="errorBox" data-testid="admin-menu-error" class="alert alert-danger d-none"></div>
    <div id="successBox" data-testid="admin-menu-success" class="alert alert-success d-none"></div>

    <div class="card shadow-sm mb-4">
        <div class="card-body">
            <h5 class="card-title">新增菜品</h5>
            <form id="createForm" data-testid="admin-menu-create-form" class="row g-2">
                <div class="col-md-2"><input id="newName" data-testid="admin-menu-create-name" class="form-control" placeholder="名称" required /></div>
                <div class="col-md-3"><input id="newDescription" data-testid="admin-menu-create-description" class="form-control" placeholder="描述" required /></div>
                <div class="col-md-2"><input id="newPrice" data-testid="admin-menu-create-price" class="form-control" placeholder="价格" required /></div>
                <div class="col-md-3"><input id="newImageUrl" data-testid="admin-menu-create-image-url" class="form-control" placeholder="图片URL" required /></div>
                <div class="col-md-1 form-check mt-2 ms-2">
                    <input id="newIsAvailable" data-testid="admin-menu-create-available" class="form-check-input" type="checkbox" checked />
                    <label class="form-check-label" for="newIsAvailable">可售</label>
                </div>
                <div class="col-md-1"><button data-testid="admin-menu-create-submit" class="btn btn-primary" type="submit">新增</button></div>
            </form>
        </div>
    </div>

    <div class="table-responsive">
        <table class="table table-bordered align-middle">
            <thead>
            <tr>
                <th>ID</th>
                <th>名称</th>
                <th>描述</th>
                <th>价格</th>
                <th>图片URL</th>
                <th>可售</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody id="menuTableBody" data-testid="admin-menu-table-body"></tbody>
        </table>
    </div>
</div>
<script>
    const ctx = '${pageContext.request.contextPath}';
    const tableBody = document.getElementById('menuTableBody');
    const errorBox = document.getElementById('errorBox');
    const successBox = document.getElementById('successBox');

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
        const response = await fetch(`\${ctx}/api/menu-items`, { credentials: 'same-origin' });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '加载菜品失败');
            return;
        }
        renderRows(payload.data || []);
    }

    function renderRows(items) {
        if (!items.length) {
            tableBody.innerHTML = '<tr><td colspan="7" class="text-center">暂无菜品</td></tr>';
            return;
        }
        tableBody.innerHTML = items.map((item) => `
            <tr data-testid="admin-menu-row-\${item.id}">
                <td data-testid="admin-menu-id-\${item.id}">\${item.id}</td>
                <td><input data-testid="admin-menu-name-\${item.id}" id="name-\${item.id}" class="form-control form-control-sm" value="\${item.name}" /></td>
                <td><input data-testid="admin-menu-desc-\${item.id}" id="desc-\${item.id}" class="form-control form-control-sm" value="\${item.description}" /></td>
                <td><input data-testid="admin-menu-price-\${item.id}" id="price-\${item.id}" class="form-control form-control-sm" value="\${item.price}" /></td>
                <td><input data-testid="admin-menu-image-\${item.id}" id="img-\${item.id}" class="form-control form-control-sm" value="\${item.imageUrl}" /></td>
                <td><input data-testid="admin-menu-avail-\${item.id}" id="avail-\${item.id}" type="checkbox" \${item.isAvailable ? 'checked' : ''} /></td>
                <td><button data-testid="admin-menu-save-\${item.id}" class="btn btn-sm btn-outline-primary" onclick="saveItem(\${item.id})">保存</button></td>
            </tr>
        `).join('');
    }

    async function saveItem(id) {
        const body = {
            name: document.getElementById(`name-\${id}`).value.trim(),
            description: document.getElementById(`desc-\${id}`).value.trim(),
            price: document.getElementById(`price-\${id}`).value.trim(),
            imageUrl: document.getElementById(`img-\${id}`).value.trim(),
            isAvailable: document.getElementById(`avail-\${id}`).checked
        };
        const response = await fetch(`\${ctx}/api/admin/menu-items/\${id}`, {
            method: 'PUT',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '保存失败');
            return;
        }
        showSuccess(`菜品 #\${id} 已更新`);
        await loadMenu();
    }

    document.getElementById('createForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        const body = {
            name: document.getElementById('newName').value.trim(),
            description: document.getElementById('newDescription').value.trim(),
            price: document.getElementById('newPrice').value.trim(),
            imageUrl: document.getElementById('newImageUrl').value.trim(),
            isAvailable: document.getElementById('newIsAvailable').checked
        };
        const response = await fetch(`\${ctx}/api/admin/menu-items`, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '新增失败');
            return;
        }
        showSuccess('新增菜品成功');
        event.target.reset();
        document.getElementById('newIsAvailable').checked = true;
        await loadMenu();
    });

    window.saveItem = saveItem;
    loadMenu();
</script>
</body>
</html>
