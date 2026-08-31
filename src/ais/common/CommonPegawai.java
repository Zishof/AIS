package ais.common;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Label;
import org.zkoss.zul.Timer;

import ais.action.master.recruitment.helper.DefaultNisGenerator;
import ais.action.master.recruitment.helper.DefaultNoRegGeneratorPegawai;
import ais.action.master.recruitment.helper.DefaultNoUjianGeneratorPegawai;
import ais.action.master.recruitment.helper.NisGenerator;
import ais.action.master.recruitment.helper.NoRegGeneratorPegawai;
import ais.action.master.recruitment.helper.NoUjianGeneratorPegawai;
import ais.action.report.Report;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.file.FotoCalonPegawai;
import ais.database.model.file.FotoPegawai;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.recruitment.RuangGelombangPendaftaranPegawaiPegawai;
import ais.database.model.recruitment.RuangPegawai;
import ais.ui.util.MyMessageboxConfig;

/**
 * Kumpulan method utilitas statis untuk mendukung alur <b>rekrutmen pegawai</b> (recruitment) di
 * AIS: penempatan calon pegawai ke ruang ujian, pembangkitan nomor ujian/nomor registrasi/NIP
 * (nomor induk pegawai) lewat mekanisme generator yang dapat dikonfigurasi (pluggable), impor
 * massal hasil kelulusan dari berkas Excel, penyalinan lampiran (foto) dari data calon pegawai ke
 * data pegawai definitif, serta penggabungan (merge) data {@link CalonPegawai} menjadi
 * {@link Pegawai} resmi begitu calon dinyatakan diterima. Kelas ini merupakan padanan pada domain
 * kepegawaian dari pola yang serupa dipakai pada domain penerimaan mahasiswa baru (PMB) di AIS —
 * keduanya berbagi konsep "calon" yang di-generate nomor ujian/registrasinya, lulus seleksi, lalu
 * di-generate nomor induk dan disalin datanya menjadi entitas definitif.
 *
 * <p>
 * <b>Pola generator ter-plugin (strategy pattern via refleksi)</b> — tiga aspek pembangkitan
 * nomor (nomor ujian lewat {@link #generateNoUjian(CalonPegawai)}, nomor registrasi lewat
 * {@link #generateNoRegistrasi(CalonPegawai)}, dan NIP/nomor induk lewat
 * {@link #onGenerateNis(CalonPegawai, NisGenerator)}/{@link #uploadKelulusan(File, EventListener)})
 * tidak menghardcode logika pembangkitan nomor di kelas ini, melainkan membaca NAMA KELAS
 * implementasi konkret dari konfigurasi sistem ({@code class_untuk_generate_no_ujian_pegawai},
 * {@code class_untuk_generate_no_reg_pegawai}, {@code class_untuk_generate_nis}, masing-masing
 * dengan fallback ke implementasi default {@link DefaultNoUjianGeneratorPegawai}/
 * {@link DefaultNoRegGeneratorPegawai}/{@link DefaultNisGenerator} bila konfigurasi belum diisi),
 * lalu membuat instance-nya secara dinamis lewat {@link Class#forName(String)} +
 * {@link Class#newInstance()}. Pola ini memungkinkan setiap institusi memakai algoritma
 * penomoran yang berbeda (mis. format NIP yang menyertakan tahun masuk, kode unit kerja, dsb.)
 * tanpa mengubah kode inti kelas ini — cukup mengganti nilai konfigurasi ke nama kelas
 * implementasi kustom yang sesuai kontrak interface terkait.
 * </p>
 *
 * <p>
 * <b>Penggabungan data calon pegawai menjadi pegawai definitif</b> —
 * {@link #savePegawai(Session, CalonPegawai, String, boolean)} adalah method inti yang
 * mengonversi satu {@link CalonPegawai} menjadi {@link Pegawai}: bila calon pegawai belum
 * memiliki relasi ke {@link Pegawai}, method ini mencoba mencocokkan pegawai yang sudah ada
 * berdasarkan kombinasi nama (exact match, case-insensitive) dan tanggal lahir sebelum memutuskan
 * membuat {@link Pegawai} baru; seluruh properti yang namanya sama pada kedua entitas disalin
 * secara generik lewat {@link ClassMetadata} Hibernate (refleksi metadata Hibernate, bukan
 * refleksi Java biasa), sehingga penambahan kolom baru pada kedua entitas di masa depan tidak
 * memerlukan perubahan kode penyalinan manual di kelas ini.
 * </p>
 *
 * <p>
 * <b>Impor massal kelulusan</b> — {@link #uploadKelulusan(File, EventListener)} membaca berkas
 * Excel (format {@code .xlsx} lewat ZK POI {@link XSSFWorkbook}) berisi daftar calon pegawai yang
 * lulus beserta NIP yang ditentukan manual (opsional; bila kosong, NIP dibangkitkan otomatis),
 * dan memprosesnya baris demi baris di dalam sebuah {@link Thread} terpisah agar antarmuka
 * pengguna tidak terkunci (progres ditampilkan lewat {@link Timer} ZK yang membaca nilai
 * {@link Label} yang diperbarui oleh thread pekerja). Kegagalan pada satu baris dicatat lewat
 * {@link ais.common.UploadReportHelper} dan tidak menghentikan pemrosesan baris-baris berikutnya.
 * </p>
 *
 * <p>
 * Kelas ini murni statis (tidak ada state instance) dan bergantung pada berbagai util Hibernate
 * AIS ({@link HibernateUtil}, {@link StreamingHibernateUtil} untuk sesi khusus data
 * biner/lampiran) serta komponen antarmuka ZK ({@link Label}, {@link Timer},
 * {@link MyMessageboxConfig}) untuk umpan balik visual ke pengguna selama proses berjalan.
 * </p>
 */
