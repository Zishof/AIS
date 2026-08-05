package ais.action.master.inventory;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.StokOpname;
import ais.database.model.inventory.Toko;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Versi <b>ZK (ZKoss 5.5)</b> dari layar <b>Stok Opname (SO) By Scan HP / PDT</b>.
 *
 * <h2>Untuk apa layar ini?</h2>
 * <p>
 * Layar ini membantu petugas menghitung ulang stok barang dengan cara <b>menembak barcode</b> —
 * memakai kamera HP atau alat PDT (scanner) — alih-alih mengetik satu per satu. Setelah jumlah fisik
 * nyata diisi, sistem menyimpan dan <b>otomatis membetulkan stok</b> sesuai selisihnya. Tujuannya:
 * opname jadi cepat, minim salah ketik, dan stok aplikasi selalu cocok dengan barang di rak.
 * </p>
 *
 * <h2>Hubungannya dengan versi JSP</h2>
 * <p>
 * Layar ini adalah <b>kembaran</b> dari versi JSP ({@code kantin/stok/opname_scan.jsp}). Keduanya
 * memakai <i>backend</i> yang sama, {@link StokOpnameScanUtil}, sehingga hasil dan perhitungannya
 * dijamin identik. Semua logika data (cari produk, hitung stok sistem, simpan, ringkasan) tinggal di
 * util tersebut agar perawatan ke depan cukup di satu tempat (maksimal <i>reuse</i>).
 * </p>
 *
 * <h2>Cara menembak barcode</h2>
 * <ul>
 * <li><b>PDT / scanner keyboard-wedge:</b> arahkan kursor ke kotak barcode, tembak — alat akan
 * mengetik kode lalu Enter, dan baris otomatis bertambah.</li>
 * <li><b>Kamera HP:</b> tekan tombol kamera; hasil baca dijembatani ke kotak barcode lewat
 * JavaScript ringan (html5-qrcode) lalu dikirim ke server melalui event {@code onOK}.</li>
 * <li><b>Manual:</b> ketik kode produk lalu Enter / tombol Tambah.</li>
 * </ul>
 * Barcode yang sama discan beruntun otomatis menambah jumlah (+1), bukan membuat baris ganda.
 *
 * <h2>Panel ringkasan (visual HTML/CSS)</h2>
 * <p>
 * Di atas tabel ditampilkan ringkasan langsung: berapa produk, total barang lebih, total barang
 * kurang, dan selisih bersih — lengkap dengan bilah proporsi lebih/kurang yang digambar murni
 * dengan HTML &amp; CSS modern (tanpa pustaka grafik berat). Ini memberi petugas gambaran cepat
 * sebelum menyimpan.
 * </p>
 *
 * <h2>Aturan sesi &amp; efisiensi</h2>
 * <p>
 * Seluruh akses data lewat {@link HibernateUtil#currentSession()} (dikelola kerangka kerja) sehingga
 * <b>tidak</b> ditutup manual. Tidak ada {@code openSession()}/{@code currentNativeSession()} yang
 * dibuka di sini, jadi tidak ada sesi yang menggantung. Daftar hasil scan disimpan sebagai
 * {@link java.util.List} ringan berisi objek {@link ScanRow} kecil (5 field) — hemat memori dan
 * cukup untuk satu sesi opname. Pencarian/penyimpanan diserahkan ke {@link StokOpnameScanUtil} yang
 * sudah dioptimalkan (kueri stok sistem digabung menjadi satu perjalanan basis data).
 * </p>
 *
 * <h2>Kompatibilitas</h2>
 * <p>
 * Kode dijaga kompatibel Java 1.7 (tanpa lambda/stream) dan {@code try/catch} gaya 1.6 (tanpa
 * <i>multi-catch</i>). Diterapkan dari {@code pages/master/inventory/stok_opname_scan.zul} pada div
 * {@code #host}.
 * </p>
 *
 * @author AIS
 */
public class StokOpnameScanKantinAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	/** Warna tema (sekali tetap) agar konsisten dan mudah diubah di satu tempat. */
	private static final String C_LEBIH = "#16a34a";
	private static final String C_KURANG = "#dc2626";
	private static final String C_NETRAL = "#64748b";

	private Div host; // wired dari ZUL

	private Toko scopeToko;
	private boolean adminBolehPilihToko = true;

	private Combobox cboToko;
	private Textbox txtScan;
	private Textbox txtKet;
	private Grid grdList;
	private Rows rows;
	private Html ringkasanBox;
	private Label lblInfo;

	private final List<ScanRow> daftar = new ArrayList<ScanRow>();
	private final String rnd = Common.getGeneratedBarCode(7);

	/** Satu baris hasil scan (ringan, hanya field yang diperlukan). */
	private static final class ScanRow {
		Long produkId;
		String kode;
		String nama;
		double sistem;
		double fisik;
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		resolveScopeToko();
		buildUI();
	}

	/** Tentukan lingkup toko: pedagang dikunci ke tokonya, admin boleh memilih. */
	private void resolveScopeToko() {
		try {
			scopeToko = Common.getCurrentToko();
		} catch (Exception e) {
			scopeToko = null;
		}
		adminBolehPilihToko = scopeToko == null
				|| (scopeToko.getBolehMelihatTokolain() != null && scopeToko.getBolehMelihatTokolain().booleanValue());
	}

	// ============================================================
	// Penyusunan tampilan
	// ============================================================

	private void buildUI() {
		host.getChildren().clear();

		Div card = new Div();
		card.setSclass("ais-crud-filter-card");
		card.setParent(host);
		Div body = new Div();
		body.setSclass("ais-crud-filter-body");
		body.setParent(card);
		body.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
				"Tembak barcode pakai kamera HP atau alat PDT, isi jumlah barang yang benar-benar ada, "
						+ "lalu Simpan. Stok langsung dibetulkan sesuai selisihnya.")));

		Hlayout baris = new Hlayout();
		baris.setStyle("gap:10px;flex-wrap:wrap;align-items:flex-end;");
		baris.setParent(body);

		baris.appendChild(kotakToko());
		baris.appendChild(kotakScan());
		baris.appendChild(tombol("Tambah", new EventListener() {
			public void onEvent(Event e) throws Exception {
				tambahDariKotak();
			}
		}));
		baris.appendChild(tombol("Scan Kamera (HP)", new EventListener() {
			public void onEvent(Event e) throws Exception {
				Clients.evalJavaScript("osZkToggle_" + rnd + "()");
			}
		}));
		baris.appendChild(tombol("Bersihkan", new EventListener() {
			public void onEvent(Event e) throws Exception {
				daftar.clear();
				renderRows();
				txtScan.focus();
			}
		}));

		// Sematan kamera (html5-qrcode) -> jembatan ke txtScan -> onOK
		Html camHtml = new Html();
		camHtml.setContent(camScript());
		camHtml.setParent(body);

		// Panel ringkasan langsung (visual HTML/CSS)
		ringkasanBox = new Html();
		ringkasanBox.setStyle("display:block;margin-top:10px;");
		ringkasanBox.setParent(host);

		// Tabel hasil scan
		grdList = new Grid();
		grdList.setParent(host);
		grdList.setStyle("margin-top:10px;");
		grdList.setSclass("ais-so-grid");
		Columns cols = new Columns();
		cols.setParent(grdList);
		kolom(cols, "No", "44px");
		kolom(cols, "Kode", "120px");
		kolom(cols, "Produk", null);
		kolom(cols, "Stok Sistem", "110px");
		kolom(cols, "Stok Fisik", "210px");
		kolom(cols, "Selisih", "100px");
		kolom(cols, "", "48px");
		rows = new Rows();
		rows.setParent(grdList);

		// Keterangan + simpan
		Hlayout bawah = new Hlayout();
		bawah.setStyle("gap:10px;align-items:flex-end;margin-top:10px;flex-wrap:wrap;");
		bawah.setParent(host);
		Vlayout cKet = new Vlayout();
		cKet.setSpacing("0");
		cKet.appendChild(labelMini("Keterangan / alasan selisih (opsional)"));
		txtKet = new Textbox();
		txtKet.setWidth("360px");
		txtKet.setTooltiptext("mis. barang basi, hilang, rusak");
		cKet.appendChild(txtKet);
		bawah.appendChild(cKet);

		MyToolbarbuttonConfig bSimpan = new MyToolbarbuttonConfig("Simpan Semua", "/img/save.gif");
		bSimpan.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) throws Exception {
				onSimpan();
			}
		});
		bawah.appendChild(bSimpan);

		lblInfo = new Label("");
		lblInfo.setStyle("margin-left:10px;color:" + C_NETRAL + ";");
		bawah.appendChild(lblInfo);

		renderRows();
		txtScan.focus();
	}

	/** Kotak pemilih toko (kombo untuk admin, label terkunci untuk pedagang). */
	private Vlayout kotakToko() {
		Vlayout c = new Vlayout();
		c.setSpacing("0");
		c.appendChild(labelMini("Toko / Kios"));
		if (adminBolehPilihToko) {
			cboToko = new Combobox();
			cboToko.setReadonly(true);
			cboToko.setWidth("220px");
			muatComboToko();
		} else {
			Label l = new Label(namaToko(scopeToko));
			l.setStyle("font-weight:700;padding:6px 0;");
			c.appendChild(l);
			return c;
		}
		c.appendChild(cboToko);
		return c;
	}

	/** Kotak barcode (PDT/manual): Enter memproses lalu fokus kembali untuk scan berikutnya. */
	private Vlayout kotakScan() {
		Vlayout c = new Vlayout();
		c.setSpacing("0");
		c.appendChild(labelMini("Barcode (scan PDT / ketik manual)"));
		txtScan = new Textbox();
		txtScan.setWidth("260px");
		txtScan.setTooltiptext("scan / ketik kode lalu Enter");
		txtScan.addEventListener("onOK", new EventListener() {
			public void onEvent(Event e) throws Exception {
				tambahDariKotak();
			}
		});
		c.appendChild(txtScan);
		return c;
	}

	private void tambahDariKotak() throws Exception {
		prosesBarcode(txtScan.getValue());
		txtScan.setValue("");
		txtScan.focus();
	}

	private void muatComboToko() {
		try {
			List<?> list = HibernateUtil.currentSession().createCriteria(Toko.class)
					.addOrder(org.hibernate.criterion.Order.asc("nama")).list();
			for (int i = 0; i < list.size(); i++) {
				Toko t = (Toko) list.get(i);
				Comboitem ci = new Comboitem(namaToko(t));
				ci.setValue(t.getId());
				ci.setParent(cboToko);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private Long tokoTerpilih() {
		if (!adminBolehPilihToko) {
			return scopeToko == null ? null : scopeToko.getId();
		}
		if (cboToko != null && cboToko.getSelectedItem() != null) {
			return (Long) cboToko.getSelectedItem().getValue();
		}
		return null;
	}

	// ============================================================
	// Inti: proses scan, render, simpan
	// ============================================================

	/** Proses satu barcode: tambah baru atau +1 bila sudah ada. */
	private void prosesBarcode(String code) throws Exception {
		if (code == null) {
			return;
		}
		code = code.trim();
		if (code.length() == 0) {
			return;
		}
		Long tokoId = tokoTerpilih();
		if (tokoId == null) {
			info("Pilih toko/kios terlebih dahulu.");
			return;
		}
		for (int i = 0; i < daftar.size(); i++) {
			if (code.equalsIgnoreCase(daftar.get(i).kode)) {
				daftar.get(i).fisik += 1d;
				renderRows();
				return;
			}
		}
		Produk p = StokOpnameScanUtil.cariProdukByBarcode(HibernateUtil.currentSession(), tokoId, code);
		if (p == null) {
			info("Barcode \"" + code + "\" tidak ditemukan pada toko ini.");
			return;
		}
		ScanRow r = new ScanRow();
		r.produkId = p.getId();
		r.kode = p.getKode();
		r.nama = p.getNama();
		r.sistem = StokOpnameScanUtil.hitungStokSistem(HibernateUtil.currentSession(), p.getId());
		r.fisik = 1d;
		daftar.add(r);
		renderRows();
	}

	/** Gambar ulang tabel + perbarui panel ringkasan dalam satu lintasan (hemat iterasi). */
	private void renderRows() {
		rows.getChildren().clear();
		double totLebih = 0d;
		double totKurang = 0d;
		for (int i = 0; i < daftar.size(); i++) {
			final ScanRow r = daftar.get(i);
			double sel = r.fisik - r.sistem;
			if (sel > 0) {
				totLebih += sel;
			} else if (sel < 0) {
				totKurang += -sel;
			}
			rows.appendChild(barisGrid(i, r, sel));
		}
		perbaruiRingkasan(totLebih, totKurang);
		if (lblInfo != null) {
			lblInfo.setValue(daftar.isEmpty() ? "" : (daftar.size() + " item siap disimpan."));
		}
	}

	/** Bangun satu baris tabel hasil scan (dengan stepper jumlah + tombol hapus). */
	private Row barisGrid(int idx, final ScanRow r, double sel) {
		Row row = new Row();
		row.appendChild(new Label(String.valueOf(idx + 1)));
		row.appendChild(new Label(r.kode == null ? "" : r.kode));
		row.appendChild(new Label(r.nama == null ? "" : r.nama));
		Label lblSis = new Label(qty(r.sistem));
		lblSis.setStyle("display:block;text-align:right;");
		row.appendChild(lblSis);

		Hlayout cFisik = new Hlayout();
		cFisik.setStyle("gap:4px;align-items:center;");
		cFisik.appendChild(tombolKecil("-", new EventListener() {
			public void onEvent(Event e) throws Exception {
				r.fisik = Math.max(0d, r.fisik - 1d);
				renderRows();
			}
		}));
		final MyDoublebox dbF = new MyDoublebox(r.fisik);
		dbF.setWidth("80px");
		dbF.addEventListener("onChange", new EventListener() {
			public void onEvent(Event e) throws Exception {
				r.fisik = dbF.getValue() == null ? 0d : dbF.getValue().doubleValue();
				renderRows();
			}
		});
		cFisik.appendChild(dbF);
		cFisik.appendChild(tombolKecil("+", new EventListener() {
			public void onEvent(Event e) throws Exception {
				r.fisik += 1d;
				renderRows();
			}
		}));
		row.appendChild(cFisik);

		Label lblSel = new Label((sel > 0 ? "+" : "") + qty(sel));
		lblSel.setStyle("display:block;text-align:right;font-weight:800;color:" + warnaSelisih(sel) + ";");
		row.appendChild(lblSel);

		row.appendChild(tombolKecil("x", new EventListener() {
			public void onEvent(Event e) throws Exception {
				daftar.remove(r);
				renderRows();
			}
		}));
		return row;
	}

	/** Susun ulang panel ringkasan (kartu angka + bilah proporsi lebih/kurang) — murni HTML/CSS. */
	private void perbaruiRingkasan(double totLebih, double totKurang) {
		double net = totLebih - totKurang;
		double total = totLebih + totKurang;
		int pctLebih = total <= 0 ? 0 : (int) Math.round(totLebih * 100d / total);
		int pctKurang = total <= 0 ? 0 : 100 - pctLebih;

		StringBuilder sb = new StringBuilder(700);
		sb.append("<div style=\"font:13px/1.4 system-ui,Segoe UI,Arial;\">");
		sb.append("<div style=\"color:").append(C_NETRAL)
				.append(";margin-bottom:6px;\">Ringkasan cepat sebelum disimpan: berapa barang yang lebih dan yang kurang.</div>");
		sb.append("<div style=\"display:flex;gap:8px;flex-wrap:wrap;\">");
		sb.append(kartu("Item discan", String.valueOf(daftar.size()), C_NETRAL));
		sb.append(kartu("Lebih (+)", qty(totLebih), C_LEBIH));
		sb.append(kartu("Kurang (-)", qty(totKurang), C_KURANG));
		sb.append(kartu("Selisih bersih", (net > 0 ? "+" : "") + qty(net), warnaSelisih(net)));
		sb.append("</div>");
		sb.append("<div style=\"display:flex;height:10px;border-radius:6px;overflow:hidden;margin-top:8px;background:#eef2f7;\">");
		sb.append("<div style=\"width:").append(pctLebih).append("%;background:").append(C_LEBIH).append(";\"></div>");
		sb.append("<div style=\"width:").append(pctKurang).append("%;background:").append(C_KURANG).append(";\"></div>");
		sb.append("</div></div>");
		ringkasanBox.setContent(sb.toString());
	}

	private String kartu(String judul, String nilai, String warna) {
		return "<div style=\"flex:1 1 120px;min-width:110px;background:#fff;border:1px solid #e5e9f0;border-radius:10px;"
				+ "padding:8px 12px;box-shadow:0 1px 2px rgba(16,24,40,.04);\">"
				+ "<div style=\"font-size:11px;color:" + C_NETRAL + ";\">" + judul + "</div>"
				+ "<div style=\"font-size:20px;font-weight:800;color:" + warna + ";\">" + nilai + "</div></div>";
	}

	private void onSimpan() throws Exception {
		if (daftar.isEmpty()) {
			info("Belum ada item untuk disimpan.");
			return;
		}
		Long tokoId = tokoTerpilih();
		if (tokoId == null) {
			info("Pilih toko/kios terlebih dahulu.");
			return;
		}
		String oleh = null;
		try {
			oleh = Common.getCurrentUser().getUserId();
		} catch (Exception ignore) {
			oleh = null;
		}
		int sukses = 0;
		StringBuilder gagal = new StringBuilder();
		String ket = txtKet == null ? null : txtKet.getValue();
		for (int i = 0; i < daftar.size(); i++) {
			ScanRow r = daftar.get(i);
			try {
				StokOpname so = StokOpnameScanUtil.simpanOpname(HibernateUtil.currentSession(), tokoId, r.produkId,
						r.fisik, ket, oleh);
				if (so != null) {
					sukses++;
				}
			} catch (Exception e) {
				gagal.append("\n- ").append(r.nama).append(": ").append(e.getMessage());
			}
		}
		daftar.clear();
		renderRows();
		if (txtKet != null) {
			txtKet.setValue("");
		}
		txtScan.focus();
		info("Berhasil menyimpan " + sukses + " item. Stok telah disesuaikan."
				+ (gagal.length() == 0 ? "" : ("\nGagal:" + gagal.toString())));
	}

	private void info(String pesan) throws Exception {
		MyMessageboxConfig.show(pesan, "Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}

	// ============================================================
	// Jembatan kamera (html5-qrcode) -> Textbox -> onOK
	// ============================================================

	/** HTML + JS kamera yang menulis hasil decode ke Textbox lalu memicu onOK ke server. */
	private String camScript() {
		String uuid = txtScan.getUuid();
		StringBuilder sb = new StringBuilder(1100);
		sb.append("<div id=\"osReaderZk_").append(rnd)
				.append("\" style=\"display:none;width:100%;max-width:420px;margin:8px auto;border:1px solid #dee2e6;border-radius:10px;overflow:hidden\"></div>");
		sb.append("<script src=\"https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js\"></script>");
		sb.append("<script>(function(){var RND='").append(rnd).append("';var UUID='").append(uuid)
				.append("';var cam=null,last='',lt=0;");
		sb.append("window['osZkPush_'+RND]=function(code){try{var w=zk.Widget.$(UUID);if(w){w.setValue(code);zAu.send(new zk.Event(w,'onOK',null,{toServer:true}));}}catch(e){}};");
		sb.append("window['osZkToggle_'+RND]=function(){var d=document.getElementById('osReaderZk_'+RND);if(!d)return;");
		sb.append("if(cam){try{cam.stop().then(function(){cam.clear();cam=null;});}catch(e){cam=null;}d.style.display='none';return;}");
		sb.append("if(typeof Html5Qrcode==='undefined'){alert('Pustaka kamera belum termuat. Gunakan alat PDT / ketik manual.');return;}");
		sb.append("d.style.display='block';cam=new Html5Qrcode('osReaderZk_'+RND);");
		sb.append("cam.start({facingMode:'environment'},{fps:10,qrbox:250},function(txt){var n=Date.now();if(txt===last&&(n-lt)<1500)return;last=txt;lt=n;window['osZkPush_'+RND](txt);},function(){}).catch(function(err){alert('Tidak bisa membuka kamera: '+err);d.style.display='none';cam=null;});");
		sb.append("};})();</script>");
		return sb.toString();
	}

	// ============================================================
	// Pembantu kecil (reuse)
	// ============================================================

	private Button tombol(String teks, EventListener onClick) {
		Button b = new Button(teks);
		b.addEventListener("onClick", onClick);
		return b;
	}

	private Button tombolKecil(String teks, EventListener onClick) {
		Button b = new Button(teks);
		b.addEventListener("onClick", onClick);
		return b;
	}

	private void kolom(Columns cols, String label, String width) {
		Column c = new Column(label);
		if (width != null) {
			c.setWidth(width);
		}
		c.setParent(cols);
	}

	private static String namaToko(Toko t) {
		if (t == null) {
			return "-";
		}
		return t.getNama() == null ? ("Toko #" + t.getId()) : t.getNama();
	}

	private static String warnaSelisih(double sel) {
		return sel < 0 ? C_KURANG : (sel > 0 ? C_LEBIH : C_NETRAL);
	}

	private static Label labelMini(String text) {
		Label l = new Label(text);
		l.setStyle("font-size:11px;color:" + C_NETRAL + ";font-weight:600;");
		return l;
	}

	/** Format angka: bulat tanpa desimal, selain itu apa adanya. */
	private static String qty(double v) {
		if (v == Math.floor(v) && !Double.isInfinite(v)) {
			return String.valueOf((long) v);
		}
		return String.valueOf(v);
	}
}
