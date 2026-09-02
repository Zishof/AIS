package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.Calendar;
import java.util.Date;
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
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Composer ZK ({@link GenericAutowireComposer}) untuk mengelola {@link RencanaTahunAkademik}
 * (periode/tahun akademik yang berlaku, per skema kampus atau sekolah): daftar rencana dalam
 * grid berpencarian dengan filter berlapis (fakultas/jurusan untuk mode perguruan tinggi, atau
 * yayasan/sekolah untuk mode sekolah — mode ditentukan otomatis lewat {@link #initModeAplikasi()}),
 * form tambah/ubah lengkap (nama/semester, rentang tanggal berlaku dan mulai belajar-mengajar,
 * lingkup fakultas/jurusan/program/status awal mahasiswa/tahun angkatan atau yayasan/sekolah,
 * keterangan), serta tab terpisah untuk hari libur nasional.
 * <p>
 * <b>Bagian terpenting</b> kelas ini adalah keluarga method statis
 * {@link #getCurrentRencanaTahunAkademik} yang dipanggil luas di seluruh aplikasi untuk
 * menentukan rencana tahun akademik "yang berlaku" bagi satu user/konteks pada satu waktu.
 * Resolusinya berbasis skor kecocokan ({@link #hitungScoreRencana}/{@link #tambahScoreJikaCocok}):
 * setiap dimensi rencana yang diisi (sekolah, yayasan, fakultas, jurusan, program, status awal
 * mahasiswa, tahun angkatan) harus cocok dengan konteks user — bila salah satu dimensi yang diisi
 * rencana TIDAK cocok, rencana tersebut gugur total (skor -1); dimensi yang kosong pada rencana
 * dianggap "berlaku untuk semua" dan tidak menambah/mengurangi skor. Bobot lebih tinggi diberikan
 * ke kecocokan yang lebih spesifik (sekolah 1000, yayasan 100, jurusan 90, fakultas 80, program
 * 40, status awal mahasiswa 30, tahun angkatan 20) sehingga rencana paling spesifik yang cocok
 * dipilih. Di antara rencana dengan skor sama, yang tanggal mulainya lebih baru dimenangkan
 * ({@link #lebihBaru}). Hasil query rencana tahun akademik di-cache di {@link Common#rencanaTahunAkademiks}
 * dan dimuat ulang otomatis bila kosong ({@link #reloadCacheRencanaTahunAkademikJikaKosong}).
 * </p>
 */
public class RencanaTahunAkademikAction extends GenericAutowireComposer {

	private static final long serialVersionUID = -5779730267402400328L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Combobox searchStatusAwalMahasiswa;
	private Textbox searchTahunAngkatan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Combobox semester;
	private Combobox nama;
	private Textbox keterangan;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;
	private Combobox statusAwalMahasiswa;
	private Textbox tahunAngkatan;
	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSampai;
	private MyDatebox tanggalMulaiBelajarMengajar;
	private Combobox yayasan;
	private Combobox sekolah;

	private boolean edit = false;
	private boolean delete = false;
	private boolean pt = false;
	private boolean ya = false;
	private boolean buatsekolah = false;

	private RencanaTahunAkademik rencanaTahunAkademik;
	private MyToolbarbuttonConfig add;
	private Tabpanel hariLiburNasional;

	/** Konstruktor default (mode aplikasi/scope ditentukan otomatis saat komposisi ZK selesai). */
	public RencanaTahunAkademikAction() {
		this(false);
	}

	/** Konstruktor dengan mode scope eksplisit: {@code buatsekolah=true} memaksa layar ke mode sekolah (yayasan/sekolah), bukan mode perguruan tinggi (fakultas/jurusan). */
	public RencanaTahunAkademikAction(boolean buatsekolah) {
		this.buatsekolah = buatsekolah;
	}

	/** Memeriksa keamanan sesi ({@link Common#doCheckSecurity()}) sebelum komponen ZK mulai dibangun. */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/** Memvalidasi hak akses baca (redirect ke logoff bila gagal), lalu menyiapkan mode aplikasi, komponen filter, data default tahun akademik, dan memuat data awal grid. */
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		initModeAplikasi();
		initKomponenAwal();
		initDataDefaultTahunAkademik();
		initPagingDanLoadData();
	        FilterLanjutHelper.setup(comp);
}

	/** Memuat konten tab "Hari Libur Nasional" secara lazy (sekali saja) lewat include ZUL saat tab tersebut pertama kali dibuka. */
	public void onHariLibur(Event event) {
		if (hariLiburNasional == null) {
			return;
		}
		if (hariLiburNasional.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(hariLiburNasional);
			include.setSrc("/pages/master/library/hari_libur_perpustakaan.zul");
		}
	}

	/** Menentukan mode layar (perguruan tinggi/{@code pt} dan/atau yayasan-sekolah/{@code ya}) dari konfigurasi institusi ({@link Common#chekPtAtauSekolah()}), dipaksa ke mode sekolah bila {@link #buatsekolah} true. */
	private void initModeAplikasi() {
		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa != null && ptYa.length > 0 && ptYa[0];
		ya = ptYa != null && ptYa.length > 1 && ptYa[1];

		if (buatsekolah) {
			pt = false;
			ya = true;
		}
	}

	/** Mengisi combobox filter tahun ajaran dan tombol tambah (visibilitas sesuai hak akses dan mode aplikasi). */
	private void initKomponenAwal() {
		Common.generateTahunAjaranDanSemua(searchnama);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		program = Common.initPrograms(program);
		Common.initPrograms(searchprogram);

		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		statusAwalMahasiswa = new Combobox();
		Common.insertComboDanSemua(statusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
	}

	/** Memastikan tersedia rencana tahun akademik default (ganjil dan genap) untuk tahun sebelumnya, tahun berjalan, dan tahun berikutnya, dibuat otomatis bila belum ada. */
	private void initDataDefaultTahunAkademik() {
		Session sessionData = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		simpanDefaultTahunAkademikJikaBelumAda(sessionData, tahun - 1);
		simpanDefaultTahunAkademikJikaBelumAda(sessionData, tahun);
		simpanDefaultTahunAkademikJikaBelumAda(sessionData, tahun + 1);
	}

	/** Membuat sepasang {@link RencanaTahunAkademik} default (semester ganjil dan genap) untuk {@code tahun}, hanya bila belum ada rencana bernama awalan tahun tersebut. */
	private void simpanDefaultTahunAkademikJikaBelumAda(Session sessionData, int tahun) {
		if (sessionData == null) {
			return;
		}

		try {
			Number count = (Number) sessionData.createCriteria(RencanaTahunAkademik.class)
					.setProjection(Projections.rowCount()).add(Restrictions.ilike("nama", tahun + "", MatchMode.START))
					.uniqueResult();

			if (count != null && count.intValue() > 0) {
				return;
			}

			Fakultas defaultFakultas = getSelectedFakultas(fakultas);
			String tahunAkademik = tahun + "/" + (tahun + 1);

			RencanaTahunAkademik ganjil = new RencanaTahunAkademik();
			ganjil.setNama(tahunAkademik);
			ganjil.setSemester(Perkuliahan.GANJIL);
			ganjil.setFakultas(defaultFakultas);
			sessionData.save(ganjil);

			RencanaTahunAkademik genap = new RencanaTahunAkademik();
			genap.setNama(tahunAkademik);
			genap.setSemester(Perkuliahan.GENAP);
			genap.setFakultas(defaultFakultas);
			sessionData.save(genap);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Mendaftarkan paging grid dan memuat data awal secara asinkron lewat timer default. */
	private void initPagingDanLoadData() {
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/** Renderer baris grid untuk {@link RencanaTahunAkademik}: nama (dengan tombol riwayat revisi), semester, rentang tanggal berlaku dan mulai belajar-mengajar, lingkup (fakultas/jurusan atau yayasan/sekolah), program, status awal mahasiswa, tahun angkatan, keterangan, dan tombol edit/hapus. */
	class RencanaTahunAkademikRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			if (!(data instanceof RencanaTahunAkademik)) {
				row.setVisible(false);
				return;
			}

			final RencanaTahunAkademik rta = (RencanaTahunAkademik) data;

			RevisiHelper.createNewRevisi(RencanaTahunAkademik.class, rta, safe(rta.getNama())).setParent(row);
			new Label(safe(rta.getSemester())).setParent(row);
			new Label(formatTanggal(rta.getTanggalMulai())).setParent(row);
			new Label(formatTanggal(rta.getTanggalSampai())).setParent(row);
			new Label(formatTanggal(rta.getTanggalMulaiBelajarMengajar())).setParent(row);
			new Label(formatUnit(rta)).setParent(row);
			new Label(rta.getProgram() == null ? "Semua" : rta.getProgram()).setParent(row);
			new Label(rta.getStatusAwalMahasiswa() == null ? "Semua" : safe(rta.getStatusAwalMahasiswa().getNama()))
					.setParent(row);
			new Label(rta.getTahunAngkatan() == null ? "Semua" : rta.getTahunAngkatan() + "").setParent(row);
			new Label(safe(rta.getKeterangan())).setParent(row);

			Hbox toolbar = new Hbox();
			toolbar.setParent(row);

			MyToolbarbuttonConfig ubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			ubah.setTooltiptext("Ubah Data");
			ubah.setVisible(edit);
			ubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(rta);
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			ubah.setParent(toolbar);

			MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					konfirmasiHapus(rta);
				}
			});
			hapus.setParent(toolbar);
		}
	}

	/** Meminta konfirmasi lalu menghapus {@code data}, menampilkan pesan bila gagal karena masih berelasi dengan data lain. */
	private void konfirmasiHapus(final RencanaTahunAkademik data) throws Exception {
		MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						int pilihan = Integer.parseInt(event.getData().toString());
						if (pilihan != MyMessageboxConfig.OK) {
							return;
						}

						try {
							Session sessionData = HibernateUtil.currentSession();
							Common.refreshDelete(sessionData, data);
							onSearchDefault(event);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							MyMessageboxConfig.show(
									"Data ini tidak dapat dihapus karena berelasi dengan data lainnya. Error: "
											+ safe(e.getMessage()));
						}
					}
				});
	}

	/** Membuka dialog tambah {@link RencanaTahunAkademik} baru. */
	public void onAdd(Event event) throws Exception {
		init(new RencanaTahunAkademik());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/** Membangun form tambah/ubah {@link RencanaTahunAkademik} untuk {@code data} (baru atau sudah ada), menyusun bagian umum, lingkup (perguruan tinggi atau sekolah sesuai mode), dan keterangan. */
	private void init(RencanaTahunAkademik data) {
		this.rencanaTahunAkademik = data == null ? new RencanaTahunAkademik() : data;

		addWindow.setTitle(data.getId() == null ? "Tambah Rencana Tahun Akademik" : "Ubah Rencana Tahun Akademik");
		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid gridForm = new MyGrid();
		gridForm.setWidth("100%");
		gridForm.setHeight("100%");
		gridForm.setParent(center);

		Columns columns = new Columns();
		columns.setParent(gridForm);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(gridForm);

		initFormUmum(rows, rencanaTahunAkademik);
		initFormPerguruanTinggi(rows, rencanaTahunAkademik);
		initFormSekolah(rows, rencanaTahunAkademik);
		initFormKeterangan(rows, rencanaTahunAkademik);
		initToolbarForm(borderlayout);

		borderlayout.setParent(addWindow);
	}

	/** Menyusun bagian umum form: tahun akademik, semester (ganjil/genap/SP), tanggal mulai/sampai berlaku (wajib), dan tanggal mulai kegiatan belajar-mengajar (opsional). */
	private void initFormUmum(Rows rows, RencanaTahunAkademik data) {
		Row row = createRow(rows, "Tahun Akademik");
		nama = new Combobox();
		Common.generateTahunAjaran(nama);
		Common.selectComboItem(nama, data.getNama());
		nama.setWidth("90%");
		nama.setReadonly(true);
		row.appendChild(nama);

		row = createRow(rows, "Semester");
		semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		semester.appendChild(comboitem);
		Common.selectComboItem(semester, data.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);
		row.appendChild(semester);

		row = createRow(rows, "Tanggal Mulai *");
		tanggalMulai = new MyDatebox(data.getTanggalMulai());
		row.appendChild(tanggalMulai);

		row = createRow(rows, "Tanggal Sampai *");
		tanggalSampai = new MyDatebox(data.getTanggalSampai());
		row.appendChild(tanggalSampai);

		row = createRow(rows, "Tanggal Mulai Kegiatan Belajar Mengajar");
		tanggalMulaiBelajarMengajar = new MyDatebox(data.getTanggalMulaiBelajarMengajar());
		row.appendChild(tanggalMulaiBelajarMengajar);
	}

	/** Menyusun bagian lingkup mode perguruan tinggi (baris hanya tampil bila {@link #pt} true): fakultas, prodi, program, status awal mahasiswa, tahun angkatan — masing-masing opsional (kosong berarti berlaku untuk semua), dengan default terisi dari fakultas/jurusan user yang login bila belum ditentukan. */
	private void initFormPerguruanTinggi(Rows rows, RencanaTahunAkademik data) {
		Tbmuser user = Common.getCurrentUser();

		Row row = createRow(rows, "Fakultas");
		row.setVisible(pt);
		Common.selectComboItem(fakultas, data.getFakultas() == null && user != null ? user.ambilFakultas()
				: data.getFakultas());
		fakultas.setWidth("90%");
		fakultas.setReadonly(false);
		row.appendChild(fakultas);

		if (pt) {
			Common.initKeterangan(rows, "Berlaku untuk \"" + Common.getBahasaConfig("Fakultas")
					+ "\" tertentu, kosongkan jika berlaku untuk semua.");
		}

		refreshJurusanByFakultas();

		row = createRow(rows, "Prodi");
		row.setVisible(pt);
		Common.pilihJurusan(jurusan, data.getJurusan() == null && user != null ? user.ambilJurusan()
				: data.getJurusan());
		jurusan.setWidth("90%");
		jurusan.setReadonly(false);
		row.appendChild(jurusan);

		if (pt) {
			Common.initKeterangan(rows, "Berlaku untuk \"" + Common.getBahasaConfig("Jurusan")
					+ "\" tertentu, kosongkan jika berlaku untuk semua.");
		}

		row = createRow(rows, "Program");
		row.setVisible(pt);
		Common.selectComboItem(program, data.getProgram());
		program.setWidth("90%");
		program.setReadonly(false);
		row.appendChild(program);

		if (pt) {
			Common.initKeterangan(rows, "Berlaku untuk \"Program\" tertentu, kosongkan jika berlaku untuk semua.");
		}

		row = createRow(rows, "Status Awal Mahasiswa");
		row.setVisible(pt);
		Common.selectComboItem(statusAwalMahasiswa, data.getStatusAwalMahasiswa());
		statusAwalMahasiswa.setWidth("90%");
		statusAwalMahasiswa.setReadonly(false);
		row.appendChild(statusAwalMahasiswa);

		if (pt) {
			Common.initKeterangan(rows,
					"Berlaku untuk mahasiswa dengan \"Status Awal Mahasiswa\" tertentu, kosongkan jika berlaku untuk semua.");
		}

		row = createRow(rows, "Tahun Angkatan Mahasiswa");
		row.setVisible(pt);
		tahunAngkatan = new Textbox(data.getTahunAngkatan() == null ? "" : data.getTahunAngkatan() + "");
		tahunAngkatan.setWidth("90%");
		row.appendChild(tahunAngkatan);

		if (pt) {
			Common.initKeterangan(rows,
					"Berlaku untuk mahasiswa dengan \"Tahun Angkatan\" tertentu, kosongkan jika berlaku untuk semua.");
		}
	}

	/** Menyusun bagian lingkup mode sekolah (baris hanya tampil bila {@link #ya} true): yayasan dan sekolah, dengan default terisi dari yayasan/sekolah user yang login bila belum ditentukan. */
	private void initFormSekolah(Rows rows, RencanaTahunAkademik data) {
		Tbmuser user = Common.getCurrentUser();
		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Row row = createRow(rows, "Yayasan");
		row.setVisible(ya);
		Common.selectComboItem(yayasan, data.getYayasan() == null && user != null ? user.ambilYayasan()
				: data.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(false);
		row.appendChild(yayasan);

		row = createRow(rows, "Sekolah");
		row.setVisible(ya);
		Common.pilihSekolah(sekolah, data.getSekolah() == null && user != null ? user.ambilSekolah()
				: data.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(false);
		row.appendChild(sekolah);
	}

	/** Menyusun baris keterangan bebas pada form. */
	private void initFormKeterangan(Rows rows, RencanaTahunAkademik data) {
		Row row = createRow(rows, "Keterangan");
		keterangan = new Textbox(safe(data.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		row.appendChild(keterangan);
	}

	/** Menyusun toolbar Batal/Simpan pada bagian selatan form. */
	private void initToolbarForm(Borderlayout borderlayout) {
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.setTooltiptext("Tutup");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		batal.setParent(toolbar);

		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.setTooltiptext("Simpan");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		simpan.setParent(toolbar);
	}

	/** Menambahkan satu baris form berlabel {@code label} ke {@code rows}, dipakai berulang oleh method {@code initForm*}. */
	private Row createRow(Rows rows, String label) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(label));
		return row;
	}

	/** Mengisi ulang combobox jurusan sesuai fakultas yang sedang dipilih pada form. */
	private void refreshJurusanByFakultas() {
		try {
			if (fakultas != null && fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
				jurusan.getItems().clear();
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Memvalidasi form ({@link #validasiForm()}) dan keunikan kombinasi tahun akademik+semester
	 * ({@link #checkNamaRencanaTahunAkademik()}), lalu menyimpan/memperbarui
	 * {@link RencanaTahunAkademik} dan menyegarkan cache statis {@link Common#rencanaTahunAkademiks}
	 * secara asinkron ({@link #reloadRencanaTahunAkademikAsync()}) agar perubahan langsung
	 * terpakai oleh {@link #getCurrentRencanaTahunAkademik} di seluruh aplikasi.
	 *
	 * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal (pesan sudah ditampilkan ke pengguna)
	 */
	public boolean onSave(Event event) throws Exception {
		if (!validasiForm()) {
			return false;
		}

		if (checkNamaRencanaTahunAkademik()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik dan semester",
					"Tahun Akademik dan semester sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan Tahun Akademik dan semester yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session sessionData = HibernateUtil.currentSession();

		if (rencanaTahunAkademik.getId() != null) {
			rencanaTahunAkademik = (RencanaTahunAkademik) sessionData.load(RencanaTahunAkademik.class,
					rencanaTahunAkademik.getId());
		}

		isiDataDariForm(rencanaTahunAkademik);
		Common.refreshSaveOrUpdate(sessionData, rencanaTahunAkademik);
		reloadRencanaTahunAkademikAsync();

		return true;
	}

	/** Memvalidasi field wajib pada form (tahun akademik, semester, tanggal mulai/sampai) dan memastikan tanggal mulai tidak melewati tanggal sampai. */
	private boolean validasiForm() throws Exception {
		if (nama == null || nama.getSelectedItem() == null || nama.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik",
					"Kolom Tahun Akademik belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun Akademik.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (semester == null || semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Semester",
					"Kolom Semester belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Semester.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tanggalMulai == null || tanggalMulai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal mulai",
					"Kolom Tanggal mulai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal mulai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tanggalSampai == null || tanggalSampai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal sampai",
					"Kolom Tanggal sampai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal sampai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tanggalMulai.getValue().after(tanggalSampai.getValue())) {
			MyMessageboxConfig.show("Tanggal mulai tidak boleh lebih besar dari tanggal sampai", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		return true;
	}

	/** Menyalin nilai form ke {@code data}: field umum selalu diisi; lingkup diisi salah satu — yayasan/sekolah bila {@link #isSekolahScopeDipilih()}, atau fakultas/jurusan/program/status awal mahasiswa/tahun angkatan bila tidak (saling meniadakan). */
	private void isiDataDariForm(RencanaTahunAkademik data) {
		boolean sekolahScope = isSekolahScopeDipilih();

		data.setNama(selectedString(nama));
		data.setSemester(selectedString(semester));
		data.setKeterangan(keterangan == null ? null : keterangan.getValue());
		data.setTanggalMulai(tanggalMulai == null ? null : tanggalMulai.getValue());
		data.setTanggalSampai(tanggalSampai == null ? null : tanggalSampai.getValue());
		data.setTanggalMulaiBelajarMengajar(
				tanggalMulaiBelajarMengajar == null ? null : tanggalMulaiBelajarMengajar.getValue());

		if (sekolahScope) {
			data.setFakultas(null);
			data.setJurusan(null);
			data.setProgram(null);
			data.setStatusAwalMahasiswa(null);
			data.setTahunAngkatan(null);
			data.setYayasan(getSelectedYayasan(yayasan));
			data.setSekolah(getSelectedSekolah(sekolah));
		} else {
			data.setFakultas(pt ? getSelectedFakultas(fakultas) : null);
			data.setJurusan(pt ? getSelectedJurusan(jurusan) : null);
			data.setProgram(pt ? selectedString(program) : null);
			data.setStatusAwalMahasiswa(pt ? getSelectedStatusAwal(statusAwalMahasiswa) : null);
			data.setTahunAngkatan(pt ? parseInteger(tahunAngkatan == null ? null : tahunAngkatan.getValue()) : null);
			data.setYayasan(null);
			data.setSekolah(null);
		}
	}

	/** Menentukan apakah data yang sedang diisi pada form termasuk lingkup sekolah (yayasan/sekolah) atau perguruan tinggi (fakultas/jurusan/dst.), berdasarkan mode aplikasi dan/atau kombinasi field mana yang benar-benar terisi pada form. */
	private boolean isSekolahScopeDipilih() {
		if (buatsekolah || (!pt && ya)) {
			return true;
		}
		if (!ya) {
			return false;
		}

		boolean adaSekolah = getSelectedYayasan(yayasan) != null || getSelectedSekolah(sekolah) != null;
		boolean adaPerguruanTinggi = getSelectedFakultas(fakultas) != null || getSelectedJurusan(jurusan) != null
				|| selectedString(program) != null || getSelectedStatusAwal(statusAwalMahasiswa) != null
				|| parseInteger(tahunAngkatan == null ? null : tahunAngkatan.getValue()) != null;

		return adaSekolah && !adaPerguruanTinggi;
	}

	/** Menjadwalkan penyegaran cache statis {@link Common#rencanaTahunAkademiks} secara asinkron lewat timer default, dipanggil setelah data disimpan agar {@link #getCurrentRencanaTahunAkademik} langsung memakai data terbaru. */
	private void reloadRencanaTahunAkademikAsync() {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.reloadRencanaTahunAkademik(HibernateUtil.currentSession());
			}
		});
	}

	/** Menyusun kriteria pencarian {@link RencanaTahunAkademik}: filter yayasan/sekolah pada mode sekolah, atau fakultas/jurusan/status awal mahasiswa/program/tahun angkatan pada mode perguruan tinggi (ditentukan lewat {@link #isSearchSekolahMode()}), ditambah filter tahun akademik; terurut nama+semester menurun bila {@code order} true. */
	public Criteria initCriteria(boolean order) {
		Session sessionData = HibernateUtil.currentSession();
		Criteria criteria = sessionData.createCriteria(RencanaTahunAkademik.class);

		if (order) {
			criteria.addOrder(Order.desc("nama")).addOrder(Order.desc("semester"));
		}

		boolean tampilSekolah = isSearchSekolahMode();
		if (tampilSekolah) {
			addEqIfSelected(criteria, "yayasan", getSelectedYayasan(searchyayasan));
			addEqIfSelected(criteria, "sekolah", getSelectedSekolah(searchsekolah));
		} else {
			addEqIfSelected(criteria, "fakultas", getSelectedFakultas(searchfakultas));
			addEqIfSelected(criteria, "jurusan", getSelectedJurusan(searchjurusan));
		}

		Object tahunAkademik = selectedValue(searchnama);
		if (tahunAkademik != null) {
			criteria.add(Restrictions.eq("nama", tahunAkademik));
		}

		Object status = selectedValue(searchStatusAwalMahasiswa);
		if (status != null && !tampilSekolah) {
			criteria.add(Restrictions.eq("statusAwalMahasiswa", status));
		}

		String selectedProgram = selectedString(searchprogram);
		if (selectedProgram != null && !tampilSekolah) {
			criteria.add(Restrictions.eq("program", selectedProgram));
		}

		Integer angkatan = parseInteger(searchTahunAngkatan == null ? null : searchTahunAngkatan.getValue());
		if (angkatan != null && !tampilSekolah) {
			criteria.add(Restrictions.eq("tahunAngkatan", angkatan));
		}

		return criteria;
	}

	/** Menentukan apakah panel pencarian sedang dalam mode sekolah (vs perguruan tinggi), berdasarkan mode aplikasi dan/atau yayasan/sekolah yang dipilih pada filter. */
	private boolean isSearchSekolahMode() {
		if (buatsekolah || (!pt && ya)) {
			return true;
		}
		if (!ya) {
			return false;
		}
		if (!pt) {
			return true;
		}
		return getSelectedYayasan(searchyayasan) != null || getSelectedSekolah(searchsekolah) != null;
	}

	/**
	 * Menambahkan filter {@code eq(property, value)} ke {@code criteria}, kecuali {@code value}
	 * berupa entitas {@link ais.database.model.GeneralValueObject} transient (belum tersimpan,
	 * {@code id} {@code null}) — kasus umum untuk item placeholder combobox seperti
	 * {@code "=Sekolah="}, yang bila tetap difilter akan memicu
	 * {@code TransientObjectException} pada Hibernate saat {@code list()} dieksekusi.
	 */
	private void addEqIfSelected(Criteria criteria, String property, Object value) {
		if (criteria == null || property == null || value == null) {
			return;
		}
		// Lewati entity TRANSIENT (belum tersimpan, id == null) — mis. item placeholder "=Sekolah=" /
		// "=Yayasan=" pada combo filter yang value-nya objek baru tanpa id. Bila tetap di-eq, Hibernate
		// gagal mengikat parameter many-to-one saat list() → TransientObjectException → grid kosong &
		// filter error. Dengan dilewati: placeholder = tanpa filter (tampil semua), pilihan nyata tetap
		// memfilter seperti biasa.
		if (value instanceof ais.database.model.GeneralValueObject
				&& ((ais.database.model.GeneralValueObject) value).getId() == null) {
			return;
		}
		criteria.add(Restrictions.eq(property, value));
	}

	/** Memuat ulang daftar {@link RencanaTahunAkademik} sesuai filter aktif (dipaginasi) ke grid. */
	@SuppressWarnings({ "unchecked" })
	public void onSearchDefault(Event event) {
		try {
			Common.initPaging(initCriteria(false), paging);

			List<RencanaTahunAkademik> list = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

			ListModel model = new SimpleListModel(list);
			grid.setRowRenderer(new RencanaTahunAkademikRenderer());
			grid.setModelCheckMobile(model);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Memeriksa apakah kombinasi tahun akademik+semester (dan, tergantung lingkup, fakultas/jurusan/program/status/tahun angkatan atau yayasan/sekolah) pada form sudah dipakai {@link RencanaTahunAkademik} lain (mengecualikan entitas yang sedang diedit). */
	public Boolean checkNamaRencanaTahunAkademik() {
		Session sessionData = HibernateUtil.currentSession();
		Criteria criteria = sessionData.createCriteria(RencanaTahunAkademik.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", selectedString(nama)))
				.add(Restrictions.eq("semester", selectedString(semester)));

		if (rencanaTahunAkademik != null && rencanaTahunAkademik.getId() != null) {
			criteria.add(Restrictions.ne("id", rencanaTahunAkademik.getId()));
		}

		boolean sekolahScope = isSekolahScopeDipilih();
		if (sekolahScope) {
			criteria.add(Restrictions.isNull("fakultas"));
			criteria.add(Restrictions.isNull("jurusan"));
			criteria.add(Restrictions.isNull("program"));
			criteria.add(Restrictions.isNull("statusAwalMahasiswa"));
			criteria.add(Restrictions.isNull("tahunAngkatan"));
			addExactNullable(criteria, "yayasan", getSelectedYayasan(yayasan));
			addExactNullable(criteria, "sekolah", getSelectedSekolah(sekolah));
		} else {
			criteria.add(Restrictions.isNull("yayasan"));
			criteria.add(Restrictions.isNull("sekolah"));
			addExactNullable(criteria, "fakultas", pt ? getSelectedFakultas(fakultas) : null);
			addExactNullable(criteria, "jurusan", pt ? getSelectedJurusan(jurusan) : null);
			addExactNullable(criteria, "program", pt ? selectedString(program) : null);
			addExactNullable(criteria, "statusAwalMahasiswa", pt ? getSelectedStatusAwal(statusAwalMahasiswa) : null);
			addExactNullable(criteria, "tahunAngkatan",
					pt ? parseInteger(tahunAngkatan == null ? null : tahunAngkatan.getValue()) : null);
		}

		Number count = (Number) criteria.uniqueResult();
		return count != null && count.intValue() > 0;
	}

	private void addExactNullable(Criteria criteria, String property, Object value) {
		if (criteria == null || property == null) {
			return;
		}
		// Akar sama dgn addEqIfSelected: entity TRANSIENT (id null, mis. placeholder combo) dianggap
		// "tidak terpilih" (null) agar tidak memicu TransientObjectException saat bind parameter many-to-one.
		if (value instanceof ais.database.model.GeneralValueObject
				&& ((ais.database.model.GeneralValueObject) value).getId() == null) {
			value = null;
		}
		criteria.add(value == null ? Restrictions.isNull(property) : Restrictions.eq(property, value));
	}

	/** Seperti {@link #getCurrentRencanaTahunAkademik(Tbmuser, Date)}, memakai user yang sedang login ({@link Common#getCurrentUser()}). */
	public static RencanaTahunAkademik getCurrentRencanaTahunAkademik(Date sekarang) {
		return getCurrentRencanaTahunAkademik(Common.getCurrentUser(), sekarang);
	}

	/**
	 * Menentukan {@link RencanaTahunAkademik} yang berlaku pada {@code sekarang} untuk konteks
	 * {@code tbmuser}: dimensi lingkup (fakultas/jurusan/program/status awal/tahun angkatan untuk
	 * mahasiswa dan dosen; sekolah untuk siswa dan guru) diturunkan otomatis dari peran user —
	 * bila user tidak punya peran spesifik (bukan mahasiswa/dosen/siswa/guru), jatuh kembali ke
	 * sekolah/yayasan konteks aplikasi saat ini ({@link SekolahUtil}). Delegasi akhir ke
	 * {@link #getCurrentRencanaTahunAkademik(Fakultas, Jurusan, Yayasan, Sekolah,
	 * StatusAwalMahasiswa, Integer, String, Date, String, String)} — lihat algoritma skor
	 * kecocokan pada dokumentasi kelas.
	 *
	 * @return rencana tahun akademik paling cocok, atau {@code null} bila tidak ada yang cocok
	 */
	public static RencanaTahunAkademik getCurrentRencanaTahunAkademik(Tbmuser tbmuser, Date sekarang) {
		Fakultas fakultas = tbmuser == null ? null : tbmuser.ambilFakultas();
		Jurusan jurusan = tbmuser == null ? null : tbmuser.ambilJurusan();
		Sekolah sekolah = tbmuser == null ? null : tbmuser.ambilSekolah();
		Yayasan yayasan = tbmuser == null ? null : tbmuser.ambilYayasan();
		String program = tbmuser == null ? null
				: (tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama());

		StatusAwalMahasiswa statusAwalMahasiswa = null;
		Integer tahunAngkatan = null;

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			fakultas = tbmuser.getMahasiswa().getJurusan() == null ? null
					: tbmuser.getMahasiswa().getJurusan().getFakultas();
			jurusan = tbmuser.getMahasiswa().getJurusan();
			program = tbmuser.getMahasiswa().getProgram();
			statusAwalMahasiswa = tbmuser.getMahasiswa().getStatusAwalMahasiswa();
			tahunAngkatan = tbmuser.getMahasiswa().getTahunangkatan();
			sekolah = null;
			yayasan = null;
		} else if (tbmuser != null && tbmuser.getDosen() != null) {
			fakultas = tbmuser.getDosen().getFakultas();
			jurusan = tbmuser.getDosen().getJurusan();
			sekolah = null;
			yayasan = null;
		} else if (tbmuser != null && tbmuser.getSiswa() != null) {
			sekolah = tbmuser.getSiswa().getSekolah();
			fakultas = null;
			jurusan = null;
		} else if (tbmuser != null && tbmuser.getGuru() != null) {
			sekolah = tbmuser.getGuru().getSekolah();
			fakultas = null;
			jurusan = null;
		}

		if (sekolah == null && fakultas == null && jurusan == null) {
			try {
				sekolah = SekolahUtil.getSekolah();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
		if (yayasan == null && fakultas == null && jurusan == null) {
			try {
				yayasan = SekolahUtil.getYayasan();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		if (fakultas != null || jurusan != null) {
			sekolah = null;
			yayasan = null;
		}

		return getCurrentRencanaTahunAkademik(fakultas, jurusan, yayasan, sekolah, statusAwalMahasiswa, tahunAngkatan,
				program, sekarang, null, null);
	}

	/**
	 * Implementasi kanonik resolusi rencana tahun akademik yang berlaku: mencari lewat cache
	 * {@link Common#rencanaTahunAkademiks} (dimuat ulang otomatis bila kosong) seluruh rencana
	 * yang waktu berlakunya cocok (lewat tanggal {@code sekarangB}, atau lewat pasangan literal
	 * {@code ta}/{@code smt} bila {@code sekarangB} {@code null} — lihat {@link #matchWaktu}),
	 * lalu di antara kandidat tersebut memilih yang skor kecocokan lingkupnya tertinggi (lihat
	 * algoritma skor pada dokumentasi kelas); bila skor seri, rencana dengan tanggal mulai lebih
	 * baru dimenangkan ({@link #lebihBaru}).
	 *
	 * @return rencana tahun akademik paling cocok/spesifik, atau {@code null} bila tidak ada kandidat yang cocok atau cache kosong
	 */
	public static RencanaTahunAkademik getCurrentRencanaTahunAkademik(Fakultas fakultas, Jurusan jurusan,
			Yayasan yayasan, Sekolah sekolah, StatusAwalMahasiswa statusAwalMahasiswa, Integer tahunAngkatan,
			String program, Date sekarangB, String ta, String smt) {

		reloadCacheRencanaTahunAkademikJikaKosong();

		if (Common.rencanaTahunAkademiks == null || Common.rencanaTahunAkademiks.isEmpty()) {
			return null;
		}

		RencanaTahunAkademik bestMatch = null;
		int highestScore = -1;

		for (RencanaTahunAkademik rta : Common.rencanaTahunAkademiks) {
			if (!matchWaktu(rta, sekarangB, ta, smt)) {
				continue;
			}

			int score = hitungScoreRencana(rta, fakultas, jurusan, yayasan, sekolah, statusAwalMahasiswa,
					tahunAngkatan, program);

			if (score < 0) {
				continue;
			}

			if (bestMatch == null || score > highestScore || (score == highestScore && lebihBaru(rta, bestMatch))) {
				bestMatch = rta;
				highestScore = score;
			}
		}

		return bestMatch;
	}

	/** Memuat ulang {@link Common#rencanaTahunAkademiks} dari database bila cache tersebut masih kosong (mis. baru setelah restart aplikasi). */
	private static void reloadCacheRencanaTahunAkademikJikaKosong() {
		if (Common.rencanaTahunAkademiks != null && !Common.rencanaTahunAkademiks.isEmpty()) {
			return;
		}

		org.hibernate.Session sessionData = null;
		try {
			sessionData = HibernateUtil.getSessionFactory().openSession();
			Common.reloadRencanaTahunAkademik(sessionData);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(sessionData);
		}
	}

	/** Mencocokkan waktu {@code rta} terhadap {@code sekarang} (harus berada dalam rentang tanggal mulai-sampai rencana), atau bila {@code sekarang} {@code null}, mencocokkan langsung nama dan semester literal ({@code ta}/{@code smt}). */
	private static boolean matchWaktu(RencanaTahunAkademik rta, Date sekarang, String ta, String smt) {
		if (rta == null) {
			return false;
		}

		if (sekarang == null) {
			return ta != null && smt != null && ta.equalsIgnoreCase(rta.getNama())
					&& smt.equalsIgnoreCase(rta.getSemester());
		}

		return rta.getTanggalMulai() != null && rta.getTanggalSampai() != null
				&& !rta.getTanggalMulai().after(sekarang) && !rta.getTanggalSampai().before(sekarang);
	}

	/** Menghitung skor kecocokan lingkup {@code rta} terhadap konteks yang diberikan — lihat penjelasan lengkap bobot dan aturan "gugur bila tidak cocok" pada dokumentasi kelas; mengembalikan {@code -1} bila ada dimensi terisi pada {@code rta} yang tidak cocok dengan konteks. */
	private static int hitungScoreRencana(RencanaTahunAkademik rta, Fakultas fakultas, Jurusan jurusan, Yayasan yayasan,
			Sekolah sekolah, StatusAwalMahasiswa statusAwalMahasiswa, Integer tahunAngkatan, String program) {
		int score = 0;

		score = tambahScoreJikaCocok(score, rta.getSekolah(), sekolah, 1000);
		if (score < 0) {
			return -1;
		}

		score = tambahScoreJikaCocok(score, rta.getYayasan(), yayasan, 100);
		if (score < 0) {
			return -1;
		}

		score = tambahScoreJikaCocok(score, rta.getFakultas(), fakultas, 80);
		if (score < 0) {
			return -1;
		}

		score = tambahScoreJikaCocok(score, rta.getJurusan(), jurusan, 90);
		if (score < 0) {
			return -1;
		}

		if (rta.getProgram() != null) {
			if (program == null || !rta.getProgram().equals(program)) {
				return -1;
			}
			score += 40;
		}

		score = tambahScoreJikaCocok(score, rta.getStatusAwalMahasiswa(), statusAwalMahasiswa, 30);
		if (score < 0) {
			return -1;
		}

		if (rta.getTahunAngkatan() != null) {
			if (tahunAngkatan == null || !rta.getTahunAngkatan().equals(tahunAngkatan)) {
				return -1;
			}
			score += 20;
		}

		return score;
	}

	/** Menambahkan {@code nilai} ke {@code score} bila {@code rtaValue} (dimensi lingkup rencana) sama dengan {@code requestValue} (dicocokkan via id, lewat refleksi {@code getId()}, atau {@code equals} sebagai fallback); tidak menambah apa pun bila {@code rtaValue} kosong (berarti rencana berlaku untuk semua); mengembalikan {@code -1} bila {@code rtaValue} terisi tapi tidak cocok — menandakan rencana ini gugur total. */
	private static int tambahScoreJikaCocok(int score, Object rtaValue, Object requestValue, int nilai) {
		if (rtaValue == null) {
			return score;
		}
		if (requestValue == null) {
			/*
			 * RTA yang mempunyai scope khusus tidak boleh dianggap cocok ketika konteks
			 * pemanggil tidak mempunyai nilai pembanding. Perilaku lama meloloskannya dengan
			 * skor nol; admin global kemudian dapat memperoleh RTA prodi/sekolah lain hanya
			 * karena tanggal mulainya paling baru.
			 */
			return -1;
		}

		try {
			Object idA = GeneralIdUtil.getId(rtaValue);
			Object idB = GeneralIdUtil.getId(requestValue);
			if (idA != null && idA.equals(idB)) {
				return score + nilai;
			}
		} catch (Exception e) {
			if (rtaValue.equals(requestValue)) {
				return score + nilai;
			}
		}

		return -1;
	}

	/** Menentukan apakah {@code a} punya tanggal mulai lebih baru dari {@code b} — dipakai sebagai tie-breaker saat dua rencana punya skor kecocokan yang sama. */
	private static boolean lebihBaru(RencanaTahunAkademik a, RencanaTahunAkademik b) {
		if (a == null || b == null) {
			return false;
		}
		if (a.getTanggalMulai() == null) {
			return false;
		}
		if (b.getTanggalMulai() == null) {
			return true;
		}
		return a.getTanggalMulai().after(b.getTanggalMulai());
	}

	/** Menutup {@code sessionData} secara bertahap (clear, disconnect, close), menelan galat di tiap tahap agar kegagalan penutupan tidak mengganggu alur pemanggil. */
	private static void closeSession(org.hibernate.Session sessionData) {
		if (sessionData == null) {
			return;
		}
		try {
			sessionData.clear();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			sessionData.disconnect();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			sessionData.close();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Utilitas refleksi kecil untuk memanggil {@code getId()} pada entitas tipe apa pun tanpa bergantung pada antarmuka bersama, dipakai {@link #tambahScoreJikaCocok} agar dapat membandingkan entitas lintas tipe (Fakultas, Jurusan, dst.) secara seragam. */
	private static class GeneralIdUtil {
		/** Memanggil {@code getId()} pada {@code object} lewat refleksi; {@code null} bila {@code object} {@code null}. */
		public static Object getId(Object object) throws Exception {
			return object == null ? null : object.getClass().getMethod("getId", new Class[] {}).invoke(object,
					new Object[] {});
		}
	}

	private Object selectedValue(Combobox combo) {
		return combo == null || combo.getSelectedItem() == null ? null : combo.getSelectedItem().getValue();
	}

	private String selectedString(Combobox combo) {
		Object value = selectedValue(combo);
		if (value == null) {
			return null;
		}
		String text = value.toString();
		return text.trim().length() == 0 ? null : text.trim();
	}

	private Fakultas getSelectedFakultas(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Fakultas ? (Fakultas) value : null;
	}

	private Jurusan getSelectedJurusan(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Jurusan ? (Jurusan) value : null;
	}

	private Yayasan getSelectedYayasan(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Yayasan ? (Yayasan) value : null;
	}

	private Sekolah getSelectedSekolah(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Sekolah ? (Sekolah) value : null;
	}

	private StatusAwalMahasiswa getSelectedStatusAwal(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof StatusAwalMahasiswa ? (StatusAwalMahasiswa) value : null;
	}

	private Integer parseInteger(String value) {
		if (value == null || value.trim().length() == 0) {
			return null;
		}
		try {
			return Integer.valueOf(value.trim());
		} catch (Exception e) {
			return null;
		}
	}

	private static String formatTanggal(Date value) {
		return value == null ? "" : Common.dateFormat4.get().format(value);
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}

	/** Menyusun teks ringkasan lingkup {@code rta} (gabungan fakultas/jurusan atau yayasan/sekolah yang terisi) untuk ditampilkan pada kolom "lingkup" di grid. */
	private static String formatUnit(RencanaTahunAkademik rta) {
		if (rta == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		if (rta.getFakultas() != null) {
			sb.append(safe(rta.getFakultas().getNama()));
		}
		if (rta.getJurusan() != null) {
			if (sb.length() > 0) {
				sb.append(" / ");
			}
			sb.append(safe(rta.getJurusan().getNama()));
		}
		if (rta.getYayasan() != null) {
			if (sb.length() > 0) {
				sb.append(" / ");
			}
			sb.append(safe(rta.getYayasan().getNama()));
		}
		if (rta.getSekolah() != null) {
			if (sb.length() > 0) {
				sb.append(" / ");
			}
			sb.append(safe(rta.getSekolah().getNama()));
		}
		return sb.length() == 0 ? "Semua" : sb.toString();
	}
}
