package ais.action.master.inventory;

import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.LampiranLain;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Versi <b>ZK (ZKoss 5.5)</b> dari fitur <b>Cetak Price Tag / POP</b> (ukuran A2/A4/A5).
 *
 * <h2>Untuk apa layar ini?</h2>
 * <p>
 * Membantu petugas mencetak label harga (price tag) produk kantin dalam ukuran besar untuk dipasang
 * di standing POP banner, shelving, atau gondola. Pasangan dari versi JSP
 * ({@code kantin/barang/pricetag.jsp}); keduanya memakai <b>logika cetak yang sama</b> dari berkas
 * JavaScript bersama {@code webapp/js/ais_pricetag_print.js} (cetak HTML/CSS {@code @page} +
 * {@code window.print()} tanpa library PDF; barcode via JsBarcode), serta kueri produk bersama
 * {@link PriceTagUtil#listProduk(org.hibernate.Session, Long, String)}.
 * </p>
 *
 * <h2>Cara kerja</h2>
 * <ol>
 * <li>Toko dikunci ke toko pedagang ({@code tbmuser.getPedagang().getToko()}); admin-kantin memilih
 * toko, lalu daftar produk dimuat ke {@link Listbox} (bisa dicentang banyak, ada "pilih semua").</li>
 * <li>Petugas memilih ukuran (A2/A4/A5), jumlah label per halaman (1/2/4), salinan, label promo, dan
 * opsi barcode/kode/nama toko.</li>
 * <li>Tombol Cetak mengumpulkan produk terpilih menjadi JSON lalu memanggil
 * {@code aisCetakPriceTag(...)} di sisi klien via {@link Clients#evalJavaScript(String)} — jendela
 * cetak terbuka dengan ukuran kertas sesuai pilihan.</li>
 * </ol>
 *
 * <h2>Aturan sesi</h2>
 * <p>
 * Memakai {@link HibernateUtil#currentSession()} (dikelola kerangka kerja) — <b>tidak</b> ditutup
 * manual; tidak membuka {@code openSession()}/{@code currentNativeSession()}. Kompatibel Java 1.7;
 * {@code try/catch} gaya 1.6. Diterapkan dari {@code pages/master/inventory/pricetag_kantin.zul} pada
 * div {@code #host}.
 * </p>
 *
 * @author AIS
 */
public class PriceTagKantinAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	private Div host; // wired dari ZUL

	private Toko scopeToko;
	private boolean adminBolehPilihToko = true;

	private Combobox cboToko;
	private Textbox txtCari;
	private Listbox lstProduk;
	private Radiogroup rgSize;
	private Combobox cboPer;
	private Combobox cboTpl;
	private Intbox inCopies;
	private Intbox inTagMarginMm;
	private Textbox txtPromo;
	private Checkbox cbBc;
	private Checkbox cbKode;
	private Checkbox cbToko;
	private Label lblInfo;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		resolveScopeToko();
		muatScriptCetak();
		buildUI();
		if (!adminBolehPilihToko) {
			loadProduk();
		}
	}

	/** Toko pedagang dikunci; admin-kantin (tanpa pedagang) boleh memilih. */
	private void resolveScopeToko() {
		try {
			scopeToko = (Common.getCurrentUser() != null && Common.getCurrentUser().getPedagang() != null)
					? Common.getCurrentUser().getPedagang().getToko() : null;
		} catch (Exception e) {
			scopeToko = null;
		}
		adminBolehPilihToko = (scopeToko == null);
	}

	/** Muat berkas JS cetak bersama (sekali) dengan context-path yang benar. */
	private void muatScriptCetak() {
		Clients.evalJavaScript("if(!window.aisCetakPriceTag){var s=document.createElement('script');s.src='"
				+ Common.ROOT + "/js/ais_pricetag_print.js';document.head.appendChild(s);}");
	}

	private void buildUI() {
		host.getChildren().clear();

		Div card = new Div();
		card.setSclass("ais-crud-filter-card");
		card.setParent(host);
		Div body = new Div();
		body.setSclass("ais-crud-filter-body");
		body.setParent(card);
		body.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
				"Pilih produk dan ukuran kertas (A2/A4/A5), lalu cetak label harga untuk standing POP, "
						+ "shelving, atau gondola.")));

		// Baris toko + cari
		Hlayout atas = new Hlayout();
		atas.setStyle("gap:10px;flex-wrap:wrap;align-items:flex-end;");
		atas.setParent(body);

		Vlayout cToko = new Vlayout();
		cToko.setSpacing("0");
		cToko.appendChild(labelMini("Toko / Kios"));
		if (adminBolehPilihToko) {
			cboToko = new Combobox();
			cboToko.setReadonly(true);
			cboToko.setWidth("220px");
			muatComboToko();
			cboToko.addEventListener("onChange", new EventListener() {
				public void onEvent(Event e) throws Exception {
					loadProduk();
				}
			});
			cToko.appendChild(cboToko);
		} else {
			Label l = new Label(namaToko());
			l.setStyle("font-weight:700;padding:6px 0;");
			cToko.appendChild(l);
		}
		atas.appendChild(cToko);

		Vlayout cCari = new Vlayout();
		cCari.setSpacing("0");
		cCari.appendChild(labelMini("Cari produk"));
		txtCari = new Textbox();
		txtCari.setWidth("220px");
		txtCari.setTooltiptext("nama / kode produk");
		txtCari.addEventListener("onOK", new EventListener() {
			public void onEvent(Event e) throws Exception {
				loadProduk();
			}
		});
		cCari.appendChild(txtCari);
		atas.appendChild(cCari);
		atas.appendChild(tombol("Cari", new EventListener() {
			public void onEvent(Event e) throws Exception {
				loadProduk();
			}
		}));
		atas.appendChild(tombol("Pilih semua", new EventListener() {
			public void onEvent(Event e) throws Exception {
				pilihSemua(true);
			}
		}));
		atas.appendChild(tombol("Kosongkan", new EventListener() {
			public void onEvent(Event e) throws Exception {
				pilihSemua(false);
			}
		}));

		// Daftar produk (centang banyak)
		lstProduk = new Listbox();
		lstProduk.setCheckmark(true);
		lstProduk.setMultiple(true);
		lstProduk.setStyle("margin-top:10px;");
		lstProduk.setRows(12);
		lstProduk.setParent(host);
		Listhead lh = new Listhead();
		lh.setParent(lstProduk);
		kolom(lh, "Kode", "140px");
		kolom(lh, "Produk", null);
		kolom(lh, "Harga", "140px");

		// Pengaturan cetak
		Div opt = new Div();
		opt.setStyle("margin-top:12px;display:flex;flex-wrap:wrap;gap:16px;align-items:flex-end;");
		opt.setParent(host);

		Vlayout cTpl = new Vlayout();
		cTpl.setSpacing("0");
		cTpl.appendChild(labelMini("Jenis cetak"));
		cboTpl = new Combobox();
		cboTpl.setReadonly(true);
		cboTpl.setWidth("230px");
		comboTpl("POP Besar (A2/A4/A5)", "pop", true);
		comboTpl("Stiker Label Warna A4 (40/lembar)", "sticker", false);
		comboTpl("Label Teks Sederhana (2 kolom)", "textlabel", false);
		cTpl.appendChild(cboTpl);
		opt.appendChild(cTpl);

		Vlayout cSize = new Vlayout();
		cSize.setSpacing("0");
		cSize.appendChild(labelMini("Ukuran kertas (POP)"));
		rgSize = new Radiogroup();
		rgSize.setParent(cSize);
		radio(rgSize, "A2", false);
		radio(rgSize, "A4", true);
		radio(rgSize, "A5", false);
		opt.appendChild(cSize);

		Vlayout cPer = new Vlayout();
		cPer.setSpacing("0");
		cPer.appendChild(labelMini("Label per halaman"));
		cboPer = new Combobox();
		cboPer.setReadonly(true);
		cboPer.setWidth("120px");
		comboPer("1 (penuh)", 1, true);
		comboPer("2", 2, false);
		comboPer("4", 4, false);
		cPer.appendChild(cboPer);
		opt.appendChild(cPer);

		Vlayout cCopies = new Vlayout();
		cCopies.setSpacing("0");
		cCopies.appendChild(labelMini("Salinan / produk"));
		inCopies = new Intbox(Integer.valueOf(1));
		inCopies.setWidth("80px");
		cCopies.appendChild(inCopies);
		opt.appendChild(cCopies);

		Vlayout cMargin = new Vlayout();
		cMargin.setSpacing("0");
		cMargin.appendChild(labelMini("Margin kotak (mm)"));
		inTagMarginMm = new Intbox(Integer.valueOf(defaultMarginKotakMm()));
		inTagMarginMm.setWidth("90px");
		inTagMarginMm.setTooltiptext("0 = sama seperti layout saat ini; isi manual untuk memberi jarak dalam tiap kotak label.");
		cMargin.appendChild(inTagMarginMm);
		opt.appendChild(cMargin);

		Vlayout cPromo = new Vlayout();
		cPromo.setSpacing("0");
		cPromo.appendChild(labelMini("Label promo (opsional)"));
		txtPromo = new Textbox();
		txtPromo.setWidth("160px");
		txtPromo.setTooltiptext("mis. PROMO");
		cPromo.appendChild(txtPromo);
		opt.appendChild(cPromo);

		Vlayout cTog = new Vlayout();
		cTog.setSpacing("0");
		cbBc = new Checkbox("Barcode");
		cbBc.setChecked(true);
		cbKode = new Checkbox("Kode produk");
		cbKode.setChecked(true);
		cbToko = new Checkbox("Nama toko");
		cbToko.setChecked(true);
		cTog.appendChild(cbBc);
		cTog.appendChild(cbKode);
		cTog.appendChild(cbToko);
		opt.appendChild(cTog);

		// Aksi
		Hlayout bawah = new Hlayout();
		bawah.setStyle("gap:10px;align-items:center;margin-top:12px;");
		bawah.setParent(host);
		MyToolbarbuttonConfig bCetak = new MyToolbarbuttonConfig("Pratinjau & Cetak", "/img/print.png");
		bCetak.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) throws Exception {
				onCetak();
			}
		});
		bawah.appendChild(bCetak);
		lblInfo = new Label("");
		lblInfo.setStyle("color:#64748b;");
		bawah.appendChild(lblInfo);
	}

	private void muatComboToko() {
		try {
			List<?> list = HibernateUtil.currentSession().createCriteria(Toko.class)
					.addOrder(org.hibernate.criterion.Order.asc("nama")).list();
			for (int i = 0; i < list.size(); i++) {
				Toko t = (Toko) list.get(i);
				Comboitem ci = new Comboitem(t.getNama() == null ? ("Toko #" + t.getId()) : t.getNama());
				ci.setValue(t.getId());
				ci.setParent(cboToko);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private Long tokoId() {
		if (!adminBolehPilihToko) {
			return scopeToko == null ? null : scopeToko.getId();
		}
		if (cboToko != null && cboToko.getSelectedItem() != null) {
			return (Long) cboToko.getSelectedItem().getValue();
		}
		return null;
	}

	private String namaToko() {
		if (!adminBolehPilihToko) {
			return scopeToko == null ? "" : (scopeToko.getNama() == null ? ("Toko #" + scopeToko.getId())
					: scopeToko.getNama());
		}
		if (cboToko != null && cboToko.getSelectedItem() != null) {
			return cboToko.getSelectedItem().getLabel();
		}
		return "";
	}

	/** Muat produk toko terpilih ke listbox (centang banyak). */
	private void loadProduk() {
		if (lstProduk == null) {
			return;
		}
		// sisakan listhead, buang listitem lama
		for (int i = lstProduk.getItemCount() - 1; i >= 0; i--) {
			lstProduk.removeItemAt(i);
		}
		Long t = tokoId();
		if (t == null) {
			return;
		}
		List<Produk> produks = PriceTagUtil.listProduk(HibernateUtil.currentSession(), t,
				txtCari == null ? null : txtCari.getValue());
		for (int i = 0; i < produks.size(); i++) {
			Produk p = produks.get(i);
			Listitem it = new Listitem();
			it.setValue(p);
			it.appendChild(new Listcell(p.getKode() == null ? "" : p.getKode()));
			it.appendChild(new Listcell(p.getNama() == null ? "" : p.getNama()));
			it.appendChild(new Listcell(rupiah(p.getHargaJual())));
			it.setParent(lstProduk);
		}
		if (lblInfo != null) {
			lblInfo.setValue(produks.size() + " produk dimuat.");
		}
	}

	private void pilihSemua(boolean on) {
		if (lstProduk == null) {
			return;
		}
		if (on) {
			lstProduk.selectAll();
		} else {
			lstProduk.clearSelection();
		}
	}

	@SuppressWarnings("unchecked")
	private void onCetak() throws Exception {
		if (lstProduk == null) {
			return;
		}
		Set<Listitem> dipilih = lstProduk.getSelectedItems();
		if (dipilih == null || dipilih.isEmpty()) {
			info("Pilih minimal satu produk.");
			return;
		}
		JSONArray tags = new JSONArray();
		for (Listitem it : dipilih) {
			Produk p = (Produk) it.getValue();
			if (p == null) {
				continue;
			}
			JSONObject t = new JSONObject();
			t.put("nama", p.getNama() == null ? "" : p.getNama());
			t.put("harga", p.getHargaJual() == null ? 0 : p.getHargaJual().doubleValue());
			t.put("kode", p.getKode() == null ? ("" + p.getId()) : p.getKode());
			tags.put(t);
		}

		JSONObject data = new JSONObject();
		data.put("tags", tags);
		data.put("copies", inCopies == null || inCopies.getValue() == null ? 1 : inCopies.getValue().intValue());
		data.put("template", cboTpl != null && cboTpl.getSelectedItem() != null
				? String.valueOf(cboTpl.getSelectedItem().getValue()) : "pop");
		data.put("size", rgSize != null && rgSize.getSelectedItem() != null
				? String.valueOf(rgSize.getSelectedItem().getValue()) : "A4");
		data.put("per", cboPer != null && cboPer.getSelectedItem() != null ? cboPer.getSelectedItem().getValue() : 1);
		data.put("showBc", cbBc != null && cbBc.isChecked());
		data.put("showKode", cbKode != null && cbKode.isChecked());
		data.put("showToko", cbToko != null && cbToko.isChecked());
		data.put("namaToko", namaToko());
		data.put("promo", txtPromo == null ? "" : txtPromo.getValue());
		data.put("logoUrl", priceTagLogoUrl());
		data.put("tagMarginMm", inTagMarginMm == null || inTagMarginMm.getValue() == null ? defaultMarginKotakMm()
				: inTagMarginMm.getValue().intValue());

		Clients.evalJavaScript("if(window.aisCetakPriceTag){aisCetakPriceTag(" + data.toString()
				+ ");}else{alert('Modul cetak belum termuat, coba lagi.');}");
	}

	private void info(String pesan) throws Exception {
		MyMessageboxConfig.show(pesan, "Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}

	private int defaultMarginKotakMm() {
		return intKonfigurasi("kantin_price_tag_margin_kotak_mm", 0);
	}

	private int intKonfigurasi(String key, int defaultValue) {
		try {
			String v = Common.getKonfigurasi(key, String.valueOf(defaultValue)).getNilai();
			return Integer.parseInt(v == null ? String.valueOf(defaultValue) : v.trim());
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private String priceTagLogoUrl() {
		try {
			LampiranLain logo = LampiranLain.ambil(LampiranLain.LOGO_PRICE_TAG, LampiranLain.LOGO_PRICE_TAG_STR);
			if (logo != null && logo.getId() != null) {
				String uri = logo.createLinkUri();
				if (uri != null && uri.trim().length() > 0) {
					return uri;
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return rootUrl(Common.getKonfigurasi("kantin_price_tag_logo_default_url", "/img/logo.png").getNilai());
	}

	private String rootUrl(String url) {
		if (url == null || url.trim().length() == 0) {
			url = "/img/logo.png";
		}
		url = url.trim();
		String lower = url.toLowerCase();
		if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("data:")) {
			return url;
		}
		if (url.startsWith(Common.ROOT + "/")) {
			return url;
		}
		if (url.startsWith("/")) {
			return Common.ROOT + url;
		}
		return Common.ROOT + "/" + url;
	}
	// ===================== pembantu kecil =====================

	private Button tombol(String teks, EventListener onClick) {
		Button b = new Button(teks);
		b.addEventListener("onClick", onClick);
		return b;
	}

	private void kolom(Listhead lh, String label, String width) {
		Listheader h = new Listheader(label);
		if (width != null) {
			h.setWidth(width);
		}
		h.setParent(lh);
	}

	private void radio(Radiogroup g, String val, boolean sel) {
		Radio r = new Radio(val);
		r.setValue(val);
		r.setParent(g);
		if (sel) {
			r.setSelected(true);
		}
	}

	private void comboPer(String label, int val, boolean sel) {
		Comboitem ci = new Comboitem(label);
		ci.setValue(Integer.valueOf(val));
		ci.setParent(cboPer);
		if (sel) {
			cboPer.setSelectedItem(ci);
		}
	}

	private void comboTpl(String label, String val, boolean sel) {
		Comboitem ci = new Comboitem(label);
		ci.setValue(val);
		ci.setParent(cboTpl);
		if (sel) {
			cboTpl.setSelectedItem(ci);
		}
	}

	private static Label labelMini(String text) {
		Label l = new Label(text);
		l.setStyle("font-size:11px;color:#64748b;font-weight:600;");
		return l;
	}

	private static String rupiah(Double v) {
		double n = v == null ? 0d : v.doubleValue();
		try {
			java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
			return "Rp " + nf.format(n);
		} catch (Exception e) {
			return "Rp " + (long) n;
		}
	}
}
