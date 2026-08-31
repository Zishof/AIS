package ais.action.master.pmb.statistik;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Group;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanel;

import ais.action.master.MahasiswaAction;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.CetakRegistrasiAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

/**
 * Composer laporan rekapitulasi pendaftar PMB/SPMB per jurusan: satu baris per {@link Jurusan}
 * (dikelompokkan per {@link Fakultas} lewat {@link Group}), menampilkan corong pendaftaran mulai
 * dari jumlah peminat, pembayaran formulir pendaftaran, kelulusan/diterima, pengisian form
 * tambahan (opsional), pembayaran daftar ulang, perolehan NIM, kolom "Selisih" (jumlah kasus yang
 * datanya tidak konsisten — sudah bayar daftar ulang tapi belum dapat NIM, atau sebaliknya sudah
 * dapat NIM tapi belum tercatat bayar), dan jumlah data mahasiswa aktual di tabel {@link Mahasiswa}
 * (dipakai sebagai pembanding silang terhadap data {@link BiodataCalonMahasiswa}). Setiap angka di
 * grid maupun baris footer total dapat diklik ({@link A}) untuk mengunduh data detail baris
 * BiodataCalonMahasiswa/Mahasiswa yang mendasarinya, lewat {@link MyEventListener}/
 * {@link MyEventListenerMahasiswa}.
 *
 * <p>
 * Tampilan sangat dikonfigurasi lewat flag {@code Konfigurasi}: {@code dibalik_nim_dan_lulus}
 * menukar urutan kolom "Dapat NIM"/"Bayar Daftar Ulang"; {@code custom_bayar_formulir_jenis_seleksi}
 * memecah kolom "Bayar Formulir" menjadi 4 sub-kolom berdasarkan jenis seleksi (Reguler Pilihan I/
 * II, RPL, KIP — dicocokkan lewat id {@link JenisSeleksi} tetap 80 dan 99); {@code
 * custom_bayar_formulir_pembayaran_tidak_dihitung} menonaktifkan syarat nominal bayar &gt; 0 pada
 * mode kustom tersebut; {@code tampil_isi_form_tambahan} menambah kolom form tambahan; dan filter
 * tanggal opsional ({@code searchTanggal}) membatasi setiap sub-perhitungan ke rentang tanggal
 * kejadian masing-masing (pendaftaran/pembayaran registrasi/kelulusan/pembayaran daftar ulang/
 * perolehan NIM — masing-masing kolom tanggal berbeda per tahap).
 * </p>
 */
public class RekapPendaftarSpmbSemua extends GenericAutowireComposer {

	private static final long serialVersionUID = 3173385938131248092L;

	private MyGrid grid;

	private Combobox jenisseleksisearch;
	private Combobox searchTahunAjaran;
	private Combobox prodiPilihanSearch;
	protected Combobox searchJenisSemester;
	private Combobox searchGelombang;
	private Combobox program;

	private Column colPesertaUjian;
	protected Tabpanel statistik;
	private MyToolbarbuttonConfig find;
	private int lebar = 400;
	private MyToolbarbuttonConfig findq;

	// Accumulators for Footer
	private int pilA1, pilB1, pilB1a1, pilB1a2, pilB1b, pilB1c, pilC1, pilCC2, pilCC1, pilD1, pilE1, pilSelisih;

	private Fakultas f = null;

	private Criterion[] critBayarReg, critBayarRegA, critBayarRegB, critBayarRegC;

	private boolean dibalik_nim_dan_lulus = false;
	private boolean custom_bayar_formulir_jenis_seleksi = false;
	private boolean custom_bayar_formulir_pembayaran_tidak_dihitung = false;
	private boolean tampil_isi_form_tambahan = true;

	private Checkbox searchTanggal;
	private MyDatebox start;
	private MyDatebox end;
	private MyLabelConfig tgl;

	/** Menjalankan pemeriksaan keamanan sebelum komponen ZK dibangun (hook siklus hidup ZK). */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/** Menginisialisasi bahasa, filter tanggal, flag konfigurasi tampilan, dropdown filter, kolom bersyarat, data awal, dan tombol Download grid; memuat data awal lewat timer default. */
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		initTanggal();
		initKonfigurasi();
		initCombobox();

		if (findq == null) {
			lebar = 1000;
		}

		if (colPesertaUjian != null) {
			colPesertaUjian.setVisible(Common.bolehKonfigurasi("tampil_colPesertaUjian"));
		}

