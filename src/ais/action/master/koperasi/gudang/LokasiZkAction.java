package ais.action.master.koperasi.gudang;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

import ais.action.master.koperasi.helper.LokasiKantinUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.JenisLokasi;
import ais.database.model.asset.Lokasi;
import ais.database.model.inventory.Toko;
import ais.database.model.sirs.Gudang;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>LokasiZkAction — pengelola master "Lokasi (Gudang)" versi <b>ZKoss</b> (native ZK 5.5).</h2>
 *
 * <p>
 * Padanan ZKoss dari halaman JSP "Lokasi (Gudang)": menampilkan daftar tempat fisik (gudang, outlet,
 * kasir, dsb) beserta jenis &amp; outlet/toko terkait dalam sebuah {@link MyGrid}, lengkap dengan
 * tambah/ubah/hapus. Sama seperti versi JSP, seluruh operasi data memakai util pusat
 * {@link LokasiKantinUtil} sehingga <b>tidak ada logika yang diduplikasi</b> dan kedua versi konsisten.
 * Combobox Jenis diisi dari daftar {@link JenisLokasi} aktif, dan Combobox Outlet/Toko dari daftar
 * {@link Toko}. Hak ubah/hapus mengikuti {@link LokasiKantinUtil#bolehKelola(HttpServletRequest)}
 * (admin, bukan pedagang/toko). Sesi memakai {@link HibernateUtil#currentSession()} (tidak ditutup
 * manual). Kompatibel Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see LokasiKantinUtil
 * @see JenisLokasiZkAction
 */
public class LokasiZkAction extends MyWindow {

	private static final long serialVersionUID = 4419021551123456002L;

	private MyGrid grid;
	private boolean boleh;

	public LokasiZkAction() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() {
		setWidth("100%");
		setHeight("100%");
		setBorder("none");
		HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		boleh = LokasiKantinUtil.bolehKelola(req);

		Vlayout root = new Vlayout();
		root.setStyle("padding:12px");
		root.setParent(this);

		Hlayout head = new Hlayout();
		head.setStyle("gap:10px;margin-bottom:8px;align-items:center");
		head.setParent(root);
		Div t = new Div();
		t.setParent(head);
		Label ttl = new Label(ais.common.Common.getBahasaConfig("Lokasi (Gudang)"));
		ttl.setStyle("font-weight:800;font-size:16px;display:block");
		ttl.setParent(t);
		Label desc = new Label(ais.common.Common.getBahasaConfig("Tempat fisik: gudang, outlet, kasir, dan lainnya. Tetapkan jenis agar rapi di laporan."));
		desc.setStyle("display:block;color:#64748b;font-size:12px");
		desc.setParent(t);
		if (boleh) {
			// Fase 2: "Gudang" (hierarki pusat/cabang, gudangInduk) sudah punya layar kelola sendiri di
			// modul SIRS -- SENGAJA tidak diduplikasi di sini, cukup tautan pintas membuka layar yang
			// sama di tab baru. Combobox Gudang pada form Tambah/Ubah di bawah otomatis memuat data
			// terbaru tiap kali form dibuka.
			Button kelolaGudang = new Button("Kelola Gudang");
			kelolaGudang.setTooltiptext(ais.common.Common.getBahasaConfig(
					"Tambah/ubah data Gudang (termasuk Gudang Induk untuk hierarki pusat/cabang) di tab baru"));
			kelolaGudang.setStyle("margin-left:auto");
			kelolaGudang.addEventListener(Events.ON_CLICK, new EventListener() {
				public void onEvent(Event e) throws Exception {
					org.zkoss.zk.ui.util.Clients.evalJavaScript(
							"window.open('" + ais.common.Common.ROOT + "/pages/master/sirs/gudang.zul','_blank');");
				}
			});
			kelolaGudang.setParent(head);

			Button tambah = new Button("Tambah");
			tambah.addEventListener(Events.ON_CLICK, new EventListener() {
				public void onEvent(Event e) throws Exception {
					openForm(null);
				}
			});
			tambah.setParent(head);
		}

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(root);
		Columns cols = new Columns();
		cols.setParent(grid);
		tambahKolom(cols, "Nama Lokasi", null);
		tambahKolom(cols, "Jenis", "150px");
		tambahKolom(cols, "Outlet/Toko", "160px");
		tambahKolom(cols, "Gudang Induk", "160px");
		tambahKolom(cols, "Alamat", null);
		tambahKolom(cols, "Aktif", "80px");
		if (boleh) {
			tambahKolom(cols, "", "120px");
		}
		new Rows().setParent(grid);

		refresh();
	}

	private void tambahKolom(Columns cols, String label, String width) {
		Column c = new Column(label);
		if (width != null) {
			c.setWidth(width);
		}
		c.setParent(cols);
	}

	private void refresh() {
		Rows rows = grid.getRows();
		rows.getChildren().clear();
		Session session = HibernateUtil.currentSession();
		List<Lokasi> daftar = LokasiKantinUtil.daftarLokasi(session, null, null, false);
		for (int i = 0; i < daftar.size(); i++) {
			final Lokasi l = daftar.get(i);
			Row r = new Row();
			r.setParent(rows);

			new Label(l.getNama() == null ? "" : l.getNama()).setParent(r);

			JenisLokasi jl = l.getJenisLokasi();
			if (jl != null) {
				String warna = jl.getWarna() == null ? "#0d6efd" : jl.getWarna();
				Label jb = new Label(jl.getNama() == null ? "" : jl.getNama());
				jb.setStyle("display:inline-block;padding:2px 10px;border-radius:999px;color:#fff;font-weight:600;background:"
						+ warna);
				jb.setParent(r);
			} else {
				new Label("-").setParent(r);
			}

			Toko tk = l.getToko();
			new Label(tk == null ? "-" : (tk.getNama() == null ? ("Toko #" + tk.getId()) : tk.getNama())).setParent(r);

			Gudang gd = l.getGudang();
			if (gd == null) {
				new Label("-").setParent(r);
			} else {
				Gudang gdInduk = gd.getGudangInduk();
				String gdLabel = gd.getNama() == null ? ("Gudang #" + gd.getId()) : gd.getNama();
				Label gdBadge = new Label(gdLabel + (gdInduk == null || gdInduk.getNama() == null ? ""
						: (" (induk: " + gdInduk.getNama() + ")")));
				gdBadge.setStyle("color:#0891b2;font-weight:600;font-size:.85em");
				gdBadge.setParent(r);
			}
			new Label(l.getAlamat() == null ? "" : l.getAlamat()).setParent(r);
			new Label(l.getAktif() != null && l.getAktif().booleanValue() ? "Aktif" : "Nonaktif").setParent(r);

			if (boleh) {
				Hlayout aksi = new Hlayout();
				aksi.setParent(r);
				Button ubah = new Button("Ubah");
				ubah.addEventListener(Events.ON_CLICK, new EventListener() {
					public void onEvent(Event e) throws Exception {
						openForm(l);
					}
				});
				ubah.setParent(aksi);
				Button hapus = new Button("Hapus");
				hapus.addEventListener(Events.ON_CLICK, new EventListener() {
					public void onEvent(Event e) throws Exception {
						onDelete(l);
					}
				});
				hapus.setParent(aksi);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void openForm(final Lokasi l) throws Exception {
		if (!boleh) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		final Window win = new Window(l == null ? "Tambah Lokasi" : "Ubah Lokasi", "normal", true);
		win.setWidth("460px");
		win.setParent(this);
		Vlayout box = new Vlayout();
		box.setStyle("padding:14px");
		box.setParent(win);

		final Textbox nama = tb(box, "Nama Lokasi *", l == null ? "" : l.getNama());

		new Label(ais.common.Common.getBahasaConfig("Jenis")).setParent(box);
		final Combobox cbJenis = new Combobox();
		cbJenis.setReadonly(true);
		cbJenis.setWidth("100%");
		cbJenis.setParent(box);
		Comboitem kosong = new Comboitem("— pilih —");
		kosong.setValue(null);
		kosong.setParent(cbJenis);
		List<JenisLokasi> jenisList = LokasiKantinUtil.daftarJenisLokasi(session, true);
		Long jenisTerpilih = (l != null && l.getJenisLokasi() != null) ? l.getJenisLokasi().getId() : null;
		for (int i = 0; i < jenisList.size(); i++) {
			JenisLokasi j = jenisList.get(i);
			Comboitem ci = new Comboitem(j.getNama());
			ci.setValue(j.getId());
			ci.setParent(cbJenis);
			if (jenisTerpilih != null && jenisTerpilih.equals(j.getId())) {
				cbJenis.setSelectedItem(ci);
			}
		}

		new Label("Outlet/Toko (opsional)").setParent(box);
		final Combobox cbToko = new Combobox();
		cbToko.setReadonly(true);
		cbToko.setWidth("100%");
		cbToko.setParent(box);
		Comboitem tkKosong = new Comboitem("— (tanpa) —");
		tkKosong.setValue(null);
		tkKosong.setParent(cbToko);
		List<Toko> tokoList = session.createCriteria(Toko.class).addOrder(Order.asc("nama")).setMaxResults(2000).list();
		Long tokoTerpilih = (l != null && l.getToko() != null) ? l.getToko().getId() : null;
		for (int i = 0; i < tokoList.size(); i++) {
			Toko tk = tokoList.get(i);
			Comboitem ci = new Comboitem(tk.getNama() == null ? ("Toko #" + tk.getId()) : tk.getNama());
			ci.setValue(tk.getId());
			ci.setParent(cbToko);
			if (tokoTerpilih != null && tokoTerpilih.equals(tk.getId())) {
				cbToko.setSelectedItem(ci);
			}
		}

		new Label(ais.common.Common.getBahasaConfig("Gudang Induk (opsional, Fase 2 — hierarki pusat/cabang)")).setParent(box);
		final Combobox cbGudang = new Combobox();
		cbGudang.setReadonly(true);
		cbGudang.setWidth("100%");
		cbGudang.setParent(box);
		Comboitem gdKosong = new Comboitem("— (tanpa) —");
		gdKosong.setValue(null);
		gdKosong.setParent(cbGudang);
		List<Object[]> gudangList = LokasiKantinUtil.daftarGudangUntukPicker(session);
		Long gudangTerpilih = (l != null && l.getGudang() != null) ? l.getGudang().getId() : null;
		for (int i = 0; i < gudangList.size(); i++) {
			Object[] g = gudangList.get(i);
			Long gid = (Long) g[0];
			Comboitem ci = new Comboitem((String) g[1]);
			ci.setValue(gid);
			ci.setParent(cbGudang);
			if (gudangTerpilih != null && gudangTerpilih.equals(gid)) {
				cbGudang.setSelectedItem(ci);
			}
		}

		final Textbox alamat = tb(box, "Alamat", l == null ? "" : l.getAlamat());
		alamat.setMultiline(true);
		alamat.setRows(2);
		final Textbox ket = tb(box, "Keterangan", l == null ? "" : l.getKeterangan());
		final Checkbox aktif = new Checkbox("Aktif");
		aktif.setChecked(l == null || (l.getAktif() != null && l.getAktif().booleanValue()));
		aktif.setParent(box);

		Hlayout foot = new Hlayout();
		foot.setStyle("margin-top:12px;gap:8px");
		foot.setParent(box);
		Button batal = new Button("Batal");
		batal.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception {
				win.detach();
			}
		});
		batal.setParent(foot);
		Button simpan = new Button("Simpan");
		simpan.setStyle("background:#0d6efd;color:#fff");
		simpan.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception {
				if (nama.getValue() == null || nama.getValue().trim().length() == 0) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi nama terlebih dahulu. Kolom Nama wajib diisi.");
					return;
				}
				Long jenisId = (cbJenis.getSelectedItem() == null) ? null : (Long) cbJenis.getSelectedItem().getValue();
				Long tokoId = (cbToko.getSelectedItem() == null) ? null : (Long) cbToko.getSelectedItem().getValue();
				Long gudangId = (cbGudang.getSelectedItem() == null) ? null : (Long) cbGudang.getSelectedItem().getValue();
				try {
					HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
					String oleh = Common.getCurrentUser(req) != null ? Common.getCurrentUser(req).getUserNama() : null;
					String olehId = Common.getCurrentUser(req) != null ? String.valueOf(Common.getCurrentUser(req).getUserId()) : null;
					LokasiKantinUtil.simpanLokasi(HibernateUtil.currentSession(), l == null ? null : l.getId(),
							nama.getValue(), ket.getValue(), alamat.getValue(), Boolean.valueOf(aktif.isChecked()),
							jenisId, tokoId, gudangId, oleh, olehId);
					win.detach();
					refresh();
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
					MyMessageboxConfig.showFormat("Mohon maaf, Bapak/Ibu. Penyimpanan data lokasi tidak dapat diselesaikan. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) periksa kembali kelengkapan dan kebenaran data yang diisi; (2) coba simpan ulang beberapa saat lagi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, ex.getMessage());
				}
			}
		});
		simpan.setParent(foot);

		win.doModal();
	}

	private void onDelete(final Lokasi l) throws Exception {
		MyMessageboxConfig.showFormatCb("Apakah Bapak/Ibu yakin ingin menghapus lokasi \"{V1}\"? Data yang telah dihapus tidak dapat dikembalikan.", "Konfirmasi",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
					public void onEvent(Event e) throws Exception {
						if (new Integer(e.getData().toString()).intValue() == MyMessageboxConfig.OK) {
							try {
								LokasiKantinUtil.hapusLokasi(HibernateUtil.currentSession(), l.getId());
								refresh();
							} catch (Exception ex) {
								Common.tampilErrorJikaAdmin(ex);
								MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu. Lokasi ini tidak dapat dihapus karena kemungkinan masih digunakan oleh transaksi lain. Langkah yang dapat dilakukan: (1) pastikan tidak ada transaksi yang masih terkait dengan lokasi ini; (2) selesaikan atau pindahkan terlebih dahulu transaksi yang terkait; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						}
					}
				}, (l.getNama() == null ? "" : l.getNama()));
	}

	private Textbox tb(Vlayout box, String label, String value) {
		new Label(label).setParent(box);
		Textbox t = new Textbox(value == null ? "" : value);
		t.setWidth("100%");
		t.setParent(box);
		return t;
	}
}
