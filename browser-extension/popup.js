const ext = typeof browser !== 'undefined' ? browser : chrome;

const userIdInput = document.getElementById('userId');
const saveBtn = document.getElementById('saveBtn');
const statusEl = document.getElementById('status');
const countEl = document.getElementById('count');

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

ext.storage.local.get(['userId', 'dailyCount', 'lastDate']).then(({ userId, dailyCount = 0, lastDate = '' }) => {
  if (userId) userIdInput.value = userId;
  const today = new Date().toDateString();
  countEl.textContent = lastDate === today ? dailyCount : 0;
});

saveBtn.addEventListener('click', save);
userIdInput.addEventListener('keydown', e => { if (e.key === 'Enter') save(); });

function save() {
  const val = userIdInput.value.trim();
  if (!UUID_REGEX.test(val)) {
    showStatus('Invalid UUID format', '#9e4a4a');
    return;
  }
  ext.storage.local.set({ userId: val }).then(() => {
    showStatus('User ID saved', '#4a9e6b');
  });
}

function showStatus(msg, color) {
  statusEl.textContent = msg;
  statusEl.style.color = color;
  setTimeout(() => { statusEl.textContent = ''; }, 2500);
}
