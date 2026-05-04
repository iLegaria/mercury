const API_BASE = 'http://localhost:8080';

// Firefox uses `browser` (Promise-based); Chrome uses `chrome` (callback-based).
const ext = typeof browser !== 'undefined' ? browser : chrome;

ext.runtime.onInstalled.addListener(() => {
  ext.contextMenus.create({
    id: 'save-snippet',
    title: 'Save to Knowledge Engine',
    contexts: ['selection'],
  });
});

ext.contextMenus.onClicked.addListener(async (info, tab) => {
  if (info.menuItemId !== 'save-snippet') return;
  if (!info.selectionText) return;

  const { userId } = await ext.storage.local.get('userId');
  if (!userId) {
    setBadge('!', '#9e4a4a');
    console.warn('[Mercury] No userId configured. Open the extension popup to set it.');
    return;
  }

  const payload = {
    userId,
    content: info.selectionText.trim(),
    sourceUrl: tab?.url ?? null,
    sourceTitle: tab?.title ?? null,
  };

  try {
    const res = await fetch(`${API_BASE}/api/v1/snippets`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (res.status === 409) {
      console.info('[Mercury] Duplicate snippet detected — similar content already saved.');
      setBadge('≈', '#b8860b');
      return;
    }

    if (!res.ok) {
      const text = await res.text();
      console.error('[Mercury] Failed to save snippet:', res.status, text);
      setBadge('!', '#9e4a4a');
      return;
    }

    const { dailyCount = 0, lastDate = '' } = await ext.storage.local.get(['dailyCount', 'lastDate']);
    const today = new Date().toDateString();
    const newCount = lastDate === today ? dailyCount + 1 : 1;
    await ext.storage.local.set({ dailyCount: newCount, lastDate: today });

    setBadge('✓', '#4a9e6b');
    console.log('[Mercury] Snippet saved successfully.');
  } catch (err) {
    console.error('[Mercury] Network error — is the backend running?', err);
    setBadge('!', '#9e4a4a');
  }
});

function setBadge(text, color) {
  ext.browserAction.setBadgeText({ text });
  ext.browserAction.setBadgeBackgroundColor({ color });
  setTimeout(() => ext.browserAction.setBadgeText({ text: '' }), 3000);
}
