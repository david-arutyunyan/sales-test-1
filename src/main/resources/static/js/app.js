const API = 'http://localhost:8080/api';
let products = [];
let cart = [];
let editingId = null;
let searchTimeout = null;

// ── INIT ──────────────────────────────────────────────────────────────────────
async function init() {
  await authInit();   // restore session first so role-based UI is ready
  await loadProducts();
  buildHeroTiles();
}

// ── PRODUCTS ──────────────────────────────────────────────────────────────────
async function loadProducts(category, search) {
  const grid = document.getElementById('productGrid');
  grid.innerHTML = '<div class="spinner">Loading…</div>';
  try {
    let url = `${API}/products`;
    if (category) url += `?category=${encodeURIComponent(category)}`;
    else if (search) url += `?search=${encodeURIComponent(search)}`;

    const res = await fetch(url);
    products = await res.json();
    renderProducts(products);
    buildCategoryPills(products);
    applyRoleUI(); // re-apply after render (cards have role-gated buttons)
  } catch (e) {
    grid.innerHTML = '<div class="spinner">⚠ Could not connect to server.<br/>Is Spring Boot running?</div>';
  }
}

function renderProducts(list) {
  const grid = document.getElementById('productGrid');
  document.getElementById('productCount').textContent = `${list.length} items`;

  if (list.length === 0) {
    grid.innerHTML = '<div class="spinner">No products found.</div>';
    return;
  }

  const canManage = isRole('SELLER', 'ADMIN');

  grid.innerHTML = list.map(p => {
    const stockLabel = p.stock === 0 ? 'Out of stock' : p.stock <= 5 ? `Only ${p.stock} left` : `${p.stock} in stock`;
    const stockClass = p.stock === 0 ? 'out' : p.stock <= 5 ? 'low' : '';
    const img = p.imageUrl || `https://picsum.photos/seed/${p.id}/400/300`;
    const editBtn = canManage
      ? `<button class="add-btn" onclick="editProduct('${p.id}')" style="background:#555">✎</button>`
      : '';
    return `
    <div class="card">
      <img class="card-img" src="${img}" alt="${p.name}" onerror="this.src='https://picsum.photos/seed/${p.id}/400/300'"/>
      <div class="card-body">
        <div class="card-cat">${p.category}</div>
        <div class="card-name">${p.name}</div>
        <div class="card-desc">${p.description || ''}</div>
        <div class="card-footer">
          <div>
            <div class="card-price">$${p.price.toFixed(2)}</div>
            <div class="card-stock ${stockClass}">${stockLabel}</div>
          </div>
          <div style="display:flex;gap:0.4rem">
            ${editBtn}
            <button class="add-btn" onclick="addToCart('${p.id}')" ${p.stock === 0 ? 'disabled' : ''}>Add</button>
          </div>
        </div>
      </div>
    </div>`;
  }).join('');
}

function buildCategoryPills(list) {
  const cats = [...new Set(list.map(p => p.category))].sort();
  const pills = document.getElementById('filterPills');
  pills.innerHTML = '';
  const allPill = document.createElement('button');
  allPill.className = 'pill active';
  allPill.textContent = 'All';
  allPill.onclick = () => filterCategory(null, allPill);
  pills.appendChild(allPill);

  cats.forEach(cat => {
    const btn = document.createElement('button');
    btn.className = 'pill';
    btn.textContent = cat;
    btn.onclick = () => filterCategory(cat, btn);
    pills.appendChild(btn);
  });
}

function filterCategory(cat, btn) {
  document.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
  btn.classList.add('active');
  document.getElementById('searchInput').value = '';
  loadProducts(cat);
}

function handleSearch(val) {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    document.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
    document.querySelector('.pill').classList.add('active');
    if (val.trim()) loadProducts(null, val.trim());
    else loadProducts();
  }, 350);
}

function scrollToProducts() {
  document.getElementById('productsSection').scrollIntoView({ behavior: 'smooth' });
}

