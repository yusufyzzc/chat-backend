/* ============================================================
   ChatApp Frontend — app.js
   Covers ALL backend API endpoints:
   - Auth: register, login, refresh, logout
   - Users: list (admin), get by id, create (admin), update, delete (admin)
   - Conversations: create, get, list by user, update, delete
   - Messages: send, list, update, delete, search
   - Health: GET /api/health
   ============================================================ */

const App = (() => {
  // ---- State ----
  const API = '';  // same origin
  let accessToken = localStorage.getItem('accessToken') || null;
  let refreshToken = localStorage.getItem('refreshToken') || null;
  let currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
  let currentConversationId = null;
  let conversations = [];
  let allUsers = [];
  let msgPage = 0;
  let msgTotalPages = 0;
  let usersPage = 0;
  let usersTotalPages = 0;
  let isSearching = false;

  // ---- Init ----
  function init() {
    if (accessToken && currentUser) {
      showApp();
    } else {
      showAuth();
    }
  }

  // ---- API Helpers ----
  async function api(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (accessToken) headers['Authorization'] = `Bearer ${accessToken}`;
    const res = await fetch(`${API}${path}`, { ...options, headers });

    if (res.status === 401 && refreshToken) {
      const refreshed = await tryRefresh();
      if (refreshed) {
        headers['Authorization'] = `Bearer ${accessToken}`;
        return fetch(`${API}${path}`, { ...options, headers });
      } else {
        logout();
        throw new Error('Session expired');
      }
    }
    return res;
  }

  // POST /api/auth/refresh
  async function tryRefresh() {
    try {
      const res = await fetch(`${API}/api/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
      });
      if (!res.ok) return false;
      const data = await res.json();
      accessToken = data.accessToken;
      refreshToken = data.refreshToken;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      return true;
    } catch { return false; }
  }

  // ---- Toast ----
  function toast(msg, type = 'info') {
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.textContent = msg;
    document.getElementById('toastContainer').appendChild(el);
    setTimeout(() => el.remove(), 3500);
  }

  // ---- Modal helpers ----
  function openModal(id) { document.getElementById(id).classList.remove('hidden'); }
  function closeModal(id) { document.getElementById(id).classList.add('hidden'); }

  // ---- Page switching ----
  function showAuth() {
    document.getElementById('authPage').classList.remove('hidden');
    document.getElementById('appPage').classList.add('hidden');
  }

  function showApp() {
    document.getElementById('authPage').classList.add('hidden');
    document.getElementById('appPage').classList.remove('hidden');
    renderUserBadge();
    checkHealth();
    loadConversations();
    loadAllUsersCache();
  }

  function showAuthTab(tab) {
    document.getElementById('loginForm').classList.toggle('hidden', tab !== 'login');
    document.getElementById('registerForm').classList.toggle('hidden', tab !== 'register');
    document.getElementById('loginTab').classList.toggle('active', tab === 'login');
    document.getElementById('registerTab').classList.toggle('active', tab === 'register');
  }

  // ---- Sidebar tab switching ----
  function switchSidebarTab(tab) {
    document.getElementById('navChats').classList.toggle('active', tab === 'chats');
    document.getElementById('navUsers').classList.toggle('active', tab === 'users');

    if (tab === 'chats') {
      document.getElementById('newConvBtn').style.display = '';
      loadConversations();
      // Show chat view or empty
      document.getElementById('adminPanel').classList.add('hidden');
      document.getElementById('profileView').classList.add('hidden');
      if (currentConversationId) {
        document.getElementById('chatView').classList.remove('hidden');
        document.getElementById('emptyState').classList.add('hidden');
      } else {
        document.getElementById('chatView').classList.add('hidden');
        document.getElementById('emptyState').classList.remove('hidden');
      }
    } else {
      document.getElementById('newConvBtn').style.display = 'none';
      loadUsersForSidebar();
      // Show admin panel
      document.getElementById('chatView').classList.add('hidden');
      document.getElementById('emptyState').classList.add('hidden');
      document.getElementById('profileView').classList.add('hidden');
      document.getElementById('adminPanel').classList.remove('hidden');
      loadAdminUsers();
    }
  }

  // ======== AUTH ENDPOINTS ========

  // POST /api/auth/login
  async function handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    try {
      const res = await fetch(`${API}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(extractErrorMsg(err, 'Login failed'));
      }
      const data = await res.json();
      accessToken = data.accessToken;
      refreshToken = data.refreshToken;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);

      // Decode JWT to get username (payload is base64)
      const payload = JSON.parse(atob(accessToken.split('.')[1]));
      // Try to find user or store minimal info
      currentUser = { username: payload.sub };
      // Fetch user list to find our ID
      await findCurrentUserDetails();

      localStorage.setItem('currentUser', JSON.stringify(currentUser));
      toast('Welcome back, ' + currentUser.username + '!', 'success');
      showApp();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // POST /api/auth/register
  async function handleRegister(e) {
    e.preventDefault();
    const username = document.getElementById('regUsername').value;
    const email = document.getElementById('regEmail').value;
    const password = document.getElementById('regPassword').value;
    try {
      const res = await fetch(`${API}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(extractErrorMsg(err, 'Registration failed'));
      }
      toast('Account created! Please sign in.', 'success');
      showAuthTab('login');
      document.getElementById('loginUsername').value = username;
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // POST /api/auth/logout
  async function logout() {
    try {
      if (refreshToken) {
        await fetch(`${API}/api/auth/logout`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        });
      }
    } catch {}
    accessToken = null;
    refreshToken = null;
    currentUser = null;
    currentConversationId = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('currentUser');
    showAuth();
    toast('Logged out', 'info');
  }

  // Find current user details by searching all users
  async function findCurrentUserDetails() {
    try {
      // Try user list (works if admin)
      const res = await api('/api/users?size=100');
      if (res.ok) {
        const data = await res.json();
        const users = data.content || [];
        const me = users.find(u => u.username === currentUser.username);
        if (me) {
          currentUser = { ...currentUser, ...me };
          currentUser.role = 'ADMIN';
          return;
        }
      }
    } catch {}
    // Fallback: user isn't admin, try getting users by id starting from 1
    // We don't know our ID yet, but we can try a few
    for (let id = 1; id <= 20; id++) {
      try {
        const res = await api(`/api/users/${id}`);
        if (res.ok) {
          const u = await res.json();
          if (u.username === currentUser.username) {
            currentUser = { ...currentUser, ...u, role: 'USER' };
            return;
          }
        }
      } catch {}
    }
  }

  // ======== HEALTH ENDPOINT ========

  // GET /api/health
  async function checkHealth() {
    try {
      const res = await fetch(`${API}/api/health`);
      const dot = document.getElementById('healthDot');
      const txt = document.getElementById('healthText');
      if (res.ok) {
        dot.classList.remove('offline');
        txt.textContent = 'Online';
      } else {
        dot.classList.add('offline');
        txt.textContent = 'Offline';
      }
    } catch {
      document.getElementById('healthDot').classList.add('offline');
      document.getElementById('healthText').textContent = 'Offline';
    }
  }

  // ======== USER ENDPOINTS ========

  // Render user badge in topbar
  function renderUserBadge() {
    if (!currentUser) return;
    const badge = document.getElementById('userBadge');
    const roleTag = currentUser.role === 'ADMIN' ? `<span class="role-tag">Admin</span>` : '';
    badge.innerHTML = `${currentUser.username} ${roleTag}`;
  }

  // Load all users into a cache (for conversation user pickers)
  async function loadAllUsersCache() {
    try {
      const res = await api('/api/users?size=100');
      if (res.ok) {
        const data = await res.json();
        allUsers = data.content || [];
        // Mark current user as admin since list endpoint succeeded
        if (currentUser && !currentUser.role) {
          currentUser.role = 'ADMIN';
          localStorage.setItem('currentUser', JSON.stringify(currentUser));
          renderUserBadge();
        }
      }
    } catch {}
  }

  // GET /api/users (admin) — paginated table
  async function loadAdminUsers(page = 0) {
    usersPage = page;
    const tbody = document.getElementById('usersTableBody');
    tbody.innerHTML = '<tr><td colspan="5"><div class="loading-center"><div class="spinner"></div></div></td></tr>';
    try {
      const res = await api(`/api/users?page=${page}&size=10&sort=id,asc`);
      if (!res.ok) throw new Error('Could not load users');
      const data = await res.json();
      const users = data.content || [];
      usersTotalPages = data.totalPages || 1;

      if (users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--text-secondary);">No users found</td></tr>';
      } else {
        tbody.innerHTML = users.map(u => `
          <tr>
            <td>${u.id}</td>
            <td>${esc(u.username)}</td>
            <td>${esc(u.email)}</td>
            <td>${formatDate(u.createdAt)}</td>
            <td>
              <div class="actions-cell">
                <button class="btn btn-sm btn-secondary" onclick="App.viewUserProfile(${u.id})">👁</button>
                <button class="btn btn-sm btn-secondary" onclick="App.openEditUserModal(${u.id},'${esc(u.username)}','${esc(u.email)}')">✏️</button>
                <button class="btn btn-sm btn-danger" onclick="App.deleteUser(${u.id})">🗑️</button>
              </div>
            </td>
          </tr>
        `).join('');
      }

      renderPagination('usersPagination', usersPage, usersTotalPages, loadAdminUsers);
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="5" style="color:var(--danger);">${esc(err.message)}</td></tr>`;
    }
  }

  // Load users in sidebar (list for non-admin or user browsing)
  async function loadUsersForSidebar() {
    const list = document.getElementById('sidebarList');
    list.innerHTML = '<div class="loading-center"><div class="spinner"></div></div>';
    try {
      const res = await api('/api/users?size=50');
      if (!res.ok) throw new Error('Could not load users');
      const data = await res.json();
      const users = data.content || [];
      if (users.length === 0) {
        list.innerHTML = '<div class="empty-state"><p>No users</p></div>';
        return;
      }
      list.innerHTML = users.map(u => `
        <div class="sidebar-item" onclick="App.viewUserProfile(${u.id})">
          <div class="avatar">${initials(u.username)}</div>
          <div class="info">
            <div class="name">${esc(u.username)}</div>
            <div class="preview">${esc(u.email)}</div>
          </div>
        </div>
      `).join('');
    } catch {
      list.innerHTML = '<div class="empty-state"><p>Could not load users (admin only)</p></div>';
    }
  }

  // GET /api/users/{id} — User profile
  async function viewUserProfile(userId) {
    document.getElementById('chatView').classList.add('hidden');
    document.getElementById('emptyState').classList.add('hidden');
    document.getElementById('adminPanel').classList.add('hidden');
    document.getElementById('profileView').classList.remove('hidden');
    const card = document.getElementById('profileCard');
    card.innerHTML = '<div class="loading-center"><div class="spinner"></div></div>';
    try {
      const res = await api(`/api/users/${userId}`);
      if (!res.ok) throw new Error('User not found');
      const u = await res.json();
      const isMe = currentUser && currentUser.id === u.id;
      card.innerHTML = `
        <div class="avatar-lg">${initials(u.username)}</div>
        <h3>${esc(u.username)}</h3>
        <p>${esc(u.email)}</p>
        <div class="detail-row"><span>User ID</span><span>${u.id}</span></div>
        <div class="detail-row"><span>Joined</span><span>${formatDate(u.createdAt)}</span></div>
        <div style="margin-top:1rem;display:flex;gap:0.5rem;justify-content:center;">
          ${isMe || currentUser.role === 'ADMIN' ? `<button class="btn btn-sm btn-secondary" onclick="App.openEditUserModal(${u.id},'${esc(u.username)}','${esc(u.email)}')">✏️ Edit</button>` : ''}
          ${currentUser.role === 'ADMIN' && !isMe ? `<button class="btn btn-sm btn-danger" onclick="App.deleteUser(${u.id})">🗑️ Delete</button>` : ''}
        </div>
      `;
    } catch (err) {
      card.innerHTML = `<p style="color:var(--danger);">${esc(err.message)}</p>`;
    }
  }

  // Extract a readable error message from backend responses
  function extractErrorMsg(errObj, fallback) {
    if (!errObj) return fallback;
    if (errObj.message) return errObj.message;
    if (errObj.errors) {
      // {errors: {field: "msg", ...}} — validation errors
      return Object.values(errObj.errors).join('; ');
    }
    if (errObj.error) return errObj.error;
    return fallback;
  }

  // POST /api/users (admin) — Create user
  async function createUserAdmin(e) {
    e.preventDefault();
    const username = document.getElementById('newUserName').value;
    const email = document.getElementById('newUserEmail').value;
    const password = document.getElementById('newUserPass').value;
    try {
      const res = await api('/api/users', {
        method: 'POST',
        body: JSON.stringify({ username, email, password })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(extractErrorMsg(err, 'Create failed'));
      }
      toast('User created!', 'success');
      closeModal('createUserModal');
      document.getElementById('newUserName').value = '';
      document.getElementById('newUserEmail').value = '';
      document.getElementById('newUserPass').value = '';
      loadAdminUsers(usersPage);
      loadAllUsersCache();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // PUT /api/users/{id}
  function openEditUserModal(id, username, email) {
    document.getElementById('editUserId').value = id;
    document.getElementById('editUserName').value = username;
    document.getElementById('editUserEmail').value = email;
    document.getElementById('editUserPass').value = '';
    openModal('editUserModal');
  }

  async function updateUser(e) {
    e.preventDefault();
    const id = document.getElementById('editUserId').value;
    const body = {};
    const username = document.getElementById('editUserName').value;
    const email = document.getElementById('editUserEmail').value;
    const password = document.getElementById('editUserPass').value;
    if (username) body.username = username;
    if (email) body.email = email;
    if (password) body.password = password;
    try {
      const res = await api(`/api/users/${id}`, {
        method: 'PUT',
        body: JSON.stringify(body)
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(extractErrorMsg(err, 'Update failed'));
      }
      toast('User updated!', 'success');
      closeModal('editUserModal');
      // Refresh relevant view
      if (document.getElementById('adminPanel').classList.contains('hidden') === false) {
        loadAdminUsers(usersPage);
      }
      loadAllUsersCache();
      // If editing own info, update local state
      if (currentUser && String(currentUser.id) === String(id)) {
        if (username) currentUser.username = username;
        if (email) currentUser.email = email;
        localStorage.setItem('currentUser', JSON.stringify(currentUser));
        renderUserBadge();
      }
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // DELETE /api/users/{id}
  async function deleteUser(id) {
    if (!confirm('Are you sure you want to delete this user?')) return;
    try {
      const res = await api(`/api/users/${id}`, { method: 'DELETE' });
      if (!res.ok && res.status !== 204) throw new Error('Delete failed');
      toast('User deleted', 'success');
      loadAdminUsers(usersPage);
      loadAllUsersCache();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  function openCreateUserModal() {
    openModal('createUserModal');
  }

  // ======== CONVERSATION ENDPOINTS ========

  // GET /api/conversations/user/{userId}
  async function loadConversations() {
    if (!currentUser || !currentUser.id) return;
    const list = document.getElementById('sidebarList');
    list.innerHTML = '<div class="loading-center"><div class="spinner"></div></div>';
    try {
      const res = await api(`/api/conversations/user/${currentUser.id}?size=50&sort=createdAt,desc`);
      if (!res.ok) throw new Error('Could not load conversations');
      const data = await res.json();
      conversations = data.content || [];
      if (conversations.length === 0) {
        list.innerHTML = '<div class="empty-state"><p>No conversations yet.<br>Start one!</p></div>';
        return;
      }
      list.innerHTML = conversations.map(c => {
        const otherUsers = c.participantUsernames.filter(u => u !== currentUser.username);
        const displayName = otherUsers.length > 0 ? otherUsers.join(', ') : c.participantUsernames.join(', ');
        const isActive = currentConversationId === c.id;
        return `
          <div class="sidebar-item ${isActive ? 'active' : ''}" onclick="App.openConversation(${c.id})">
            <div class="avatar">${initials(displayName)}</div>
            <div class="info">
              <div class="name">${esc(displayName)}</div>
              <div class="preview">Conversation #${c.id}</div>
            </div>
            <span class="time-badge">${formatDateShort(c.createdAt)}</span>
          </div>
        `;
      }).join('');
    } catch (err) {
      list.innerHTML = `<div class="empty-state"><p>${esc(err.message)}</p></div>`;
    }
  }

  // GET /api/conversations/{id}  +  messages
  async function openConversation(convId) {
    currentConversationId = convId;
    isSearching = false;
    document.getElementById('searchBar').classList.add('hidden');
    document.getElementById('emptyState').classList.add('hidden');
    document.getElementById('adminPanel').classList.add('hidden');
    document.getElementById('profileView').classList.add('hidden');
    document.getElementById('chatView').classList.remove('hidden');

    // Highlight in sidebar
    document.querySelectorAll('.sidebar-item').forEach(el => el.classList.remove('active'));
    // re-render sidebar selection
    const items = document.querySelectorAll('.sidebar-item');
    items.forEach(el => {
      if (el.onclick && el.onclick.toString().includes(convId)) {
        el.classList.add('active');
      }
    });

    // Load conversation info
    try {
      const res = await api(`/api/conversations/${convId}`);
      if (!res.ok) throw new Error('Could not load conversation');
      const conv = await res.json();
      const otherUsers = conv.participantUsernames.filter(u => u !== (currentUser ? currentUser.username : ''));
      const displayName = otherUsers.length > 0 ? otherUsers.join(', ') : conv.participantUsernames.join(', ');
      document.getElementById('chatAvatar').textContent = initials(displayName);
      document.getElementById('chatName').textContent = displayName;
      document.getElementById('chatSubtitle').textContent = `${conv.participantUsernames.length} participants · Conversation #${conv.id}`;
    } catch {}

    // Load messages
    msgPage = 0;
    await loadMessages();

    // Re-render sidebar to update active state
    if (document.getElementById('navChats').classList.contains('active')) {
      loadConversations();
    }
  }

  // GET /api/conversations/{id}/messages
  async function loadMessages(append = false) {
    const area = document.getElementById('messagesArea');
    if (!append) {
      area.innerHTML = '<div class="loading-center"><div class="spinner"></div></div>';
    }
    try {
      const res = await api(`/api/conversations/${currentConversationId}/messages?page=${msgPage}&size=30&sort=createdAt,asc`);
      if (!res.ok) throw new Error('Could not load messages');
      const data = await res.json();
      const messages = data.content || [];
      msgTotalPages = data.totalPages || 1;

      if (!append) area.innerHTML = '';

      if (messages.length === 0 && !append) {
        area.innerHTML = '<div class="empty-state"><div class="empty-icon">📭</div><h3>No messages yet</h3><p>Send the first message!</p></div>';
        return;
      }

      // Render pagination button at top if more pages
      if (msgPage > 0 && !append) {
        // Already showing earlier messages
      }

      const html = messages.map(m => {
        const isSent = currentUser && m.senderUsername === currentUser.username;
        return `
          <div class="message-group ${isSent ? 'sent' : ''}" id="msg-${m.id}">
            <div class="message-avatar">${initials(m.senderUsername)}</div>
            <div class="message-bubble">
              ${!isSent ? `<div class="message-sender">${esc(m.senderUsername)}</div>` : ''}
              <div class="message-text" id="msg-text-${m.id}">${esc(m.content)}</div>
              <div class="message-meta">
                <span>${formatTime(m.createdAt)}</span>
                <div class="message-actions">
                  ${isSent ? `<button onclick="App.startEditMessage(${m.id}, ${currentConversationId})">edit</button>` : ''}
                  ${isSent || (currentUser && currentUser.role === 'ADMIN') ? `<button class="delete-btn" onclick="App.deleteMessage(${m.id})">delete</button>` : ''}
                </div>
              </div>
            </div>
          </div>
        `;
      }).join('');

      if (append) {
        area.insertAdjacentHTML('beforeend', html);
      } else {
        area.innerHTML = html;
      }

      // Scroll to bottom
      area.scrollTop = area.scrollHeight;
    } catch (err) {
      if (!append) area.innerHTML = `<div class="empty-state"><p style="color:var(--danger);">${esc(err.message)}</p></div>`;
    }
  }

  // POST /api/conversations/{id}/messages
  async function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    if (!content || !currentConversationId || !currentUser) return;
    input.value = '';
    try {
      const res = await api(`/api/conversations/${currentConversationId}/messages`, {
        method: 'POST',
        body: JSON.stringify({ senderId: currentUser.id, content })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(extractErrorMsg(err, 'Send failed'));
      }
      // Reload messages
      msgPage = 0;
      await loadMessages();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // PUT /api/conversations/{convId}/messages/{msgId}
  function startEditMessage(msgId, convId) {
    const textEl = document.getElementById(`msg-text-${msgId}`);
    const currentText = textEl.textContent;
    textEl.innerHTML = `
      <input class="edit-input" id="edit-msg-input-${msgId}" value="${esc(currentText)}"
        onkeydown="if(event.key==='Enter')App.saveEditMessage(${msgId},${convId});if(event.key==='Escape')App.cancelEditMessage(${msgId},'${escAttr(currentText)}');">
      <div style="display:flex;gap:0.3rem;margin-top:0.3rem;">
        <button class="btn btn-sm btn-primary" onclick="App.saveEditMessage(${msgId},${convId})">Save</button>
        <button class="btn btn-sm btn-secondary" onclick="App.cancelEditMessage(${msgId},'${escAttr(currentText)}')">Cancel</button>
      </div>
    `;
    document.getElementById(`edit-msg-input-${msgId}`).focus();
  }

  function cancelEditMessage(msgId, oldText) {
    document.getElementById(`msg-text-${msgId}`).textContent = oldText;
  }

  async function saveEditMessage(msgId, convId) {
    const input = document.getElementById(`edit-msg-input-${msgId}`);
    const content = input.value.trim();
    if (!content) return;
    try {
      const res = await api(`/api/conversations/${convId}/messages/${msgId}`, {
        method: 'PUT',
        body: JSON.stringify({ content })
      });
      if (!res.ok) throw new Error('Update failed');
      toast('Message updated', 'success');
      await loadMessages();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // DELETE /api/conversations/{convId}/messages/{msgId}
  async function deleteMessage(msgId) {
    if (!confirm('Delete this message?')) return;
    try {
      const res = await api(`/api/conversations/${currentConversationId}/messages/${msgId}`, { method: 'DELETE' });
      if (!res.ok && res.status !== 204) throw new Error('Delete failed');
      toast('Message deleted', 'success');
      await loadMessages();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // GET /api/conversations/{id}/messages/search?keyword=
  function toggleSearchBar() {
    const bar = document.getElementById('searchBar');
    bar.classList.toggle('hidden');
    if (!bar.classList.contains('hidden')) {
      document.getElementById('searchInput').focus();
    } else {
      clearSearch();
    }
  }

  async function searchMessages() {
    const keyword = document.getElementById('searchInput').value.trim();
    if (!keyword || !currentConversationId) return;
    isSearching = true;
    const area = document.getElementById('messagesArea');
    area.innerHTML = '<div class="loading-center"><div class="spinner"></div></div>';
    try {
      const res = await api(`/api/conversations/${currentConversationId}/messages/search?keyword=${encodeURIComponent(keyword)}&size=50`);
      if (!res.ok) throw new Error('Search failed');
      const data = await res.json();
      const messages = data.content || [];
      if (messages.length === 0) {
        area.innerHTML = '<div class="empty-state"><div class="empty-icon">🔍</div><h3>No results</h3><p>No messages match your search.</p></div>';
        return;
      }
      area.innerHTML = messages.map(m => {
        const isSent = currentUser && m.senderUsername === currentUser.username;
        return `
          <div class="message-group ${isSent ? 'sent' : ''}">
            <div class="message-avatar">${initials(m.senderUsername)}</div>
            <div class="message-bubble">
              ${!isSent ? `<div class="message-sender">${esc(m.senderUsername)}</div>` : ''}
              <div class="message-text">${highlightKeyword(esc(m.content), keyword)}</div>
              <div class="message-meta"><span>${formatTime(m.createdAt)}</span></div>
            </div>
          </div>
        `;
      }).join('');
    } catch (err) {
      area.innerHTML = `<div class="empty-state"><p style="color:var(--danger);">${esc(err.message)}</p></div>`;
    }
  }

  function clearSearch() {
    document.getElementById('searchInput').value = '';
    if (isSearching) {
      isSearching = false;
      loadMessages();
    }
  }

  // POST /api/conversations
  function openNewConversationModal() {
    const list = document.getElementById('convUserList');
    const otherUsers = allUsers.filter(u => currentUser && u.id !== currentUser.id);
    if (otherUsers.length === 0) {
      list.innerHTML = `
        <div style="padding:1rem;text-align:center;">
          <p style="color:var(--text-secondary);margin-bottom:1rem;">No other users found. Register a new user first:</p>
          <div class="form-group" style="text-align:left;">
            <label for="quickRegUser">Username</label>
            <input type="text" id="quickRegUser" placeholder="New username" required minlength="3">
          </div>
          <div class="form-group" style="text-align:left;">
            <label for="quickRegEmail">Email</label>
            <input type="email" id="quickRegEmail" placeholder="user@example.com" required>
          </div>
          <div class="form-group" style="text-align:left;">
            <label for="quickRegPass">Password</label>
            <input type="password" id="quickRegPass" placeholder="Min 6 characters" required minlength="6">
          </div>
          <button class="btn btn-primary btn-sm btn-full" onclick="App.quickRegisterUser()">Register & Select User</button>
        </div>
      `;
    } else {
      list.innerHTML = otherUsers.map(u => `
          <label class="user-select-item">
            <input type="radio" name="convUser" value="${u.id}">
            <div class="avatar" style="width:30px;height:30px;font-size:0.7rem;">${initials(u.username)}</div>
            <span>${esc(u.username)}</span>
          </label>
        `).join('');
    }
    openModal('newConvModal');
  }

  // Quick-register a user from the New Conversation modal, then auto-create the conversation
  async function quickRegisterUser() {
    const username = document.getElementById('quickRegUser').value.trim();
    const email = document.getElementById('quickRegEmail').value.trim();
    const password = document.getElementById('quickRegPass').value;
    if (!username || !email || !password) { toast('Fill all fields', 'error'); return; }
    try {
      let newUserId = null;
      // Try admin endpoint first (returns UserResponse with id)
      let res = await api('/api/users', {
        method: 'POST',
        body: JSON.stringify({ username, email, password })
      });
      // If admin create fails (forbidden), fall back to public register
      if (res.status === 403 || res.status === 401) {
        res = await fetch(`${API}/api/auth/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, email, password })
        });
      }
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(extractErrorMsg(err, 'Registration failed'));
      }
      const created = await res.json();
      newUserId = created.id;

      toast('User "' + username + '" created! Starting conversation…', 'success');

      // If we didn't get an ID from the response, refresh user cache and find them
      if (!newUserId) {
        await loadAllUsersCache();
        const found = allUsers.find(u => u.username === username);
        if (found) newUserId = found.id;
      }

      if (!newUserId) {
        // Fallback: just refresh the modal so user can select manually
        await loadAllUsersCache();
        openNewConversationModal();
        return;
      }

      // Automatically create the conversation with the new user
      closeModal('newConvModal');
      const convRes = await api('/api/conversations', {
        method: 'POST',
        body: JSON.stringify({ userId1: currentUser.id, userId2: newUserId })
      });
      if (!convRes.ok) {
        const err = await convRes.json().catch(() => ({}));
        throw new Error(extractErrorMsg(err, 'Could not create conversation'));
      }
      const conv = await convRes.json();
      toast('Conversation started with ' + username + '!', 'success');
      await loadAllUsersCache();
      await loadConversations();
      openConversation(conv.id);
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  async function createConversation() {
    const selected = document.querySelector('input[name="convUser"]:checked');
    if (!selected) { toast('Please select a user', 'error'); return; }
    try {
      const res = await api('/api/conversations', {
        method: 'POST',
        body: JSON.stringify({
          userId1: currentUser.id,
          userId2: parseInt(selected.value)
        })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(extractErrorMsg(err, 'Could not create conversation'));
      }
      const conv = await res.json();
      toast('Conversation created!', 'success');
      closeModal('newConvModal');
      await loadConversations();
      openConversation(conv.id);
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // PUT /api/conversations/{id}
  function openEditConversationModal() {
    if (!currentConversationId) return;
    const list = document.getElementById('editConvUserList');
    // Find current conversation to pre-check participants
    const conv = conversations.find(c => c.id === currentConversationId);
    const participantNames = conv ? conv.participantUsernames : [];

    list.innerHTML = allUsers.map(u => {
      const checked = participantNames.includes(u.username) ? 'checked' : '';
      return `
        <label class="user-select-item ${checked ? 'selected' : ''}">
          <input type="checkbox" name="editConvUser" value="${u.id}" ${checked}>
          <div class="avatar" style="width:30px;height:30px;font-size:0.7rem;">${initials(u.username)}</div>
          <span>${esc(u.username)}</span>
        </label>
      `;
    }).join('');
    openModal('editConvModal');
  }

  async function updateConversation() {
    const checked = document.querySelectorAll('input[name="editConvUser"]:checked');
    const ids = Array.from(checked).map(el => parseInt(el.value));
    if (ids.length < 2) { toast('At least 2 participants required', 'error'); return; }
    try {
      const res = await api(`/api/conversations/${currentConversationId}`, {
        method: 'PUT',
        body: JSON.stringify({ participantIds: ids })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(extractErrorMsg(err, 'Update failed'));
      }
      toast('Conversation updated!', 'success');
      closeModal('editConvModal');
      loadConversations();
      openConversation(currentConversationId);
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // DELETE /api/conversations/{id}
  async function deleteConversation() {
    if (!currentConversationId) return;
    if (!confirm('Delete this conversation?')) return;
    try {
      const res = await api(`/api/conversations/${currentConversationId}`, { method: 'DELETE' });
      if (!res.ok && res.status !== 204) throw new Error('Delete failed');
      toast('Conversation deleted', 'success');
      currentConversationId = null;
      document.getElementById('chatView').classList.add('hidden');
      document.getElementById('emptyState').classList.remove('hidden');
      loadConversations();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // ======== PAGINATION HELPER ========
  function renderPagination(containerId, currentPage, totalPages, loadFn) {
    const el = document.getElementById(containerId);
    if (totalPages <= 1) { el.innerHTML = ''; return; }
    el.innerHTML = `
      <button ${currentPage === 0 ? 'disabled' : ''} onclick="App._paginateFn('${containerId}', ${currentPage - 1})">← Prev</button>
      <span class="page-info">Page ${currentPage + 1} of ${totalPages}</span>
      <button ${currentPage >= totalPages - 1 ? 'disabled' : ''} onclick="App._paginateFn('${containerId}', ${currentPage + 1})">Next →</button>
    `;
    // Store the function reference
    App._paginationFns = App._paginationFns || {};
    App._paginationFns[containerId] = loadFn;
  }

  function _paginateFn(containerId, page) {
    const fn = App._paginationFns && App._paginationFns[containerId];
    if (fn) fn(page);
  }

  // ======== UTILITY HELPERS ========
  function esc(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  function escAttr(str) {
    return (str || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
  }

  function initials(name) {
    if (!name) return '?';
    const parts = name.split(/[\s,]+/).filter(Boolean);
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
    return name.substring(0, 2).toUpperCase();
  }

  function formatDate(dt) {
    if (!dt) return '—';
    return new Date(dt).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  function formatDateShort(dt) {
    if (!dt) return '';
    const d = new Date(dt);
    const now = new Date();
    if (d.toDateString() === now.toDateString()) {
      return d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    }
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  function formatTime(dt) {
    if (!dt) return '';
    return new Date(dt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }

  function highlightKeyword(text, keyword) {
    if (!keyword) return text;
    const regex = new RegExp(`(${keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
    return text.replace(regex, '<mark style="background:var(--accent-glow);color:var(--text-primary);padding:0 2px;border-radius:2px;">$1</mark>');
  }

  // ---- Start ----
  document.addEventListener('DOMContentLoaded', init);

  // ---- Public API ----
  return {
    showAuthTab,
    handleLogin,
    handleRegister,
    logout,
    switchSidebarTab,
    openNewConversationModal,
    createConversation,
    openConversation,
    openEditConversationModal,
    updateConversation,
    deleteConversation,
    sendMessage,
    startEditMessage,
    saveEditMessage,
    cancelEditMessage,
    deleteMessage,
    toggleSearchBar,
    searchMessages,
    clearSearch,
    viewUserProfile,
    openCreateUserModal,
    openEditUserModal,
    createUserAdmin,
    updateUser,
    deleteUser,
    quickRegisterUser,
    closeModal,
    _paginateFn,
    _paginationFns: {}
  };
})();
