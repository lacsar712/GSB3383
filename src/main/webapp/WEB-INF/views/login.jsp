<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
	<head>
		<meta charset="UTF-8" />
		<meta name="viewport" content="width=device-width, initial-scale=1" />
		<title>登录 - 在线订餐系统</title>
		<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
		<link href="${pageContext.request.contextPath}/assets/css/theme.css" rel="stylesheet" />
	</head>
	<body class="page-auth">
		<div class="container py-5">
			<div class="auth-layout">
				<section class="auth-visual auth-visual-login">
					<div class="auth-brand-wrap">
						<div class="auth-brand">FoodFlow Dining</div>
						<span class="auth-scene-tag">午高峰模式</span>
					</div>
					<p class="auth-kicker">SMART ORDERING EXPERIENCE</p>
					<h1>把下单流程缩短到 60 秒</h1>
					<p class="auth-subtitle">
						从家常热菜到轻食沙拉，快速完成点餐，实时追踪每一笔订单状态，工作日午餐也能稳定提速。
					</p>
					<ul class="auth-feature-list">
						<li><span class="auth-feature-dot"></span>智能菜单筛选与快速定位</li>
						<li><span class="auth-feature-dot"></span>购物车即时同步与数量校验</li>
						<li><span class="auth-feature-dot"></span>订单进度可视化追踪</li>
					</ul>
					<div class="auth-metrics">
						<div class="auth-metric-card">
							<span class="auth-metric-label">平均下单时长</span>
							<strong>58 秒</strong>
						</div>
						<div class="auth-metric-card">
							<span class="auth-metric-label">今日完成订单</span>
							<strong>1,286 单</strong>
						</div>
						<div class="auth-metric-card">
							<span class="auth-metric-label">满意度</span>
							<strong>97.8%</strong>
						</div>
					</div>
					<div class="auth-preview">
						<p class="auth-preview-title">今日人气菜品</p>
						<div class="auth-preview-item"><span>黑椒牛肉饭</span><strong>¥28</strong></div>
						<div class="auth-preview-item"><span>宫保鸡丁饭</span><strong>¥24</strong></div>
					</div>
					<div class="auth-journey">
						<p class="auth-journey-title">高峰下单路径</p>
						<div class="auth-journey-step">
							<span>01</span>
							<p>登录后按关键词筛菜</p>
						</div>
						<div class="auth-journey-step">
							<span>02</span>
							<p>加入购物车并确认数量</p>
						</div>
						<div class="auth-journey-step">
							<span>03</span>
							<p>提交订单并追踪状态</p>
						</div>
					</div>
				</section>
				<section class="auth-form-card card shadow-sm">
					<div class="card-body p-4 p-lg-5">
						<div class="auth-form-head">
							<span class="auth-form-badge">欢迎回来</span>
							<h4 class="auth-form-title">用户登录</h4>
							<p class="auth-form-desc">欢迎回来，请先登录继续点餐，系统将自动恢复你上次的购物车状态。</p>
						</div>
						<div id="errorBox" data-testid="login-error" class="alert alert-danger d-none"></div>
						<form id="loginForm" data-testid="login-form">
							<div class="mb-3">
								<label for="username" class="form-label">用户名</label>
								<input
									id="username"
									data-testid="login-username"
									class="form-control"
									placeholder="请输入用户名"
									autocomplete="username"
									required
								/>
							</div>
							<div class="mb-3">
								<label for="password" class="form-label">密码</label>
								<input
									id="password"
									data-testid="login-password"
									type="password"
									class="form-control"
									placeholder="请输入密码"
									autocomplete="current-password"
									required
								/>
							</div>
							<button data-testid="login-submit" class="btn btn-primary w-100 mt-2 auth-action" type="submit">
								登录
							</button>
						</form>
						<p class="auth-form-tip">登录后可直接管理购物车与订单状态。</p>
						<div class="mt-4 text-center auth-switch">
							<a href="${pageContext.request.contextPath}/register">没有账号？去注册</a>
						</div>
					</div>
				</section>
			</div>
		</div>
		<script>
			const ctx = '${pageContext.request.contextPath}'
			const errorBox = document.getElementById('errorBox')

			function showError(message) {
				errorBox.textContent = message
				errorBox.classList.remove('d-none')
			}

			document.getElementById('loginForm').addEventListener('submit', async (event) => {
				event.preventDefault()
				errorBox.classList.add('d-none')

				const username = document.getElementById('username').value.trim()
				const password = document.getElementById('password').value

				const response = await fetch(`\${ctx}/api/auth/login`, {
					method: 'POST',
					credentials: 'same-origin',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ username, password }),
				})
				const payload = await response.json()
				if (!response.ok || !payload.success) {
					showError(payload.error?.message || '登录失败')
					return
				}
				window.location.href = `\${ctx}/menu`
			})
		</script>
	</body>
</html>
