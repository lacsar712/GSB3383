<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
	<head>
		<meta charset="UTF-8" />
		<meta name="viewport" content="width=device-width, initial-scale=1" />
		<title>注册 - 在线订餐系统</title>
		<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" />
		<link href="${pageContext.request.contextPath}/assets/css/theme.css" rel="stylesheet" />
	</head>
	<body class="page-auth">
		<div class="container py-5">
			<div class="auth-layout">
				<section class="auth-visual auth-visual-register">
					<div class="auth-brand-wrap">
						<div class="auth-brand">FoodFlow Dining</div>
						<span class="auth-scene-tag">新用户引导</span>
					</div>
					<p class="auth-kicker">CREATE YOUR ACCOUNT</p>
					<h1>创建账户，开启更顺滑的点餐体验</h1>
					<p class="auth-subtitle">
						注册后即可保存常点口味、快速复购，并集中管理每一次下单记录，首次下单全程引导更清晰。
					</p>
					<ul class="auth-feature-list">
						<li><span class="auth-feature-dot"></span>收藏常点菜品</li>
						<li><span class="auth-feature-dot"></span>快速复购下单</li>
						<li><span class="auth-feature-dot"></span>统一查看订单</li>
					</ul>
					<div class="auth-metrics">
						<div class="auth-metric-card">
							<span class="auth-metric-label">注册耗时</span>
							<strong>30 秒</strong>
						</div>
						<div class="auth-metric-card">
							<span class="auth-metric-label">新用户复购率</span>
							<strong>62%</strong>
						</div>
						<div class="auth-metric-card">
							<span class="auth-metric-label">首单完成率</span>
							<strong>95%</strong>
						</div>
					</div>
					<div class="auth-preview">
						<p class="auth-preview-title">注册后可用能力</p>
						<div class="auth-preview-item"><span>购物车跨页面保留</span><strong>实时</strong></div>
						<div class="auth-preview-item"><span>订单状态更新提醒</span><strong>自动</strong></div>
					</div>
					<div class="auth-journey">
						<p class="auth-journey-title">三步完成注册</p>
						<div class="auth-journey-step">
							<span>01</span>
							<p>设置用户名与密码</p>
						</div>
						<div class="auth-journey-step">
							<span>02</span>
							<p>确认密码并提交</p>
						</div>
						<div class="auth-journey-step">
							<span>03</span>
							<p>跳转登录并开始点餐</p>
						</div>
					</div>
				</section>
				<section class="auth-form-card card shadow-sm">
					<div class="card-body p-4 p-lg-5">
						<div class="auth-form-head">
							<span class="auth-form-badge">3 步完成注册</span>
							<h4 class="auth-form-title">用户注册</h4>
							<p class="auth-form-desc">设置账号信息，立即开始使用在线订餐系统，首单流程将自动引导。</p>
						</div>
						<div id="errorBox" data-testid="register-error" class="alert alert-danger d-none"></div>
						<div id="successBox" data-testid="register-success" class="alert alert-success d-none"></div>
						<form id="registerForm" data-testid="register-form">
							<div class="mb-3">
								<label for="username" class="form-label">用户名</label>
								<input
									id="username"
									data-testid="register-username"
									class="form-control"
									placeholder="请设置用户名"
									autocomplete="username"
									required
								/>
							</div>
							<div class="mb-3">
								<label for="password" class="form-label">密码</label>
								<input
									id="password"
									data-testid="register-password"
									type="password"
									class="form-control"
									placeholder="请设置密码"
									autocomplete="new-password"
									required
								/>
							</div>
							<div class="mb-3">
								<label for="confirmPassword" class="form-label">确认密码</label>
								<input
									id="confirmPassword"
									data-testid="register-confirm-password"
									type="password"
									class="form-control"
									placeholder="请再次输入密码"
									autocomplete="new-password"
									required
								/>
							</div>
							<button data-testid="register-submit" class="btn btn-primary w-100 mt-2 auth-action" type="submit">
								注册
							</button>
						</form>
						<p class="auth-form-tip">建议使用 6-64 位密码，支持字母、数字与符号组合。</p>
						<div class="mt-4 text-center auth-switch">
							<a href="${pageContext.request.contextPath}/login">已有账号？去登录</a>
						</div>
					</div>
				</section>
			</div>
		</div>
		<script>
			const ctx = '${pageContext.request.contextPath}'
			const errorBox = document.getElementById('errorBox')
			const successBox = document.getElementById('successBox')

			function showError(message) {
				successBox.classList.add('d-none')
				errorBox.textContent = message
				errorBox.classList.remove('d-none')
			}

			function showSuccess(message) {
				errorBox.classList.add('d-none')
				successBox.textContent = message
				successBox.classList.remove('d-none')
			}

			document.getElementById('registerForm').addEventListener('submit', async (event) => {
				event.preventDefault()
				errorBox.classList.add('d-none')
				successBox.classList.add('d-none')

				const username = document.getElementById('username').value.trim()
				const password = document.getElementById('password').value
				const confirmPassword = document.getElementById('confirmPassword').value

				if (password !== confirmPassword) {
					showError('两次输入的密码不一致')
					return
				}

				const response = await fetch(`\${ctx}/api/auth/register`, {
					method: 'POST',
					credentials: 'same-origin',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ username, password }),
				})
				const payload = await response.json()
				if (!response.ok || !payload.success) {
					showError(payload.error?.message || '注册失败')
					return
				}
				showSuccess('注册成功，2 秒后跳转登录页')
				setTimeout(() => {
					window.location.href = `\${ctx}/login`
				}, 2000)
			})
		</script>
	</body>
</html>
