<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>购物车 - 在线订餐系统</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
    <link href="${pageContext.request.contextPath}/assets/css/theme.css" rel="stylesheet" />
</head>
<body class="page-cart">
<%@ include file="fragments/nav.jspf" %>
<div class="container pb-5">
    <h4 class="mb-3">购物车</h4>
    <div id="errorBox" data-testid="cart-error" class="alert alert-danger d-none"></div>
    <div id="successBox" data-testid="cart-success" class="alert alert-success d-none"></div>
    <div class="table-responsive mb-3">
        <table class="table table-bordered align-middle">
            <thead>
            <tr>
                <th>菜品</th>
                <th>单价</th>
                <th>数量</th>
                <th>小计</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody id="cartBody" data-testid="cart-body"></tbody>
        </table>
    </div>
    <div class="mb-4 text-end fw-bold">总金额：¥ <span id="totalAmount" data-testid="cart-total">0.00</span></div>

    <div class="card shadow-sm">
        <div class="card-body">
            <h5 class="card-title">提交订单</h5>
            <form id="orderForm" data-testid="order-form" class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">联系人</label>
                    <input id="contactName" data-testid="order-contact-name" class="form-control" required />
                </div>
                <div class="col-md-4">
                    <label class="form-label">联系电话</label>
                    <input id="contactPhone" data-testid="order-contact-phone" class="form-control" required />
                </div>
                <div class="col-md-4">
                    <label class="form-label">配送地址</label>
                    <input id="deliveryAddress" data-testid="order-delivery-address" class="form-control" required />
                </div>
                <div class="col-12">
                    <button data-testid="order-submit" class="btn btn-primary" type="submit">提交订单</button>
                </div>
            </form>
        </div>
    </div>
</div>
<script>
    const ctx = '${pageContext.request.contextPath}';
    const cartBody = document.getElementById('cartBody');
    const totalAmount = document.getElementById('totalAmount');
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

    async function loadCart() {
        errorBox.classList.add('d-none');
        const response = await fetch(`\${ctx}/api/cart`, { credentials: 'same-origin' });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '加载购物车失败');
            return;
        }
        renderCart(payload.data || { items: [], totalAmount: 0 });
    }

    function renderCart(cart) {
        const items = cart.items || [];
        totalAmount.textContent = Number(cart.totalAmount || 0).toFixed(2);
        if (!items.length) {
            cartBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">购物车为空</td></tr>';
            return;
        }

        cartBody.innerHTML = items.map((item) => `
            <tr data-testid="cart-row-\${item.id}">
                <td data-testid="cart-name-\${item.id}">\${item.name}</td>
                <td data-testid="cart-unit-price-\${item.id}">¥ \${Number(item.unitPrice).toFixed(2)}</td>
                <td style="max-width: 120px;">
                    <input data-testid="cart-qty-\${item.id}" class="form-control form-control-sm" id="qty-\${item.id}" type="number" min="0" step="1" value="\${item.quantity}" />
                </td>
                <td data-testid="cart-line-total-\${item.id}">¥ \${Number(item.lineTotal).toFixed(2)}</td>
                <td>
                    <button data-testid="cart-update-\${item.id}" class="btn btn-sm btn-outline-primary me-1" onclick="updateQuantity(\${item.id})">更新</button>
                    <button data-testid="cart-delete-\${item.id}" class="btn btn-sm btn-outline-danger" onclick="deleteItem(\${item.id})">删除</button>
                </td>
            </tr>
        `).join('');
    }

    async function updateQuantity(cartItemId) {
        const input = document.getElementById(`qty-\${cartItemId}`);
        const quantity = Number(input.value);
        if (!Number.isInteger(quantity) || quantity < 0) {
            showError('数量必须为大于等于 0 的整数');
            return;
        }

        const response = await fetch(`\${ctx}/api/cart/items/\${cartItemId}`, {
            method: 'PUT',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ quantity })
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '更新失败');
            return;
        }
        if (quantity === 0) {
            showSuccess('商品已从购物车移除');
        } else {
            showSuccess('数量更新成功');
        }
        renderCart(payload.data);
    }

    async function deleteItem(cartItemId) {
        const response = await fetch(`\${ctx}/api/cart/items/\${cartItemId}`, {
            method: 'DELETE',
            credentials: 'same-origin'
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '删除失败');
            return;
        }
        showSuccess('已删除购物车项');
        renderCart(payload.data);
    }

    document.getElementById('orderForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        const contactName = document.getElementById('contactName').value.trim();
        const contactPhone = document.getElementById('contactPhone').value.trim();
        const deliveryAddress = document.getElementById('deliveryAddress').value.trim();

        const response = await fetch(`\${ctx}/api/orders`, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ contactName, contactPhone, deliveryAddress })
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            showError(payload.error?.message || '下单失败');
            return;
        }
        showSuccess('下单成功，正在跳转订单页');
        setTimeout(() => {
            window.location.href = `\${ctx}/orders`;
        }, 1000);
    });

    window.updateQuantity = updateQuantity;
    window.deleteItem = deleteItem;
    loadCart();
</script>
</body>
</html>
