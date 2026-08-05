/*
 * ais_pricetag_print.js — pembuat jendela cetak Price Tag / POP / Label.
 * Dipakai BERSAMA versi JSP (kantin/barang/pricetag.jsp) & ZK (PriceTagKantinAction).
 * Cetak murni HTML/CSS @page + window.print() (tanpa library PDF); barcode via JsBarcode (CODE128).
 *
 * aisCetakPriceTag({
 *   tags:[{nama,harga,kode}], copies,
 *   template: 'pop' | 'sticker' | 'textlabel',   // jenis cetak
 *   size, per,                 // hanya utk template 'pop' (A2/A4/A5, 1/2/4 per halaman)
 *   showBc, showKode, showToko, namaToko, promo
 * });
 *
 * Template:
 *  - 'pop'       : 1 label besar (A2/A4/A5), 1/2/4 per halaman — untuk standing POP/banner.
 *  - 'sticker'   : stiker label warna A4, 5 kolom x 8 baris (40/lembar) — PROMO/Nama/Harga/Barcode.
 *  - 'textlabel' : label teks sederhana 2 kolom A4 (Toko/Nama/Harga/Barcode/Kode).
 */
(function () {
	function rupiah(n) {
		n = Number(n) || 0;
		try { return n.toLocaleString('id-ID'); } catch (e) { return '' + n; }
	}
	function esc(s) {
		return (s == null ? '' : String(s)).replace(/[&<>"']/g, function (c) {
			return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
		});
	}
	function expand(src, copies) {
		copies = Math.max(1, parseInt(copies) || 1);
		var out = [];
		for (var j = 0; j < src.length; j++) { for (var i = 0; i < copies; i++) out.push(src[j]); }
		return out;
	}
	function bcSvg(kode, h) {
		return '<svg class="bc" jsbarcode-value="' + esc(kode || '') + '" jsbarcode-format="CODE128" '
			+ 'jsbarcode-height="' + h + '" jsbarcode-displayValue="false" jsbarcode-margin="0"></svg>';
	}
	/** Buka jendela cetak, tulis HTML, render barcode, lalu print. */
	function cetak(css, bodyHtml) {
		var w = window.open('', '_blank');
		if (!w) {
			if (typeof tampilkanPesanGagalFormal === 'function') {
				tampilkanPesanGagalFormal(
					"pencetakan label harga (price tag)",
					"Peramban Bapak/Ibu memblokir jendela pop-up (popup) yang digunakan untuk menampilkan pratinjau cetak.",
					["Izinkan pop-up untuk situs ini pada pengaturan peramban Bapak/Ibu.", "Setelah diizinkan, ulangi kembali proses cetak label harga."]
				);
			} else {
				alert('Popup diblokir. Izinkan popup untuk mencetak.');
			}
			return;
		}
		w.document.write('<!doctype html><html><head><meta charset="utf-8"><title>Cetak Label</title><style>' + css + '</style>'
			+ '<scr' + 'ipt src="https://cdn.jsdelivr.net/npm/jsbarcode@3.11.6/dist/JsBarcode.all.min.js"></scr' + 'ipt></head><body>'
			+ bodyHtml
			+ '<scr' + 'ipt>window.onload=function(){try{JsBarcode(".bc").init();}catch(e){} setTimeout(function(){window.focus();window.print();},500);};</scr' + 'ipt>'
			+ '</body></html>');
		w.document.close();
	}

	// ============ Template POP besar (A2/A4/A5, 1/2/4 per halaman) ============
	function cetakPop(data) {
		var tags = expand(data.tags, data.copies);
		var size = data.size || 'A4', per = parseInt(data.per) || 1;
		var showBc = !!data.showBc, showKode = !!data.showKode, showToko = !!data.showToko;
		var namaToko = data.namaToko || '', promo = (data.promo || '').trim();
		var sizeScale = size === 'A2' ? 2.0 : (size === 'A5' ? 0.6 : 1.0);
		var perScale = per === 1 ? 1.0 : (per === 2 ? 0.62 : 0.46);
		var k = sizeScale * perScale;
		var fH = Math.round(80 * k), fN = Math.round(30 * k), fT = Math.round(15 * k), fP = Math.round(22 * k), bcH = Math.round(70 * k);
		var html = '';
		for (var i = 0; i < tags.length; i += per) {
			var cells = tags.slice(i, i + per).map(function (p) {
				return '<div class="tag">'
					+ (showToko && namaToko ? '<div class="t-toko">' + esc(namaToko) + '</div>' : '')
					+ (promo ? '<div class="t-promo">' + esc(promo) + '</div>' : '')
					+ '<div class="t-nama">' + esc(p.nama) + '</div>'
					+ '<div class="t-harga">Rp ' + rupiah(p.harga) + '</div>'
					+ ((showBc || showKode) ? '<div class="t-bc">' + (showBc ? bcSvg(p.kode, bcH) : '')
						+ (showKode ? '<div class="t-kode">' + esc(p.kode || '') + '</div>' : '') + '</div>' : '')
					+ '</div>';
			}).join('');
			html += '<div class="page per-' + per + '">' + cells + '</div>';
		}
		var css = '@page{size:' + size + ';margin:8mm;}'
			+ '*{box-sizing:border-box;} body{margin:0;font-family:Arial,Helvetica,sans-serif;-webkit-print-color-adjust:exact;print-color-adjust:exact;}'
			+ '.page{width:100%;height:100vh;display:grid;gap:6mm;page-break-after:always;}'
			+ '.per-1{grid-template-columns:1fr;grid-template-rows:1fr;} .per-2{grid-template-columns:1fr;grid-template-rows:1fr 1fr;} .per-4{grid-template-columns:1fr 1fr;grid-template-rows:1fr 1fr;}'
			+ '.tag{border:3px dashed #cbd5e1;border-radius:10px;padding:4%;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;overflow:hidden;}'
			+ '.t-toko{font-size:' + fT + 'px;color:#475569;font-weight:700;letter-spacing:.04em;text-transform:uppercase;margin-bottom:2%;}'
			+ '.t-promo{background:#dc2626;color:#fff;font-weight:900;font-size:' + fP + 'px;padding:.1em .6em;border-radius:8px;margin-bottom:3%;letter-spacing:.04em;}'
			+ '.t-nama{font-size:' + fN + 'px;font-weight:800;color:#0f172a;line-height:1.1;margin-bottom:3%;}'
			+ '.t-harga{font-size:' + fH + 'px;font-weight:900;color:#b91c1c;line-height:1;}'
			+ '.t-bc{margin-top:4%;display:flex;flex-direction:column;align-items:center;} .t-kode{font-size:' + fT + 'px;color:#334155;letter-spacing:.12em;margin-top:1%;}'
			+ '.bc{max-width:90%;height:auto;}';
		cetak(css, html);
	}

	// ============ Template Stiker warna A4 (5 kolom x 8 baris = 40/lembar) ============
	function cetakSticker(data) {
		var tags = expand(data.tags, data.copies);
		var showBc = data.showBc !== false, showKode = data.showKode !== false;
		var promo = (data.promo || 'PROMO').trim();
		var PER = 40; // 5 x 8
		var html = '';
		for (var i = 0; i < tags.length; i += PER) {
			var cells = tags.slice(i, i + PER).map(function (p) {
				return '<div class="lbl">'
					+ '<div class="lbl-top"><span class="lbl-promo">' + esc(promo || 'PROMO') + '</span><span class="lbl-rp">Rp</span></div>'
					+ '<div class="lbl-nama">' + esc(p.nama) + '</div>'
					+ '<div class="lbl-harga"><span class="rp">Rp</span> <b>' + rupiah(p.harga) + '</b> <span class="pcs">/pcs</span></div>'
					+ '<div class="lbl-foot">' + (showBc ? bcSvg(p.kode, 26) : '')
					+ (showKode ? '<span class="lbl-kode">KODE : ' + esc(p.kode || '') + '</span>' : '') + '</div>'
					+ '</div>';
			}).join('');
			html += '<div class="sheet">' + cells + '</div>';
		}
		var css = '@page{size:A4;margin:4mm;}'
			+ '*{box-sizing:border-box;} body{margin:0;font-family:Arial,Helvetica,sans-serif;-webkit-print-color-adjust:exact;print-color-adjust:exact;}'
			+ '.sheet{width:100%;height:100vh;display:grid;grid-template-columns:repeat(5,1fr);grid-template-rows:repeat(8,1fr);gap:1.2mm;page-break-after:always;}'
			+ '.lbl{border:1px solid #d1d5db;border-radius:3px;overflow:hidden;display:flex;flex-direction:column;font-size:7px;}'
			+ '.lbl-top{display:flex;justify-content:space-between;align-items:center;background:#e11d2a;color:#fff;padding:1px 3px;font-weight:900;}'
			+ '.lbl-top .lbl-promo{font-size:7px;letter-spacing:.02em;} .lbl-top .lbl-rp{background:#fff;color:#e11d2a;border-radius:2px;padding:0 6px;font-size:7px;}'
			+ '.lbl-nama{background:#ffd400;color:#111;font-weight:800;text-align:center;font-size:8px;line-height:1.05;padding:1px 2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}'
			+ '.lbl-harga{flex:1;display:flex;align-items:center;justify-content:center;gap:2px;color:#0033a0;}'
			+ '.lbl-harga .rp{font-size:7px;font-weight:700;} .lbl-harga b{font-size:15px;font-weight:900;line-height:1;} .lbl-harga .pcs{font-size:6px;align-self:flex-end;}'
			+ '.lbl-foot{display:flex;align-items:center;justify-content:space-between;padding:0 3px 1px;gap:2px;}'
			+ '.lbl-foot .bc{height:14px;max-width:60%;} .lbl-kode{font-size:6px;color:#111;font-weight:700;white-space:nowrap;}';
		cetak(css, html);
	}

	// ============ Template Label Teks Sederhana (2 kolom A4) ============
	function cetakTextLabel(data) {
		var tags = expand(data.tags, data.copies);
		var showBc = data.showBc !== false, showKode = data.showKode !== false, showToko = data.showToko !== false;
		var namaToko = (data.namaToko || '').trim();
		var html = '<div class="grid">';
		html += tags.map(function (p) {
			return '<div class="tl">'
				+ (showToko && namaToko ? '<div class="tl-toko">' + esc(namaToko) + '</div>' : '')
				+ '<div>Nama Produk : <span class="tl-v">' + esc(p.nama) + '</span></div>'
				+ '<div class="tl-harga">Harga : Rp <span class="tl-v">' + rupiah(p.harga) + '</span></div>'
				+ (showBc ? '<div class="tl-bc">Barcode : ' + bcSvg(p.kode, 28) + '</div>' : '')
				+ (showKode ? '<div>Kode : <span class="tl-v">' + esc(p.kode || '') + '</span></div>' : '')
				+ '</div>';
		}).join('');
		html += '</div>';
		var css = '@page{size:A4;margin:12mm;}'
			+ '*{box-sizing:border-box;} body{margin:0;font-family:"Times New Roman",Georgia,serif;-webkit-print-color-adjust:exact;print-color-adjust:exact;}'
			+ '.grid{display:grid;grid-template-columns:1fr 1fr;gap:10mm 14mm;}'
			+ '.tl{font-size:12px;line-height:1.5;page-break-inside:avoid;}'
			+ '.tl-toko{font-weight:800;font-size:13px;margin-bottom:2px;}'
			+ '.tl-harga{font-weight:800;} .tl-v{font-weight:600;} .tl-bc .bc{height:16px;vertical-align:middle;}';
		cetak(css, html);
	}

	window.aisCetakPriceTag = function (data) {
		data = data || {};
		if (!data.tags || !data.tags.length) {
			if (typeof tampilkanPesanGagalFormal === 'function') {
				tampilkanPesanGagalFormal(
					"pencetakan label harga (price tag)",
					"Belum ada satu pun produk yang dipilih untuk dicetak labelnya.",
					["Pilih minimal satu produk pada daftar terlebih dahulu.", "Setelah produk terpilih, ulangi kembali proses cetak label harga."]
				);
			} else {
				alert('Pilih minimal satu produk.');
			}
			return;
		}
		var t = data.template || 'pop';
		if (t === 'sticker') { cetakSticker(data); return; }
		if (t === 'textlabel') { cetakTextLabel(data); return; }
		cetakPop(data);
	};
})();