		initDataAwal();

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (find != null) {
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
			toolbarbutton.setParent(find.getParent());
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					UIUtil.downloadGrid(grid);
				}
			});
		}
	}

	/** Menyiapkan checkbox filter tanggal dan field mulai/selesai (default 5 tahun lalu s.d. besok), termasuk menyembunyikan/menampilkannya sesuai status checkbox dan memicu pencarian ulang saat checkbox diklik. */
	private void initTanggal() {
		if (searchTanggal == null) return;
		if (tgl != null) tgl.setVisible(searchTanggal.isChecked());
		if (start != null) start.setVisible(searchTanggal.isChecked());
		if (end != null) end.setVisible(searchTanggal.isChecked());

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 5);
		if (start != null) start.setValue(calendar.getTime());
		
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		searchTanggal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tgl != null) tgl.setVisible(searchTanggal.isChecked());
				if (start != null) start.setVisible(searchTanggal.isChecked());
				if (end != null) end.setVisible(searchTanggal.isChecked());

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(null);
					}
				});
			}
		});
	}

	/** Membaca seluruh flag konfigurasi yang menentukan variasi tampilan laporan (lihat javadoc kelas). */
	private void initKonfigurasi() {
		dibalik_nim_dan_lulus = Common.bolehKonfigurasi("dibalik_nim_dan_lulus", Konfigurasi.TIDAK_AKTIF);
		custom_bayar_formulir_jenis_seleksi = Common.bolehKonfigurasi("custom_bayar_formulir_jenis_seleksi", Konfigurasi.TIDAK_AKTIF);
		custom_bayar_formulir_pembayaran_tidak_dihitung = Common.bolehKonfigurasi("custom_bayar_formulir_pembayaran_tidak_dihitung", Konfigurasi.TIDAK_AKTIF);
		tampil_isi_form_tambahan = Common.bolehKonfigurasi("tampil_isi_form_tambahan");
	}

	/** Mengisi dropdown jenis semester (Ganjil/Genap/Semua), dropdown pilihan prodi peminat (Pilihan I-V sesuai konfigurasi, plus "Semua Peminatan"), dan dropdown program. */
	private void initCombobox() {
		// Jenis Semester
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchJenisSemester.appendChild(comboitem);
		searchJenisSemester.setSelectedItem(comboitem);
		searchJenisSemester.setReadonly(true);

		// Prodi Pilihan
		appendProdiCombo("Peminat Pilihan I", "prodi1");
		appendProdiCombo("Peminat Pilihan II", "prodi2");

		if (Common.bolehKonfigurasi("pilihan_peminat_3", Konfigurasi.TIDAK_AKTIF)) appendProdiCombo("Peminat Pilihan III", "prodi3");
		if (Common.bolehKonfigurasi("pilihan_peminat_4", Konfigurasi.TIDAK_AKTIF)) appendProdiCombo("Peminat Pilihan IV", "prodi4");
		if (Common.bolehKonfigurasi("pilihan_peminat_5", Konfigurasi.TIDAK_AKTIF)) appendProdiCombo("Peminat Pilihan V", "prodi5");

		appendProdiCombo("Semua Peminatan", "gabungan");
		prodiPilihanSearch.setReadonly(true);
		prodiPilihanSearch.setSelectedIndex(0);

		Common.initPrograms(program);
	}

	/** Menambahkan satu item dropdown pilihan prodi peminat dengan {@code label} dan {@code value} (nama field prodi1..5/"gabungan"). */
	private void appendProdiCombo(String label, String value) {
		org.zkoss.zul.Comboitem comboitem = new MyComboitemConfig();
		comboitem.setLabel(label);
		comboitem.setValue(value);
		prodiPilihanSearch.appendChild(comboitem);
	}

	/** Mengisi dropdown jenis seleksi dan tahun ajaran (default dari konfigurasi/tahun akademik berjalan), serta dropdown gelombang pendaftaran yang dimuat ulang otomatis mengikuti tahun ajaran terpilih. */
	private void initDataAwal() {
		Common.insertComboDanSemua(jenisseleksisearch, new String[] { "nama" }, "deskripsi", JenisSeleksi.class, "=Jenis Seleksi=", Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		String tahunAkademikPenerimaanMahasiswaBaru = Common.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		EventListener gelombangEventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertComboDanSemua(searchGelombang, new String[] { "nama" }, "tahunAkademik", GelombangPendaftaran.class, "=Gelombang=",
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue())));
			}
		};

		try {
			gelombangEventListener.onEvent(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/statistik/RekapPendaftarSpmbSemua.java:239");
		}
		searchTahunAjaran.addEventListener("onChange", gelombangEventListener);
	}

	/** Menambahkan satu kolom grid dengan label, perataan opsional, dan dukungan teks multi-baris opsional. */
	private MyColumnConfig createColumn(Columns parent, String label, String align, boolean multiline) {
		MyColumnConfig col = new MyColumnConfig();
		col.setParent(parent);
		col.setLabel(label);
		if (align != null) col.setAlign(align);
		if (multiline) col.setStyle("white-space: normal;");
		return col;
	}

	/**
	 * Membangun ulang seluruh grid rekapitulasi: menentukan kolom sesuai flag konfigurasi aktif,
	 * lalu untuk setiap {@link Jurusan} aktif (dikelompokkan per fakultas) menjalankan serangkaian
	 * hitungan bersyarat lewat {@link #tampilandata} (peminat, bayar formulir — dipecah per jenis
	 * seleksi bila {@code custom_bayar_formulir_jenis_seleksi} aktif, lulus, isi form tambahan,
	 * bayar daftar ulang, dapat NIM) dan menghitung kolom "Selisih" (kasus data pembayaran/NIM
	 * yang tidak konsisten). Setiap nilai ditautkan ke unduhan data detail. Baris footer total
	 * dirender di akhir lewat {@link #renderFooter}.
	 */
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		// Memory Optimization: Extract UI values OUTSIDE the loop to prevent repetitive object creation.
		final String jen = prodiPilihanSearch.getSelectedItem() != null ? (String) prodiPilihanSearch.getSelectedItem().getValue() : null;
		final String selectedProgram = program.getSelectedItem() != null ? (String) program.getSelectedItem().getValue() : null;
		final String selectedSemester = searchJenisSemester.getSelectedItem() != null ? (String) searchJenisSemester.getSelectedItem().getValue() : null;
		//final Long selectedJenisSeleksiId = jenisseleksisearch.getSelectedItem() != null && jenisseleksisearch.getSelectedItem().getValue() != null ? ((JenisSeleksi) jenisseleksisearch.getSelectedItem().getValue()).getId() : null;
		final JenisSeleksi selectedJenisSeleksi = jenisseleksisearch.getSelectedItem() != null ? (JenisSeleksi) jenisseleksisearch.getSelectedItem().getValue() : null;
		final String selectedTahunAjaran = searchTahunAjaran.getSelectedItem() != null ? (String) searchTahunAjaran.getSelectedItem().getValue() : null;
		final GelombangPendaftaran selectedGelombang = searchGelombang.getSelectedItem() != null ? (GelombangPendaftaran) searchGelombang.getSelectedItem().getValue() : null;
		
		int tempTahunAngkatan = 0;
		if (selectedTahunAjaran != null && selectedTahunAjaran.contains("/")) {
			try {
				tempTahunAngkatan = Integer.parseInt(selectedTahunAjaran.split("/")[0]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/statistik/RekapPendaftarSpmbSemua.java:270"); }
		}
		final int tahunangkatan = tempTahunAngkatan;

		List<Jurusan> jurusans = ConstantValues.simpleList(session.createCriteria(Jurusan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("fakultas", "fakultas")
				.add(perguruanTinggi == null || perguruanTinggi.getId() == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi))
				.add(Restrictions.or(Restrictions.isNull("fakultas.aktif"), Restrictions.eq("fakultas.aktif", true)))
				.addOrder(Order.asc("fakultas.nama")).addOrder(Order.asc("nama")), Jurusan.class);

		Common.clear(grid);

		Columns columns = new Columns();
		columns.setParent(grid);
		
		MyColumnConfig column = createColumn(columns, "Jurusan", null, false);
		column.setWidth("30%");
		createColumn(columns, "Pendaftar", "right", false);

		if (custom_bayar_formulir_jenis_seleksi) {
			createColumn(columns, "Byr Formulir Reguler Pil I", "right", false);
			createColumn(columns, "Byr Formulir Reguler Pil II", "right", false);
			createColumn(columns, "Byr Formulir RPL", "right", false);
			createColumn(columns, "Byr Formulir KIP", "right", false);
		} else {
			createColumn(columns, "Bayar Formulir", "right", false);
		}

		createColumn(columns, "Lulus/Diterima", "right", false);

		if (tampil_isi_form_tambahan) {
			createColumn(columns, "Isi Form Tambahan", "right", false);
		}

		if (dibalik_nim_dan_lulus) {
			createColumn(columns, "Dapat NIM", "right", false);
			createColumn(columns, "Bayar Daftar Ulang", "right", false);
		} else {
			createColumn(columns, "Bayar Daftar Ulang", "right", false);
			createColumn(columns, "Dapat NIM", "right", false);
		}

		// Kolom Baru: Selisih antara Dapat NIM dan Bayar Daftar Ulang (Multiline Support)
		createColumn(columns, "Selisih NIM &\nByr Dftr Ulang", "right", true);
		createColumn(columns, "Jml Data Mhs", "right", false);

		resetAccumulators();
		f = null;
		critBayarReg = new Criterion[] { Restrictions.gt("pembayaranRegistrasi.dibayar", 0.1) };

		Rows rows = new Rows();
		rows.setParent(grid);

		Criterion tglDataDefault = Restrictions.sqlRestriction("true");
		String dbStartDate = "", dbEndDate = "";
		final boolean isDateChecked = searchTanggal != null && searchTanggal.isChecked();
		if (isDateChecked) {
			if (start != null && start.getValue() != null) dbStartDate = Common.databaseDateFormat.get().format(start.getValue());
			if (end != null && end.getValue() != null) dbEndDate = Common.databaseDateFormat.get().format(end.getValue());
			if (!dbStartDate.isEmpty() && !dbEndDate.isEmpty()) {
				tglDataDefault = Restrictions.sqlRestriction("date(this_.tanggalpendaftaran) between date('" + dbStartDate + "') and date('" + dbEndDate + "')");
			}
		}
		final boolean isDateFiltered = isDateChecked && !dbStartDate.isEmpty() && !dbEndDate.isEmpty();

		Criterion[] critBayarDaftarUlang;
		Criterion[] critDapatNIM;

		for (Jurusan j : jurusans) {
			try {
				if (f == null || !f.getId().equals(j.getFakultas().getId())) {
					Group group = new Group(j.getFakultas().getNama());
					rows.appendChild(group);
					f = j.getFakultas();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/statistik/RekapPendaftarSpmbSemua.java:347");
			}

			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);

			if (lebar < 999) {
				new Label(j.getNama()).setParent(row);
			} else {
				new MyLabelBoldAja(j.getNama()).setParent(row);
			}

			int Peminat = tampilandata(session, row, j, jen, true, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, tglDataDefault);
			int BayarReg = 0;
			
			if (custom_bayar_formulir_jenis_seleksi) {
				Criterion bayar = custom_bayar_formulir_pembayaran_tidak_dihitung ? Restrictions.sqlRestriction("true") : Restrictions.gt("pembayaranRegistrasi.dibayar", 0.1);
				Criterion tglDataPembayaranReg = isDateFiltered ? Restrictions.sqlRestriction("date(this_.tanggalpembayaranregistrasi) between date('" + dbStartDate + "') and date('" + dbEndDate + "')") : null;

				critBayarRegA = buildArray(bayar, Restrictions.not(Restrictions.or(Restrictions.eq("program", "RPL"), Restrictions.in("jenisSeleksi.id", new Long[] { 80L, 99L }))), tglDataPembayaranReg);
				int BayarRegReguler1 = tampilandata(session, row, j, "prodi1", false, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarRegA);
				int BayarRegReguler2 = tampilandata(session, row, j, "prodi2", false, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarRegA);

				critBayarRegB = buildArray(bayar, Restrictions.or(Restrictions.eq("program", "RPL"), Restrictions.in("jenisSeleksi.id", new Long[] { 80L })), tglDataPembayaranReg);
				int BayarRegRPL = tampilandata(session, row, j, jen, jen != null && jen.equalsIgnoreCase("gabungan"), selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarRegB);

				critBayarRegC = buildArray(bayar, Restrictions.in("jenisSeleksi.id", new Long[] { 99L }), tglDataPembayaranReg);
				int BayarRegKIP = tampilandata(session, row, j, jen, jen != null && jen.equalsIgnoreCase("gabungan"), selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarRegC);
				
				BayarReg = BayarRegReguler1 + BayarRegReguler2 + BayarRegRPL + BayarRegKIP;

				pilB1a1 += BayarRegReguler1;
				pilB1a2 += BayarRegReguler2;
				pilB1b += BayarRegRPL;
				pilB1c += BayarRegKIP;
			} else {
				Criterion tglDataPembayaranReg = isDateFiltered ? Restrictions.sqlRestriction("date(this_.tanggalpembayaranregistrasi) between date('" + dbStartDate + "') and date('" + dbEndDate + "')") : null;
				critBayarReg = buildArray(Restrictions.gt("pembayaranRegistrasi.dibayar", 0.1), tglDataPembayaranReg);
				BayarReg = tampilandata(session, row, j, jen, jen != null && jen.equalsIgnoreCase("gabungan"), selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarReg);
			}

			Criterion tglDataLulus = isDateFiltered ? Restrictions.sqlRestriction("date(this_.tanggalditerima) between date('" + dbStartDate + "') and date('" + dbEndDate + "')") : null;
			Criterion[] critLulus = buildArray(Restrictions.isNotNull("prodiLulus"), Restrictions.eq("prodiLulus", j), tglDataLulus);
			int Lulus = tampilandata(session, row, null, jen, true, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critLulus);

			if (tampil_isi_form_tambahan) {
				Criterion[] critLulusFormTamabahan = buildArray(Restrictions.isNotNull("prodiLulus"), Restrictions.eq("prodiLulus", j), Restrictions.isNotNull("parameterTambahanInds"), Restrictions.ne("parameterTambahanInds", ""));
				int LulusFormTamabahan = tampilandata(session, row, null, jen, jen != null && jen.equalsIgnoreCase("gabungan"), selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critLulusFormTamabahan);
				pilCC2 += LulusFormTamabahan;
			}

			int BayarDaftar = 0;
			int DapatNIM = 0;
			Criterion tglDataNIM = isDateFiltered ? Restrictions.sqlRestriction("date(this_.tanggal_masuk) between date('" + dbStartDate + "') and date('" + dbEndDate + "')") : null;
			Criterion tglDataBayarDaftar = isDateFiltered ? Restrictions.sqlRestriction("date(this_.tanggalpembayarandaftarulang) between date('" + dbStartDate + "') and date('" + dbEndDate + "')") : null;
			
			critDapatNIM = buildArray(Restrictions.isNotNull("mahasiswa"), Restrictions.isNotNull("prodiLulus"), Restrictions.eq("prodiLulus", j), tglDataNIM);
			critBayarDaftarUlang = buildArray(Restrictions.gt("pembayaranDaftarUlang.dibayar", 0.1), Restrictions.isNotNull("prodiLulus"), Restrictions.eq("prodiLulus", j), tglDataBayarDaftar);

			if (dibalik_nim_dan_lulus) {
				DapatNIM = tampilandata(session, row, null, jen, true, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critDapatNIM);
				BayarDaftar = tampilandata(session, row, null, jen, true, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarDaftarUlang);
			} else {
				BayarDaftar = tampilandata(session, row, null, jen, true, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarDaftarUlang);
				DapatNIM = tampilandata(session, row, null, jen, true, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critDapatNIM);
			}

			// Proses Logika Selisih
			int selisihVal = Math.abs(DapatNIM - BayarDaftar);
			pilSelisih += selisihVal;
			
			Criterion selisihPaid = Restrictions.gt("pembayaranDaftarUlang.dibayar", 0.1);
			Criterion selisihNotPaid = Restrictions.or(Restrictions.isNull("pembayaranDaftarUlang"), Restrictions.le("pembayaranDaftarUlang.dibayar", 0.1));
			Criterion selisihHasNIM = Restrictions.isNotNull("mahasiswa");
			Criterion selisihNoNIM = Restrictions.isNull("mahasiswa");
			
			Criterion critKondisiSelisih = Restrictions.or(
				Restrictions.and(selisihPaid, selisihNoNIM),
				Restrictions.and(selisihNotPaid, selisihHasNIM)
			);
			
			Criterion[] critSelisihArr = buildArray(Restrictions.isNotNull("prodiLulus"), Restrictions.eq("prodiLulus", j), critKondisiSelisih);
			
			MyEventListener selisihListener = new MyEventListener(j, jen, jen != null && jen.equalsIgnoreCase("gabungan"), selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critSelisihArr);
			A aSelisih = new A(Common.numberFormat.get().format(selisihVal));
			aSelisih.setStyle(lebar < 999 ? "font-size:10px" : "font-size:14px");
			aSelisih.addEventListener("onClick", selisihListener);
			row.appendChild(aSelisih);

			pilA1 += Peminat;
			pilB1 += BayarReg;
			pilC1 += Lulus;
			pilCC1 += BayarDaftar;
			pilD1 += DapatNIM;

			Criteria criteria = session.createCriteria(Mahasiswa.class)
					.add(isDateFiltered ? Restrictions.sqlRestriction("date(this_.tanggal_masuk) between date('" + dbStartDate + "') and date('" + dbEndDate + "')") : Restrictions.sqlRestriction("true"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(selectedProgram == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("program", selectedProgram))
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jurusan", j))
					.add(selectedSemester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semesterMulai", selectedSemester))
					.add(selectedJenisSeleksi == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jenisSeleksi", selectedJenisSeleksi))
					.add(Restrictions.eq("tahunangkatan", tahunangkatan));

			Object obj = criteria.uniqueResult();
			int jumlahPeserta = ((Number) (obj == null ? 0 : obj)).intValue();

			MyEventListenerMahasiswa eventListenerMhs = new MyEventListenerMahasiswa(j, selectedProgram, selectedSemester, selectedJenisSeleksi, tahunangkatan);
			A a = new A(Common.numberFormat.get().format(jumlahPeserta));
			a.setStyle(lebar < 999 ? "font-size:10px" : "font-size:14px");
			a.addEventListener("onClick", eventListenerMhs);
			a.setParent(row);

			pilE1 += jumlahPeserta;
		}

		renderFooter(grid, jen, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, tahunangkatan);
	}

	/** Mengembalikan seluruh akumulator kolom (untuk baris footer total) ke nol. */
	private void resetAccumulators() {
		pilA1 = pilB1 = pilB1a1 = pilB1a2 = pilB1b = pilB1c = pilC1 = pilCC1 = pilCC2 = pilD1 = pilE1 = pilSelisih = 0;
	}

	/** Menyaring {@code crits} menjadi array baru tanpa elemen {@code null} (memudahkan penggabungan kriteria opsional, mis. filter tanggal yang mungkin tidak aktif). */
	private Criterion[] buildArray(Criterion... crits) {
		int count = 0;
		for (Criterion c : crits) if (c != null) count++;
		Criterion[] result = new Criterion[count];
		int i = 0;
		for (Criterion c : crits) if (c != null) result[i++] = c;
		return result;
	}

	/** Merender baris footer "Total" yang menjumlahkan seluruh kolom lintas jurusan, dengan setiap nilai juga dapat diklik untuk mengunduh data detail agregat (bukan per-jurusan). */
	private void renderFooter(MyGrid grid, String jen, String selectedProgram, String selectedSemester, JenisSeleksi selectedJenisSeleksi, String selectedTahunAjaran, GelombangPendaftaran selectedGelombang, int tahunangkatan) {
		Foot foot = new Foot();
		foot.setParent(grid);
		foot.appendChild(new Footer("Total : "));

		boolean isGabungan = jen != null && jen.equalsIgnoreCase("gabungan");
		
		addFooterItem(foot, pilA1, new MyEventListener(null, jen, isGabungan, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang));

		if (custom_bayar_formulir_jenis_seleksi) {
			addFooterItem(foot, pilB1a1, new MyEventListener(null, "prodi1", false, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarRegA));
			addFooterItem(foot, pilB1a2, new MyEventListener(null, "prodi2", false, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarRegA));
			addFooterItem(foot, pilB1b, new MyEventListener(null, jen, isGabungan, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarRegB));
			addFooterItem(foot, pilB1c, new MyEventListener(null, jen, isGabungan, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarRegC));
		} else {
			addFooterItem(foot, pilB1, new MyEventListener(null, jen, isGabungan, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarReg));
		}

		addFooterItem(foot, pilC1, new MyEventListener(null, jen, isGabungan, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, Restrictions.isNotNull("prodiLulus")));

		if (tampil_isi_form_tambahan) {
			Criterion[] critLulusFormTamabahan = new Criterion[] { Restrictions.isNotNull("prodiLulus"), Restrictions.isNotNull("parameterTambahanInds"), Restrictions.ne("parameterTambahanInds", "") };
			addFooterItem(foot, pilCC2, new MyEventListener(null, jen, isGabungan, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critLulusFormTamabahan));
		}

		Criterion[] critDapatNIM = new Criterion[] { Restrictions.isNotNull("mahasiswa"), Restrictions.isNotNull("prodiLulus") };
		Criterion[] critBayarDaftarUlang = new Criterion[] { Restrictions.gt("pembayaranDaftarUlang.dibayar", 0.1), Restrictions.isNotNull("prodiLulus") };

		if (dibalik_nim_dan_lulus) {
			addFooterItem(foot, pilD1, new MyEventListener(null, jen, isGabungan, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critDapatNIM));
			addFooterItem(foot, pilCC1, new MyEventListener(null, jen, isGabungan, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarDaftarUlang));
		} else {
			addFooterItem(foot, pilCC1, new MyEventListener(null, jen, isGabungan, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critBayarDaftarUlang));
			addFooterItem(foot, pilD1, new MyEventListener(null, jen, isGabungan, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critDapatNIM));
		}

		// Footer Kolom Selisih
		Criterion selisihPaid = Restrictions.gt("pembayaranDaftarUlang.dibayar", 0.1);
		Criterion selisihNotPaid = Restrictions.or(Restrictions.isNull("pembayaranDaftarUlang"), Restrictions.le("pembayaranDaftarUlang.dibayar", 0.1));
		Criterion selisihHasNIM = Restrictions.isNotNull("mahasiswa");
		Criterion selisihNoNIM = Restrictions.isNull("mahasiswa");
		Criterion critKondisiSelisih = Restrictions.or(Restrictions.and(selisihPaid, selisihNoNIM), Restrictions.and(selisihNotPaid, selisihHasNIM));
		Criterion[] critSelisihArr = buildArray(Restrictions.isNotNull("prodiLulus"), critKondisiSelisih);
		
		addFooterItem(foot, pilSelisih, new MyEventListener(null, jen, isGabungan, selectedProgram, selectedSemester, selectedJenisSeleksi, selectedTahunAjaran, selectedGelombang, critSelisihArr));

		// Footer Kolom Mhs
		addFooterItem(foot, pilE1, new MyEventListenerMahasiswa(null, selectedProgram, selectedSemester, selectedJenisSeleksi, tahunangkatan));
	}

	/** Menambahkan satu sel footer berisi angka {@code value} yang dapat diklik untuk memicu {@code listener} (biasanya mengunduh data detail). */
	private void addFooterItem(Foot foot, int value, EventListener listener) {
		Footer footer = new Footer();
		A a = new A(Common.numberFormat.get().format(value));
		a.setStyle(lebar < 999 ? "font-size:8px" : "font-size:14px");
		a.addEventListener("onClick", listener);
		footer.appendChild(a);
		foot.appendChild(footer);
	}

	/** Listener klik angka pada grid/footer (jalur {@link BiodataCalonMahasiswa}): membuka unduhan data detail calon mahasiswa sesuai kombinasi jurusan/pilihan/program/semester/jenis seleksi/tahun ajaran/gelombang dan kriteria tambahan yang sama persis dipakai untuk menghitung angka tersebut. */
	public class MyEventListener implements EventListener {
		private Jurusan j;
		private Criterion[] crits;
		private boolean gabungan;
		private String pilihan;
		private String programVal, semesterVal, tahunAjaranVal;
		private JenisSeleksi jenisSeleksiVal;
		private GelombangPendaftaran gelombangVal;

		/** Menyimpan seluruh konteks filter yang dipakai untuk membangun ulang kriteria unduhan detail saat diklik. */
		public MyEventListener(Jurusan j, String pilihan, boolean gabungan, String programVal, String semesterVal, JenisSeleksi jenisSeleksiVal, String tahunAjaranVal, GelombangPendaftaran gelombangVal, Criterion... crits) {
			this.j = j;
			this.pilihan = pilihan;
			this.gabungan = gabungan;
			this.programVal = programVal;
			this.semesterVal = semesterVal;
			this.jenisSeleksiVal = jenisSeleksiVal;
			this.tahunAjaranVal = tahunAjaranVal;
			this.gelombangVal = gelombangVal;
			this.crits = crits;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {
			EventListener eventListener = (EventListener) Common.cetakDataCustomButton(BiodataCalonMahasiswa.class, new DataCriteriaWithColumn() {
				@Override
				public Object[] initCriteria(boolean order) {
					try {
						Session session = HibernateUtil.currentSession();
						Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.createAlias("pembayaranDaftarUlang", "pembayaranDaftarUlang", Criteria.LEFT_JOIN)
								.createAlias("pembayaranRegistrasi", "pembayaranRegistrasi", Criteria.LEFT_JOIN)
								.add(programVal == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("program", programVal)) // PERBAIKAN: Filter programVal diterapkan di sini
								.add(semesterVal == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semesterMulai", semesterVal))
								.add(jenisSeleksiVal == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jenisSeleksi", jenisSeleksiVal))
								.add(tahunAjaranVal == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tahunAkademik", tahunAjaranVal))
								.add(gelombangVal == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("gelombangPendaftaran", gelombangVal));

						if (j != null && pilihan != null) {
							if (!gabungan) {
								criteria.add(Restrictions.eq(pilihan, j));
							} else {
								criteria.add(Restrictions.or(Restrictions.eq("prodi1", j), Restrictions.or(Restrictions.eq("prodi2", j), Restrictions.or(Restrictions.eq("prodi3", j), Restrictions.or(Restrictions.eq("prodi4", j), Restrictions.eq("prodi5", j))))));
							}
						}

						for (Criterion crit : crits) {
							if (crit != null) criteria.add(crit);
						}

						return new Object[] { criteria, CetakRegistrasiAction.contents };
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
					return null;
				}
			}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN", new String[80]).getAttribute("eventListener");
			
			eventListener.onEvent(null);
		}
	}
	
	/** Listener klik angka kolom "Jml Data Mhs" pada grid/footer (jalur {@link Mahasiswa}): membuka unduhan data detail mahasiswa aktif sesuai kombinasi jurusan/program/semester/jenis seleksi/tahun angkatan yang sama persis dipakai untuk menghitung angka tersebut. */
	public class MyEventListenerMahasiswa implements EventListener {
		private Jurusan j;
		private Criterion[] crits;
		private String programVal, semesterVal;
		private JenisSeleksi jenisSeleksiVal;
		private int tahunAngkatanVal;

		/** Menyimpan seluruh konteks filter yang dipakai untuk membangun ulang kriteria unduhan detail mahasiswa saat diklik. */
		public MyEventListenerMahasiswa(Jurusan j, String programVal, String semesterVal, JenisSeleksi jenisSeleksiVal, int tahunAngkatanVal, Criterion... crits) {
			this.j = j;
			this.programVal = programVal;
			this.semesterVal = semesterVal;
			this.jenisSeleksiVal = jenisSeleksiVal;
			this.tahunAngkatanVal = tahunAngkatanVal;
			this.crits = crits;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {
			EventListener eventListener = (EventListener) Common.cetakDataCustomButton(Mahasiswa.class, new DataCriteriaWithColumn() {
				@Override
				public Object[] initCriteria(boolean order) {
					try {
						Session session = HibernateUtil.currentSession();
						Criteria criteria = session.createCriteria(Mahasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(programVal == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("program", programVal))
								.add(semesterVal == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semesterMulai", semesterVal))
								.add(j == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", j))
								.add(jenisSeleksiVal == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jenisSeleksi", jenisSeleksiVal))
								.add(Restrictions.eq("tahunangkatan", tahunAngkatanVal));

						for (Criterion crit : crits) {
							if (crit != null) criteria.add(crit);
						}
						return new Object[] { criteria, MahasiswaAction.contents };
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
					return null;
				}
			}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN", new String[80]).getAttribute("eventListener");
			
			eventListener.onEvent(null);
		}
	}

	/**
	 * Menghitung jumlah {@link BiodataCalonMahasiswa} yang memenuhi kombinasi filter dasar
	 * (program/semester/jenis seleksi/tahun ajaran/gelombang), kecocokan jurusan pada satu kolom
	 * pilihan tertentu (atau gabungan seluruh 5 slot pilihan bila {@code gabungan}), dan kriteria
	 * tambahan bebas ({@code crits}, mis. syarat pembayaran/kelulusan/tanggal). Menambahkan satu
	 * sel angka yang dapat diklik ke {@code row} (bila diberikan) yang membuka unduhan detail
	 * dengan kriteria yang sama persis.
	 *
	 * @return jumlah baris yang cocok
	 */
	private int tampilandata(Session session, Row row, Jurusan j, String pilihan, boolean gabungan, String programVal, String semesterVal, JenisSeleksi jenisSeleksiVal, String tahunAjaranVal, GelombangPendaftaran gelombangVal, Criterion... crits) {
		Criteria c = session.createCriteria(BiodataCalonMahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("pembayaranDaftarUlang", "pembayaranDaftarUlang", Criteria.LEFT_JOIN)
				.createAlias("pembayaranRegistrasi", "pembayaranRegistrasi", Criteria.LEFT_JOIN)
				.add(programVal == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("program", programVal))
				.setProjection(Projections.rowCount())
				.add(semesterVal == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semesterMulai", semesterVal))
				.add(jenisSeleksiVal == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jenisSeleksi", jenisSeleksiVal))
				.add(tahunAjaranVal == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tahunAkademik", tahunAjaranVal))
				.add(gelombangVal == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("gelombangPendaftaran", gelombangVal));

		if (j != null && pilihan != null) {
			if (!gabungan) {
				c.add(Restrictions.eq(pilihan, j));
			} else {
				c.add(Restrictions.or(Restrictions.eq("prodi1", j), Restrictions.or(Restrictions.eq("prodi2", j), Restrictions.or(Restrictions.eq("prodi3", j), Restrictions.or(Restrictions.eq("prodi4", j), Restrictions.eq("prodi5", j))))));
			}
		}

		for (Criterion crit : crits) {
			if (crit != null) c.add(crit);
		}

		Object obj = c.uniqueResult();
		int jumlahPeserta = ((Number) (obj == null ? 0 : obj)).intValue();
		MyEventListener eventListener = new MyEventListener(j, pilihan, gabungan, programVal, semesterVal, jenisSeleksiVal, tahunAjaranVal, gelombangVal, crits);

		A a = new A(Common.numberFormat.get().format(jumlahPeserta));
		a.setStyle(lebar < 999 ? "font-size:10px" : "font-size:14px");
		a.addEventListener("onClick", eventListener);
		
		if (row != null) {
			row.appendChild(a);
		}
		return jumlahPeserta;
	}
}