package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.DetailAbsenPiketHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AbsenPiket;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Layar CRUD master data Absen Piket (sesi pencatatan kehadiran siswa oleh guru piket) pada
 * modul sekolah, diimplementasikan langsung di atas {@link GenericAutowireComposer} (pola ZK
 * "manual", seperti {@link ais.action.master.payroll.AsuransiPegawaiAction}) sambil
 * mengimplementasikan {@link ais.ui.util.DataCriteria}, {@link DataSearchDefault}, dan
 * {@link DataInitDefault}. Setiap baris {@link AbsenPiket} mengaitkan yayasan, sekolah, kelas,
 * guru piket (atau petugas non-guru lewat {@code pegawai}), tahun ajaran/semester, dan tanggal.
 *
 * <p>
 * Detail kehadiran siswa per sesi ditampilkan lewat baris yang dapat diperluas, didelegasikan ke
 * {@link DetailAbsenPiketHelper}. Combobox kelas pada form maupun filter bersifat cascading
 * terhadap tahun ajaran/sekolah, dan pada form tambah/ubah juga terhadap guru terpilih (kelas
 * yang mensyaratkan guru pembina/BK tertentu hanya muncul bila guru terpilih cocok); memilih
 * kelas yang sudah punya guru pembina otomatis mengunci field guru ke guru pembina tersebut.
 * Pencarian mendukung rentang tanggal (default 3 bulan ke belakang hingga besok), filter
 * yayasan/sekolah/kelas/tahun ajaran/semester/keterangan, guru (dicocokkan ke salah satu dari 5
 * kolom guru pengampu, guru pembina kelas, atau pegawai terkait guru tersebut), dan siswa
 * (nama/NIS/NISN) — bagi pengguna orang tua, daftar otomatis dibatasi ke kelas anak-anaknya.
 * Method {@link #onAbsen(Event)} memuat iframe {@code /welsis.zul} (integrasi absensi eksternal)
 * secara lazy saat panel absen pertama kali dibuka.
 * </p>
 */
public class AbsenPiketAction extends GenericAutowireComposer
		implements ais.ui.util.DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	protected Textbox searchketerangan;
	protected Textbox searchsiswa;

	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchta;
	private Combobox searchsmt;

	private Combobox searchkelas;
	private AmbilDataGuruBanbox searchguru;

	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private AbsenPiket absenPiket;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Combobox kelas;
	private AmbilDataGuruBanbox guru;
	private Combobox tahunAjaran;
	private Combobox semester;

	private Tbmuser tbmuser;
	private MyDatebox tanggal;

	private MyDatebox start;
	private MyDatebox end;

	protected Tabpanel absenPanel;

	private PerguruanTinggi perguruanTinggi;
	private AmbilDataPegawaiBanbox pegawai;

	/** Memuat iframe {@code /welsis.zul} (integrasi absensi eksternal) ke dalam {@link #absenPanel} secara lazy, hanya sekali saat panel masih kosong. */
	public void onAbsen(Event event) {
		if (absenPanel != null && absenPanel.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(absenPanel);
			MyInclude iframe = new MyInclude("/welsis.zul");
			iframe.setParent(window);
		}
	}

	/** Memaksa pemeriksaan keamanan halaman ({@code Common#doCheckSecurity}) sebelum komponen ZUL dikomposisi. */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Menyiapkan seluruh filter pencarian (tahun ajaran, semester, rentang tanggal default 3
	 * bulan ke belakang, kelas cascading terhadap tahun ajaran, yayasan/sekolah), menghitung hak
	 * tambah/ubah/hapus, memasang paging, menambahkan tombol cetak/unggah data massal ke
	 * toolbar, dan memuat data awal.
	 */
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		Common.generateTahunAjaran(searchta);

		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		tbmuser = Common.getCurrentUser();

		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(1); }
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(2); }
		searchsmt.appendChild(comboitem);
		if (searchsmt != null) { searchsmt.setCols(2); }

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		if (searchsmt != null) { searchsmt.setReadonly(true); }

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 3);
		if (start != null) start.setValue(calendar.getTime());
		
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		final EventListener kelasEvent = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (searchta == null || searchta.getSelectedItem() == null) return;
				String ta = (String) searchta.getSelectedItem().getValue();
				Sekolah s = tbmuser == null ? null : tbmuser.ambilSekolah();
				
				Common.insertComboDanSemua(searchkelas, new String[] { "nama", "tahunAjaran", "ruang" }, "keterangan",
						KelasSiswa.class,
						Restrictions.and(Restrictions.eq("tahunAjaran", ta), Restrictions.and(
								Restrictions.or(Restrictions.isNull("sekolah"),
										s == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

				Common.selectComboItem(searchkelas, null);
			}
		};

		if (searchta != null) searchta.addEventListener("onChange", kelasEvent);

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents1 = new String[] { "id", "tahunAjaran", "semester", "tanggal", "kelas", "guru", "guru2",
				"guru3", "guru4", "guru5", "pegawai", "sekolah", "keterangan" };
		
		if (add != null && add.getParent() != null) {
			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents1);
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

			MyToolbarbuttonConfig upload = Common.uploadData(this, AbsenPiket.class, contents1);
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
			Common.appendKeToolbar(upload, add, comp);
		}

		if (searchguru != null) {
			searchguru.setEventListener(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});
		}

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				kelasEvent.onEvent(arg0);
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	/** Perenderan satu baris tabel absen piket: detail kehadiran siswa yang dapat diperluas (memuat lewat {@link DetailAbsenPiketHelper} saat dibuka), tahun ajaran/semester, tanggal (tautan riwayat revisi), nama sekolah, nama kelas, guru/petugas piket, keterangan, dan tombol edit/hapus. */
	class AbsenPiketRenderer extends ais.ui.util.MyRowRenderer {
		private DetailAbsenPiketHelper detailAbsenPiketHelper = new DetailAbsenPiketHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final AbsenPiket ap = (AbsenPiket) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (detail.getChildren().isEmpty() && detail.isOpen()) {
						detailAbsenPiketHelper.displayDetailPA(ap, detail, addWindow);
					}
				}
			});

			new Label(ap.getTahunAjaran() + "/" + ap.getSemester()).setParent(arg0);
			RevisiHelper.createNewRevisi(AbsenPiket.class, ap, Common.dateFormat5.get().format(ap.getTanggal())).setParent(arg0);
			new Label(ap.getSekolah() == null ? "" : ap.getSekolah().getNama()).setParent(arg0);
			new Label(ap.getKelas() == null ? "" : ap.getKelas().getNama()).setParent(arg0);

			if (ap.getGuru() != null) {
				Common.displayGuruAbsenPiket(arg0, ap, false);
			} else {
				new Label(ap.getPegawai() == null ? "" : ap.getPegawai().getNama()).setParent(arg0);
			}
			new Label(ap.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, ap, AbsenPiketAction.this).setParent(arg0);
		}
	}

	/** Membuka dialog tambah dengan entitas {@link AbsenPiket} baru (kosong). */
	public void onAdd(Event event) throws Exception {
		init(new AbsenPiket());
		if (addWindow != null) {
			addWindow.setVisible(true);
			addWindow.onModal();
		}
	}

	/** Membuka dialog ubah untuk entitas {@code obj} yang diberikan (dipanggil dari tombol edit baris tabel). */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		absenPiket = (AbsenPiket) obj;
		init(absenPiket);
		if (addWindow != null) {
			addWindow.setVisible(true);
			addWindow.onModal();
		}
	}

	/**
	 * Membangun form tambah/ubah absen piket (tahun ajaran, semester, yayasan, sekolah, guru,
	 * kelas, petugas, tanggal, keterangan) beserta toolbar Batal/Simpan. Kombinasi
	 * yayasan/sekolah/guru/kelas saling cascading: kelas yang tersedia bergantung tahun
	 * ajaran+sekolah+guru terpilih; memilih kelas dengan guru pembina otomatis mengunci field
	 * guru ke guru pembina tersebut. Bagi data baru, petugas diisi otomatis dari pegawai
	 * pengguna yang login.
	 */
	private void init(final AbsenPiket absenPiket) throws Exception {
		this.absenPiket = absenPiket;
		addWindow.setTitle(absenPiket.getId() == null ? "Tambah Presensi / Kehadiran" : "Ubah Presensi / Kehadiran");
		addWindow.setWidth("550px");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center centerComponent = new Center();
		centerComponent.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(centerComponent, true);
		
		MyGrid formGrid = new MyGrid();
		formGrid.setWidth("100%");
		formGrid.setParent(centerComponent);
		formGrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(formGrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(formGrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		tahunAjaran = Common.generateTahunAjaran(tahunAjaran);
		Common.selectComboItem(true, tahunAjaran, absenPiket.getTahunAjaran());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(semester = new Combobox());
		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(1);
		semester.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(2);
		semester.appendChild(comboitem);
		Common.selectComboItem(true, semester, absenPiket.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);

		yayasan = new Combobox();
		sekolah = new Combobox();
		guru = new AmbilDataGuruBanbox();
		kelas = new Combobox();

		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, absenPiket.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, absenPiket.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru *"));
		row.appendChild(guru);

		if (searchguru != null && searchguru.getAttribute("guru") != null) {
			absenPiket.setGuru((Guru) searchguru.getAttribute("guru"));
			guru.setDisabled(searchguru.isDisabled());
		}

		guru.setAttribute("guru", absenPiket.getGuru());
		guru.setAttribute("myValue", absenPiket.getGuru());
		guru.setValue(absenPiket.getGuru() == null ? "" : absenPiket.getGuru().getNamaGuru());
		guru.setWidth("90%");
		guru.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas *"));
		row.appendChild(kelas);
		kelas.setWidth("90%");
		kelas.setReadonly(true);

		EventListener kelasEvent = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Guru g = (Guru) guru.getAttribute("guru");
				if (tahunAjaran == null || tahunAjaran.getSelectedItem() == null) return;
				String ta = (String) tahunAjaran.getSelectedItem().getValue();
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				
				Common.insertCombo(kelas, new String[] { "nama", "tahunAjaran", "ruang" }, "keterangan",
						KelasSiswa.class,
						Restrictions.and(
								g != null ? Restrictions.or(
										Restrictions.or(
												Restrictions.or(Restrictions.isNull("absensiharusGuruPembina"), Restrictions.eq("absensiharusGuruPembina", false)),
												Restrictions.eq("guruPembina", g)),
										Restrictions.or(Restrictions.eq("absensiharusGuruBk", false), Restrictions.eq("guruBk", g)))
								: Restrictions.sqlRestriction("true"),
								Restrictions.and(Restrictions.eq("tahunAjaran", ta),
										Restrictions.and(
												Restrictions.or(Restrictions.isNull("sekolah"), s == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", s)),
												Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))));

				Common.selectComboItem(kelas, absenPiket.getKelas());
			}
		};

		tahunAjaran.addEventListener("onChange", kelasEvent);
		sekolah.addEventListener("onChange", kelasEvent);
		guru.setEventListener(kelasEvent);

		Common.createDefaultTimer(kelasEvent);

		EventListener kelasGuruEvent = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (guru.getAttribute("guru") == null) {
					KelasSiswa kelasSiswaData = (KelasSiswa) (kelas.getSelectedItem() == null ? null : kelas.getSelectedItem().getValue());

					if (kelasSiswaData != null && kelasSiswaData.getGuruPembina() != null) {
						guru.setDisabled(true);
						guru.setAttribute("guru", kelasSiswaData.getGuruPembina());
						guru.setAttribute("myValue", kelasSiswaData.getGuruPembina());
						guru.setValue(kelasSiswaData.getGuruPembina().getNamaGuru());
					} else {
						guru.setDisabled(false);
						guru.setAttribute("guru", null);
						guru.setAttribute("myValue", null);
						guru.setValue("");
					}
				}
			}
		};
		kelas.addEventListener("onChange", kelasGuruEvent);

		if (absenPiket.getId() == null && tbmuser != null && tbmuser.getPegawai() != null) {
			absenPiket.setPegawai(tbmuser.getPegawai());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Petugas"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());

		pegawai.setAttribute("pegawai", absenPiket.getPegawai());
		pegawai.setAttribute("myValue", absenPiket.getPegawai());
		pegawai.setValue(absenPiket.getPegawai() == null ? "" : absenPiket.getPegawai().getNama());
		pegawai.setWidth("90%");
		pegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal/Waktu Absen *"));
		row.appendChild(tanggal = new MyDatebox(absenPiket.getTanggal()));
		tanggal.setFormat(Common.dateFormat3.get().toPattern());
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(absenPiket.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}


	/**
	 * Memvalidasi (yayasan, sekolah, kelas, dan guru piket wajib dipilih) dan menyimpan
	 * (create-or-update, dalam transaksi eksplisit dengan rollback saat gagal) entitas absen
	 * piket dari isian form.
	 *
	 * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (pesan
	 *         peringatan sudah ditampilkan ke pengguna)
	 * @throws Exception diteruskan ulang setelah rollback bila penyimpanan gagal
	 */
	public boolean onSave(Event event) throws Exception {
		if (yayasan == null || yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Yayasan belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Yayasan dan pilih yayasan yang sesuai; (2) pastikan yayasan terpilih sebelum menyimpan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah == null || sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Sekolah belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Sekolah dan pilih sekolah yang sesuai; (2) pastikan yayasan sudah dipilih agar daftar sekolah tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kelas == null || kelas.getSelectedItem() == null || kelas.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kelas belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Kelas dan pilih kelas yang sesuai dari daftar; (2) pastikan sekolah sudah dipilih agar daftar kelas tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (guru == null || guru.getAttribute("guru") == null) {
			MyMessageboxConfig.show("Mohon maaf, Guru Piket belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Guru dan cari atau pilih nama guru piket; (2) pastikan nama guru ditemukan dan terpilih dalam daftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();

			if (absenPiket.getId() != null) {
				absenPiket = (AbsenPiket) session.load(AbsenPiket.class, absenPiket.getId());
			}
			
			absenPiket.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
			absenPiket.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
			absenPiket.setKelas((KelasSiswa) kelas.getSelectedItem().getValue());
			absenPiket.setGuru((Guru) guru.getAttribute("guru"));
			absenPiket.setKeterangan(keterangan.getValue());
			absenPiket.setTahunAjaran((String) tahunAjaran.getSelectedItem().getValue());
			absenPiket.setSemester((Integer) semester.getSelectedItem().getValue());
			absenPiket.setTanggal(tanggal.getValue());
			absenPiket.setPerguruanTinggi(perguruanTinggi);
			absenPiket.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));

			Common.refreshSaveOrUpdate(session, absenPiket);
			tx.commit();
			return true;
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			ais.common.Common.tampilErrorJikaAdmin(e);
			throw e;
		} finally {
			if (session != null) {
				try { session.clear(); session.disconnect(); session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/AbsenPiketAction.java:543");}
			}
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Membangun kriteria pencarian daftar absen piket menggunakan {@code session} yang
	 * diberikan: difilter berdasarkan rentang tanggal, kelas siswa milik anak pengguna orang tua
	 * (bila relevan), nama/NIS/NISN siswa, keterangan, guru (mencocokkan salah satu dari 5 kolom
	 * guru pengampu, guru pembina kelas, atau pegawai terkait guru tersebut lewat OR
	 * disjunction), kelas, semester, tahun ajaran, sekolah, dan yayasan — sesuai isian yang
	 * diberikan pada masing-masing filter.
	 */
	@SuppressWarnings("unchecked")
	public Criteria initCriteria(Session session, boolean order) {
		Criteria criteria = session.createCriteria(AbsenPiket.class)
				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(start.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")))
				.createAlias("kelas", "kelas");

		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			List<Long> kelasIds = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("kelasSiswa.id"))
					.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa())).list();

			if (!kelasIds.isEmpty()) {
				criteria.add(Restrictions.in("kelas.id", kelasIds));
			}
		}

		if (searchsiswa != null && !searchsiswa.getValue().trim().isEmpty()) {
			List<Long> kelasIds = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("kelasSiswa.id")).createAlias("siswa", "siswa")
					.add(Restrictions.or(
							Restrictions.ilike("siswa.nama", searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("siswa.nomorInduk", searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
									Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(), MatchMode.ANYWHERE))))
					.list();

			if (!kelasIds.isEmpty()) {
				criteria.add(Restrictions.in("kelas.id", kelasIds));
			}
		}

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}

		criteria.add(searchketerangan == null || searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE));

		// PERBAIKAN: Kriteria pencarian Multi Guru & Pegawai menggunakan OR Disjunction
		if (searchguru != null) {
			Guru selectedGuru = (Guru) searchguru.getAttribute("guru");
			if (selectedGuru != null) {
				Disjunction guruOr = Restrictions.disjunction();
				guruOr.add(Restrictions.eq("guru", selectedGuru));
				guruOr.add(Restrictions.eq("guru2", selectedGuru));
				guruOr.add(Restrictions.eq("guru3", selectedGuru));
				guruOr.add(Restrictions.eq("guru4", selectedGuru));
				guruOr.add(Restrictions.eq("guru5", selectedGuru));
				guruOr.add(Restrictions.eq("kelas.guruPembina", selectedGuru));
				
				// Gabungkan join ke pegawai terkait dari Guru tersebut
				if (selectedGuru.getPegawai() != null) {
					guruOr.add(Restrictions.eq("pegawai", selectedGuru.getPegawai()));
				}
				criteria.add(guruOr);
			}
		}

		criteria.add(searchkelas == null || searchkelas.getSelectedItem() == null || searchkelas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("kelas", searchkelas.getSelectedItem().getValue()));

		criteria.add(searchsmt == null || searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()));

		criteria.add(searchta == null || searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()));

		criteria.add(searchsekolah == null ? Restrictions.sqlRestriction("1=1") : CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false));

		criteria.add(searchyayasan == null ? Restrictions.sqlRestriction("1=1") : CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		return criteria;
	}

	/** Implementasi {@link ais.ui.util.DataCriteria} legacy: mendelegasikan ke {@link #initCriteria(Session, boolean)} memakai sesi Hibernate saat ini. */
	@Override
	public Criteria initCriteria(boolean order) {
		// Fallback method untuk interface legacy DataCriteria
		return initCriteria(HibernateUtil.currentSession(), order);
	}

	/** Memuat ulang halaman daftar absen piket sesuai kriteria pencarian saat ini (memakai sesi terpisah yang selalu ditutup di {@code finally}), memperbarui paging dan grid. */
	@SuppressWarnings("unchecked")
	@Override
	public void onSearchDefault(Event event) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Common.initPaging(initCriteria(session, false), paging);

			List<AbsenPiket> listData = initCriteria(session, true)
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
					.list();
					
			ListModel strset = new SimpleListModel(listData);
			grid.setRowRenderer(new AbsenPiketRenderer());
			grid.setModelCheckMobile(strset);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try { session.clear(); session.disconnect(); session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/AbsenPiketAction.java:650");}
			}
			HibernateUtil.closeSession();
		}
	}
}
