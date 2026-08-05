<%--
  _modul_dasbor.jsp — Komponen CSS reusable untuk halaman dasbor modul.
  Include sekali di <head> setiap halaman dasbor modul.
--%>
<style>
.md-banner{background:var(--ais2-grad);border-radius:16px;padding:22px 26px;color:#fff;display:flex;align-items:center;gap:16px;margin-bottom:18px;position:relative;overflow:hidden}
.md-banner::after{content:'';position:absolute;right:-30px;top:-50px;width:200px;height:200px;border-radius:50%;background:rgba(255,255,255,.07);pointer-events:none}
.md-banner h2{font-size:20px;font-weight:800;margin:0 0 4px}
.md-banner p{opacity:.85;font-size:13px;margin:0;max-width:500px}
.md-ico{width:52px;height:52px;background:rgba(255,255,255,.2);border-radius:14px;display:flex;align-items:center;justify-content:center;font-size:22px;flex-shrink:0}
.md-stats{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:12px;margin-bottom:18px}
.md-stat{background:var(--ais2-card-bg);border:1px solid var(--ais2-border);border-radius:12px;padding:14px;box-shadow:0 2px 8px rgba(0,0,0,.05)}
.md-si{width:34px;height:34px;border-radius:9px;display:flex;align-items:center;justify-content:center;margin-bottom:10px;font-size:15px;color:#fff}
.md-sv{font-size:24px;font-weight:800;color:var(--ais2-text);line-height:1.1}
.md-sl{font-size:11.5px;color:var(--ais2-text-sec);margin-top:3px}
.md-links{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:12px;margin-bottom:18px}
.md-lc{background:var(--ais2-card-bg);border:1px solid var(--ais2-border);border-radius:12px;padding:14px 16px;cursor:pointer;display:flex;align-items:center;gap:12px;transition:.15s;text-decoration:none;color:var(--ais2-text)}
.md-lc:hover{border-color:var(--ais2-pri2);transform:translateY(-2px);box-shadow:0 6px 20px rgba(0,0,0,.1)}
.md-li{width:40px;height:40px;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:16px;color:#fff;flex-shrink:0}
.md-lc h4{font-size:13px;font-weight:700;margin:0 0 2px;color:var(--ais2-text)}
.md-lc p{font-size:11.5px;color:var(--ais2-text-sec);margin:0;line-height:1.4}
.md-section-head{font-size:14px;font-weight:700;color:var(--ais2-text);margin:0 0 12px;display:flex;align-items:center;gap:8px}
.md-section-head i{color:var(--ais2-pri2)}
@media(max-width:600px){.md-stats{grid-template-columns:1fr 1fr}.md-links{grid-template-columns:1fr}.md-banner{padding:16px}}
</style>
