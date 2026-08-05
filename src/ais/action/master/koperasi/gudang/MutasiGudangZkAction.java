package ais.action.master.koperasi.gudang;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import ais.action.master.inventory.StokLokasiUtil;
import ais.action.master.koperasi.helper.LokasiKantinUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>MutasiGudangZkAction — pencatatan mutasi stok per lokasi versi <b>ZKoss</b> (native ZK 5.5).</h2>
 *
 * <p>
 * Padanan ZKoss dari halaman JSP "Mutasi Gudang": mencatat barang <b>Masuk</b>, <b>Keluar</b>,
 * <b>Transfer</b> antar lokasi, dan <b>Penyesuaian</b> (opname), lengkap dengan tampilan stok berjalan
 * dan riwayat mutasi terbaru. Seluruh penulisan memakai mesin pusat {@link StokLokasiUtil}
 * (catatMasuk/Keluar/Transfer/Penyesuaian &amp; qtyStok) sehingga aturan tanda qty, pembentukan
 * pasangan transfer, dan perhitungan stok <b>tidak diduplikasi</b> — identik dengan versi JSP. Hanya
 * admin (bukan pedagang/toko) yang boleh mencatat, mengikuti
 * {@link LokasiKantinUtil#bolehKelola(HttpServletRequest)}. Sesi memakai
 * {@link HibernateUtil#currentSession()} (tidak ditutup manual). Kompatibel Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see StokLokasiUtil
 */
public class MutasiGudangZkAction extends MyWindow {

	private static final long serialVersionUID = 4419021551123456003L;

	private boolean boleh;
	private Combobox cbJenis;
	private Combobox cbLokasi;
	private Combobox cbTujuan;
	private Combobox cbProduk;
	private Decimalbox qty;
	private Decimalbox harga;
	private Datebox tanggal;
	private Textbox keterangan;
	private Label stokLabel;
	private MyGrid gridRiwayat;

	public MutasiGudangZkAction() {
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

		Vlayout root = new Vlayout();
		root.setStyle("padding:12px");
		root.setParent(this);
		Label ttl = new Label(ais.common.Common.getBahasaConfig("Mutasi Gudang"));
		ttl.setStyle("font-weight:800;font-size:16px;display:block");
		ttl.setParent(root);
		Label desc = new Label(ais.common.Common.getBahasaConfig("Catat barang masuk, keluar, pindah antar gudang, atau koreksi stok. Stok tiap lokasi dihitung otomatis."));
		desc.setStyle("display:block;color:#64748b;font-size:12px;margin-bottom:8px");
		desc.setParent(root);

		Hlayout kolom = new Hlayout();
		kolom.setStyle("gap:16px;align-items:flex-start");
		kolom.setWidth("100%");
		kolom.setParent(root);

		if (boleh) {
			Vlayout form = new Vlayout();
			form.setStyle("min-width:320px;max-width:360px");
			form.setParent(kolom);

			new Label(ais.common.Common.getBahasaConfig("Jenis")).setParent(form);
			cbJenis = combo(form);
			addItem(cbJenis, "MASUK", "Masuk");
			addItem(cbJenis, "KELUAR", "Keluar");
			addItem(cbJenis, "TRANSFER", "Transfer antar lokasi");
			addItem(cbJenis, "PENYESUAIAN", "Penyesuaian / Opname");
			cbJenis.setSelectedIndex(0);

			new Label(ais.common.Common.getBahasaConfig("Lokasi (asal)")).setParent(form);
			cbLokasi = combo(form);
			new Label(ais.common.Common.getBahasaConfig("Lokasi Tujuan (khusus Transfer)")).setParent(form);
			cbTujuan = combo(form);
			List<Lokasi> lokasis = LokasiKantinUtil.daftarLokasi(session, null, null, true);
			for (int i = 0; i < lokasis.size(); i++) {
				Lokasi l = lokasis.get(i);
				addItem(cbLokasi, String.valueOf(l.getId()), l.getNama());
				addItem(cbTujuan, String.valueOf(l.getId()), l.getNama());
			}

			new Label(ais.common.Common.getBahasaConfig("Produk")).setParent(form);
			cbProduk = new Combobox();
			cbProduk.setWidth("100%");
			cbProduk.setParent(form);
			SQLQuery pq = session.createSQLQuery(
					"select id, kode, nama, coalesce(hargabeli,0) from koperasi.produk where aktif=true order by nama asc");
			pq.setMaxResults(5000);
			List<?> prows = pq.list();
			for (int i = 0; i < prows.size(); i++) {
				Object[] a = (Object[]) prows.get(i);
				Comboitem ci = new Comboitem((a[1] == null ? "" : a[1].toString() + " — ") + (a[2] == null ? "" : a[2].toString()));
				ci.setValue(a[0]);
				ci.setAttribute("harga", a[3]);
				ci.setParent(cbProduk);
			}

			stokLabel = new Label(ais.common.Common.getBahasaConfig("Stok di lokasi: -"));
			stokLabel.setStyle("display:block;color:#334155;font-size:12px;margin:4px 0");
			stokLabel.setParent(form);
			EventListener cekStok = new EventListener() {
				public void onEvent(Event e) throws Exception {
					perbaruiStok();
				}
			};
			cbLokasi.addEventListener(Events.ON_SELECT, cekStok);
			cbProduk.addEventListener(Events.ON_SELECT, cekStok);

			Hlayout baris = new Hlayout();
			baris.setStyle("gap:8px");
			baris.setParent(form);
			Vlayout c1 = new Vlayout();
			c1.setParent(baris);
			new Label(ais.common.Common.getBahasaConfig("Jumlah")).setParent(c1);
			qty = new Decimalbox();
			qty.setWidth("140px");
			qty.setParent(c1);
			Vlayout c2 = new Vlayout();
			c2.setParent(baris);
			new Label(ais.common.Common.getBahasaConfig("Harga Satuan")).setParent(c2);
			harga = new Decimalbox();
			harga.setWidth("140px");
			harga.setParent(c2);

			new Label(ais.common.Common.getBahasaConfig("Tanggal")).setParent(form);
			tanggal = new Datebox(new Date());
			tanggal.setWidth("100%");
			tanggal.setParent(form);
			new Label(ais.common.Common.getBahasaConfig("Keterangan")).setParent(form);
			keterangan = new Textbox();
			keterangan.setWidth("100%");
			keterangan.setParent(form);

			Button simpan = new Button("Simpan Mutasi");
			simpan.setStyle("background:#0d6efd;color:#fff;margin-top:10px");
			simpan.addEventListener(Events.ON_CLICK, new EventListener() {
				public void onEvent(Event e) throws Exception {
					simpan();
				}
			});
			simpan.setParent(form);
		}

		Vlayout kanan = new Vlayout();
		kanan.setStyle("flex:1");
		kanan.setWidth(boleh ? "60%" : "100%");
		kanan.setParent(kolom);
		Label rt = new Label(ais.common.Common.getBahasaConfig("Riwayat Mutasi Terbaru"));
		rt.setStyle("font-weight:700;display:block;margin-bottom:6px");
		rt.setParent(kanan);
		gridRiwayat = new MyGrid();
		gridRiwayat.setWidth("100%");
		gridRiwayat.setParent(kanan);
		Columns cols = new Columns();
		cols.setParent(gridRiwayat);
		kolomGrid(cols, "Tanggal");
		kolomGrid(cols, "Jenis");
		kolomGrid(cols, "Lokasi");
		kolomGrid(cols, "Produk");
		kolomGrid(cols, "Qty");
		new Rows().setParent(gridRiwayat);

		muatRiwayat();
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

	private void perbaruiStok() {
		Long lok = idTerpilih(cbLokasi);
		Long prd = idTerpilih(cbProduk);
		if (lok == null || prd == null) {
			stokLabel.setValue("Stok di lokasi: -");
			return;
		}
		double s = StokLokasiUtil.qtyStok(HibernateUtil.currentSession(), lok, prd);
		stokLabel.setValue("Stok di lokasi: " + java.text.NumberFormat.getInstance().format(s));
	}

	private void simpan() throws Exception {
		String jenis = cbJenis.getSelectedItem() == null ? "MASUK" : cbJenis.getSelectedItem().getValue().toString();
		Long lok = idTerpilih(cbLokasi);
		Long prd = idTerpilih(cbProduk);
		if (lok == null) {
			MyMessageboxConfig.show("Mohon maaf, lokasi asal belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar Lokasi (asal); (2) pilih salah satu lokasi; (3) ulangi penyimpanan mutasi.");
			return;
		}
		if (prd == null) {
			MyMessageboxConfig.show("Mohon maaf, produk belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar Produk; (2) pilih produk yang akan dimutasi; (3) ulangi penyimpanan mutasi.");
			return;
		}
		double j = qty.getValue() == null ? 0d : qty.getValue().doubleValue();
		if (!"PENYESUAIAN".equals(jenis) && !(j > 0)) {
			MyMessageboxConfig.show("Mohon maaf, jumlah mutasi harus lebih besar dari 0. Langkah yang dapat dilakukan: (1) isi kolom Jumlah dengan angka lebih dari 0; (2) ulangi penyimpanan mutasi.");
			return;
		}
		if ("PENYESUAIAN".equals(jenis) && j == 0) {
			MyMessageboxConfig.show("Mohon maaf, nilai selisih penyesuaian tidak boleh 0. Langkah yang dapat dilakukan: (1) isi kolom Jumlah dengan selisih stok yang sebenarnya (boleh positif atau negatif); (2) ulangi penyimpanan penyesuaian.");
			return;
		}
		Double h = harga.getValue() == null ? null : Double.valueOf(harga.getValue().doubleValue());
		Date tgl = tanggal.getValue() == null ? new Date() : tanggal.getValue();
		String ket = keterangan.getValue();
		HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		Tbmuser u = Common.getCurrentUser(req);
		String oleh = u != null && u.getUserNama() != null ? u.getUserNama() : (u == null ? null : String.valueOf(u.getUserId()));
		String olehId = u == null ? null : String.valueOf(u.getUserId());
		Session session = HibernateUtil.currentSession();
		try {
			if ("TRANSFER".equals(jenis)) {
				Long tujuan = idTerpilih(cbTujuan);
				if (tujuan == null) {
					MyMessageboxConfig.show("Mohon maaf, lokasi tujuan belum dipilih untuk mutasi jenis Transfer. Langkah yang dapat dilakukan: (1) pilih Lokasi Tujuan; (2) pastikan berbeda dari lokasi asal; (3) ulangi penyimpanan mutasi.");
					return;
				}
				StokLokasiUtil.catatTransfer(session, lok, tujuan, prd, j, h, tgl, ket, oleh, olehId);
			} else if ("KELUAR".equals(jenis)) {
				StokLokasiUtil.catatKeluar(session, lok, prd, j, h, tgl, ket, null, oleh, olehId);
			} else if ("PENYESUAIAN".equals(jenis)) {
				StokLokasiUtil.catatPenyesuaian(session, lok, prd, j, h, tgl, ket, oleh, olehId);
			} else {
				StokLokasiUtil.catatMasuk(session, lok, prd, j, h, tgl, ket, null, oleh, olehId);
			}
			qty.setValue((java.math.BigDecimal) null);
			keterangan.setValue("");
			perbaruiStok();
			muatRiwayat();
			MyMessageboxConfig.show("Mutasi stok telah berhasil disimpan. Terima kasih.");
		} catch (IllegalArgumentException iae) {
			MyMessageboxConfig.show(iae.getMessage());
		} catch (Exception ex) {
			Common.tampilErrorJikaAdmin(ex);
			MyMessageboxConfig.show(MyMessageboxConfig.format(
					"Mohon maaf, penyimpanan mutasi gagal diproses. Rincian: {V1}. Langkah yang dapat dilakukan: (1) periksa kembali data yang diisi; (2) coba simpan ulang; (3) apabila masih gagal, mohon hubungi administrator sistem.",
					ex.getMessage()));
		}
	}

	@SuppressWarnings("unchecked")
	private void muatRiwayat() {
		Rows rows = gridRiwayat.getRows();
		rows.getChildren().clear();
		Session session = HibernateUtil.currentSession();
		SQLQuery q = session.createSQLQuery(
				"select m.tanggal, m.jenis, l.nama, p.kode, p.nama, m.qty from asset.mutasi_lokasi m "
						+ "join asset.lokasi l on l.id=m.lokasi join koperasi.produk p on p.id=m.produk "
						+ "order by m.tanggal desc, m.id desc");
		q.setMaxResults(100);
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		List<Object[]> data = q.list();
		for (int i = 0; i < data.size(); i++) {
			Object[] a = data.get(i);
			Row r = new Row();
			r.setParent(rows);
			new Label(a[0] == null ? "" : df.format((Date) a[0])).setParent(r);
			new Label(a[1] == null ? "" : a[1].toString()).setParent(r);
			new Label(a[2] == null ? "" : a[2].toString()).setParent(r);
			new Label((a[3] == null ? "" : a[3].toString() + " ") + (a[4] == null ? "" : a[4].toString())).setParent(r);
			double v = a[5] == null ? 0d : ((Number) a[5]).doubleValue();
			Label ql = new Label(String.valueOf(v));
			ql.setStyle(v < 0 ? "color:#dc2626;font-weight:700" : "color:#16a34a;font-weight:700");
			ql.setParent(r);
		}
	}
}
