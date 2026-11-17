// Lets CourtCompanion start without a connection (gyms often have poor reception). Every request goes to the network
// first, so a new version shows up as soon as it is published; the cached copy is used when the network fails or is
// slower than a few seconds.
const CACHE = 'courtcompanion';
const NETWORK_TIMEOUT_MS = 4000;

self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', event => event.waitUntil(self.clients.claim()));

self.addEventListener('fetch', event => {
    const request = event.request;
    if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) return;
    event.respondWith(fromNetworkOrCache(request));
});

async function fromNetworkOrCache(request) {
    const cache = await caches.open(CACHE);
    const network = fetch(request).then(response => {
        if (response.ok) cache.put(request, response.clone());
        return response;
    });
    const cached = await cache.match(request);
    if (!cached) return network;
    const timeout = new Promise(resolve => setTimeout(() => resolve(cached), NETWORK_TIMEOUT_MS));
    return Promise.race([network.catch(() => cached), timeout]);
}
