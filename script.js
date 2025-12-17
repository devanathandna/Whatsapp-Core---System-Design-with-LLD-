const API = 'http://localhost:8000';

let currentUser = localStorage.getItem('currentUser');
let selectedContact = null;
let pollInterval = null;

// Check if logged in
if (!currentUser) {
    window.location.href = '/login';
}

// Initialize
document.getElementById('currentUserDisplay').textContent = currentUser;
loadContacts();

async function loadContacts() {
    try {
        const res = await fetch(API + '/api/users?current=' + encodeURIComponent(currentUser));
        const users = await res.json();
        
        const list = document.getElementById('contactsList');
        list.innerHTML = '';
        
        users.forEach(user => {
            const div = document.createElement('div');
            div.className = 'contact';
            div.dataset.userId = user.userId;
            div.innerHTML = 
                '<div class=\"contact-avatar\">' + user.userId.charAt(0).toUpperCase() + '</div>' +
                '<div class=\"contact-name\">' + escapeHtml(user.userId) + '</div>';
            div.onclick = () => selectContact(user.userId);
            list.appendChild(div);
        });
    } catch (err) {
        console.error('Error loading contacts:', err);
    }
}

function selectContact(userId) {
    selectedContact = userId;
    
    // Update UI
    document.querySelectorAll('.contact').forEach(c => c.classList.remove('active'));
    document.querySelector('.contact[data-user-id=\"' + userId + '\"]').classList.add('active');
    
    document.getElementById('noChat').style.display = 'none';
    document.getElementById('chatHeader').style.display = 'flex';
    document.getElementById('messages').style.display = 'block';
    document.getElementById('messageForm').style.display = 'flex';
    
    document.getElementById('chatAvatar').textContent = userId.charAt(0).toUpperCase();
    document.getElementById('chatName').textContent = userId;
    
    // Start polling
    if (pollInterval) clearInterval(pollInterval);
    fetchMessages();
    pollInterval = setInterval(fetchMessages, 2000);
}

async function fetchMessages() {
    if (!selectedContact) return;
    
    try {
        const url = API + '/api/messages?user1=' + encodeURIComponent(currentUser) + '&user2=' + encodeURIComponent(selectedContact);
        const res = await fetch(url);
        const messages = await res.json();
        renderMessages(messages);
    } catch (err) {
        console.error('Error fetching messages:', err);
    }
}

function renderMessages(messages) {
    const container = document.getElementById('messages');
    container.innerHTML = '';
    
    if (messages.length === 0) {
        container.innerHTML = '<div style=\"text-align: center; color: #888; margin-top: 2rem;\">No messages yet. Say hello!</div>';
        return;
    }
    
    messages.forEach(msg => {
        const isSent = msg.sender === currentUser;
        const time = new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        
        const div = document.createElement('div');
        div.className = 'msg ' + (isSent ? 'sent' : 'received');
        div.innerHTML = 
            '<div class=\"msg-text\">' + escapeHtml(msg.content) + '</div>' +
            '<div class=\"msg-time\">' + time + '</div>';
        container.appendChild(div);
    });
    
    container.scrollTop = container.scrollHeight;
}

document.getElementById('messageForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    if (!content || !selectedContact) return;
    
    try {
        await fetch(API + '/api/messages', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sender: currentUser,
                receiver: selectedContact,
                content: content
            })
        });
        input.value = '';
        fetchMessages();
    } catch (err) {
        console.error('Error sending message:', err);
    }
});

function logout() {
    localStorage.removeItem('currentUser');
    if (pollInterval) clearInterval(pollInterval);
    window.location.href = '/login';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
