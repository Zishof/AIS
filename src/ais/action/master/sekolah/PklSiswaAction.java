package ais.action.master.sekolah;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.SiswaDapatKelompokPkl;
import ais.database.model.Tbmuser;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * <h2>PklSiswaAction &mdash; Pendataan PKL (Praktik Kerja Lapangan) untuk Siswa</h2>
 *
 * <p><b>Untuk apa (bahasa sederhana):</b> halaman bagi admin/guru untuk mendata <b>kelompok PKL
 * siswa</b> beserta anggotanya. Di sini pengguna dapat membuat kelompok PKL (nama, sekolah, tanggal
 * mulai/selesai, alamat tempat PKL, keterangan), lalu memasukkan siswa-siswa yang mengikuti PKL ke
 * dalam kelompok tersebut, serta mengubah atau menghapusnya. Data inilah yang kemudian muncul di
 * e-learning masing-masing siswa sebagai "kelas PKL".</p>
 *
 * <h3>Mengapa memakai ulang engine PKL yang ada</h3>
 * <p>Agar tidak menggandakan model (pelajaran dari kasus modul lain), halaman ini memakai ulang
 * entitas {@link KelompokPkl} yang selama ini dipakai PKL mahasiswa. Sebuah {@code KelompokPkl}
 * ditandai sebagai "PKL siswa" ketika field {@link KelompokPkl#getSekolah() sekolah}-nya terisi;
 * anggotanya disimpan pada entitas {@link SiswaDapatKelompokPkl} (pasangan sekolah dari
 * {@code MahasiswaDapatKelompokPkl}). Dengan begitu, seluruh perilaku PKL mahasiswa tetap utuh
 * (kelompok tanpa sekolah = PKL mahasiswa), sementara jenjang sekolah mendapat alur sendiri yang
 * ringkas namun konsisten.</p>
 *
 * <h3>Cara kerja teknis</h3>
 * <p>Kelas ini adalah <i>composer</i> ZK ({@link GenericAutowireComposer}) yang menempel pada
 * {@code pkl_siswa.zul}. Komponen di ZUL (grid data, paging, kotak pencarian, tombol) di-<i>autowire</i>
 * berdasarkan id, sedangkan seluruh form (tambah/ubah kelompok) dan jendela "Kelola Anggota" dibangun
 * secara terprogram di Java agar mudah dirawat di satu tempat. Daftar kelompok dimuat per halaman
 * ({@code setMaxResults}/{@code setFirstResult}) sehingga hemat memori (tidak memuat seluruh tabel).
 * Seluruh akses basis data memakai {@code HibernateUtil.currentSession()} yang dikelola kerangka
 * kerja dan <b>tidak ditutup manual</b>; tidak ada {@code openSession()}/{@code currentNativeSession()}
 * yang dibuka di kelas ini sehingga tidak diperlukan penutupan di {@code finally}.</p>
 *
 * <h3>Ketahanan &amp; kompatibilitas</h3>
 * <p>Setiap aksi (simpan/hapus/tambah anggota) dibungkus penjagaan {@code null} dan {@code try/catch}
 * gaya 1.6 agar satu kegagalan tidak merusak seluruh halaman; pesan kesalahan ditampilkan secara
 * ramah. Kode dijaga kompatibel Java 1.7 (tanpa lambda/stream). Filter pencarian memakai
 * pencocokan sebagian (ANYWHERE) yang aman terhadap huruf besar/kecil.</p>
 *
 * @author eCampus
 * @see KelompokPkl
 * @see SiswaDapatKelompokPkl
 */
public class PklSiswaAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 8925101220301468191L;

	// === Komponen ZUL (autowire by id) ===
	protected MyGrid grid;
	protected Paging paging;
	protected Paging paging2;
	protected Combobox searchsekolah;
	protected Textbox searchnama;
	protected MyWindow addWindow;

	// === Field form (tambah/ubah) ===
	private KelompokPkl kelompokPkl;
	private Combobox comboSekolah;
	private Textbox namaKelompok;
	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSelesai;
	private Textbox alamat;
	private Textbox keterangan;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		isiComboSekolah(searchsekolah, true);
		loadData(null);
	}

	/**
	 * Mengisi combobox sekolah dengan seluruh sekolah (urut nama). Bila {@code pakaiSemua} benar,
	 * ditambahkan item "== Semua Sekolah ==" bernilai {@code null} di atas dan dipilih sebagai default
	 * (untuk kotak filter). Untuk form tambah/ubah, {@code pakaiSemua} bernilai {@code false} sehingga
	 * pengguna wajib memilih satu sekolah.
	 */
	@SuppressWarnings("unchecked")
	private void isiComboSekolah(Combobox combo, boolean pakaiSemua) {
		if (combo == null) {
			return;
		}
		combo.getItems().clear();
		combo.setReadonly(true);
		if (pakaiSemua) {
			MyComboitemConfig semua = new MyComboitemConfig("== Semua Sekolah ==");
			semua.setValue(null);
			combo.appendChild(semua);
			combo.setSelectedItem(semua);
		} else {
			MyComboitemConfig pilih = new MyComboitemConfig("-- Pilih Sekolah --");
			pilih.setValue(null);
			combo.appendChild(pilih);
			combo.setSelectedItem(pilih);
		}
		try {
			Session session = HibernateUtil.currentSession();
			List<Sekolah> sekolahs = session.createCriteria(Sekolah.class).addOrder(Order.asc("nama"))
					.setMaxResults(Common.MAX_RESULT_500).list();
			for (Sekolah s : sekolahs) {
				if (s == null) {
					continue;
				}
				Comboitem ci = new Comboitem(s.getNama() == null ? "-" : s.getNama());
				ci.setValue(s);
				ci.setParent(combo);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Membangun kriteria daftar kelompok PKL SISWA (hanya yang punya scope sekolah) + filter. */
	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria crit = session.createCriteria(KelompokPkl.class).add(Restrictions.isNotNull("sekolah"));

		Sekolah sekolahFilter = (searchsekolah != null && searchsekolah.getSelectedItem() != null)
				? (Sekolah) searchsekolah.getSelectedItem().getValue()
				: null;
		if (sekolahFilter != null) {
			crit.add(Restrictions.eq("sekolah", sekolahFilter));
		}
		String nama = searchnama == null ? "" : searchnama.getValue().trim();
		if (!nama.isEmpty()) {
			crit.add(Restrictions.ilike("nama_kelompok", nama, MatchMode.ANYWHERE));
		}
		if (order) {
			crit.addOrder(Order.desc("id"));
		}
		return crit;
	}

	public void onSearchDefault(Event event) {
		loadData(null);
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		if (grid == null) {
			return;
		}
		Common.initPaging1(initCriteria(false), paging);
		List<KelompokPkl> list = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_1)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_1 * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel model = new SimpleListModel(list);
		grid.setRowRenderer(new KelompokPklSiswaRenderer());
		grid.setModelCheckMobile(model);
		grid.setSclass("dgrid");
	}

	/** Renderer satu baris daftar kelompok PKL siswa: info + tombol aksi. */
	class KelompokPklSiswaRenderer extends MyRowRenderer {
		@Override
		public void render(Row row, Object data) throws Exception {
			row.setValign("top");
			final KelompokPkl k = (KelompokPkl) data;

			new Label(k.getNama_kelompok() == null ? "-" : k.getNama_kelompok()).setParent(row);
			new Label(k.getSekolah() == null || k.getSekolah().getNama() == null ? "-" : k.getSekolah().getNama())
					.setParent(row);

			String tgl = (k.getTanggal_mulai() == null ? "-" : Common.dateFormat.get().format(k.getTanggal_mulai()))
					+ " s/d "
					+ (k.getTanggal_selesai() == null ? "-" : Common.dateFormat.get().format(k.getTanggal_selesai()));
			new Label(tgl).setParent(row);

			int jml = 0;
			try {
				Session session = HibernateUtil.currentSession();
				Number n = (Number) session.createCriteria(SiswaDapatKelompokPkl.class)
						.add(Restrictions.eq("kelompokPkl", k)).setProjection(Projections.rowCount()).uniqueResult();
				jml = n == null ? 0 : n.intValue();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			new Label(jml + " siswa").setParent(row);

			Hbox aksi = new Hbox();
			aksi.setSpacing("2px");
			aksi.setParent(row);

			MyToolbarbuttonConfig bAnggota = new MyToolbarbuttonConfig("Kelola Anggota", "/img/group.gif");
			bAnggota.setParent(aksi);
			bAnggota.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bukaKelolaAnggota(k);
				}
			});

			MyToolbarbuttonConfig bEdit = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			bEdit.setTooltiptext("Ubah kelompok");
			bEdit.setParent(aksi);
			bEdit.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onAdd(new Event("onEdit", null, k));
				}
			});

			MyToolbarbuttonConfig bHapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			bHapus.setTooltiptext("Hapus kelompok");
			bHapus.setParent(aksi);
			bHapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Hapus kelompok PKL \"" + k.getNama_kelompok() + "\" beserta anggotanya ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event ev) throws Exception {
									if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
										return;
									}
									hapusKelompok(k);
								}
							});
				}
			});
		}
	}

	/** Menghapus kelompok PKL siswa beserta seluruh anggotanya (SiswaDapatKelompokPkl). */
	@SuppressWarnings("unchecked")
	private void hapusKelompok(KelompokPkl k) throws Exception {
		try {
			Session session = HibernateUtil.currentSession();
			if (k.getId() != null) {
				List<SiswaDapatKelompokPkl> anggota = session.createCriteria(SiswaDapatKelompokPkl.class)
						.add(Restrictions.eq("kelompokPkl", k)).list();
				for (SiswaDapatKelompokPkl a : anggota) {
					Common.refreshDelete(a);
				}
			}
			Common.refreshDelete(k);
			loadData(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			MyMessageboxConfig.show("Data tidak dapat dihapus karena berelasi dengan data lain: " + e.getMessage());
		}
	}

	/** Membuka form tambah/ubah kelompok PKL siswa. Event {@code onEdit} membawa KelompokPkl yang diedit. */
	public void onAdd(Event event) throws Exception {
		KelompokPkl target = (event != null && event.getData() instanceof KelompokPkl) ? (KelompokPkl) event.getData()
				: new KelompokPkl();
		initForm(target);
	}

	private void initForm(final KelompokPkl target) throws Exception {
		this.kelompokPkl = target;
		Common.clear(addWindow);
		addWindow.setTitle(target.getId() == null ? "Tambah Kelompok PKL Siswa" : "Ubah Kelompok PKL Siswa");

		Borderlayout borderlayout = new MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ZkCompat.setFlex(center, true);

		MyGrid g = new MyGrid();
		g.setSclass("fgrid");
		g.setWidth("100%");
		g.setHeight("100%");
		g.setParent(center);

		Columns cols = new Columns();
		cols.setParent(g);
		MyColumnConfig c1 = new MyColumnConfig();
		c1.setWidth("35%");
		c1.setParent(cols);
		MyColumnConfig c2 = new MyColumnConfig();
		c2.setParent(cols);

		Rows rows = new Rows();
		rows.setParent(g);

		MyFormRow r = new MyFormRow();
		r.setValign("top");
		r.setParent(rows);
		r.appendChild(new MyLabelConfig("Sekolah (*)"));
		comboSekolah = new Combobox();
		comboSekolah.setWidth("95%");
		isiComboSekolah(comboSekolah, false);
		if (target.getSekolah() != null) {
			Common.selectComboItem(comboSekolah, target.getSekolah());
		}
		r.appendChild(comboSekolah);

		r = new MyFormRow();
		r.setParent(rows);
		r.appendChild(new MyLabelConfig("Nama Kelompok (*)"));
		namaKelompok = new Textbox(target.getNama_kelompok());
		namaKelompok.setWidth("95%");
		r.appendChild(namaKelompok);

		r = new MyFormRow();
		r.setParent(rows);
		r.appendChild(new MyLabelConfig("Tanggal Mulai"));
		tanggalMulai = new MyDatebox(target.getTanggal_mulai());
		tanggalMulai.setFormat(Common.dateFormat.get().toPattern());
		r.appendChild(tanggalMulai);

		r = new MyFormRow();
		r.setParent(rows);
		r.appendChild(new MyLabelConfig("Tanggal Selesai"));
		tanggalSelesai = new MyDatebox(target.getTanggal_selesai());
		tanggalSelesai.setFormat(Common.dateFormat.get().toPattern());
		r.appendChild(tanggalSelesai);

		r = new MyFormRow();
		r.setParent(rows);
		r.appendChild(new MyLabelConfig("Alamat / Tempat PKL"));
		alamat = new Textbox(target.getAlamat());
		alamat.setWidth("95%");
		alamat.setRows(2);
		r.appendChild(alamat);

		r = new MyFormRow();
		r.setParent(rows);
		r.appendChild(new MyLabelConfig("Keterangan"));
		keterangan = new Textbox(target.getKeterangan());
		keterangan.setWidth("95%");
		keterangan.setRows(2);
		r.appendChild(keterangan);

		South south = new South();
		ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		batal.setParent(toolbar);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave()) {
					addWindow.setVisible(false);
					loadData(null);
				}
			}
		});
		simpan.setParent(toolbar);

		borderlayout.setParent(addWindow);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private boolean onSave() throws Exception {
		Sekolah sekolah = comboSekolah.getSelectedItem() == null ? null
				: (Sekolah) comboSekolah.getSelectedItem().getValue();
		if (sekolah == null) {
			MyMessageboxConfig.show("Sekolah wajib dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (namaKelompok.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Nama kelompok wajib diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		Session session = HibernateUtil.currentSession();
		if (kelompokPkl.getId() != null) {
			kelompokPkl = (KelompokPkl) session.load(KelompokPkl.class, kelompokPkl.getId());
		}
		kelompokPkl.setSekolah(sekolah);
		kelompokPkl.setNama_kelompok(namaKelompok.getValue().trim());
		kelompokPkl.setTanggal_mulai(tanggalMulai.getValue());
		kelompokPkl.setTanggal_selesai(tanggalSelesai.getValue());
		kelompokPkl.setAlamat(alamat.getValue());
		kelompokPkl.setKeterangan(keterangan.getValue());
		Common.refreshSaveOrUpdate(session, kelompokPkl);
		return true;
	}

	// ============================================================
	// KELOLA ANGGOTA SISWA
	// ============================================================

	/**
	 * Membuka jendela pengelolaan anggota siswa pada sebuah kelompok PKL: daftar anggota (dengan
	 * tombol keluarkan) + kotak pencari siswa untuk menambah anggota baru.
	 */
	private void bukaKelolaAnggota(final KelompokPkl k) throws Exception {
		final MyWindow win = new MyWindow();
		win.setTitle("Anggota PKL — " + (k.getNama_kelompok() == null ? "" : k.getNama_kelompok()));
		if (Common.isMobile()) {
			win.setWidth("100%");
			win.setHeight("100%");
		} else {
			win.setWidth("720px");
			win.setHeight("95%");
		}
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(win);

		Borderlayout bl = new MyBorderlayout();
		Center center = new Center();
		center.setParent(bl);
		ZkCompat.setFlex(center, true);

		final MyGrid gAnggota = new MyGrid();
		gAnggota.setSclass("fgrid");
		gAnggota.setWidth("100%");
		gAnggota.setHeight("100%");
		gAnggota.setParent(center);
		Columns cols = new Columns();
		cols.setParent(gAnggota);
		new MyColumnConfig("No. Induk").setParent(cols);
		new MyColumnConfig("Nama Siswa").setParent(cols);
		new MyColumnConfig("Kelas").setParent(cols);
		MyColumnConfig cAksi = new MyColumnConfig("");
		cAksi.setWidth("90px");
		cAksi.setParent(cols);

		final EventListener reload = new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				muatAnggota(gAnggota, k);
			}
		};

		// Panel atas: pencari siswa untuk menambah anggota.
		South south = new South();
		south.setHeight("70px");
		south.setTitle("Tambah anggota");
		south.setCollapsible(false);
		ZkCompat.setFlex(south, true);
		south.setParent(bl);

		Hbox tambah = new Hbox();
		tambah.setStyle("padding:8px;");
		tambah.setParent(south);
		tambah.appendChild(new MyLabelConfig("Cari & pilih siswa : "));
		final AmbilDataSiswaBanbox banbox = new AmbilDataSiswaBanbox(Boolean.TRUE, Boolean.FALSE, k.getSekolah(), null,
				null, null);
		banbox.setWidth("360px");
		tambah.appendChild(banbox);
		banbox.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				Object obj = banbox.getAttribute("siswa");
				if (obj instanceof Siswa) {
					tambahAnggota(k, (Siswa) obj, reload);
					banbox.setValue("");
				}
			}
		});
		MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				win.detach();
				loadData(null);
			}
		});
		tambah.appendChild(tutup);

		bl.setParent(win);
		muatAnggota(gAnggota, k);
		win.setVisible(true);
		win.onModal();
	}

	@SuppressWarnings("unchecked")
	private void muatAnggota(final MyGrid gAnggota, final KelompokPkl k) {
		try {
			Session session = HibernateUtil.currentSession();
			List<SiswaDapatKelompokPkl> anggota = session.createCriteria(SiswaDapatKelompokPkl.class)
					.add(Restrictions.eq("kelompokPkl", k)).addOrder(Order.asc("id")).list();
			gAnggota.setRowRenderer(new MyRowRenderer() {
				@Override
				public void render(Row row, Object data) throws Exception {
					row.setValign("top");
					final SiswaDapatKelompokPkl a = (SiswaDapatKelompokPkl) data;
					Siswa s = a.getSiswa();
					new Label(s == null || s.getNomorInduk() == null ? "-" : s.getNomorInduk()).setParent(row);
					new Label(s == null || s.getNama() == null ? "-" : s.getNama()).setParent(row);
					String kelas = "-";
					try {
						if (s != null && s.getKelas() != null) {
							kelas = s.getKelas().getNama();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PklSiswaAction.java:541");
					}
					new Label(kelas).setParent(row);
					MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
					hapus.setTooltiptext("Keluarkan dari kelompok");
					hapus.setParent(row);
					hapus.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							try {
								Common.refreshDelete(a);
								muatAnggota(gAnggota, k);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}
					});
				}
			});
			gAnggota.setModelCheckMobile(new SimpleListModel(anggota));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Menambahkan seorang siswa sebagai anggota kelompok PKL (jika belum tergabung). */
	@SuppressWarnings("unchecked")
	private void tambahAnggota(KelompokPkl k, Siswa siswa, EventListener reload) {
		try {
			if (siswa == null || siswa.getId() == null) {
				return;
			}
			Session session = HibernateUtil.currentSession();
			Number ada = (Number) session.createCriteria(SiswaDapatKelompokPkl.class)
					.add(Restrictions.eq("kelompokPkl", k)).add(Restrictions.eq("siswa", siswa))
					.setProjection(Projections.rowCount()).uniqueResult();
			if (ada != null && ada.intValue() > 0) {
				MyMessageboxConfig.show("Siswa \"" + siswa.getNama() + "\" sudah menjadi anggota kelompok ini.",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
			SiswaDapatKelompokPkl anggota = new SiswaDapatKelompokPkl();
			anggota.setKelompokPkl(k);
			anggota.setSiswa(siswa);
			anggota.setDiterima(true);
			Tbmuser u = Common.getCurrentUser();
			if (u != null) {
				anggota.setOleh(u.getUserNama());
				anggota.setOlehId(Common.generateOlehId(u));
			}
			Common.refreshSaveOrUpdate(session, anggota);
			if (reload != null) {
				reload.onEvent(null);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}
}