public class CommonPegawai {
	/**
	 * Mencarikan ruang ujian yang masih tersedia (belum penuh) untuk gelombang pendaftaran
	 * pegawai yang sama dengan {@code calonPegawai}, lalu menempatkan {@code calonPegawai} ke
	 * ruang tersebut (membuat atau memperbarui baris penempatan
	 * {@link RuangGelombangPendaftaranPegawaiPegawai}).
	 *
	 * <p>
	 * Ruang dipilih berdasarkan id terkecil ({@code Projections.min("id")}) di antara ruang-ruang
	 * yang belum ditandai penuh ({@code penuh = 0}) untuk gelombang yang sama. Setelah calon
	 * ditempatkan, jumlah peserta yang sudah memiliki nomor ujian di ruang tersebut dihitung
	 * ulang; bila jumlah tersebut (ditambah margin 2) sudah mencapai atau melebihi kapasitas
	 * ruangan, ruang tersebut langsung ditandai penuh ({@code penuh = 1}) agar tidak dipilih lagi
	 * untuk calon berikutnya. Bila calon sudah pernah memiliki penempatan ruang untuk gelombang
	 * ini (bersyarat memiliki nomor ujian), baris penempatan yang sudah ada diperbarui alih-alih
	 * membuat baris baru.
	 * </p>
	 *
	 * @param calonPegawai calon pegawai yang hendak dicarikan/ditempatkan ke ruang ujian
	 * @return baris penempatan {@link RuangGelombangPendaftaranPegawaiPegawai} yang
	 *         disimpan/diperbarui, atau {@code null} bila tidak ada ruang yang tersedia (semua
	 *         ruang pada gelombang tersebut sudah penuh)
	 */
	public static RuangGelombangPendaftaranPegawaiPegawai dapatkanRuangUjian(CalonPegawai calonPegawai) {
		Session session = HibernateUtil.currentSession();

		Long idmin = (Long) session.createCriteria(RuangPegawai.class).createAlias("ujianPegawai", "ujianPegawai")
				.add(Restrictions.eq("gelombangPendaftaranPegawai", calonPegawai.getGelombangPendaftaranPegawai()))
				.add(Restrictions.eq("penuh", 0))
				.add(Restrictions.eq("ujianPegawai.gelombangPendaftaranPegawai",
						calonPegawai.getGelombangPendaftaranPegawai()))
				.setProjection(Projections.min("id")).uniqueResult();

		if (idmin == null) {
			return null;
		}

		RuangPegawai ruangSelected = (RuangPegawai) session.createCriteria(RuangPegawai.class)
				.add(Restrictions.idEq(idmin)).uniqueResult();

		Number t = ((Number) (session.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
				.createAlias("calonPegawai", "calonPegawai").add(Restrictions.ne("calonPegawai.noUjian", ""))
				.add(Restrictions.isNotNull("calonPegawai.noUjian")).add(Restrictions.eq("ruangPegawai", ruangSelected))
				.setProjection(Projections.rowCount()).uniqueResult()));

		Integer isiRuang = t == null ? 0 : t.intValue();

		RuangGelombangPendaftaranPegawaiPegawai ruangGelombangPendaftaranPegawaiPegawai = (RuangGelombangPendaftaranPegawaiPegawai) session
				.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
				.createAlias("calonPegawai", "calonPegawai").add(Restrictions.ne("calonPegawai.noUjian", ""))
				.add(Restrictions.isNotNull("calonPegawai.noUjian")).add(Restrictions.eq("calonPegawai", calonPegawai))
				.setMaxResults(1).uniqueResult();
		if (ruangGelombangPendaftaranPegawaiPegawai == null) {
			ruangGelombangPendaftaranPegawaiPegawai = new RuangGelombangPendaftaranPegawaiPegawai();
		}
		ruangGelombangPendaftaranPegawaiPegawai.setCalonPegawai(calonPegawai);
		ruangGelombangPendaftaranPegawaiPegawai.setRuangPegawai(ruangSelected);
		Common.refreshSaveOrUpdate(session, ruangGelombangPendaftaranPegawaiPegawai);

		if (ruangSelected.getKapasitasRuangan() < (isiRuang + 2)) {
			ruangSelected.setPenuh(1);
			Common.refreshUpdate(session, ruangSelected);
		}

		return ruangGelombangPendaftaranPegawaiPegawai;
	}

