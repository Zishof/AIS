const CACHE='ais-repository-static-v2',PAGES='ais-repository-pages-v2';
const STATIC=['./css/repository-modern.css','./js/repository-modern.js','./repository-manifest.json'];
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(STATIC)).then(()=>self.skipWaiting())));
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>(key.startsWith('ais-repository-static-')&&key!==CACHE)||(key.startsWith('ais-repository-pages-')&&key!==PAGES)).map(key=>caches.delete(key)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',event=>{
  if(event.request.method!=='GET')return;
  const url=new URL(event.request.url);
  if(url.origin!==self.location.origin)return;
  if(url.pathname.includes('/repository?action=')||url.pathname.includes('/repository-workspace'))return;
  if(event.request.destination==='style'||event.request.destination==='script'||url.pathname.endsWith('/repository-manifest.json')){
    event.respondWith(caches.match(event.request).then(hit=>hit||fetch(event.request).then(response=>{const copy=response.clone();caches.open(CACHE).then(cache=>cache.put(event.request,copy));return response;})));return;
  }
  if(event.request.mode==='navigate'&&url.pathname.includes('/repository'))event.respondWith(fetch(event.request).then(response=>{if(response.ok&&response.headers.get('X-Repository-Cacheable')==='public'){const copy=response.clone();caches.open(PAGES).then(cache=>cache.put(event.request,copy));}return response;}).catch(()=>caches.match(event.request).then(hit=>hit||caches.open(PAGES).then(cache=>cache.match('./repository')))));
});
