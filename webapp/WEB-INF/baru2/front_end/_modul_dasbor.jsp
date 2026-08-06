<%--
  _modul_dasbor.jsp — Komponen CSS reusable untuk halaman dasbor modul.
  Include sekali di <head> setiap halaman dasbor modul.
--%>
<style>
.md-banner{background:var(--ais2-grad);border-radius:18px;padding:24px 28px;color:#fff;display:flex;align-items:center;gap:16px;margin-bottom:16px;position:relative;overflow:hidden;box-shadow:0 14px 34px rgba(10,61,117,.18)}
.md-banner::before,.md-banner::after{content:'';position:absolute;border-radius:50%;background:rgba(255,255,255,.07);pointer-events:none}
.md-banner::before{right:120px;bottom:-90px;width:230px;height:230px}
.md-banner::after{right:-45px;top:-75px;width:240px;height:240px}
.md-banner h2{font-size:21px;font-weight:800;margin:0 0 5px;letter-spacing:-.3px;position:relative}
.md-banner p{opacity:.88;font-size:13px;margin:0;max-width:650px;position:relative;line-height:1.55}
.md-ico{width:54px;height:54px;background:rgba(255,255,255,.16);border:1px solid rgba(255,255,255,.2);border-radius:15px;display:flex;align-items:center;justify-content:center;font-size:22px;flex-shrink:0;position:relative}
.md-stats{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px;margin-bottom:16px}
.md-stat{background:var(--ais2-card);border:1px solid var(--ais2-line);border-radius:14px;padding:15px 16px;box-shadow:var(--ais2-shadow);position:relative;min-height:112px;transition:var(--ais2-transition)}
.md-stat:hover{transform:translateY(-2px);box-shadow:var(--ais2-shadow-md)}
.md-si{width:36px;height:36px;border-radius:10px;display:flex;align-items:center;justify-content:center;margin-bottom:11px;font-size:15px;color:#fff}
.md-sv{font-size:25px;font-weight:800;color:var(--ais2-ink);line-height:1.05;letter-spacing:-.5px}
.md-sl{font-size:12px;color:var(--ais2-ink2);margin-top:4px;line-height:1.35}
.md-links{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:12px;margin-bottom:18px}
.md-lc{background:var(--ais2-card);border:1px solid var(--ais2-line);border-radius:14px;padding:15px 16px;min-height:78px;cursor:pointer;display:flex;align-items:center;gap:13px;transition:var(--ais2-transition);text-decoration:none;color:var(--ais2-ink);box-shadow:var(--ais2-shadow)}
.md-lc:hover{border-color:#93c5fd;transform:translateY(-2px);box-shadow:var(--ais2-shadow-md);color:var(--ais2-ink)}
.md-lc:focus-visible{outline:3px solid rgba(37,99,235,.3);outline-offset:2px}
.md-li{width:42px;height:42px;border-radius:11px;display:flex;align-items:center;justify-content:center;font-size:16px;color:#fff;flex-shrink:0}
.md-lc h4{font-size:13.5px;font-weight:750;margin:0 0 3px;color:var(--ais2-ink)}
.md-lc p{font-size:12px;color:var(--ais2-ink2);margin:0;line-height:1.45}
.md-section-head{font-size:14px;font-weight:700;color:var(--ais2-ink);margin:0 0 12px;display:flex;align-items:center;gap:8px}
.md-section-head i{color:var(--ais2-pri2)}
@media(max-width:900px){.md-stats{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:600px){.md-stats{grid-template-columns:1fr 1fr;gap:9px}.md-links{grid-template-columns:1fr}.md-banner{padding:19px 18px;align-items:flex-start}.md-banner h2{font-size:18px}.md-ico{width:45px;height:45px}.md-stat{min-height:104px;padding:13px}.md-sv{font-size:22px}}
</style>
