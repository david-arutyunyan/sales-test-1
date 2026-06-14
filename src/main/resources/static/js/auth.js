const AUTH_TOKEN_KEY = 'vault_auth_token';
const AUTH_USER_KEY  = 'vault_auth_user';

let currentUser = null;

// ── INIT ──────────────────────────────────────────────────────────────────────
async function authInit() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (!token) { renderNavGuest(); return; }

  try {
    const res = await fetch(`${API}/auth/me`, { headers: authHeaders() });
    if (res.ok) {
      currentUser = await res.json();
      renderNavUser();
    } else {
      clearSession();
      renderNavGuest();
    }
  } catch {
    renderNavGuest();
  }
}

// ── SESSION HELPERS ───────────────────────────────────────────────────────────
function authHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  return token ? { 'X-Auth-Token': token } : {};
}

function authFetchOptions(method = 'GET', body = null) {
  const opts = { method, headers: { ...authHeaders() } };
  if (body) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  return opts;
}

function clearSession() {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
  currentUser = null;
}

function isLoggedIn() { return currentUser !== null; }
function isRole(...roles) { return currentUser && roles.includes(currentUser.role); }

// ── NAV RENDER ────────────────────────────────────────────────────────────────
function renderNavGuest() {
  document.getElementById('navAuth').innerHTML = `
    <button class="btn-login" onclick="openLogin()">Log In</button>`;
  applyRoleUI();
}

function renderNavUser() {
  document.getElementById('navAuth').innerHTML = `
    <div class="nav-user">
      <div>
        <div class="nav-user-name">${currentUser.name}</div>
        <div class="nav-user-role">${currentUser.role}</div>
      </div>
      <button class="btn-logout" onclick="logout()">Log Out</button>
    </div>`;
  applyRoleUI();
}

// Show/hide elements based on role
function applyRoleUI() {
  const canManageProducts = isRole('SELLER', 'ADMIN');
  const isAdmin = isRole('ADMIN');

  // "Add Product" toolbar button
  const addBtn = document.getElementById('addProductBtn');
  if (addBtn) addBtn.style.display = canManageProducts ? '' : 'none';

  // "Manage Users" admin link
  const usersLink = document.getElementById('usersLink');
  if (usersLink) usersLink.style.display = isAdmin ? '' : 'none';

  // "Dashboard" link for sellers and admins
  const dashLink = document.getElementById('dashboardLink');
  if (dashLink) dashLink.style.display = canManageProducts ? '' : 'none';

  // "My Orders" link — show for customers and always-authenticated
  const ordersLink = document.getElementById('myOrdersLink');
  if (ordersLink) ordersLink.style.display = isLoggedIn() ? '' : 'none';
}

// ── LOGIN ─────────────────────────────────────────────────────────────────────
function openLogin() {
  clearAuthError('loginError');
  document.getElementById('loginOverlay').classList.add('open');
}
function closeLogin() {
  document.getElementById('loginOverlay').classList.remove('open');
}

async function submitLogin() {
  const email    = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;
  if (!email || !password) { showAuthError('loginError', 'Please fill in all fields.'); return; }

  try {
    const res = await fetch(`${API}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    const data = await res.json();
    if (!res.ok) { showAuthError('loginError', data.error || 'Login failed'); return; }

    localStorage.setItem(AUTH_TOKEN_KEY, data.token);
    currentUser = { userId: data.userId, email: data.email, name: data.name, role: data.role };
    closeLogin();
    renderNavUser();
    showToast(`Welcome back, ${data.name}!`);
    loadProducts(); // refresh to apply role-based UI on cards
  } catch {
    showAuthError('loginError', 'Could not reach server');
  }
}

// ── REGISTER ──────────────────────────────────────────────────────────────────
function openRegister() {
  closeLogin();
  clearAuthError('registerError');
  document.getElementById('registerOverlay').classList.add('open');
}
function closeRegister() {
  document.getElementById('registerOverlay').classList.remove('open');
}

async function submitRegister() {
  const name     = document.getElementById('regName').value.trim();
  const email    = document.getElementById('regEmail').value.trim();
  const password = document.getElementById('regPassword').value;
  const role     = document.getElementById('regRole').value;

  if (!name || !email || !password) { showAuthError('registerError', 'Please fill in all fields.'); return; }
  if (password.length < 6) { showAuthError('registerError', 'Password must be at least 6 characters.'); return; }

  try {
    const res = await fetch(`${API}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, password, role })
    });
    const data = await res.json();
    if (!res.ok) { showAuthError('registerError', data.error || 'Registration failed'); return; }

    localStorage.setItem(AUTH_TOKEN_KEY, data.token);
    currentUser = { userId: data.userId, email: data.email, name: data.name, role: data.role };
    closeRegister();
    renderNavUser();
    showToast(`Account created! Welcome, ${data.name}`);
    loadProducts();
  } catch {
    showAuthError('registerError', 'Could not reach server');
  }
}

