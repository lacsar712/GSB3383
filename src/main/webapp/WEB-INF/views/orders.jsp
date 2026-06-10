<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>我的订单 - 在线订餐系统</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
    <link href="${pageContext.request.contextPath}/assets/css/theme.css" rel="stylesheet" />
</head>
<body class="page-orders">
<%@ include file="fragments/nav.jspf" %>
<div class="container pb-5">
    <h4 class="mb-3">我的订单</h4>
    <div id="errorBox" data-testid="orders-error" class="alert alert-danger d-none"></div>
    <div id="orderList" data-testid="orders-list" class="vstack gap-3"></div>
</div>
<script>
    const ctx = '${pageContext.request.contextPath}';
    const orderList = document.getElementById('orderList');
    const errorBox = document.getElementById('errorBox');

    function showError(message) {
        errorBox.textContent = message;
        errorBox.classList.remove('d-none');
    }

    async function loadOrders() {
        const response = await fetch(`\${ctx}/api/orders?includeItems=true`, { credentials: 'same-origin' });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '加载订单失败');
            return;
        }
        renderOrders(payload.data || []);
    }

    function renderOrders(orders) {
        if (!orders.length) {
            orderList.innerHTML = '<div class="alert alert-secondary">暂无订单</div>';
            return;
        }

        orderList.innerHTML = orders.map((order) => `
            <div class="card shadow-sm" data-testid="order-card-\${order.id}">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h5 class="card-title mb-0" data-testid="order-title-\${order.id}">订单 #\${order.id}</h5>
                        <span class="badge text-bg-primary" data-testid="order-status-\${order.id}">\${order.status}</span>
                    </div>
                    <div class="small text-muted mb-2" data-testid="order-time-\${order.id}">下单时间：\${order.createdAt} | 更新时间：\${order.updatedAt}</div>
                    <div class="mb-2" data-testid="order-contact-\${order.id}">联系人：\${order.contactName}，电话：\${order.contactPhone}</div>
                    <div class="mb-2" data-testid="order-address-\${order.id}">地址：\${order.deliveryAddress}</div>
                    <div class="mb-2">
                        \${(order.items || []).map((item) =>
                            `<div class="small" data-testid="order-item-\${order.id}-\${item.id}">\${item.itemNameSnapshot} × \${item.quantity} = ¥ \${Number(item.lineTotal).toFixed(2)}</div>`
                        ).join('')}
                    </div>
                    <div class="fw-bold" data-testid="order-total-\${order.id}">总金额：¥ \${Number(order.totalAmount).toFixed(2)}</div>
                </div>
            </div>
        `).join('');
    }

    loadOrders();
</script>
</body>
</html>
