/*
 * ais_image_compress.js — kompres/resize gambar produk di sisi browser SEBELUM diunggah.
 * Sesuai instruksi performa POS: gambar produk tampil kecil di layar kasir, jadi cukup ~200-300px.
 * Mengecilkan ukuran file → cache lokal kasir ringan & unduh awal lebih cepat.
 *
 * Pemakaian:
 *   aisCompressImage(file, {maxPx:300, quality:0.8, mime:'image/jpeg'}).then(function(blob){ ... unggah blob ... });
 *   // atau pada <input type=file onchange>:
 *   aisCompressImageInput(inputEl, function(blob, originalFile){ ... });
 *
 * Mengembalikan Promise<Blob>. Bila bukan gambar / gagal → mengembalikan file asli.
 */
(function () {
	function compress(file, opt) {
		opt = opt || {};
		var maxPx = opt.maxPx || 300, quality = opt.quality || 0.8, mime = opt.mime || 'image/jpeg';
		return new Promise(function (resolve) {
			if (!file || !/^image\//.test(file.type) || /svg/.test(file.type)) { resolve(file); return; }
			var url = URL.createObjectURL(file);
			var img = new Image();
			img.onload = function () {
				try {
					var w = img.naturalWidth, h = img.naturalHeight;
					var scale = Math.min(1, maxPx / Math.max(w, h));
					var nw = Math.max(1, Math.round(w * scale)), nh = Math.max(1, Math.round(h * scale));
					var canvas = document.createElement('canvas');
					canvas.width = nw; canvas.height = nh;
					canvas.getContext('2d').drawImage(img, 0, 0, nw, nh);
					URL.revokeObjectURL(url);
					canvas.toBlob(function (blob) { resolve(blob || file); }, mime, quality);
				} catch (e) { URL.revokeObjectURL(url); resolve(file); }
			};
			img.onerror = function () { URL.revokeObjectURL(url); resolve(file); };
			img.src = url;
		});
	}
	window.aisCompressImage = compress;
	window.aisCompressImageInput = function (inputEl, cb, opt) {
		if (!inputEl || !inputEl.files || !inputEl.files.length) { return; }
		var f = inputEl.files[0];
		compress(f, opt).then(function (blob) { try { cb(blob, f); } catch (e) { } });
	};
})();
