
package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.dashboard.admin.DashboardDataNilaiMahasiswa;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.PenilaianMahasiswaHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS;
import ais.database.model.Perkuliahan;
import ais.database.model.Staff;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan khs. Kelas ini mengubah data domain menjadi bentuk
 * laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke lapisan
 * report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox semesterAbsensiUjian}, {@code
 * AmbilDataMahasiswaBanbox bandboxMahasiswa}, {@code MyCheckboxConfig hitungUlang}, {@code MyCheckboxConfig
 * remedial}, {@code Center center}, {@code Toolbar toolbar}, {@code MyDatebox tanggal}, {@code MyCheckboxConfig
 * semesterPendek}; inisialisasi/lifecycle ({@code initKHS()}, {@code init()}, {@code initParametermapKhs()});
 * operasi domain lain ({@code generateParameter()}, {@code generateParameter()}, {@code onKHS()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanKHS extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = -397946194166101691L;
	// private Combobox tahunAkademikUjianAkhirSemester;
	private Combobox semesterAbsensiUjian;
	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private MyCheckboxConfig hitungUlang = new MyCheckboxConfig("Hitung Ulang IP/IPK");
	private MyCheckboxConfig remedial = new MyCheckboxConfig("Remedial");
	private Center center;
	private Toolbar toolbar;

	public LaporanKHS() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan KHS", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKHS(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		// tahunAkademikUjianAkhirSemester = new Combobox();
		semesterAbsensiUjian = new Combobox();
		// if (tahunAkademikUjianAkhirSemester != null) {
		// tahunAkademikUjianAkhirSemester = Common
		// .generateTahunAjaran(tahunAkademikUjianAkhirSemester);
		for (int i = 1; i <= 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterAbsensiUjian.appendChild(comboitem);
		}
		Common.selectComboItem(semesterAbsensiUjian, 1);
		// }
	}

	private MyDatebox tanggal;
	private MyCheckboxConfig semesterPendek;
	private MyDatebox tanggalDicetak;

	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Mahasiswa mhs = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
				// Guard: bila Semester belum dipilih, getSelectedItem() null → NPE. Beri peringatan.
				if (semesterAbsensiUjian.getSelectedItem() == null) {
					ais.ui.util.MyMessageboxConfig.show("Mohon maaf, Semester belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Semester dari daftar dropdown yang tersedia; (2) Pastikan data semester sudah dikonfigurasi di sistem; (3) Ulangi proses cetak Kartu Hasil Studi. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
							ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
					return;
				}
				Integer smt = (Integer) semesterAbsensiUjian.getSelectedItem().getValue();
				if (mhs != null) {

					Date dicetak = tanggalDicetak.getValue();
					try {

						final Integer tahunAngkatanMhs = mhs.getTahunangkatan();
						String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

						Integer tahunAkademikMulai = Common.getTahunAkademik(smt, tahunAngkatanMhs,
								mhs.getSemesterMulai());

						Konfigurasi konfigurasi = Common.getKonfigurasi("tanggal_ditetapkan_khs_"
								+ (mhs.getJurusan() == null ? "0" : mhs.getJurusan().getId()) + "_" + tahunAkademikMulai + "_" + semesterMulai,
								Common.dateFormat1.get().format(dicetak));

						if (event.getTarget() == tanggalDicetak) {
							konfigurasi.setNilai(Common.dateFormat1.get().format(tanggalDicetak.getValue()));
							Common.refreshSaveOrUpdate(konfigurasi);
						}

						dicetak = Common.dateFormat1.get().parse(konfigurasi.getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanKHS.java:150");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan KHS", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});

					}

					tanggalDicetak.setValue(dicetak);

					onKHS(event);
				}

			}
		};

		final Tbmuser tbmuser = Common.getCurrentUser();
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Hasil Studi");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("KHS Tipe 1");
		tab2.setVisible(false);
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("KHS Per-prodi dan angkatan");
		tab3.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		tab3.setParent(tabs);

		MyTabConfig tab31 = new MyTabConfig("KRS Per-prodi dan angkatan");
		tab31.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		tab31.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Kehadiran dan Nilai");
		tab4.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setVisible(false);
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					LaporanKHSType1 laporanKHS = new LaporanKHSType1();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel2);
				}
			}
		});

		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
		tabpanel3.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		tabpanel3.setParent(tabpanels);
		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel3.getChildren().size() == 0) {
					LaporanKHSPerProdiDanAngkatan laporanKHS = new LaporanKHSPerProdiDanAngkatan();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel3);
				}
			}
		});

		final Tabpanel tabpanel31 = new ais.ui.util.MyTabpanel();
		tabpanel31.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		tabpanel31.setParent(tabpanels);
		tab31.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel31.getChildren().size() == 0) {
					LaporanKRSPerProdiDanAngkatan laporanKrsPerProdiDanAngkatan = new LaporanKRSPerProdiDanAngkatan();
					laporanKrsPerProdiDanAngkatan.setHeight("100%");
					laporanKrsPerProdiDanAngkatan.setWidth("100%");
					laporanKrsPerProdiDanAngkatan.setParent(tabpanel31);
				}
			}
		});

		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
		tabpanel4.setParent(tabpanels);
		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel4.getChildren().size() == 0) {
					DashboardDataNilaiMahasiswa laporan = new DashboardDataNilaiMahasiswa(
							tbmuser != null ? tbmuser.getMahasiswa() : null);
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel4);
				}
			}
		});

		borderlayout.setParent(tabpanel1);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensiUjian);
		semesterAbsensiUjian.setWidth("90%");
		semesterAbsensiUjian.addEventListener("onChange", eventListener);
		semesterAbsensiUjian.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
		row.appendChild(bandboxMahasiswa = new AmbilDataMahasiswaBanbox());
		bandboxMahasiswa.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() != null) {
			Mahasiswa mahasiswa = Common.getCurrentUser().getMahasiswa();
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
			bandboxMahasiswa.setDisabled(true);
			// Mahasiswa sudah pasti (pengguna = mahasiswa) → Semester default = semester berjalannya.
			Common.selectComboItem(semesterAbsensiUjian, mahasiswa.currentSemester());
		}

		// Saat mahasiswa DIPILIH lewat banbox, Semester otomatis diisi = semester berjalan mahasiswa
		// tersebut (mahasiswa.currentSemester()) supaya pengguna tak perlu memilih manual; setelah itu
		// tetap boleh diganti manual. Pengisian HANYA tepat saat mahasiswa dipilih, lalu delegasikan
		// ke eventListener bersama (yang menghasilkan/preview laporan) agar perilaku lama terjaga.
		bandboxMahasiswa.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Mahasiswa mhsTerpilih = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
				if (mhsTerpilih != null) {
					Common.selectComboItem(semesterAbsensiUjian, mhsTerpilih.currentSemester());
				}
				eventListener.onEvent(arg0);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hitungUlang);
		hitungUlang.addEventListener("onClick", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.semesterPendek = new MyCheckboxConfig("Semester Pendek"));
		semesterPendek.addEventListener("onClick", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(remedial);
		remedial.addEventListener("onClick", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Ditetapkan"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Cetak"));
		tanggalDicetak = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggalDicetak);
		tanggalDicetak.setWidth("90%");
		tanggalDicetak.addEventListener("onChange", eventListener);
		tanggalDicetak.setDisabled(tbmuser == null || tbmuser.getMahasiswa() != null);
		tanggalDicetak.setReadonly(true);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// row = new MyFormRow();
		//		// row.setParent(rows);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				// if (tahunAkademikUjianAkhirSemester.getSelectedItem()
				// ==
				// null) {
				// MyMessageboxConfig.show("Pilih salah satu tahun akademik",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.INFORMATION);
				// return null;
				// }
				if (semesterAbsensiUjian.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Semester (Ganjil/Genap) belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Ganjil atau Genap sesuai semester yang diinginkan; (2) Pastikan pilihan semester sesuai dengan tahun akademik aktif; (3) Ulangi proses cetak KHS. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
					MyMessageboxConfig.show("Mohon maaf, Mahasiswa belum dipilih. Langkah yang dapat dilakukan: (1) Ketik NIM atau nama mahasiswa pada kolom pencarian lalu pilih dari hasil yang muncul; (2) Pastikan data mahasiswa terdaftar di sistem; (3) Ulangi proses cetak KHS. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Kartu_Hasil_Studi", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		onKHS(null);

	}

	@SuppressWarnings({ "rawtypes" })
	public Map generateParameter() throws Exception {
		// if (tahunAkademikUjianAkhirSemester.getSelectedItem() == null) {
		// // MyMessageboxConfig.show("Pilih salah satu tahun akademik",
		// "Peringatan",
		// // 1,
		// // MyMessageboxConfig.INFORMATION);
		// return null;
		// }
		if (semesterAbsensiUjian.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih ganjil atau genap", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			// MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");

		int semester = (Integer) semesterAbsensiUjian.getSelectedItem().getValue();

		return LaporanKHS.generateParameter(mahasiswa, semester, hitungUlang.isChecked(),
				semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null, remedial.isChecked(),
				tanggal.getValue(), tanggalDicetak.getValue());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(Mahasiswa mahasiswa, int semester, boolean hitungUang, Integer sp,
			boolean remedial, Date tanggal, Date tanggalDicetak) throws Exception {

		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa, semester)) {
			return null;
		}

		Integer tahap = mahasiswa.currentTahapan(semester);

		Session session = HibernateUtil.currentSession();
		Staff staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "prodi"))
				.setMaxResults(1).uniqueResult();

		JenjangProgramStudi jenjangProgramStudi = (JenjangProgramStudi) session
				.createCriteria(JenjangProgramStudi.class).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
				.setMaxResults(1).uniqueResult();

		Common.reloadNilai(mahasiswa, semester, false);

		if (hitungUang) {
			mahasiswa.reInitDetailperkuliahan(session);
		}

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, sp, hitungUang);
		if (krsMahasiswa == null) {
			ais.common.ErrorAuditUtil.record(
					new NullPointerException("krsMahasiswa null untuk mahasiswa=" + mahasiswa.getId() + " semester=" + semester),
					"src/ais/action/report/format1/akademik/LaporanKHS.java:generateParameter krsMahasiswa null (data KRS tidak ditemukan)");
			return null;
		}

		Fakultas fakultas = mahasiswa.getJurusan() == null ? null : mahasiswa.getJurusan().getFakultas();
		Jurusan jurusan = mahasiswa.getJurusan();
		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = Common.getIpkUntukPengambilanKRS(
				mahasiswa, semester, mahasiswa.getTahunangkatan(), fakultas, jurusan, mahasiswa.getProgram(),
				krsMahasiswa.getSemesterPendek());

		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRSBerikutNya = Common
				.getIpkUntukPengambilanKRS(mahasiswa, semester + 1, mahasiswa.getTahunangkatan(), fakultas, jurusan,
						mahasiswa.getProgram(), krsMahasiswa.getSemesterPendek());

		Map parameters = ais.common.HashMapGenerator.getRandStringObject();
		Common.insertProperty(Mahasiswa.class, mahasiswa, parameters, "");
		Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs", 1, "mahasiswa");
		mahasiswa.putPhoto(parameters);
		parameters.put("max_sks", pembatasanNilaiIPKUntukPengambilanKRS == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil());

		parameters.put("max_sks_next", pembatasanNilaiIPKUntukPengambilanKRSBerikutNya == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRSBerikutNya.getBatasMaksimumIPKYangBolehDiambil());

		parameters.put("max_sks_berikut", pembatasanNilaiIPKUntukPengambilanKRSBerikutNya == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRSBerikutNya.getBatasMaksimumIPKYangBolehDiambil());
		parameters.put("remedial", remedial);
		parameters.put("semester", semester);
		// parameters.put("tahun_ajaran", tahunAkademik);
		parameters.put("pembantu_dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("mahasiswa", mahasiswa.getId());
		// parameters.put("nip", staffDekan.getNip());
		parameters.put("tanggal", tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal);
		parameters.put("tanggal_dicetak", tanggalDicetak == null ? ais.ui.util.WaktuUtil.getDate() : tanggalDicetak);
		parameters.put("nuptkosenpa",
				krsMahasiswa.getDosenPa() == null ? ""
						: krsMahasiswa.getDosenPa().getNuptk());
		parameters.put("dosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "......................." : krsMahasiswa.getDosenPa().getNama());
		parameters.put("nipdosenpa",
				krsMahasiswa.getDosenPa() == null ? "......................."
						: (krsMahasiswa.getDosenPa().getCode().isEmpty() ? krsMahasiswa.getDosenPa().getNidn()
								: krsMahasiswa.getDosenPa().getCode()));

		Double ipmhs = krsMahasiswa.getIps();
		Double ipkmhs = krsMahasiswa.getIpk();

		Integer sksmhss = krsMahasiswa.getSksYangDiambil();
		Integer sksmhs = krsMahasiswa.getSksk();

		if (semester > 1) {
			Double iplast = Common.ipTerakhir(mahasiswa, semester);
			parameters.put("ip_sebelumnya", iplast);
		}
		parameters.put("ipk", ipkmhs);
		parameters.put("ips", ipmhs);
		parameters.put("sksk", sksmhs);
		parameters.put("sks", sksmhss);

		parameters.put("ip_kumulatif", ipkmhs);

		parameters.put("ip_semester", ipmhs);

		krsMahasiswa = semester <= 1 ? null
				: Common.singkronkanKrsMahasiswa(mahasiswa, semester - 1, null, sp, hitungUang);

		ipmhs = krsMahasiswa == null ? 0.0 : krsMahasiswa.getIps();
		ipkmhs = krsMahasiswa == null ? 0.0 : krsMahasiswa.getIpk();

		sksmhss = krsMahasiswa == null ? 0 : krsMahasiswa.getSksYangDiambil();
		sksmhs = krsMahasiswa == null ? 0 : krsMahasiswa.getSksk();

		parameters.put("ipk_1", ipkmhs);
		parameters.put("ips_1", ipmhs);
		parameters.put("sksk_1", sksmhs);
		parameters.put("sks_1", sksmhss);

		parameters.put("ip_kumulatif_1", ipkmhs);

		parameters.put("ip_semester_1", ipmhs);

		if (Common.bolehKonfigurasi("tampilkan_total_nilai_di_krs", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("total_nilai", mahasiswa.hitungNilaiSampaiSemester(semester, null, null, true));
			if (semester > 1) {
				parameters.put("total_nilai_1", mahasiswa.hitungNilaiSampaiSemester(semester - 1, null, null, true));
			}
		}

		if (Common.bolehKonfigurasi("tampilkan_total_mutu_di_krs", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("total_nilai_mutu", mahasiswa.hitungMutuSampaiSemester(semester, null, null, true));
			parameters.put("total_nilai_ip", mahasiswa.hitungNilaiIpSampaiSemester(semester, null, null, true));
		}

		if (jenjangProgramStudi != null && jenjangProgramStudi.getNmKaPS() != null
				&& !jenjangProgramStudi.getNmKaPS().trim().equals("")) {
			parameters.put("kaprodi", jenjangProgramStudi == null ? "(                                          )"
					: jenjangProgramStudi.getNmKaPS());
			parameters.put("nip", jenjangProgramStudi == null ? "" : jenjangProgramStudi.getNidnKaPS());
		} else {
			Dosen dosen = jurusan.getKaprodi();
			parameters.put("kaprodi", dosen == null ? "(                                          )" : dosen.getNama());
			parameters.put("nip", dosen == null ? "" : dosen.getCode());
		}

		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, mahasiswa.getTahunangkatan(),
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

		parameters.put("bar", "2-" + tahunAkademik + "-" + semester + "-" + mahasiswa.getId());

		if (Common.bolehKonfigurasi("khs_dipisahkan_tiap_jenjang", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("nama_laporan", "Kartu_Hasil_Studi_" + mahasiswa.getJenjang().getId());

		}

		try {
			FileFotoLain lampiranLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLain.TTD_MAHASISWA,
					LampiranLain.class);
			if (lampiranLain != null) {
				File file = lampiranLain.ambilFile();
				if (file != null && file.exists()) {
					parameters.put("ttd_mahasiswa", file.getAbsolutePath());
				}
				file = null;
			}
			lampiranLain = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanKHS.java:594");
//			e.printStackTrace();
		}
		parameters.put("semester_pendek", sp);
		boolean konversiMasuk = Common.bolehKonfigurasi("masukkan_mk_konversi_di_khs", Konfigurasi.TIDAK_AKTIF);
		LaporanKHS.initParametermapKhs(parameters, sp, remedial, mahasiswa, semester, tahap, krsMahasiswa,
				konversiMasuk, hitungUang);

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void initParametermapKhs(Map<String, Object> parameters, Integer semesterPendek, boolean remedial,
			Mahasiswa mahasiswa, Integer semester, Integer tahap, KrsMahasiswa krsMahasiswa, boolean termsukKonversi,
			boolean hitungUang) throws Exception {
		if (krsMahasiswa != null) {
			krsMahasiswa.masukkanData("cetak_khs");
		}

		Judisium judisium = Common.hitungJudisium(mahasiswa, semester, null);
		parameters.put("judisium", judisium == null ? "" : judisium.getNama());
		parameters.put("judisium_en", judisium == null ? "" : judisium.getNamaen());

		if (Common.bolehKonfigurasi("khs_ambil_dari_parameter")) {
			String tetap_tampil_nilai_walau_0 = Common
					.getKonfigurasi("tetap_tampil_nilai_walau_0", Konfigurasi.TIDAK_AKTIF).getNilai();
			String nilai0MasukPenghitungan = Common
					.getKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.AKTIF).getNilai();
			Double minimal = 0.1;
			try {
				minimal = Double.parseDouble(Common
						.getKonfigurasi("nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1").getNilai().trim()
						.replace(',', '.'));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanKHS.java:626");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan KHS", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});

			}

			List<Map> maps = new ArrayList<Map>();
			List<Long> detailperkuliahansDataSebelumnya = Common.getDetailperkuliahans(mahasiswa, semester - 1, tahap,
					null, semesterPendek, remedial, false, false, false);
			List<Long> detailperkuliahansData = Common.getDetailperkuliahans(mahasiswa, semester, tahap, null,
					semesterPendek, remedial, false, false, false);

//			System.out.println("detailperkuliahansData -> " + detailperkuliahansData + " semester " + semester
//					+ " tahap " + tahap + " semesterPendek " + semesterPendek);

			for (Long detailperkuliahanid : detailperkuliahansData) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null && (termsukKonversi
						|| (!termsukKonversi && detailperkuliahan.getMatakuliahKonversi() == null))) {
					if (nilai0MasukPenghitungan.equals(Konfigurasi.TIDAK_AKTIF)
							|| tetap_tampil_nilai_walau_0.equalsIgnoreCase(Konfigurasi.AKTIF)
							|| detailperkuliahan.getTotalNilai() > minimal) {
						Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
						Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
								? detailperkuliahan.getPerkuliahan().getMatakuliah()
								: detailperkuliahan.getMatakuliahKonversi();

						if (!matakuliah.getExtraKulikuler()) {

							Map map = new java.util.HashMap();

							map.put("periode", perkuliahan == null ? "SP" : perkuliahan.getGanjilGenap());

							map.put("id_pejabat_prodi_1", mahasiswa.getJurusan().getPegawai1() == null ? -1L
									: mahasiswa.getJurusan().getPegawai1().getId());

							map.put("jenis_pejabat_prodi_1", mahasiswa.getJurusan().getLabelPejabat1());
							map.put("nama_pejabat_prodi_1", mahasiswa.getJurusan().getPegawai1() == null ? ""
									: mahasiswa.getJurusan().getPegawai1().getNama());
							map.put("nip_pejabat_prodi_1", mahasiswa.getJurusan().getPegawai1() == null ? ""
									: mahasiswa.getJurusan().getPegawai1().getCode());

							map.put("id_pejabat_prodi_2", mahasiswa.getJurusan().getPegawai2() == null ? -1L
									: mahasiswa.getJurusan().getPegawai2().getId());
							map.put("jenis_pejabat_prodi_2", mahasiswa.getJurusan().getLabelPejabat2());
							map.put("nama_pejabat_prodi_2", mahasiswa.getJurusan().getPegawai2() == null ? ""
									: mahasiswa.getJurusan().getPegawai2().getNama());
							map.put("nip_pejabat_prodi_2", mahasiswa.getJurusan().getPegawai2() == null ? ""
									: mahasiswa.getJurusan().getPegawai2().getCode());

							map.put("id_pejabat_prodi_3", mahasiswa.getJurusan().getPegawai3() == null ? -1L
									: mahasiswa.getJurusan().getPegawai3().getId());
							map.put("jenis_pejabat_prodi_3", mahasiswa.getJurusan().getLabelPejabat3());
							map.put("nama_pejabat_prodi_3", mahasiswa.getJurusan().getPegawai3() == null ? ""
									: mahasiswa.getJurusan().getPegawai3().getNama());
							map.put("nip_pejabat_prodi_3", mahasiswa.getJurusan().getPegawai3() == null ? ""
									: mahasiswa.getJurusan().getPegawai3().getCode());

							map.put("id_pejabat_fakultas_1",
									mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? -1L
											: mahasiswa.getJurusan().getFakultas().getPegawai1().getId());

							map.put("jenis_pejabat_fakultas_1",
									mahasiswa.getJurusan().getFakultas().getLabelPejabat1());
							map.put("nama_pejabat_fakultas_1",
									mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getPegawai1().getNama());
							map.put("nip_pejabat_fakultas_1",
									mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getPegawai1().getCode());

							map.put("id_pejabat_fakultas_2",
									mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? -1L
											: mahasiswa.getJurusan().getFakultas().getPegawai1().getId());
							map.put("jenis_pejabat_fakultas_2",
									mahasiswa.getJurusan().getFakultas().getLabelPejabat2());
							map.put("nama_pejabat_fakultas_2",
									mahasiswa.getJurusan().getFakultas().getPegawai2() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getPegawai2().getNama());
							map.put("nip_pejabat_fakultas_2",
									mahasiswa.getJurusan().getFakultas().getPegawai2() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getPegawai2().getCode());

							map.put("id_pejabat_fakultas_3",
									mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? -1L
											: mahasiswa.getJurusan().getFakultas().getPegawai1().getId());
							map.put("jenis_pejabat_fakultas_3",
									mahasiswa.getJurusan().getFakultas().getLabelPejabat3());
							map.put("nama_pejabat_fakultas_3",
									mahasiswa.getJurusan().getFakultas().getPegawai3() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getPegawai3().getNama());
							map.put("nip_pejabat_fakultas_3",
									mahasiswa.getJurusan().getFakultas().getPegawai3() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getPegawai3().getCode());

							map.put("nama_mahasiswa", mahasiswa.getNama());
							map.put("nama", mahasiswa.getNama());
							map.put("kelas", mahasiswa.getKelas());
							map.put("hari", perkuliahan == null ? "" : perkuliahan.getHari());
							map.put("tahunangkatan", mahasiswa.getTahunangkatan());
							map.put("nim", mahasiswa.getNim());
							map.put("jurusan", mahasiswa.getJurusan().getNama());
							map.put("nama_jurusan", mahasiswa.getJurusan().getNama());
							map.put("id_fakultas", mahasiswa.getJurusan().getFakultas().getId());
							map.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
							map.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
							map.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
							map.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());
							map.put("semester", semester);
							map.put("tahun_ajaran", detailperkuliahan.getTahunAkademik());
							map.put("kode_mata_kuliah", matakuliah.getKode());
							map.put("mata_kuliah", matakuliah.getNama());
							map.put("nama_matakuliah", matakuliah.getNama());
							map.put("nama_matakuliah_en", matakuliah.getNamaEn());
							map.put("kode_matakuliah", matakuliah.getKode());
							map.put("semester_pk", perkuliahan == null ? null : perkuliahan.getSemester());

							map.put("sks", matakuliah.getSks());

							map.put("waktu_mulai", perkuliahan == null ? "" : perkuliahan.getWaktuMulai());
							map.put("waktu_selesai", perkuliahan == null ? "" : perkuliahan.getWaktuSelesai());
							map.put("kelas", perkuliahan == null ? "" : perkuliahan.getKelas());
							map.put("ruang", perkuliahan == null || perkuliahan.getRuang() == null ? ""
									: perkuliahan.getRuang().getNama());
							map.put("ruangan", perkuliahan == null || perkuliahan.getRuang() == null ? ""
									: perkuliahan.getRuang().getNama());
							map.put("dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
									: krsMahasiswa.getDosenPa().getNama());
							map.put("nip_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
									: krsMahasiswa.getDosenPa().getCode());
							map.put("nidn_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
									: krsMahasiswa.getDosenPa().getNidn());

							map.put("id_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? -1L
									: krsMahasiswa.getDosenPa().getId());
							map.put("id_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? -1L
									: mahasiswa.getJurusan().getKaprodi().getId());
							map.put("id_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? -1L
									: mahasiswa.getJurusan().getFakultas().getDekan().getId());
							map.put("id_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? -1L
									: mahasiswa.getJurusan().getFakultas().getPudek1().getId());
							map.put("id_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? -1L
									: mahasiswa.getJurusan().getFakultas().getPudek2().getId());
							map.put("id_kajur",
									mahasiswa.getJurusan().getGrupJurusan() == null
											|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? -1L
													: mahasiswa.getJurusan().getGrupJurusan().getKajur().getId());

							map.put("nama_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
									: mahasiswa.getJurusan().getKaprodi().getNama());
							map.put("nip_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
									: mahasiswa.getJurusan().getKaprodi().getCode());
							map.put("nidn_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
									: mahasiswa.getJurusan().getKaprodi().getNidn());

							map.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getDekan().getNama());

							map.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
							map.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

							map.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());

							map.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
							map.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

							map.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());

							map.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
							map.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

							map.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
							map.put("id_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? -1L
									: mahasiswa.getJurusan().getFakultas().getPudek3().getId());
							map.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
							map.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

							map.put("nama_kajur",
									mahasiswa.getJurusan().getGrupJurusan() == null
											|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
													: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNama());

							map.put("nip_kajur",
									mahasiswa.getJurusan().getGrupJurusan() == null
											|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
													: mahasiswa.getJurusan().getGrupJurusan().getKajur().getCode());
							map.put("nidn_kajur",
									mahasiswa.getJurusan().getGrupJurusan() == null
											|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
													: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNidn());

							map.put("dosen", perkuliahan == null ? "" : perkuliahan.ambilNamaDosens());
							map.put("merupakan_paralel",
									perkuliahan == null ? false : perkuliahan.getMerupakan_paralel());
							map.put("nama_perguruan_tinggi",
									mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama());
							map.put("alamat1", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat1());
							map.put("alamat2", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat2());
							map.put("telepon", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getTelepon());
							map.put("faksimili", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFaksimili());

							map.put("perkuliahandimulai",
									perkuliahan == null ? null : perkuliahan.getPerkuliahanDimulai());
							map.put("perkuliahansampai",
									perkuliahan == null ? null : perkuliahan.getPerkuliahanSampai());

							map.put("catatan", krsMahasiswa == null ? "" : krsMahasiswa.getCatatanKhs());

							map.put("catatan_krs", krsMahasiswa == null ? "" : krsMahasiswa.getCatatan());
							map.put("keteranganjadwal", perkuliahan == null ? null : perkuliahan.getKeteranganJadwal());

							map.put("total_nilai", detailperkuliahan.getTotalNilai());
							map.put("nilai_huruf", detailperkuliahan.getNilaiHuruf());
							map.put("nilai_ip", detailperkuliahan.getTotalIP());
							map.put("nilai_ipk", detailperkuliahan.getTotalIP());
							map.put("lulus", detailperkuliahan.getLulus() ? "Lulus" : "Tidak Lulus");

							if (perkuliahan != null) {
								List<Dosen> dosens = perkuliahan.populateDosenBuNama();
								int indexDosen = 1;
								for (Dosen d : dosens) {
									map.put("dosen_id_" + indexDosen, d.getId());
									map.put("dosen_nama_" + indexDosen, d.getNama());
									map.put("dosen_kode_" + indexDosen, d.getMycode());
									map.put("dosen_nidn_" + indexDosen, d.getNidn());
									map.put("dosen_nuptk_" + indexDosen, d.getNuptk());

									map.put("dosen_nip_" + indexDosen, d.getCode());
									String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(d));
									map.put("foto_dosen_" + indexDosen, url);
									indexDosen++;
								}
							}

							boolean ngulang = false;
							Integer ngulang_smt = 0;
							for (Long ngulangDid : detailperkuliahansDataSebelumnya) {
								Detailperkuliahan ngulangD = (Detailperkuliahan) GeneralValueObject
										.ambilData(Detailperkuliahan.class, ngulangDid.toString());
								if (ngulangD != null) {
									Matakuliah matakuliah1 = ngulangD.getPerkuliahan() != null
											? ngulangD.getPerkuliahan().getMatakuliah()
											: ngulangD.getMatakuliahKonversi();
									if (matakuliah1 != null && matakuliah != null
											&& matakuliah1.getKode().equalsIgnoreCase(matakuliah.getKode())) {
										ngulang = true;
										ngulang_smt = ngulangD.getSemester();
									}
								}
							}
							map.put("ngulang", ngulang);
							map.put("ngulang_smt", ngulang_smt);
							map.put("keteranganjadwal", perkuliahan == null ? null : perkuliahan.getKeteranganJadwal());

							maps.add(map);
						}
					}
				}
			}
			parameters.put("maps", maps);
			detailperkuliahansData = null;
		} else {
			parameters.put("maps", null);
		}
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			boolean boleh_cetak_khs = Common.bolehKonfigurasi("mahasiswa_boleh_melihat_khs_sendiri");
			try {
				if (!boleh_cetak_khs) {

					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu Mahasiswa untuk sementara tidak diperkenankan mencetak Kartu Hasil Studi (KHS) secara mandiri. Langkah yang dapat dilakukan: (1) hubungi bagian administrasi akademik untuk memperoleh KHS Anda; (2) sampaikan Nomor Induk Mahasiswa kepada petugas; (3) hubungi admin apabila memerlukan informasi lebih lanjut.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

					return;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan KHS", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
			}
		}
		try {

			Map parameters = generateParameter();
			if (parameters == null) {
				if (bandboxMahasiswa.getAttribute("mahasiswa") != null && semesterAbsensiUjian.getSelectedItem() != null) {
					Integer semester = (Integer) semesterAbsensiUjian.getSelectedItem().getValue();
					MyMessageboxConfig.show(
							"Laporan KHS semester " + semester
									+ " belum dapat dibuat. Kemungkinan data KRS/KHS semester ini belum tersinkron, nilai belum lengkap, atau mahasiswa belum memenuhi syarat pembayaran/blokir nilai. Silakan klik Hitung Ulang IP/IPK lalu cetak ulang; bila tetap gagal, cek status pembayaran mahasiswa dan sinkronkan kembali data KRS.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
				return;
			}

			File file = Report.generateFileReportWithProgress(Report.PDF, parameters, "Kartu_Hasil_Studi",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			if (file == null) {
				// FIX (blank KHS): generateParameter()/Report.generateFileReportWithProgress(...) balik
				// null secara diam-diam (mis. mahasiswa belum boleh melihat nilai karena status
				// pembayaran/blokir -- lihat NilaiValidator.checkBolehLihatNilai) dan CommonReport.
				// tampilkanReportPDF juga diam-diam no-op saat file==null, sehingga area konten tampak
				// kosong total tanpa penjelasan. Hanya tampilkan peringatan ini bila mahasiswa & semester
				// SUDAH dipilih pengguna (bukan saat window baru dibuka / belum ada pilihan -- kedua
				// kondisi itu juga membuat generateParameter() balik null tapi memang belum saatnya
				// menampilkan laporan apa pun).
				if (bandboxMahasiswa.getAttribute("mahasiswa") != null && semesterAbsensiUjian.getSelectedItem() != null) {
					MyMessageboxConfig.show(
							"Laporan KHS tidak dapat ditampilkan untuk mahasiswa/semester ini. Kemungkinan mahasiswa "
									+ "belum diperkenankan melihat nilai (mis. status pembayaran belum memenuhi syarat, "
									+ "atau ada blokir nilai), atau data belum lengkap. Hubungi admin bila masalah berlanjut.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
				return;
			}
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan KHS", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