// ── LOGOUT ────────────────────────────────────────────────────────────────────
async function logout() {
  try {
    await fetch(`${API}/auth/logout`, authFetchOptions('POST'));
  } catch {}
  clearSession();
  renderNavGuest();
  showToast('Logged out');
  loadProducts();
}

// ── ADMIN: USER LIST ──────────────────────────────────────────────────────────
async function loadUsers() {
  const tbody = document.getElementById('usersTableBody');
  tbody.innerHTML = '<tr><td colspan="4" style="padding:1rem;color:#aaa">Loading…</td></tr>';
  try {
    const res = await fetch(`${API}/admin/users`, authFetchOptions());
    const users = await res.json();
    if (!res.ok) { tbody.innerHTML = `<tr><td colspan="4" style="color:var(--accent)">${users.error}</td></tr>`; return; }
    tbody.innerHTML = users.map(u => `
      <tr>
        <td>${u.name}</td>
        <td>${u.email}</td>
        <td><span class="role-badge ${u.role}">${u.role}</span></td>
        <td>${u.id === currentUser?.userId ? '—' : `<button class="del-user-btn" onclick="deleteUser('${u.id}')">Delete</button>`}</td>
      </tr>`).join('');
  } catch {
    tbody.innerHTML = '<tr><td colspan="4" style="color:var(--accent)">Failed to load users</td></tr>';
  }
}

async function deleteUser(id) {
  if (!confirm('Delete this user?')) return;
  const res = await fetch(`${API}/admin/users/${id}`, authFetchOptions('DELETE'));
  if (res.ok) { showToast('User deleted'); loadUsers(); }
  else showToast('Failed to delete user', true);
}

// ── MY ORDERS ────────────────────────────────────────────────────────────────
function openUsersPanel() {
  document.getElementById('usersPanelOverlay').classList.add('open');
  loadUsers();
}
function closeUsersPanel() {
  document.getElementById('usersPanelOverlay').classList.remove('open');
}

async function viewMyOrders() {
  if (!isLoggedIn()) { openLogin(); return; }
  document.getElementById('myOrdersOverlay').classList.add('open');
  const tbody = document.getElementById('myOrdersBody');
  tbody.innerHTML = '<tr><td colspan="4" style="padding:1rem;color:#aaa">Loading…</td></tr>';
  try {
    const res = await fetch(`${API}/orders?email=${encodeURIComponent(currentUser.email)}`, authFetchOptions());
    const orders = await res.json();
    if (!res.ok) { tbody.innerHTML = `<tr><td colspan="4" style="color:var(--accent)">${orders.error}</td></tr>`; return; }
    if (orders.length === 0) { tbody.innerHTML = '<tr><td colspan="4" style="padding:1rem;color:#aaa">No orders yet.</td></tr>'; return; }
    tbody.innerHTML = orders.map(o => `
      <tr>
        <td style="font-family:'DM Mono',monospace;font-size:0.75rem">${o.id.slice(-8)}</td>
        <td>${o.items.map(i => `${i.productName} ×${i.quantity}`).join(', ')}</td>
        <td style="font-family:'DM Serif Display',serif">$${o.totalAmount.toFixed(2)}</td>
        <td><span class="role-badge CUSTOMER">${o.status}</span></td>
      </tr>`).join('');
  } catch {
    tbody.innerHTML = '<tr><td colspan="4" style="color:var(--accent)">Failed to load orders</td></tr>';
  }
}
function closeMyOrders() {
  document.getElementById('myOrdersOverlay').classList.remove('open');
}

// ── HELPERS ───────────────────────────────────────────────────────────────────
function showAuthError(id, msg) {
  const el = document.getElementById(id);
  el.textContent = msg;
  el.classList.add('show');
}
function clearAuthError(id) {
  const el = document.getElementById(id);
  if (el) { el.textContent = ''; el.classList.remove('show'); }
}

// Allow Enter key to submit forms
document.addEventListener('keydown', e => {
  if (e.key !== 'Enter') return;
  if (document.getElementById('loginOverlay').classList.contains('open')) submitLogin();
  else if (document.getElementById('registerOverlay').classList.contains('open')) submitRegister();
});