// ── CART ──────────────────────────────────────────────────────────────────────
function addToCart(id) {
  const product = products.find(p => p.id === id);
  if (!product) return;
  const existing = cart.find(i => i.id === id);
  if (existing) {
    if (existing.qty >= product.stock) { showToast('Not enough stock', true); return; }
    existing.qty++;
  } else {
    cart.push({ ...product, qty: 1 });
  }
  updateCartCount();
  showToast(`${product.name} added to cart`);
}

function updateCartCount() {
  document.getElementById('cartCount').textContent = cart.reduce((s, i) => s + i.qty, 0);
}

function openCart() {
  renderCart();
  document.getElementById('overlay').classList.add('open');
  document.getElementById('cartPanel').classList.add('open');
}

function closeCart() {
  document.getElementById('overlay').classList.remove('open');
  document.getElementById('cartPanel').classList.remove('open');
}

function renderCart() {
  const body = document.getElementById('cartBody');
  if (cart.length === 0) {
    body.innerHTML = '<div class="cart-empty">Your cart is empty.</div>';
    document.getElementById('cartTotal').textContent = '$0.00';
    return;
  }
  body.innerHTML = cart.map(item => `
    <div class="cart-item">
      <img src="${item.imageUrl || `https://picsum.photos/seed/${item.id}/64/64`}" alt="${item.name}"/>
      <div class="cart-item-info">
        <div class="cart-item-name">${item.name}</div>
        <div class="cart-item-price">$${item.price.toFixed(2)} each</div>
        <div class="qty-ctrl">
          <button class="qty-btn" onclick="changeQty('${item.id}',-1)">−</button>
          <span class="qty-val">${item.qty}</span>
          <button class="qty-btn" onclick="changeQty('${item.id}',1)">+</button>
          <button class="remove-item" onclick="removeFromCart('${item.id}')">Remove</button>
        </div>
      </div>
    </div>`).join('');
  const total = cart.reduce((s, i) => s + i.price * i.qty, 0);
  document.getElementById('cartTotal').textContent = `$${total.toFixed(2)}`;
}

function changeQty(id, delta) {
  const item = cart.find(i => i.id === id);
  const product = products.find(p => p.id === id);
  if (!item) return;
  item.qty += delta;
  if (item.qty <= 0) { cart = cart.filter(i => i.id !== id); }
  else if (product && item.qty > product.stock) { item.qty = product.stock; }
  updateCartCount();
  renderCart();
}

function removeFromCart(id) {
  cart = cart.filter(i => i.id !== id);
  updateCartCount();
  renderCart();
}

// ── CHECKOUT ──────────────────────────────────────────────────────────────────
function openCheckout() {
  if (cart.length === 0) { showToast('Your cart is empty', true); return; }

  if (!isLoggedIn()) {
    closeCart();
    showToast('Please log in to place an order', true);
    openLogin();
    return;
  }

  const summary = document.getElementById('orderSummary');
  const total = cart.reduce((s, i) => s + i.price * i.qty, 0);
  summary.innerHTML = `
    <div class="order-summary-title">Order Summary</div>
    ${cart.map(i => `<div class="order-line"><span>${i.name} × ${i.qty}</span><span>$${(i.price*i.qty).toFixed(2)}</span></div>`).join('')}
    <div class="order-line" style="margin-top:0.5rem;font-weight:600;border-top:1px solid #ccc;padding-top:0.5rem">
      <span>Total</span><span>$${total.toFixed(2)}</span>
    </div>`;

  // Pre-fill name from session
  if (currentUser) {
    document.getElementById('custName').value = currentUser.name;
    document.getElementById('custEmail').value = currentUser.email;
  }

  closeCart();
  document.getElementById('checkoutOverlay').classList.add('open');
}

function closeCheckout() {
  document.getElementById('checkoutOverlay').classList.remove('open');
  openCart();
}

async function placeOrder() {
  if (!isLoggedIn()) { openLogin(); return; }

  const name  = document.getElementById('custName').value.trim();
  const email = document.getElementById('custEmail').value.trim();
  if (!name || !email) { showToast('Please fill in your name and email', true); return; }

  const order = {
    customerName: name,
    customerEmail: email,
    items: cart.map(i => ({
      productId: i.id,
      productName: i.name,
      quantity: i.qty,
      unitPrice: i.price
    }))
  };

  try {
    const res = await fetch(`${API}/orders`, authFetchOptions('POST', order));
    if (!res.ok) {
      const err = await res.json();
      showToast(err.error || 'Order failed', true);
      return;
    }
    cart = [];
    updateCartCount();
    document.getElementById('checkoutOverlay').classList.remove('open');
    showToast('Order placed successfully! 🎉');
    loadProducts();
  } catch (e) {
    showToast('Could not reach server', true);
  }
}

// ── ADMIN: PRODUCTS ───────────────────────────────────────────────────────────
function openAdmin() {
  if (!isRole('SELLER', 'ADMIN')) { showToast('Seller or Admin access required', true); return; }
  editingId = null;
  document.getElementById('adminTitle').textContent = 'Add Product';
  document.getElementById('adminSubmitBtn').textContent = 'Add Product';
  ['aName','aDesc','aPrice','aCat','aStock','aImg'].forEach(id => document.getElementById(id).value = '');
  document.getElementById('adminOverlay').classList.add('open');
}

function editProduct(id) {
  if (!isRole('SELLER', 'ADMIN')) { showToast('Seller or Admin access required', true); return; }
  const p = products.find(x => x.id === id);
  if (!p) return;
  editingId = id;
  document.getElementById('adminTitle').textContent = 'Edit Product';
  document.getElementById('adminSubmitBtn').textContent = 'Save Changes';
  document.getElementById('aName').value = p.name;
  document.getElementById('aDesc').value = p.description || '';
  document.getElementById('aPrice').value = p.price;
  document.getElementById('aCat').value = p.category;
  document.getElementById('aStock').value = p.stock;
  document.getElementById('aImg').value = p.imageUrl || '';
  document.getElementById('adminOverlay').classList.add('open');
}

function closeAdmin() {
  document.getElementById('adminOverlay').classList.remove('open');
}

async function submitProduct() {
  const body = {
    name: document.getElementById('aName').value.trim(),
    description: document.getElementById('aDesc').value.trim(),
    price: parseFloat(document.getElementById('aPrice').value),
    category: document.getElementById('aCat').value.trim(),
    stock: parseInt(document.getElementById('aStock').value),
    imageUrl: document.getElementById('aImg').value.trim() || null
  };

  if (!body.name || !body.category || isNaN(body.price) || isNaN(body.stock)) {
    showToast('Please fill in all required fields', true); return;
  }

  try {
    const url = editingId ? `${API}/products/${editingId}` : `${API}/products`;
    const method = editingId ? 'PUT' : 'POST';
    const res = await fetch(url, authFetchOptions(method, body));
    if (!res.ok) { showToast('Failed to save product', true); return; }
    closeAdmin();
    showToast(editingId ? 'Product updated' : 'Product added');
    loadProducts();
  } catch (e) {
    showToast('Could not reach server', true);
  }
}

// ── HERO TILES ────────────────────────────────────────────────────────────────
function buildHeroTiles() {
  const seeds = ['hero1','hero2','hero3','hero4'];
  document.getElementById('heroTiles').innerHTML = seeds.map(s =>
    `<div class="hero-tile"><img src="https://picsum.photos/seed/${s}/300/250" alt=""/></div>`
  ).join('');
}

// ── TOAST ──────────────────────────────────────────────────────────────────────
function showToast(msg, isError = false) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'toast show' + (isError ? ' error' : '');
  setTimeout(() => t.className = 'toast', 2800);
}

init();
