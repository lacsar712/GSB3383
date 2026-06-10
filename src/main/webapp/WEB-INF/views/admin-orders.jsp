<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>后台订单管理 - 在线订餐系统</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
    <link href="${pageContext.request.contextPath}/assets/css/theme.css" rel="stylesheet" />
</head>
<body class="page-admin-orders">
<%@ include file="fragments/nav.jspf" %>
<div class="container pb-5">
    <h4 class="mb-3">后台订单管理</h4>
    <div id="errorBox" data-testid="admin-orders-error" class="alert alert-danger d-none"></div>
    <div id="successBox" data-testid="admin-orders-success" class="alert alert-success d-none"></div>

    <div class="row g-2 mb-3">
        <div class="col-md-3">
            <select id="statusFilter" data-testid="admin-orders-filter" class="form-select">
                <option value="">全部状态</option>
                <option value="PLACED">PLACED</option>
                <option value="CONFIRMED">CONFIRMED</option>
                <option value="COMPLETED">COMPLETED</option>
                <option value="CANCELED">CANCELED</option>
            </select>
        </div>
        <div class="col-auto">
            <button id="filterBtn" data-testid="admin-orders-filter-submit" class="btn btn-primary">筛选</button>
        </div>
    </div>

    <div id="orderList" data-testid="admin-orders-list" class="vstack gap-3"></div>
</div>
<script>
    const ctx = '${pageContext.request.contextPath}';
    const orderList = document.getElementById('orderList');
    const statusFilter = document.getElementById('statusFilter');
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

    async function loadOrders() {
        const status = statusFilter.value;
        const query = status ? `?status=\${encodeURIComponent(status)}&includeItems=true` : '?includeItems=true';
        const response = await fetch(`\${ctx}/api/admin/orders\${query}`, { credentials: 'same-origin' });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '加载订单失败');
            return;
        }
        renderOrders(payload.data || []);
    }

    function actionButtons(order) {
        const actions = [];
        if (order.status === 'PLACED') {
            actions.push(`<button data-testid="admin-order-confirm-\${order.id}" class="btn btn-sm btn-outline-primary me-1" onclick="updateStatus(\${order.id}, 'CONFIRMED')">确认</button>`);
            actions.push(`<button data-testid="admin-order-cancel-\${order.id}" class="btn btn-sm btn-outline-danger" onclick="updateStatus(\${order.id}, 'CANCELED')">取消</button>`);
        }
        if (order.status === 'CONFIRMED') {
            actions.push(`<button data-testid="admin-order-complete-\${order.id}" class="btn btn-sm btn-outline-success me-1" onclick="updateStatus(\${order.id}, 'COMPLETED')">完成</button>`);
            actions.push(`<button data-testid="admin-order-cancel-\${order.id}" class="btn btn-sm btn-outline-danger" onclick="updateStatus(\${order.id}, 'CANCELED')">取消</button>`);
        }
        return actions.join('');
    }

    function renderOrders(orders) {
        if (!orders.length) {
            orderList.innerHTML = '<div class="alert alert-secondary">暂无订单</div>';
            return;
        }
        orderList.innerHTML = orders.map((order) => `
            <div class="card shadow-sm" data-testid="admin-order-card-\${order.id}">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h5 class="mb-0" data-testid="admin-order-title-\${order.id}">订单 #\${order.id}（用户：\${order.username}）</h5>
                        <span class="badge text-bg-primary" data-testid="admin-order-status-\${order.id}">\${order.status}</span>
                    </div>
                    <div class="small text-muted mb-2" data-testid="admin-order-time-\${order.id}">创建：\${order.createdAt} | 更新：\${order.updatedAt}</div>
                    <div class="mb-2">联系人：\${order.contactName}，电话：\${order.contactPhone}</div>
                    <div class="mb-2">地址：\${order.deliveryAddress}</div>
                    <div class="mb-2">
                        \${(order.items || []).map((item) =>
                            `<div class="small">\${item.itemNameSnapshot} × \${item.quantity} = ¥ \${Number(item.lineTotal).toFixed(2)}</div>`
                        ).join('')}
                    </div>
                    <div class="fw-bold mb-2" data-testid="admin-order-total-\${order.id}">总金额：¥ \${Number(order.totalAmount).toFixed(2)}</div>
                    <div>\${actionButtons(order)}</div>
                </div>
            </div>
        `).join('');
    }

    async function updateStatus(orderId, status) {
        const response = await fetch(`\${ctx}/api/admin/orders/\${orderId}/status`, {
            method: 'PUT',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status })
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '状态更新失败');
            return;
        }
        showSuccess(`订单 #\${orderId} 状态已更新为 \${status}`);
        await loadOrders();
    }

    document.getElementById('filterBtn').addEventListener('click', loadOrders);
    window.updateStatus = updateStatus;
    loadOrders();
</script>
</body>
</html>
