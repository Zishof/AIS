package ais.action.master.koperasi.gudang;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import ais.action.master.inventory.PengirimanGudangUtil;
import ais.action.master.inventory.StokLokasiUtil;
import ais.action.master.koperasi.helper.LokasiKantinUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.PengirimanGudang;
import ais.database.model.asset.PengirimanGudangDetail;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>PengirimanGudangZkAction — pengiriman Gudang Pusat ↔ Cabang/Outlet versi <b>ZKoss</b>.</h2>
 *
 * <p>
 * Padanan ZKoss dari halaman JSP "Pengiriman Antar Gudang": dua tab — <b>Kirim Baru</b> (buat dokumen
 * multi-baris produk) dan <b>Perlu Diterima</b> (inbox per lokasi tujuan, konfirmasi qty diterima,
 * boleh sebagian). Seluruh penulisan memakai {@link PengirimanGudangUtil} (yang sendiri HANYA
 * memanggil {@link StokLokasiUtil#catatKeluar}/{@link StokLokasiUtil#catatMasuk} yang sudah ada —
 * lihat javadoc {@code PengirimanGudang} soal desain lokasi transit virtual) sehingga logika stok
 * IDENTIK dengan versi JSP, tidak diduplikasi. Hanya admin (bukan pedagang/toko) yang boleh
 * kirim/terima, mengikuti {@link LokasiKantinUtil#bolehKelola}. Sesi memakai
 * {@link HibernateUtil#currentSession()} (tidak ditutup manual). Kompatibel Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see PengirimanGudangUtil
 */
public class PengirimanGudangZkAction extends MyWindow {

	private static final long serialVersionUID = 4419021551123457002L;

	private boolean boleh;
	private List<Lokasi> daftarLokasi;
	private List<Object[]> daftarProduk; // {id, kode, nama, hargabeli}

	// Tab "Kirim Baru"
	private Combobox cbAsal;
	private Combobox cbTujuan;
	private MyGrid gridBaris;
	private Datebox tglKirim;
	private Textbox ketKirim;
	private MyGrid gridRiwayatAsal;

	// Tab "Perlu Diterima"
	private Combobox cbLokasiSaya;
	private Combobox cbFilterStatus;
	private MyGrid gridInbox;
	private Div panelTerima;
	private Label lblTerimaKode;
	private MyGrid gridTerimaBaris;
	private Textbox ketTerima;
	private Long pengirimanTerimaAktifId;

	public PengirimanGudangZkAction() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		setWidth("100%");
		setHeight("100%");
		setBorder("none");
		HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		boleh = LokasiKantinUtil.bolehKelola(req);
		Session session = HibernateUtil.currentSession();
		daftarLokasi = LokasiKantinUtil.daftarLokasi(session, null, null, true);
		daftarProduk = muatDaftarProduk(session);

		Vlayout root = new Vlayout();
		root.setStyle("padding:12px");
		root.setParent(this);
		Label ttl = new Label(Common.getBahasaConfig("Pengiriman Antar Gudang"));
		ttl.setStyle("font-weight:800;font-size:16px;display:block");
		ttl.setParent(root);
		Label desc = new Label(Common.getBahasaConfig(
				"Kirim barang dari Gudang Pusat ke Cabang/Outlet -- stok tujuan baru bertambah SETELAH penerima mengonfirmasi terima (boleh penuh atau sebagian)."));
		desc.setStyle("display:block;color:#64748b;font-size:12px;margin-bottom:8px");
		desc.setParent(root);

		Tabbox tabbox = new Tabbox();
		tabbox.setWidth("100%");
		tabbox.setParent(root);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		new Tab(Common.getBahasaConfig("Kirim Baru")).setParent(tabs);
		new Tab(Common.getBahasaConfig("Perlu Diterima")).setParent(tabs);
		Tabpanels panels = new Tabpanels();
		panels.setParent(tabbox);
		Tabpanel panelKirim = new Tabpanel();
		panelKirim.setParent(panels);
		Tabpanel panelInbox = new Tabpanel();
		panelInbox.setParent(panels);

		buildTabKirim(panelKirim, session);
		buildTabInbox(panelInbox);
	}

	// ======================== Tab: Kirim Baru ========================

	private void buildTabKirim(Tabpanel panel, Session session) {
		Vlayout isi = new Vlayout();
		isi.setStyle("padding:10px;gap:10px");
		isi.setWidth("100%");
		isi.setParent(panel);

		if (boleh) {
			Hlayout lokasiRow = new Hlayout();
			lokasiRow.setStyle("gap:12px");
			lokasiRow.setParent(isi);
			Vlayout c1 = new Vlayout();
			c1.setParent(lokasiRow);
			new Label(Common.getBahasaConfig("Lokasi Asal (mis. Gudang Pusat)")).setParent(c1);
			cbAsal = combo(c1);
			Vlayout c2 = new Vlayout();
			c2.setParent(lokasiRow);
			new Label(Common.getBahasaConfig("Lokasi Tujuan (mis. Cabang/Outlet)")).setParent(c2);
			cbTujuan = combo(c2);
			for (int i = 0; i < daftarLokasi.size(); i++) {
				Lokasi l = daftarLokasi.get(i);
				addItem(cbAsal, String.valueOf(l.getId()), l.getNama());
				addItem(cbTujuan, String.valueOf(l.getId()), l.getNama());
			}
			cbAsal.addEventListener(Events.ON_SELECT, new EventListener() {
				public void onEvent(Event e) throws Exception {
					muatRiwayatAsal();
				}
			});

			Label judulBaris = new Label(Common.getBahasaConfig("Baris Produk"));
			judulBaris.setStyle("font-weight:700;display:block;margin-top:6px");
			judulBaris.setParent(isi);
			gridBaris = new MyGrid();
			gridBaris.setWidth("100%");
			gridBaris.setParent(isi);
			Columns colsBaris = new Columns();
			colsBaris.setParent(gridBaris);
			kolomGrid(colsBaris, "Produk");
			kolomGrid(colsBaris, "Qty");
			kolomGrid(colsBaris, "Harga Satuan");
			kolomGrid(colsBaris, "");
			new Rows().setParent(gridBaris);

			Button tambahBaris = new Button(Common.getBahasaConfig("+ Tambah Baris"));
			tambahBaris.setStyle("margin-top:4px");
			tambahBaris.addEventListener(Events.ON_CLICK, new EventListener() {
				public void onEvent(Event e) throws Exception {
					tambahBarisKirim();
				}
			});
			tambahBaris.setParent(isi);
			tambahBarisKirim();

			Hlayout metaRow = new Hlayout();
			metaRow.setStyle("gap:12px;margin-top:6px");
			metaRow.setParent(isi);
			Vlayout c3 = new Vlayout();
			c3.setParent(metaRow);
			new Label(Common.getBahasaConfig("Tanggal Kirim")).setParent(c3);
			tglKirim = new Datebox(new Date());
			tglKirim.setWidth("160px");
			tglKirim.setParent(c3);
			Vlayout c4 = new Vlayout();
			c4.setStyle("flex:1");
			c4.setParent(metaRow);
			new Label(Common.getBahasaConfig("Keterangan")).setParent(c4);
			ketKirim = new Textbox();
			ketKirim.setWidth("100%");
			ketKirim.setParent(c4);

			Button kirim = new Button(Common.getBahasaConfig("Kirim Sekarang"));
			kirim.setStyle("background:#0d6efd;color:#fff;margin-top:10px");
			kirim.addEventListener(Events.ON_CLICK, new EventListener() {
				public void onEvent(Event e) throws Exception {
					prosesKirim();
				}
			});
			kirim.setParent(isi);
		}

		Label judulRiwayat = new Label(Common.getBahasaConfig("Riwayat Kirim dari Lokasi Asal Terpilih"));
		judulRiwayat.setStyle("font-weight:700;display:block;margin-top:10px");
		judulRiwayat.setParent(isi);
		gridRiwayatAsal = new MyGrid();
		gridRiwayatAsal.setWidth("100%");
		gridRiwayatAsal.setParent(isi);
		Columns colsRiwayat = new Columns();
		colsRiwayat.setParent(gridRiwayatAsal);
		kolomGrid(colsRiwayat, "Kode");
		kolomGrid(colsRiwayat, "Tujuan");
		kolomGrid(colsRiwayat, "Tanggal Kirim");
		kolomGrid(colsRiwayat, "Status");
		new Rows().setParent(gridRiwayatAsal);
	}

	private void tambahBarisKirim() {
		Rows rows = gridBaris.getRows();
		final Row r = new Row();
		r.setParent(rows);
		final Combobox cbProduk = new Combobox();
		cbProduk.setReadonly(true);
		cbProduk.setWidth("220px");
		for (int i = 0; i < daftarProduk.size(); i++) {
			Object[] p = daftarProduk.get(i);
			Comboitem ci = new Comboitem((p[1] == null ? "" : p[1].toString() + " — ") + (p[2] == null ? "" : p[2].toString()));
			ci.setValue(p[0]);
			ci.setAttribute("harga", p[3]);
			ci.setParent(cbProduk);
		}
		cbProduk.setParent(r);
		final Decimalbox qty = new Decimalbox();
		qty.setWidth("100px");
		qty.setParent(r);
		final Decimalbox harga = new Decimalbox();
		harga.setWidth("120px");
		harga.setParent(r);
		cbProduk.addEventListener(Events.ON_SELECT, new EventListener() {
			public void onEvent(Event e) throws Exception {
				if (cbProduk.getSelectedItem() != null && harga.getValue() == null) {
					Object h = cbProduk.getSelectedItem().getAttribute("harga");
					if (h instanceof Number) {
						harga.setValue(BigDecimal.valueOf(((Number) h).doubleValue()));
					}
				}
			}
		});
		Button hapus = new Button("✕");
		hapus.setStyle("color:#dc2626");
		hapus.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception {
				r.detach();
			}
		});
		hapus.setParent(r);
	}

	private void prosesKirim() throws InterruptedException {
		Long asal = idTerpilih(cbAsal);
		Long tujuan = idTerpilih(cbTujuan);
		if (asal == null || tujuan == null) {
			MyMessageboxConfig.show("Mohon maaf, Lokasi Asal dan Lokasi Tujuan wajib dipilih terlebih dahulu.");
			return;
		}
		if (asal.equals(tujuan)) {
			MyMessageboxConfig.show("Mohon maaf, Lokasi Asal dan Lokasi Tujuan tidak boleh sama.");
			return;
		}
		List<PengirimanGudangUtil.DetailKirim> baris = new ArrayList<PengirimanGudangUtil.DetailKirim>();
		@SuppressWarnings("unchecked")
		List<Row> rows = gridBaris.getRows().getChildren();
		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			@SuppressWarnings("unchecked")
			List<org.zkoss.zk.ui.Component> anak = r.getChildren();
			Combobox cbProduk = (Combobox) anak.get(0);
			Decimalbox qty = (Decimalbox) anak.get(1);
			Decimalbox harga = (Decimalbox) anak.get(2);
			Long produkId = cbProduk.getSelectedItem() == null ? null : (Long) cbProduk.getSelectedItem().getValue();
			double q = qty.getValue() == null ? 0d : qty.getValue().doubleValue();
			if (produkId != null && q > 0) {
				Double h = harga.getValue() == null ? null : Double.valueOf(harga.getValue().doubleValue());
				baris.add(new PengirimanGudangUtil.DetailKirim(produkId, q, h));
			}
		}
		if (baris.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, tambahkan minimal satu baris produk dengan jumlah lebih dari 0.");
			return;
		}
		Date tgl = tglKirim.getValue() == null ? new Date() : tglKirim.getValue();
		String ket = ketKirim.getValue();
		HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		Tbmuser u = Common.getCurrentUser(req);
		String oleh = u != null && u.getUserNama() != null ? u.getUserNama() : (u == null ? null : String.valueOf(u.getUserId()));
		String olehId = u == null ? null : String.valueOf(u.getUserId());
		try {
			PengirimanGudang p = PengirimanGudangUtil.kirim(HibernateUtil.currentSession(), asal, tujuan, baris, tgl, ket, oleh, olehId);
			gridBaris.getRows().getChildren().clear();
			tambahBarisKirim();
			ketKirim.setValue("");
			muatRiwayatAsal();
			MyMessageboxConfig.show("Pengiriman " + p.getKode() + " berhasil disimpan.");
		} catch (IllegalArgumentException iae) {
			MyMessageboxConfig.show(iae.getMessage());
		} catch (Exception ex) {
			Common.tampilErrorJikaAdmin(ex);
			MyMessageboxConfig.show(MyMessageboxConfig.format(
					"Mohon maaf, pengiriman gagal diproses. Rincian: {V1}. Silakan periksa kembali data yang diisi lalu coba lagi.",
					ex.getMessage()));
		}
	}

	private void muatRiwayatAsal() {
		Rows rows = gridRiwayatAsal.getRows();
		rows.getChildren().clear();
		Long asal = idTerpilih(cbAsal);
		if (asal == null) {
			return;
		}
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		List<PengirimanGudang> daftar = PengirimanGudangUtil.daftarUntukLokasiAsal(HibernateUtil.currentSession(), asal);
		for (int i = 0; i < daftar.size(); i++) {
			PengirimanGudang p = daftar.get(i);
			Row r = new Row();
			r.setParent(rows);
			new Label(p.getKode()).setParent(r);
			new Label(p.getLokasiTujuan().getNama()).setParent(r);
			new Label(p.getTanggalKirim() == null ? "" : df.format(p.getTanggalKirim())).setParent(r);
			labelStatus(p.getStatus()).setParent(r);
		}
	}

	// ======================== Tab: Perlu Diterima ========================

	private void buildTabInbox(Tabpanel panel) {
		Vlayout isi = new Vlayout();
		isi.setStyle("padding:10px;gap:10px");
		isi.setWidth("100%");
		isi.setParent(panel);

		Hlayout filterRow = new Hlayout();
		filterRow.setStyle("gap:12px");
		filterRow.setParent(isi);
		Vlayout c1 = new Vlayout();
		c1.setParent(filterRow);
		new Label(Common.getBahasaConfig("Lokasi Saya (Tujuan)")).setParent(c1);
		cbLokasiSaya = combo(c1);
		for (int i = 0; i < daftarLokasi.size(); i++) {
			Lokasi l = daftarLokasi.get(i);
			addItem(cbLokasiSaya, String.valueOf(l.getId()), l.getNama());
		}
		cbLokasiSaya.addEventListener(Events.ON_SELECT, new EventListener() {
			public void onEvent(Event e) throws Exception {
				muatInbox();
			}
		});
		Vlayout c2 = new Vlayout();
		c2.setParent(filterRow);
		new Label(Common.getBahasaConfig("Filter Status")).setParent(c2);
		cbFilterStatus = combo(c2);
		addItem(cbFilterStatus, "", "Semua");
		addItem(cbFilterStatus, PengirimanGudang.DIKIRIM, "Dikirim (belum diterima)");
		addItem(cbFilterStatus, PengirimanGudang.DITERIMA_SEBAGIAN, "Diterima Sebagian");
		addItem(cbFilterStatus, PengirimanGudang.DITERIMA, "Diterima Penuh");
		cbFilterStatus.setSelectedIndex(1);
		cbFilterStatus.addEventListener(Events.ON_SELECT, new EventListener() {
			public void onEvent(Event e) throws Exception {
				muatInbox();
			}
		});

		gridInbox = new MyGrid();
		gridInbox.setWidth("100%");
		gridInbox.setParent(isi);
		Columns cols = new Columns();
		cols.setParent(gridInbox);
		kolomGrid(cols, "Kode");
		kolomGrid(cols, "Dari");
		kolomGrid(cols, "Tanggal Kirim");
		kolomGrid(cols, "Status");
		kolomGrid(cols, "");
		new Rows().setParent(gridInbox);

		panelTerima = new Div();
		panelTerima.setStyle("display:none;border:1px solid #e2e8f0;border-radius:12px;padding:12px;margin-top:10px;background:#f8fafc");
		panelTerima.setParent(isi);
		Label judulTerima = new Label(Common.getBahasaConfig("Konfirmasi Terima"));
		judulTerima.setStyle("font-weight:800;display:block;margin-bottom:6px");
		judulTerima.setParent(panelTerima);
		lblTerimaKode = new Label("");
		lblTerimaKode.setStyle("display:block;color:#64748b;font-size:12px;margin-bottom:8px");
		lblTerimaKode.setParent(panelTerima);
		gridTerimaBaris = new MyGrid();
		gridTerimaBaris.setWidth("100%");
		gridTerimaBaris.setParent(panelTerima);
		Columns colsTerima = new Columns();
		colsTerima.setParent(gridTerimaBaris);
		kolomGrid(colsTerima, "Produk");
		kolomGrid(colsTerima, "Qty Dikirim");
		kolomGrid(colsTerima, "Sudah Diterima");
		kolomGrid(colsTerima, "Terima Sekarang");
		kolomGrid(colsTerima, "Kondisi Rusak");
		kolomGrid(colsTerima, "Alasan Rusak");
		new Rows().setParent(gridTerimaBaris);
		new Label(Common.getBahasaConfig("Keterangan Penerimaan (opsional -- isi bila ada selisih)")).setParent(panelTerima);
		ketTerima = new Textbox();
		ketTerima.setWidth("100%");
		ketTerima.setParent(panelTerima);
		Hlayout aksiTerima = new Hlayout();
		aksiTerima.setStyle("gap:8px;margin-top:8px");
		aksiTerima.setParent(panelTerima);
		Button simpanTerima = new Button(Common.getBahasaConfig("Simpan Penerimaan"));
		simpanTerima.setStyle("background:#16a34a;color:#fff");
		simpanTerima.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception {
				prosesTerima();
			}
		});
		simpanTerima.setParent(aksiTerima);
		Button batalTerima = new Button(Common.getBahasaConfig("Batal"));
		batalTerima.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception {
				panelTerima.setStyle(panelTerima.getStyle() + ";display:none");
			}
		});
		batalTerima.setParent(aksiTerima);
	}

	private void muatInbox() {
		Rows rows = gridInbox.getRows();
		rows.getChildren().clear();
		panelTerima.setStyle("display:none;border:1px solid #e2e8f0;border-radius:12px;padding:12px;margin-top:10px;background:#f8fafc");
		Long tujuan = idTerpilih(cbLokasiSaya);
		if (tujuan == null) {
			return;
		}
		String status = cbFilterStatus.getSelectedItem() == null ? "" : String.valueOf(cbFilterStatus.getSelectedItem().getValue());
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		List<PengirimanGudang> daftar = PengirimanGudangUtil.daftarUntukLokasiTujuan(HibernateUtil.currentSession(), tujuan, status);
		for (int i = 0; i < daftar.size(); i++) {
			final PengirimanGudang p = daftar.get(i);
			Row r = new Row();
			r.setParent(rows);
			new Label(p.getKode()).setParent(r);
			new Label(p.getLokasiAsal().getNama()).setParent(r);
			new Label(p.getTanggalKirim() == null ? "" : df.format(p.getTanggalKirim())).setParent(r);
			labelStatus(p.getStatus()).setParent(r);
			boolean bisaTerima = PengirimanGudang.DIKIRIM.equals(p.getStatus()) || PengirimanGudang.DITERIMA_SEBAGIAN.equals(p.getStatus());
			if (bisaTerima) {
				Button btnTerima = new Button(Common.getBahasaConfig("Terima"));
				btnTerima.setStyle("background:#0d6efd;color:#fff");
				btnTerima.addEventListener(Events.ON_CLICK, new EventListener() {
					public void onEvent(Event e) throws Exception {
						bukaPanelTerima(p.getId());
					}
				});
				btnTerima.setParent(r);
			} else {
				new Label("").setParent(r);
			}
		}
	}

	private void bukaPanelTerima(Long pengirimanId) throws InterruptedException {
		Session session = HibernateUtil.currentSession();
		PengirimanGudang p = (PengirimanGudang) session.get(PengirimanGudang.class, pengirimanId);
		if (p == null) {
			MyMessageboxConfig.show("Mohon maaf, dokumen pengiriman tidak ditemukan.");
			return;
		}
		pengirimanTerimaAktifId = pengirimanId;
		lblTerimaKode.setValue(p.getKode() + " (" + p.getLokasiAsal().getNama() + " → " + p.getLokasiTujuan().getNama() + ")");
		Rows rows = gridTerimaBaris.getRows();
		rows.getChildren().clear();
		List<PengirimanGudangDetail> baris = PengirimanGudangUtil.detailPengiriman(session, pengirimanId);
		for (int i = 0; i < baris.size(); i++) {
			final PengirimanGudangDetail d = baris.get(i);
			double qtyKirim = d.getQtyKirim().doubleValue();
			double qtyTerimaSudah = d.getQtyTerima() == null ? 0d : d.getQtyTerima().doubleValue();
			double sisa = qtyKirim - qtyTerimaSudah;
			Row r = new Row();
			r.setAttribute("detailId", d.getId());
			r.setParent(rows);
			new Label((d.getProduk().getKode() == null ? "" : d.getProduk().getKode() + " — ") + d.getProduk().getNama()).setParent(r);
			new Label(String.valueOf(qtyKirim)).setParent(r);
			new Label(String.valueOf(qtyTerimaSudah)).setParent(r);
			Decimalbox inputTerima = new Decimalbox(BigDecimal.valueOf(sisa));
			inputTerima.setWidth("100px");
			inputTerima.setAttribute("detailId", d.getId());
			inputTerima.setParent(r);

			// Fitur "cek kondisi barang" (gap analisis PDF klien 2026-07-26): porsi dari "Terima
			// Sekarang" di atas yg kondisinya rusak -- TIDAK ikut menambah stok tujuan, otomatis
			// dicatat sbg ReturBarang (lihat PengirimanGudangUtil.terima varian baru).
			Decimalbox inputRusak = new Decimalbox(BigDecimal.ZERO);
			inputRusak.setWidth("90px");
			inputRusak.setParent(r);
			Textbox inputAlasanRusak = new Textbox();
			inputAlasanRusak.setWidth("95%");
			// ZK 5.5 di proyek ini tidak mendukung setPlaceholder -- petunjuk cukup lewat tooltip.
			inputAlasanRusak.setTooltiptext("mis. kemasan pecah");
			inputAlasanRusak.setParent(r);
		}
		panelTerima.setStyle("display:block;border:1px solid #e2e8f0;border-radius:12px;padding:12px;margin-top:10px;background:#f8fafc");
	}

	private void prosesTerima() throws InterruptedException {
		if (pengirimanTerimaAktifId == null) {
			return;
		}
		Map<Long, Double> qtyMap = new HashMap<Long, Double>();
		Map<Long, Double> qtyRusakMap = new HashMap<Long, Double>();
		Map<Long, String> alasanRusakMap = new HashMap<Long, String>();
		boolean adaIsi = false;
		@SuppressWarnings("unchecked")
		List<Row> rows = gridTerimaBaris.getRows().getChildren();
		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			Long detailId = (Long) r.getAttribute("detailId");
			@SuppressWarnings("unchecked")
			List<org.zkoss.zk.ui.Component> anak = r.getChildren();
			Decimalbox input = (Decimalbox) anak.get(3);
			double q = input.getValue() == null ? 0d : input.getValue().doubleValue();
			if (q > 0) {
				adaIsi = true;
			}
			qtyMap.put(detailId, Double.valueOf(q));

			Decimalbox inputRusak = (Decimalbox) anak.get(4);
			double qRusak = inputRusak.getValue() == null ? 0d : inputRusak.getValue().doubleValue();
			qtyRusakMap.put(detailId, Double.valueOf(qRusak));
			Textbox inputAlasan = (Textbox) anak.get(5);
			alasanRusakMap.put(detailId, inputAlasan.getValue());
		}
		if (!adaIsi) {
			MyMessageboxConfig.show("Mohon maaf, isi jumlah diterima minimal satu baris dengan nilai lebih dari 0.");
			return;
		}
		HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		Tbmuser u = Common.getCurrentUser(req);
		String oleh = u != null && u.getUserNama() != null ? u.getUserNama() : (u == null ? null : String.valueOf(u.getUserId()));
		String olehId = u == null ? null : String.valueOf(u.getUserId());
		try {
			PengirimanGudang p = PengirimanGudangUtil.terima(HibernateUtil.currentSession(), pengirimanTerimaAktifId, qtyMap,
					qtyRusakMap, alasanRusakMap, ketTerima.getValue(), oleh, olehId);
			panelTerima.setStyle(panelTerima.getStyle() + ";display:none");
			pengirimanTerimaAktifId = null;
			muatInbox();
			MyMessageboxConfig.show("Penerimaan tersimpan (" + p.getStatus() + ").");
		} catch (IllegalArgumentException iae) {
			MyMessageboxConfig.show(iae.getMessage());
		} catch (IllegalStateException ise) {
			MyMessageboxConfig.show(ise.getMessage());
		} catch (Exception ex) {
			Common.tampilErrorJikaAdmin(ex);
			MyMessageboxConfig.show(MyMessageboxConfig.format(
					"Mohon maaf, penerimaan gagal diproses. Rincian: {V1}. Silakan periksa kembali data yang diisi lalu coba lagi.",
					ex.getMessage()));
		}
	}

	// ======================== Util ========================

	@SuppressWarnings("unchecked")
	private List<Object[]> muatDaftarProduk(Session session) {
		SQLQuery pq = session.createSQLQuery(
				"select id, kode, nama, coalesce(hargabeli,0) from koperasi.produk where aktif=true order by nama asc");
		pq.setMaxResults(5000);
		return pq.list();
	}

	private Combobox combo(Vlayout parent) {
		Combobox c = new Combobox();
		c.setReadonly(true);
		c.setWidth("100%");
		c.setParent(parent);
		return c;
	}

	private void addItem(Combobox cb, String value, String label) {
		Comboitem ci = new Comboitem(label);
		ci.setValue(value);
		ci.setParent(cb);
	}

	private void kolomGrid(Columns cols, String label) {
		new Column(label).setParent(cols);
	}

	private Long idTerpilih(Combobox cb) {
		if (cb == null || cb.getSelectedItem() == null) {
			return null;
		}
		Object v = cb.getSelectedItem().getValue();
		if (v == null) {
			return null;
		}
		try {
			return Long.valueOf(v.toString());
		} catch (Exception e) {
			return null;
		}
	}

	private Label labelStatus(String status) {
		Label l = new Label(status);
		String warna = "#64748b";
		if (PengirimanGudang.DIKIRIM.equals(status)) {
			warna = "#d97706";
		} else if (PengirimanGudang.DITERIMA_SEBAGIAN.equals(status)) {
			warna = "#0d6efd";
		} else if (PengirimanGudang.DITERIMA.equals(status)) {
			warna = "#16a34a";
		}
		l.setStyle("font-weight:700;color:" + warna);
		return l;
	}
}