	/**
	 * Membangkitkan nomor ujian untuk {@code calonPegawai} lewat implementasi
	 * {@link NoUjianGeneratorPegawai} yang dikonfigurasi (konfigurasi
	 * {@code class_untuk_generate_no_ujian_pegawai}, default {@link DefaultNoUjianGeneratorPegawai}),
	 * dibuat secara dinamis lewat refleksi ({@link Class#forName(String)}).
	 *
	 * <p>
	 * Bila pembangkitan gagal (kelas generator tidak ditemukan, gagal diinstansiasi, atau
	 * melempar pengecualian saat menghasilkan nomor), kotak pesan informasi ditampilkan kepada
	 * pengguna dan kegagalan dicatat lewat {@link Common#tampilErrorJikaAdmin(Exception)}; method
	 * mengembalikan string kosong alih-alih melempar pengecualian ke pemanggil.
	 * </p>
	 *
	 * @param calonPegawai calon pegawai yang hendak dibangkitkan nomor ujiannya
	 * @return nomor ujian yang dibangkitkan, atau string kosong ({@code ""}) bila pembangkitan
	 *         gagal
	 * @throws Exception dideklarasikan pada signature namun praktiknya tidak dilempar keluar
	 *                    (kegagalan ditangani secara internal)
	 */
	@SuppressWarnings({})
	public static String generateNoUjian(CalonPegawai calonPegawai) throws Exception {

		try {
			NoUjianGeneratorPegawai noUjianGenerator = (NoUjianGeneratorPegawai) Class
					.forName(Common.getKonfigurasi("class_untuk_generate_no_ujian_pegawai",
							DefaultNoUjianGeneratorPegawai.class.getName()).getNilai())
					.newInstance();
			return noUjianGenerator.generateNoUjian(calonPegawai);
		} catch (Exception e) {
			MyMessageboxConfig.show("Nomor Ujian tidak bisa di generate. Harap segera menghubungi Administrator",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			Common.tampilErrorJikaAdmin(e);
		}

		return "";

	}

	/**
	 * Membangkitkan nomor registrasi untuk {@code calonPegawai} lewat implementasi
	 * {@link NoRegGeneratorPegawai} yang dikonfigurasi (konfigurasi
	 * {@code class_untuk_generate_no_reg_pegawai}, default {@link DefaultNoRegGeneratorPegawai}),
	 * dibuat secara dinamis lewat refleksi. Perilaku penanganan kegagalan identik dengan
	 * {@link #generateNoUjian(CalonPegawai)}.
	 *
	 * @param calonPegawai calon pegawai yang hendak dibangkitkan nomor registrasinya
	 * @return nomor registrasi yang dibangkitkan, atau string kosong ({@code ""}) bila
	 *         pembangkitan gagal
	 * @throws Exception dideklarasikan pada signature namun praktiknya tidak dilempar keluar
	 *                    (kegagalan ditangani secara internal)
	 */
	public static String generateNoRegistrasi(CalonPegawai calonPegawai) throws Exception {
		try {

			NoRegGeneratorPegawai noRegGenerator = (NoRegGeneratorPegawai) Class.forName(Common
					.getKonfigurasi("class_untuk_generate_no_reg_pegawai", DefaultNoRegGeneratorPegawai.class.getName())
					.getNilai()).newInstance();
			return noRegGenerator.generateNoReg(calonPegawai);
		} catch (Exception e) {
			MyMessageboxConfig.show("Nomor Registrasi tidak bisa di generate. Harap segera menghubungi Administrator",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			Common.tampilErrorJikaAdmin(e);
		}
		return "";
	}

	/**
	 * Membangkitkan Nomor Induk Pegawai (NIP/NIS) untuk satu {@code calonPegawai} yang sudah
	 * dinyatakan lulus/diterima, lalu menggabungkan datanya menjadi entitas {@link Pegawai}
	 * definitif lewat {@link #savePegawai(Session, CalonPegawai, String, boolean)} dan mencetak
	 * laporan biodata dalam bentuk PDF.
	 *
	 * <p>
	 * Alur kerja: (1) bila {@code calonPegawai} belum dinyatakan diterima
	 * ({@code getTelahDiterima()} bernilai {@code false}), tampilkan peringatan dan hentikan
	 * proses (kembalikan string kosong); (2) bila calon SUDAH memiliki relasi {@link Pegawai}
	 * (NIP sudah pernah dibangkitkan sebelumnya), tampilkan info NIP yang sudah ada dan, setelah
	 * kotak pesan ditutup, cetak ulang laporan biodata PDF tanpa membangkitkan NIP baru; (3) bila
	 * belum, bangkitkan NIP baru lewat {@code nimGenerator}, simpan ke
	 * {@code calonPegawai.setNomorInduk(nim)}, panggil {@link #savePegawai} untuk membuat/
	 * memperbarui entitas {@link Pegawai}, tampilkan info NIP baru, lalu — setelah kotak pesan
	 * ditutup — salin lampiran foto lewat {@link #copyLampiran(CalonPegawai, Pegawai)} dan cetak
	 * laporan biodata PDF.
	 * </p>
	 *
	 * @param calonPegawai calon pegawai yang hendak dibangkitkan NIP-nya; harus sudah berstatus
	 *                      diterima
	 * @param nimGenerator strategi pembangkitan NIP yang dipakai bila calon belum memiliki
	 *                      {@link Pegawai}
	 * @return NIP yang dibangkitkan (atau NIP yang sudah ada bila calon sudah memiliki
	 *         {@link Pegawai}); string kosong bila calon belum dinyatakan diterima
	 * @throws Exception diteruskan dari kegagalan pencetakan laporan PDF atau operasi Hibernate
	 *                    yang tidak tertangkap
	 */
	public static String onGenerateNis(final CalonPegawai calonPegawai, NisGenerator nimGenerator) throws Exception {

		if (!calonPegawai.getTelahDiterima()) {
			MyMessageboxConfig.show("Calon pegawai ini belum dinyatakan lulus / belum diterima!", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return "";

		} else {
			String nim = "";
			if (calonPegawai.getPegawai() != null) {
				MyMessageboxConfig.show(
						"Pegawai telah mendapatkan Kode Pegawai, yaitu : " + calonPegawai.getPegawai().getCode(),
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Map<String, Serializable> parameters = ais.common.HashMapGenerator
										.getRandStringSerializable();
								parameters.put("nim", calonPegawai.getPegawai().getCode());
								parameters.put("biodata_id", calonPegawai.getId());

								calonPegawai.putPhoto(parameters);

								Report.generatePDFReport(Report.PDF, parameters, "recruitment/Biodata_Nis",
										ais.ui.util.WaktuUtil.getDate());
							}
						});
				return nim;
			} else {
				nim = nimGenerator.generateNis(calonPegawai);
			}
			calonPegawai.setNomorInduk(nim);
			Session session = HibernateUtil.currentSession();
			final Pegawai pegawai = CommonPegawai.savePegawai(session, calonPegawai, nim, false);

			MyMessageboxConfig.show("Nomor Induk Pegawai berhasil di-generate: \"" + nim + "\"", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							CommonPegawai.copyLampiran(calonPegawai, pegawai);

							Map<String, Serializable> parameters = ais.common.HashMapGenerator
									.getRandStringSerializable();
							parameters.put("nim", pegawai.getCode());
							parameters.put("biodata_id", calonPegawai.getId());
							calonPegawai.putPhoto(parameters);

							Report.generatePDFReport(Report.PDF, parameters, "recruitment/Biodata_Nis",
									ais.ui.util.WaktuUtil.getDate());
						}
					});

			return nim;

		}
	}

	/**
	 * Mengimpor hasil kelulusan seleksi pegawai secara massal dari sebuah berkas Excel
	 * ({@code .xlsx}), mencocokkan setiap baris dengan {@link CalonPegawai} yang sudah ada
	 * (berdasarkan kode/id pada kolom pertama, atau nomor registrasi pada kolom kedua sebagai
	 * fallback), membangkitkan/menetapkan NIP, dan menggabungkan data menjadi {@link Pegawai}
	 * definitif — seluruhnya dijalankan pada {@link Thread} terpisah agar antarmuka tidak
	 * terkunci selama proses berjalan.
	 *
	 * <p>
	 * Format kolom yang diharapkan pada setiap baris data (mulai baris kedua, indeks 1): kolom 0
	 * = kode/id {@link CalonPegawai} (angka); kolom 1 = nomor registrasi (dipakai sebagai fallback
	 * pencarian bila kolom 0 tidak berupa angka valid); kolom 3 = NIP yang ditentukan manual
	 * (opsional — bila kosong, NIP dibangkitkan otomatis lewat {@link NisGenerator} yang
	 * dikonfigurasi, konfigurasi {@code class_untuk_generate_nis}, default
	 * {@link DefaultNisGenerator}).
	 * </p>
	 *
	 * <p>
	 * Untuk setiap baris: bila NIP hasil pembacaan/pembangkitan sudah dipakai oleh
	 * {@link Pegawai} LAIN (bukan milik {@link CalonPegawai} baris ini), baris tersebut dilewati
	 * dan dicatat sebagai peringatan (bukan kegagalan fatal) pada {@code peringatan}; bila NIP
	 * valid dan belum dipakai, data disimpan dalam transaksi Hibernate tersendiri per baris lewat
	 * {@link #savePegawai(Session, CalonPegawai, String, boolean)} (dengan
	 * {@code commitMaual=false} karena transaksi sudah dibuka/di-commit secara eksplisit di
	 * pemanggil di sini), lampiran foto disalin lewat {@link #copyLampiran(CalonPegawai, Pegawai)},
	 * dan keberhasilan/kegagalan baris dicatat ke {@link ais.common.UploadReportHelper}. Kegagalan
	 * pada satu baris (exception apa pun) tidak menghentikan pemrosesan baris lain — baris
	 * tersebut dicatat sebagai gagal pada laporan dan iterasi berlanjut ke baris berikutnya.
	 * </p>
	 *
	 * <p>
	 * Progres ditampilkan secara berkala lewat {@link Timer} ZK (interval 200ms) yang membaca
	 * nilai {@link Label} {@code label} yang diperbarui thread pekerja; begitu {@code label}
	 * kembali kosong (menandakan thread pekerja selesai), timer menampilkan ringkasan hasil
	 * (lewat {@link ais.common.UploadReportHelper#getRingkasan()}), mengunduh berkas laporan hasil
	 * proses (bila ada), memanggil {@code eventListener} yang diberikan, lalu melepas dirinya
	 * sendiri dari halaman ({@code timer.detach()}).
	 * </p>
	 *
	 * @param file          berkas Excel {@code .xlsx} berisi daftar kelulusan yang hendak diimpor
	 * @param eventListener listener yang dipanggil setelah kotak pesan ringkasan hasil impor
	 *                      ditutup oleh pengguna
	 * @throws Exception diteruskan dari kegagalan pembacaan berkas Excel awal atau inisialisasi
	 *                    komponen antarmuka sebelum thread pekerja dimulai
	 */
	public static void uploadKelulusan(final File file, final EventListener eventListener) throws Exception {

		final Label peringatan = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		final Label downloadPath = new Label("");
		// FIX compile "cannot find symbol: report": harus dideklarasikan sebelum
		// timer.addEventListener(...) karena dipakai di dalam closure onTimer di bawah.
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Kelulusan Pegawai");
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					if (!downloadPath.getValue().isEmpty()) {
						try { org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) { ais.common.ErrorAuditUtil.record(eD, "auto-audit(empty-catch) CommonPegawai download-laporan"); }
					}
					MyMessageboxConfig.show(
							"Upload kelulusan berhasil dilakukan."
									+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue())
									+ "\n\n" + report.getRingkasan(),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {
					NisGenerator nisGenerator = (NisGenerator) Class.forName(
							Common.getKonfigurasi("class_untuk_generate_nis", DefaultNisGenerator.class.getName())
									.getNilai().trim())
							.newInstance();

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);
					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Long code = -1L;
							try {
								String content = Common.getCellContent(Common.getCell(sheet, 0, i));
								System.out.println("content = " + content);
								code = Long.parseLong(content);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPegawai.java:231");

							}
							if (code.equals(-1L)) {
								try {

									String content = Common.getCellContent(Common.getCell(sheet, 1, i));
									System.out.println("noRegistrasi = " + content);
									code = (Long) session.createCriteria(CalonPegawai.class)
											.add(Restrictions.eq("noRegistrasi", content))
											.setProjection(Projections.property("id")).setMaxResults(1).uniqueResult();
								} catch (Exception e) {
									continue;
								}

							}

							System.out.println("code = " + code);

							if (code == null || code.equals(-1L)) {
								continue;
							}

							String nim = "";
							try {

								nim = Common.getCellContent(Common.getCell(sheet, 3, i));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPegawai.java:258");

							}

							CalonPegawai calonPegawai = (CalonPegawai) session.createCriteria(CalonPegawai.class)
									.add(Restrictions.idEq(code)).uniqueResult();
							if (calonPegawai == null) {
								continue;
							}

							label.setValue("Upload data \"" + calonPegawai.getNama() + "\" ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

							if (nim.trim().isEmpty()
									&& (calonPegawai.getPegawai() == null || calonPegawai.getPegawai().getNim() == null
											|| calonPegawai.getPegawai().getNim().trim().isEmpty())) {
								nim = nisGenerator.generateNis(calonPegawai);
							} else if (nim.trim().isEmpty() && calonPegawai.getPegawai() != null
									&& calonPegawai.getPegawai().getNim() != null
									&& calonPegawai.getPegawai().getNim().trim().isEmpty()) {
								nim = calonPegawai.getPegawai().getNim();
							}

							int count = ((Number) session.createCriteria(Pegawai.class).add(Restrictions.eq("nim", nim))
									.add(Restrictions.ne("calonPegawai", calonPegawai.getId()))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
							if (count > 0) {
								System.out.println("nim = " + nim + " sudah ada");
								String my = "Calon pegawai \"" + calonPegawai + "\" nim = " + nim
										+ " sudah ada di database.\n";
								peringatan.setValue(peringatan.getValue() + my);

								continue;
							}

							System.out.println("calonPegawai = " + calonPegawai + ", nim = " + nim);

							Pegawai pegawai = null;
							session.getTransaction().begin();
							if (!nim.trim().isEmpty()) {
								pegawai = CommonPegawai.savePegawai(session, calonPegawai, nim.trim(), false);
							} else {
								Common.refreshUpdate(session, calonPegawai);
							}
							session.getTransaction().commit();

							report.sukses(i, nim + " – " + calonPegawai.getNama(), "");
							if (pegawai != null) {
								CommonPegawai.copyLampiran(calonPegawai, pegawai);
							}

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPegawai.java:308");
							report.gagal(i, String.valueOf(i), e, "Periksa data baris " + i);
						}

					}

					HibernateUtil.closeSession();

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/CommonPegawai.java:318");
				}

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) CommonPegawai laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	/**
	 * Menyalin foto milik {@link CalonPegawai} ({@link FotoCalonPegawai}) menjadi foto milik
	 * {@link Pegawai} definitif ({@link FotoPegawai}), memakai sesi Hibernate streaming
	 * ({@link StreamingHibernateUtil}) karena data foto bersifat biner/besar. Bila
	 * {@link Pegawai} yang bersangkutan sudah memiliki foto sebelumnya (baris {@link FotoPegawai}
	 * terbaru berdasarkan id), foto lama dihapus terlebih dahulu sebelum foto baru disimpan
	 * (mengganti, bukan menambah foto lain di sampingnya).
	 *
	 * <p>
	 * Bila {@code calonPegawai} tidak memiliki foto tersimpan, method tidak melakukan apa pun.
	 * Kegagalan (mis. sesi Hibernate bermasalah) ditangkap secara diam-diam dan dicatat lewat
	 * {@link ais.common.ErrorAuditUtil#record(Throwable, String)}, tidak dilempar ke pemanggil.
	 * </p>
	 *
	 * @param calonPegawai sumber foto (calon pegawai)
	 * @param pegawai      tujuan foto (pegawai definitif yang baru dibuat/diperbarui)
	 */
	public static void copyLampiran(CalonPegawai calonPegawai, Pegawai pegawai) {

		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

			FotoCalonPegawai fotocalonPegawai = (FotoCalonPegawai) streamingSession
					.createCriteria(FotoCalonPegawai.class).add(Restrictions.eq("calonPegawai", calonPegawai.getId()))
					.setMaxResults(1).uniqueResult();

			if (fotocalonPegawai != null && fotocalonPegawai.getFoto() != null) {

				FotoPegawai fotoPegawai = (FotoPegawai) streamingSession.createCriteria(FotoPegawai.class)
						.addOrder(Order.desc("id")).add(Restrictions.eq("pegawai", pegawai.getId())).setMaxResults(1)
						.uniqueResult();

				if (fotoPegawai != null) {
					streamingSession.getTransaction().begin();
					streamingSession.delete(fotoPegawai);
					streamingSession.getTransaction().commit();
				}

				fotoPegawai = new FotoPegawai();
				fotoPegawai.setNama(fotocalonPegawai.getNama());
				fotoPegawai.setKeterangan(fotocalonPegawai.getKeterangan());
				fotoPegawai.setPegawai(pegawai.getId());
				fotoPegawai.setFoto(fotocalonPegawai.getFoto());

				streamingSession.getTransaction().begin();
				streamingSession.save(fotoPegawai);
				streamingSession.getTransaction().commit();

			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPegawai.java:362");
			// TODO: handle exception
		}
	}

	/**
	 * Method inti penggabungan (merge) data {@link CalonPegawai} menjadi entitas {@link Pegawai}
	 * definitif: mencari {@link Pegawai} yang sudah ada terkait calon (baik lewat relasi langsung
	 * maupun pencocokan nama+tanggal lahir), menyalin seluruh properti yang namanya sama dari
	 * {@code calonPegawai} ke {@code pegawai} secara generik lewat metadata Hibernate
	 * ({@link ClassMetadata}), menetapkan kode/NIP, lalu menyimpan.
	 *
	 * <p>
	 * Urutan kerja: (1) bila {@code calonPegawai} belum memiliki relasi {@link Pegawai}, dicoba
	 * ditemukan {@link Pegawai} yang sudah ada dengan nama yang PERSIS sama (case-insensitive,
	 * {@link MatchMode#EXACT}) DAN tanggal lahir yang sama — ini menangani skenario di mana
	 * pegawai yang sama pernah terdaftar dari jalur lain sebelum proses rekrutmen ini; (2) bila
	 * tetap tidak ditemukan, {@link Pegawai} baru dibuat; bila DITEMUKAN (baik lewat relasi
	 * langsung maupun pencocokan), parameter {@code nim} yang diberikan pemanggil DIABAIKAN dan
	 * digantikan oleh kode {@link Pegawai} yang sudah ada ({@code pegawai.getCode()}) — sehingga
	 * pegawai yang sudah punya kode tidak akan tertimpa kode baru secara tidak sengaja; (3)
	 * seluruh nama properti pada {@link CalonPegawai} disalin nilainya ke properti bernama sama
	 * pada {@link Pegawai} lewat {@link ClassMetadata#getPropertyValue}/{@code setPropertyValue}
	 * (kegagalan penyalinan satu properti — mis. karena tipe tidak kompatibel atau properti tidak
	 * ada pada tujuan — dilewati secara diam-diam per properti, tidak menghentikan penyalinan
	 * properti lain); (4) kode/NIP final ditetapkan ke {@code pegawai}; (5) relasi dua arah
	 * {@code pegawai.setCalonPegawai(id)} dan {@code calonPegawai.setPegawai(pegawai)} ditetapkan;
	 * (6) hasil disimpan lewat {@link Common#refreshSaveOrUpdate(Session, Object)}.
	 * </p>
	 *
	 * @param session      sesi Hibernate aktif yang dipakai untuk pencarian dan penyimpanan
	 * @param calonPegawai sumber data yang hendak digabungkan menjadi pegawai definitif
	 * @param nim          kode/NIP yang diusulkan; HANYA dipakai bila tidak ditemukan
	 *                      {@link Pegawai} yang sudah ada untuk calon ini — jika ditemukan, nilai
	 *                      ini diabaikan dan digantikan kode {@link Pegawai} yang sudah ada
	 * @param commitMaual  bila {@code true}, method membuka dan meng-commit transaksi Hibernate
	 *                      sendiri di sekeliling penyimpanan; bila {@code false}, pemanggil
	 *                      bertanggung jawab atas transaksi (dipakai saat pemanggil sudah berada
	 *                      di dalam transaksi yang lebih besar, mis. {@link #uploadKelulusan})
	 * @return entitas {@link Pegawai} yang sudah disimpan/diperbarui (baru atau yang sudah ada
	 *         sebelumnya, tergabung dengan data terbaru dari {@code calonPegawai})
	 */
	public static Pegawai savePegawai(Session session, CalonPegawai calonPegawai, String nim, boolean commitMaual) {

		Pegawai pegawai = calonPegawai.getPegawai();
		if (pegawai == null && calonPegawai != null) {
			pegawai = (Pegawai) session.createCriteria(Pegawai.class)
					.add(Restrictions.ilike("nama", calonPegawai.getNama(), MatchMode.EXACT))
					.add(Restrictions.eq("tanggalLahir", calonPegawai.getTanggalLahir())).setMaxResults(1)
					.uniqueResult();
		}
		if (pegawai == null) {
			pegawai = new Pegawai();
		} else {
			nim = pegawai.getCode();
		}
		pegawai.setCalonPegawai(calonPegawai.getId());

		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(CalonPegawai.class);
		ClassMetadata classMetadataPegawai = HibernateUtil.getClassMetadata(Pegawai.class);
		for (String p : classMetadata.getPropertyNames()) {
			try {
				Object val = classMetadata.getPropertyValue(calonPegawai, p, EntityMode.POJO);
				classMetadataPegawai.setPropertyValue(pegawai, p, val, EntityMode.POJO);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPegawai.java:389");

			}
		}

		pegawai.setCode(nim);

		if (commitMaual) {
			session.getTransaction().begin();
		}
		Common.refreshSaveOrUpdate(session, pegawai);
		calonPegawai.setPegawai(pegawai);
		if (commitMaual) {
			session.getTransaction().commit();
		}

		return pegawai;
	}
}
