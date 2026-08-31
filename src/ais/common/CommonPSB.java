package ais.common;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
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

import ais.action.master.sekolah.psb.nis.DefaultNisGenerator;
import ais.action.master.sekolah.psb.nis.NisGenerator;
import ais.action.master.sekolah.psb.noreg.DefaultNoRegGeneratorPsb;
import ais.action.master.sekolah.psb.noreg.NoRegGeneratorPsb;
import ais.action.master.sekolah.psb.noujian.DefaultNoUjianGeneratorPsb;
import ais.action.master.sekolah.psb.noujian.NoUjianGeneratorPsb;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.Report;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.FotoSiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.FormatNis;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB;
import ais.database.model.sekolah.RuangPSB;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyMessageboxConfig;

/**
 * Kelas utilitas statis pusat untuk modul PSB (Penerimaan Siswa Baru) sekolah pada AIS,
 * menangani seluruh siklus hidup seorang calon siswa SETELAH pendaftaran hingga menjadi siswa
 * resmi: alokasi ruang ujian, pembangkitan nomor ujian/nomor registrasi/nomor induk siswa
 * (NIS), konversi massal calon siswa yang lulus menjadi siswa aktif (lewat upload Excel), serta
 * penyalinan data terkait (foto, keanggotaan kelas reguler/les) dari {@link CalonSiswa} ke
 * {@link Siswa} hasil konversi.
 *
 * <p>
 * Kelompok fungsi utama pada kelas ini:
 * </p>
 * <ol>
 * <li><b>Alokasi ruang ujian</b> — {@link #dapatkanRuangUjian(CalonSiswa)} (dan varian internal
 * {@link #dapatkanRuangUjian(CalonSiswa, Session)}) menempatkan calon siswa ke ruang ujian PSB
 * yang belum penuh, memilih ruang dengan id terkecil yang tersedia untuk gelombang pendaftaran
 * terkait, lalu menandai ruang penuh bila kapasitasnya nyaris tercapai.</li>
 * <li><b>Pembangkitan nomor identitas</b> — {@link #generateNoUjian(CalonSiswa, List)}/
 * {@link #generateNoUjian(CalonSiswa)} (nomor ujian, lewat implementasi {@link
 * NoUjianGeneratorPsb} yang dapat diganti via konfigurasi {@code class_untuk_generate_no_ujian_psb}),
 * {@link #generateNoRegistrasi(CalonSiswa)} (nomor registrasi, lewat {@link NoRegGeneratorPsb}
 * via konfigurasi {@code class_untuk_generate_no_reg_psb}), serta {@link
 * #generateCode(FormatNis, CalonSiswa)}/{@link #getindex(FormatNis, CalonSiswa)} (nomor
 * agenda/surat berformat kustom lewat {@link FormatNis}, dengan nomor urut otomatis atau indeks
 * manual). Pola generator yang dapat diganti lewat konfigurasi kelas (reflection
 * {@link Class#forName(String)}) ini konsisten dipakai di seluruh kelas untuk memungkinkan
 * kustomisasi format nomor per instalasi AIS tanpa mengubah kode inti.</li>
 * <li><b>Konversi calon siswa menjadi siswa</b> — {@link #onGenerateNis(CalonSiswa,
 * NisGenerator)}/{@link #onGenerateNis(CalonSiswa, NisGenerator, boolean)} adalah alur INDIVIDUAL
 * (satu calon siswa, dipicu manual dari layar admin): membangkitkan NIS (lewat {@link FormatNis}
 * bila sekolah punya format aktif, atau {@link NisGenerator} sebagai fallback), memanggil
 * {@link #saveSiswa} untuk membuat/memperbarui record {@link Siswa}, lalu (opsional) mencetak
 * bukti PDF dan menyalin lampiran/keanggotaan kelas. {@link #uploadKelulusan(File, EventListener)}
 * adalah alur MASSAL (banyak calon siswa sekaligus lewat berkas Excel), dijalankan di THREAD
 * TERPISAH dengan progres dilaporkan lewat {@link Timer} ZK yang di-polling setiap 200ms,
 * memvalidasi kuota gelombang pendaftaran per baris, dan memakai {@link
 * ais.common.UploadReportHelper} untuk merangkum baris yang sukses/gagal menjadi satu laporan
 * yang dapat diunduh pengguna di akhir proses.</li>
 * <li><b>Method pendukung konversi</b> — {@link #saveSiswa(Session, CalonSiswa, String,
 * boolean)} adalah implementasi INTI pembuatan/pembaruan record {@link Siswa} dari
 * {@link CalonSiswa} (menyalin seluruh properti yang namanya sama persis di kedua entitas lewat
 * refleksi metadata Hibernate), sedangkan {@link #copyLampiran(CalonSiswa, Siswa)},
 * {@link #masukkanKelas(CalonSiswa, Siswa)}, dan {@link #masukkanKelasLes(CalonSiswa, Siswa)}
 * menyalin data pendukung (foto, keanggotaan kelas reguler, keanggotaan kelas les) setelah
 * siswa baru terbentuk.</li>
 * </ol>
 *
 * <p>
 * <b>Manajemen sesi & transaksi Hibernate</b> — kelas ini memakai CAMPURAN sesi mandiri
 * ({@code HibernateUtil.getSessionFactory().openSession()}, dipakai
 * {@link #dapatkanRuangUjian(CalonSiswa)} dan alur upload massal di dalam thread terpisah pada
 * {@link #uploadKelulusan}) dan sesi thread-local ({@code HibernateUtil.currentNativeSession()},
 * dipakai method-method lain). Beberapa method secara eksplisit menutup lalu membuka ulang sesi
 * thread-local di tengah proses (lihat komentar "session mungkin ditutup oleh masukkanKelas/Les
 * — buka ulang" pada {@link #uploadKelulusan}) karena method pendukung seperti
 * {@link #masukkanKelas}/{@link #masukkanKelasLes} menutup sesi thread-local di akhir
 * eksekusinya sebagai bagian dari pola "buka-pakai-tutup" per operasi.
 * </p>
 *
 * <p>
 * <b>Ketahanan alur upload massal</b> — {@link #uploadKelulusan} dirancang agar KEGAGALAN SATU
 * BARIS (mis. data siswa sudah dipakai calon siswa lain, transaksi database gagal) TIDAK
 * menggagalkan seluruh proses: setiap baris diproses dalam blok try-catch tersendiri, kegagalan
 * transaksi di-rollback dan sesi dibersihkan ({@code session.clear()}) sebelum melanjutkan ke
 * baris berikutnya (mencegah error "current transaction is aborted" PostgreSQL merambat ke
 * baris lain), dan setiap hasil (sukses/gagal beserta alasannya) dicatat ke
 * {@link ais.common.UploadReportHelper} untuk laporan akhir yang dapat diunduh pengguna.
 * </p>
 *
 * <p>
 * <b>CATATAN KEAMANAN:</b> pada {@link #saveSiswa(Session, CalonSiswa, String, boolean)}, kata
 * sandi awal akun siswa baru diisi dari NIS itu sendiri dan dienkripsi memakai
 * {@code Common.desEncrypter} — yaitu enkripsi SIMETRIS REVERSIBEL (DES), BUKAN fungsi hash
 * satu-arah (mis. bcrypt/SHA). Pola ini konsisten dengan kelas utilitas AIS lain
 * ({@code Common.desEncrypter}) sehingga bukan sesuatu yang unik untuk kelas ini, namun tetap
 * berarti siapa pun yang memegang kunci enkripsi DES dan akses ke kolom {@code pass} di
 * database berpotensi memulihkan kata sandi asli seluruh akun siswa (yang notabene sama dengan
 * NIS yang sudah publik/diketahui banyak pihak). Javadoc ini TIDAK mengubah mekanisme tersebut
 * sesuai instruksi; dicatat di sini sebagai observasi keamanan untuk ditindaklanjuti bila
 * dianggap perlu (mis. migrasi ke hashing satu-arah untuk akun baru).
 * </p>
 */
public class CommonPSB {

	/**
	 * Mencari dan mengalokasikan ruang ujian PSB yang tersedia bagi seorang calon siswa,
	 * dijalankan dalam sesi & transaksi Hibernate MANDIRI (bukan sesi thread-local) untuk
	 * isolasi proses dan mencegah kebocoran koneksi. Delegasi logika inti ke overload privat
	 * {@link #dapatkanRuangUjian(CalonSiswa, Session)}.
	 *
	 * @param calonSiswa calon siswa yang akan dialokasikan ruang ujian; bila {@code null} atau
	 *                   belum memiliki {@code gelombangPendaftaranPsb}, method langsung
	 *                   mengembalikan {@code null} tanpa membuka sesi database
	 * @return record {@link RuangGelombangPendaftaranPsbPSB} (alokasi ruang) yang berhasil
	 *         disimpan, atau {@code null} bila tidak ada ruang tersedia atau terjadi kegagalan
	 *         (transaksi di-rollback, kegagalan dicatat ke audit, tidak dilempar ke pemanggil)
	 */
	public static RuangGelombangPendaftaranPsbPSB dapatkanRuangUjian(CalonSiswa calonSiswa) {
		// Validasi awal untuk mencegah NullPointerException
		if (calonSiswa == null || calonSiswa.getGelombangPendaftaranPsb() == null) {
			return null;
		}

		Session session = null;
		Transaction tx = null;

		try {
			// Menggunakan openSession() untuk isolasi proses yang aman dan mencegah
			// kebocoran koneksi
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();

			RuangGelombangPendaftaranPsbPSB ruangAlokasi = dapatkanRuangUjian(calonSiswa, session);

			// Komit transaksi jika semua proses penyisipan data berhasil
			if (tx != null && tx.isActive()) {
				tx.commit();
			}
			return ruangAlokasi;

		} catch (Exception e) {
			// Rollback (kembalikan state database) jika terjadi error agar tabel tidak
			// terkunci (lock)
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/CommonPSB.java:83");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPSB.java:86");
			return null;

		} finally {
			// WAJIB: Menutup session di dalam blok finally agar dieksekusi dalam kondisi
			// apapun
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPSB.java:96");
				}
			}
		}
	}

	/**
	 * Implementasi inti pencarian & alokasi ruang ujian, dijalankan dalam {@code session} yang
	 * diberikan pemanggil (transaksi sudah dimulai di luar method ini).
	 *
	 * <p>
	 * Alur: (1) mencari {@link RuangPSB} dengan status belum penuh ({@code penuh=0}) untuk
	 * gelombang pendaftaran terkait, id terkecil (satu query gabungan, dioptimalkan dari
	 * sebelumnya dua query terpisah); (2) menghitung jumlah calon siswa yang sudah memiliki
	 * nomor ujian di ruang tersebut; (3) mengecek apakah calon siswa ini SUDAH memiliki alokasi
	 * ruang sebelumnya (untuk memperbarui, bukan menduplikasi); (4) menyimpan/memperbarui
	 * alokasi lewat {@link Common#refreshSaveOrUpdate(Session, Object)}; (5) bila kapasitas
	 * ruang akan terlampaui setelah penambahan calon siswa ini (isi + 2 melebihi kapasitas —
	 * ambang aman untuk menghindari race condition alokasi bersamaan), menandai ruang sebagai
	 * penuh ({@code setPenuh(1)}).
	 * </p>
	 *
	 * @param calonSiswa calon siswa yang akan dialokasikan; bila {@code null} atau belum
	 *                   memiliki {@code gelombangPendaftaranPsb}, langsung mengembalikan
	 *                   {@code null}
	 * @param session    sesi Hibernate aktif (transaksi dikelola oleh pemanggil)
	 * @return record alokasi {@link RuangGelombangPendaftaranPsbPSB} (baru atau yang
	 *         diperbarui), atau {@code null} bila tidak ada ruang ujian yang tersedia
	 */
	private static RuangGelombangPendaftaranPsbPSB dapatkanRuangUjian(CalonSiswa calonSiswa, Session session) {
		if (calonSiswa == null || calonSiswa.getGelombangPendaftaranPsb() == null) {
			return null;
		}

		// OPTIMASI: Menggabungkan 2 kueri (mencari ID terkecil dan mengambil objeknya)
		// menjadi 1 kueri tunggal.
		// Jauh lebih efisien untuk memori (RAM) dan mengurangi beban I/O database.
		RuangPSB ruangSelected = (RuangPSB) session.createCriteria(RuangPSB.class).createAlias("ujianPSB", "ujianPSB")
				.add(Restrictions.eq("gelombangPendaftaranPsb", calonSiswa.getGelombangPendaftaranPsb()))
				.add(Restrictions.eq("penuh", 0))
				.add(Restrictions.eq("ujianPSB.gelombangPendaftaranPsb", calonSiswa.getGelombangPendaftaranPsb()))
				.addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();

		if (ruangSelected == null) {
			return null; // Tidak ada ruang ujian yang tersedia
		}

		// Menghitung jumlah isi ruang saat ini
		Number t = (Number) session.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
				.createAlias("calonSiswa", "calonSiswa").add(Restrictions.isNotNull("calonSiswa.noUjian"))
				.add(Restrictions.ne("calonSiswa.noUjian", "")).add(Restrictions.eq("ruangPSB", ruangSelected))
				.setProjection(Projections.rowCount()).uniqueResult();

		int isiRuang = (t == null) ? 0 : t.intValue();

		// Mengecek apakah calon siswa sudah memiliki alokasi ruang ujian sebelumnya
		RuangGelombangPendaftaranPsbPSB ruangGelombangPendaftaranPsbPSB = (RuangGelombangPendaftaranPsbPSB) session
				.createCriteria(RuangGelombangPendaftaranPsbPSB.class).createAlias("calonSiswa", "calonSiswa")
				.add(Restrictions.isNotNull("calonSiswa.noUjian")).add(Restrictions.ne("calonSiswa.noUjian", ""))
				.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

		// Jika belum memiliki alokasi ruang, buat objek alokasi baru
		if (ruangGelombangPendaftaranPsbPSB == null) {
			ruangGelombangPendaftaranPsbPSB = new RuangGelombangPendaftaranPsbPSB();
			ruangGelombangPendaftaranPsbPSB.setCalonSiswa(calonSiswa);
		}

		ruangGelombangPendaftaranPsbPSB.setRuangPSB(ruangSelected);

		// Menyimpan atau memperbarui data alokasi
		Common.refreshSaveOrUpdate(session, ruangGelombangPendaftaranPsbPSB);

		// Validasi kapasitas ruangan. Jika setelah ditambah calon siswa ini ruangan
		// menjadi penuh,
		// maka status ruang diperbarui menjadi penuh (1).
		if (ruangSelected.getKapasitasRuangan() != null && ruangSelected.getKapasitasRuangan() < (isiRuang + 2)) {
			ruangSelected.setPenuh(1);
			Common.refreshUpdate(session, ruangSelected);
		}

		return ruangGelombangPendaftaranPsbPSB;
	}

	/**
	 * Membangkitkan nomor ujian PSB untuk seorang calon siswa lewat implementasi
	 * {@link NoUjianGeneratorPsb} yang dikonfigurasi ({@code class_untuk_generate_no_ujian_psb},
	 * default {@link DefaultNoUjianGeneratorPsb}), dimuat lewat refleksi
	 * ({@link Class#forName(String)} + {@code newInstance()}). Varian ini menampung pesan
	 * kegagalan ke {@code warnings} (dipakai bila pemanggil mengumpulkan banyak peringatan
	 * sekaligus, mis. proses batch) alih-alih menampilkan dialog langsung.
	 *
	 * @param calonSiswa calon siswa yang nomor ujiannya akan dibangkitkan
	 * @param warnings   list yang akan ditambahi pesan peringatan bila pembangkitan gagal
	 * @return nomor ujian yang dibangkitkan, atau string kosong bila gagal (pesan kegagalan
	 *         ditambahkan ke {@code warnings})
	 * @throws Exception dideklarasikan untuk kompatibilitas signature; kegagalan generator
	 *                    ditangkap secara internal dan tidak dilempar ulang
	 */
	@SuppressWarnings({})
	public static String generateNoUjian(CalonSiswa calonSiswa, List<String> warnings) throws Exception {

		try {
			NoUjianGeneratorPsb noUjianGenerator = (NoUjianGeneratorPsb) Class.forName(Common
					.getKonfigurasi("class_untuk_generate_no_ujian_psb", DefaultNoUjianGeneratorPsb.class.getName())
					.getNilai()).newInstance();
			return noUjianGenerator.generateNoUjian(calonSiswa);
		} catch (Exception e) {
			warnings.add("Nomor Ujian tidak bisa di generate. Harap segera menghubungi Administrator. \n\n"
					+ e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPSB.java:167");
		}

		return "";

	}

	/**
	 * Seperti {@link #generateNoUjian(CalonSiswa, List)}, namun untuk pemakaian INTERAKTIF
	 * (bukan batch): kegagalan ditampilkan langsung ke pengguna lewat
	 * {@link PesanFormalHelper#tampilkanGagalException} alih-alih ditampung ke list peringatan.
	 *
	 * @param calonSiswa calon siswa yang nomor ujiannya akan dibangkitkan
	 * @return nomor ujian yang dibangkitkan, atau string kosong bila gagal (pesan kegagalan
	 *         sudah ditampilkan ke pengguna)
	 * @throws Exception dideklarasikan untuk kompatibilitas signature; kegagalan generator
	 *                    ditangkap secara internal dan tidak dilempar ulang
	 */
	@SuppressWarnings({})
	public static String generateNoUjian(CalonSiswa calonSiswa) throws Exception {

		try {
			NoUjianGeneratorPsb noUjianGenerator = (NoUjianGeneratorPsb) Class.forName(Common
					.getKonfigurasi("class_untuk_generate_no_ujian_psb", DefaultNoUjianGeneratorPsb.class.getName())
					.getNilai()).newInstance();
			return noUjianGenerator.generateNoUjian(calonSiswa);
		} catch (Exception e) {
			PesanFormalHelper.tampilkanGagalException("pembuatan Nomor Ujian PSB otomatis", e,
					new String[] {
							"Periksa kembali konfigurasi kelas generator Nomor Ujian PSB (class_untuk_generate_no_ujian_psb).",
							"Coba ulangi proses ini beberapa saat lagi." });
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPSB.java:185");
		}

		return "";

	}

	/**
	 * Membangkitkan nomor registrasi PSB untuk seorang calon siswa lewat implementasi
	 * {@link NoRegGeneratorPsb} yang dikonfigurasi ({@code class_untuk_generate_no_reg_psb},
	 * default {@link DefaultNoRegGeneratorPsb}), dimuat lewat refleksi. Kegagalan ditampilkan
	 * ke pengguna lewat {@link PesanFormalHelper#tampilkanGagalException} DAN dicatat lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param calonSiswa calon siswa yang nomor registrasinya akan dibangkitkan
	 * @return nomor registrasi yang dibangkitkan, atau string kosong bila gagal
	 * @throws Exception dideklarasikan untuk kompatibilitas signature; kegagalan generator
	 *                    ditangkap secara internal dan tidak dilempar ulang
	 */
	public static String generateNoRegistrasi(CalonSiswa calonSiswa) throws Exception {
		try {

			NoRegGeneratorPsb noRegGenerator = (NoRegGeneratorPsb) Class.forName(
					Common.getKonfigurasi("class_untuk_generate_no_reg_psb", DefaultNoRegGeneratorPsb.class.getName())
							.getNilai())
					.newInstance();
			return noRegGenerator.generateNoReg(calonSiswa);
		} catch (Exception e) {
			PesanFormalHelper.tampilkanGagalException("pembuatan Nomor Registrasi PSB otomatis", e,
					new String[] {
							"Periksa kembali konfigurasi kelas generator Nomor Registrasi PSB (class_untuk_generate_no_reg_psb).",
							"Coba ulangi proses ini beberapa saat lagi." });
			Common.tampilErrorJikaAdmin(e);
		}
		return "";
	}

	/**
	 * Membangkitkan satu string nomor (mis. NIS, nomor surat/agenda) berformat kustom dari
	 * {@link FormatNis}. Indeks urut diambil dari nomor indeks tetap {@code
	 * formatNis.getNomorIndex()} bila {@code formatNis} dikonfigurasi memakai indeks manual
	 * ({@code gunakanIndexUrut=true}, dan nomor indeks tersebut LANGSUNG dinaikkan/disimpan
	 * lewat {@link FormatNis#tambahIndexNomorSurat(FormatNis)} setelah dipakai — efek samping
	 * ini terjadi SETIAP kali method dipanggil), atau dihitung otomatis lewat
	 * {@link #getindex(FormatNis, CalonSiswa)} bila tidak.
	 *
	 * @param formatNis  konfigurasi format nomor yang dipakai
	 * @param calonSiswa calon siswa yang datanya (tahun masuk) dipakai untuk memformat nomor
	 * @return nomor hasil format sesuai {@link FormatNis#format(Long, int)}
	 */
	public static String generateCode(FormatNis formatNis, CalonSiswa calonSiswa) {

		Long index = formatNis.getGunakanIndexUrut() ? formatNis.getNomorIndex() : getindex(formatNis, calonSiswa);
		if (formatNis.getGunakanIndexUrut()) {
			FormatNis.tambahIndexNomorSurat(formatNis);
		}
		String noAgenda = formatNis.format(index, calonSiswa.getTahunMasuk());
		return noAgenda;
	}

	public static Long getindex(FormatNis formatNis, CalonSiswa calonSiswa) {
		if (formatNis == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = calonSiswa.getTahunMasuk();
		Date sekarang = calonSiswa.getTanggalPendaftaran();
		Number indexO = (Number) session.createCriteria(CalonSiswa.class)
				.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))

				.add(formatNis.getResetUrutanTiapTahun() ? Restrictions.eq("tahunMasuk", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(formatNis.getResetTiap() != null && (Common.dateFormat8.get().format(formatNis.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang)) || formatNis.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggal", formatNis.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	public static String onGenerateNis(CalonSiswa calonSiswa, NisGenerator nimGenerator) throws Exception {
		return onGenerateNis(calonSiswa, nimGenerator, true);
	}

	public static String onGenerateNis(final CalonSiswa calonSiswa, NisGenerator nimGenerator, boolean cetak)
			throws Exception {

		if (!calonSiswa.getTelahDiterima()) {
			MyMessageboxConfig.show("Calon siswa ini belum dinyatakan lulus / belum diterima!", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return "";

		} else {
			String nis = "";
			if (calonSiswa.getSiswa() != null) {
				if (cetak) {
					MyMessageboxConfig.show(
							"Siswa telah mendapatkan NIS, yaitu : " + calonSiswa.getSiswa().getNomorInduk(),
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Map<String, Serializable> parameters = ais.common.HashMapGenerator
											.getRandStringSerializable();
									parameters.put("nim", calonSiswa.getSiswa().getNomorInduk());
									parameters.put("biodata_id", calonSiswa.getId());
									calonSiswa.putPhoto(parameters);

									Report.generatePDFReport(Report.PDF, parameters, "sekolah/Biodata_Nis",
											ais.ui.util.WaktuUtil.getDate());
								}
							});
				}
				return nis;
			} else {

				Session session = HibernateUtil.currentNativeSession();
				FormatNis formatNis = (FormatNis) ConstantValues.simpleObject(session.createCriteria(FormatNis.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("sekolah", calonSiswa.getSekolah())).setMaxResults(1)
						.addOrder(Order.desc("id")), FormatNis.class);
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				HibernateUtil.closeSession();

				if (formatNis != null && formatNis.getId() != null) {
					nis = generateCode(formatNis, calonSiswa);
				} else {
					nis = nimGenerator.generateNis(calonSiswa);
				}
			}
			calonSiswa.setNomorInduk(nis);
			Session session = HibernateUtil.currentNativeSession();

			String sql = "update sekolah.siswa set calonsiswa=null where calonsiswa=" + calonSiswa.getId();
			session.createSQLQuery(sql).executeUpdate();

			final Siswa siswa = CommonPSB.saveSiswa(session, calonSiswa, nis, true);
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();

			if (cetak) {
				try {
					MyMessageboxConfig.show("Nomor Induk Siswa berhasil di-generate: \"" + nis + "\"", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									CommonPSB.copyLampiran(calonSiswa, siswa);
									CommonPSB.masukkanKelas(calonSiswa, siswa);
									CommonPSB.masukkanKelasLes(calonSiswa, siswa);

									Map<String, Serializable> parameters = ais.common.HashMapGenerator
											.getRandStringSerializable();
									GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
									LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PSB);
									if (kop != null) {
										try {
											parameters.put("kop_file", kop.ambilFile().getAbsolutePath());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:332");
											// TODO: handle exception
										}
									}

									parameters.put("nim", siswa.getNomorInduk());
									parameters.put("biodata_id", calonSiswa.getId());
									calonSiswa.putPhoto(parameters);

									Report.generatePDFReport(Report.PDF, parameters, "sekolah/Biodata_Nis",
											ais.ui.util.WaktuUtil.getDate());
								}
							});
				} catch (Exception e) {
					CommonPSB.copyLampiran(calonSiswa, siswa);
					CommonPSB.masukkanKelas(calonSiswa, siswa);
					CommonPSB.masukkanKelasLes(calonSiswa, siswa);
				}
			} else {
				CommonPSB.copyLampiran(calonSiswa, siswa);
				CommonPSB.masukkanKelas(calonSiswa, siswa);
				CommonPSB.masukkanKelasLes(calonSiswa, siswa);
			}

			return nis;

		}
	}

	public static void uploadKelulusan(final File file, final EventListener eventListener) throws Exception {

		final Label peringatan = new Label("");

		final Sekolah sekolah = SekolahUtil.getSekolah();

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		final Label downloadPath = new Label("");
		// FIX compile "cannot find symbol: report": harus dideklarasikan sebelum
		// timer.addEventListener(...) karena dipakai di dalam closure onTimer di bawah.
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Kelulusan PSB");
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());

				if (label.getValue().equalsIgnoreCase("Penuh")) {

					MyMessageboxConfig.show("Jumlah kuota siswa yang diterima telah penuh", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);

					Clients.clearBusy();
					timer.detach();
				}

				else if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					if (!downloadPath.getValue().isEmpty()) {
						try { org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) { ais.common.ErrorAuditUtil.record(eD, "auto-audit(empty-catch) CommonPSB download-laporan"); }
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
						Long code = -1L;
						// FIX compile "cannot find symbol: calonSiswa": dideklarasikan di luar try
						// supaya tetap terlihat di blok catch-nya (variabel lokal di dalam try TIDAK
						// terlihat di catch pasangannya).
						CalonSiswa calonSiswa = null;
						try {

							try {
								String content = Common.getCellContent(Common.getCell(sheet, 0, i));
								System.out.println("content = " + content);
								code = Long.parseLong(content);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:426");

							}
							if (code.equals(-1L)) {
								try {

									String content = Common.getCellContent(Common.getCell(sheet, 1, i));
									System.out.println("noRegistrasi = " + content);
									code = (Long) session.createCriteria(CalonSiswa.class)
											.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
											.add(Restrictions.eq("noRegistrasi", content))
											.add(sekolah != null && sekolah.getId() != null
													? Restrictions.sqlRestriction("true")
													: Restrictions.eq("sekolah", sekolah))
											.setProjection(Projections.property("id")).setMaxResults(1).uniqueResult();
								} catch (Exception e) {
									System.err.println("[GenNIS] ERROR cari via noRegistrasi baris=" + i + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
									ais.common.ErrorAuditUtil.record(e, "uploadGenNIS-cariNoReg baris=" + i);
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
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:456");

							}

							String noRegistrasi = "";
							try {

								noRegistrasi = Common.getCellContent(Common.getCell(sheet, 1, i));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:464");

							}

							String diterima = "";
							try {
								diterima = Common.getCellContent(Common.getCell(sheet, 6, i));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:471");

							}

							System.out.println("[GenNIS] baris=" + i + " id=" + code + " nim=" + nim + " noReg=" + noRegistrasi + " diterima=" + diterima);

							calonSiswa = (CalonSiswa) session.createCriteria(CalonSiswa.class)
									.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).add(Restrictions.idEq(code))
									.uniqueResult();

							if (calonSiswa == null && !noRegistrasi.trim().isEmpty()) {
								System.out.println("[GenNIS] calonSiswa tidak ditemukan via ID=" + code + ", coba via noRegistrasi=" + noRegistrasi);
								calonSiswa = (CalonSiswa) session.createCriteria(CalonSiswa.class)
										.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
										.add(sekolah != null && sekolah.getId() != null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("sekolah", sekolah))
										.add(Restrictions.eq("noRegistrasi", noRegistrasi)).uniqueResult();
							}

							if (calonSiswa == null) {
								System.out.println("[GenNIS] SKIP: calonSiswa null untuk id=" + code + " noReg=" + noRegistrasi + " (tidak ada gelombangPendaftaranPsb atau tidak ditemukan)");
								continue;
							}

							int count = ((Number) session.createCriteria(CalonSiswa.class)
									.add(Restrictions.eq("gelombangPendaftaranPsb",
											calonSiswa.getGelombangPendaftaranPsb()))
									.add(Restrictions.eq("telahDiterima", true)).setProjection(Projections.rowCount())
									.uniqueResult()).intValue();

							System.out.println("[GenNIS] calonSiswa=" + calonSiswa.getId() + "-" + calonSiswa.getNama() + " | kuota=" + count + "/" + calonSiswa.getGelombangPendaftaranPsb().getKuotaDiterima() + " | gelombang=" + calonSiswa.getGelombangPendaftaranPsb().getId());

							if (count >= calonSiswa.getGelombangPendaftaranPsb().getKuotaDiterima()) {

								System.out.println("[GenNIS] BERHENTI: kuota penuh " + count + ">=" + calonSiswa.getGelombangPendaftaranPsb().getKuotaDiterima());
								label.setValue("Penuh");

								return;
							} else {

								calonSiswa
										.setTelahDiterima(diterima != null && diterima.trim().equalsIgnoreCase("true"));
								try {
									session.getTransaction().begin();
									Common.refreshUpdate(session, calonSiswa);
									session.getTransaction().commit();
								} catch (Exception exSimpan) {
									// Baris ini gagal disimpan (mis. duplikat calon_siswa.siswa_id — siswa
									// sudah tertaut CalonSiswa lain). JANGAN gagalkan SELURUH upload:
									// rollback, bersihkan session, catat, lalu lanjut ke baris berikutnya.
									System.err.println("[GenNIS] ERROR simpan telahDiterima baris=" + i + " id=" + code + ": " + exSimpan.getClass().getSimpleName() + ": " + exSimpan.getMessage());
									ais.common.ErrorAuditUtil.record(exSimpan, "uploadGenNIS-simpanTelahDiterima baris=" + i + " id=" + code);
									try {
										if (session.getTransaction().isActive())
											session.getTransaction().rollback();
									} catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:518");
									}
									try { session.clear(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:520");}
									Common.tampilErrorJikaAdmin(exSimpan);
									report.gagal(i, calonSiswa.getNoRegistrasi() + " – " + calonSiswa.getNama(), exSimpan, "Data siswa sudah dipakai");
									label.setValue("Lewati \"" + calonSiswa.getNama()
											+ "\" (data siswa sudah dipakai) ("
											+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
									continue;
								}

								label.setValue("Upload data \"" + calonSiswa.getNama() + "\" ("
										+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

								report.sukses(i, calonSiswa.getNoRegistrasi() + " – " + calonSiswa.getNama(), "");
								if (nim.trim().isEmpty()) {
									// Cek apakah siswa sudah punya NIS (nomorInduk)
									boolean siswaAdaNis = calonSiswa.getSiswa() != null
											&& calonSiswa.getSiswa().getNomorInduk() != null
											&& !calonSiswa.getSiswa().getNomorInduk().trim().isEmpty();
									if (siswaAdaNis) {
										// Pertahankan NIS yang sudah ada, jangan generate ulang
										nim = calonSiswa.getSiswa().getNomorInduk();
									} else {
										// Siswa belum punya NIS, generate otomatis
										nim = nisGenerator.generateNis(calonSiswa);
									}
								}

								String sql = "update sekolah.siswa set calonsiswa=null where calonsiswa="
										+ calonSiswa.getId();
								session.createSQLQuery(sql).executeUpdate();

								System.out.println("calonSiswa = " + calonSiswa + ", nim = " + nim);

								Siswa siswa = null;
								session.getTransaction().begin();
								if (!nim.trim().isEmpty()) {
									siswa = CommonPSB.saveSiswa(session, calonSiswa, nim.trim(), false);
								} else {
									Common.refreshUpdate(session, calonSiswa);
								}
								session.getTransaction().commit();

								if (siswa != null) {
									CommonPSB.copyLampiran(calonSiswa, siswa);
									CommonPSB.masukkanKelas(calonSiswa, siswa);
									CommonPSB.masukkanKelasLes(calonSiswa, siswa);
								}
								// masukkanKelas/Les menutup session ThreadLocal — buka ulang agar
								// iterasi berikutnya tidak gagal dengan "session is closed"
								session = HibernateUtil.currentNativeSession();
							}
						} catch (Exception e) {
							// Rollback transaksi yang aborted agar iterasi berikutnya tidak
							// terdampak "current transaction is aborted" dari PostgreSQL
							System.err.println("[GenNIS] ERROR baris=" + i + " id=" + code + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
							e.printStackTrace();
							ais.common.ErrorAuditUtil.record(e, "uploadGenNIS baris=" + i + " id=" + code);
							try {
								if (session != null && session.isOpen() && session.getTransaction().isActive()) {
									session.getTransaction().rollback();
								}
							} catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:569");}
							try { if (session != null && session.isOpen()) session.clear(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:570");}
							// Session mungkin ditutup oleh masukkanKelas/Les — buka ulang
							try {
								if (session == null || !session.isOpen()) {
									session = HibernateUtil.currentNativeSession();
									System.err.println("[GenNIS] session dibuka ulang setelah error baris=" + i);
								}
							} catch (Exception ex3) { ais.common.ErrorAuditUtil.record(ex3, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:GenNIS-reopen"); }
							report.gagal(i, (calonSiswa != null ? calonSiswa.getNoRegistrasi() + " – " + calonSiswa.getNama() : String.valueOf(code)), e, "Periksa data baris " + i);
						}

					}

					HibernateUtil.closeSession();

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/CommonPSB.java:579");
				}

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) CommonPSB laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	public static void copyLampiran(CalonSiswa calonSiswa, Siswa siswa) {

		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

			FotoCalonSiswa fotocalonSiswa = (FotoCalonSiswa) streamingSession.createCriteria(FotoCalonSiswa.class)
					.add(Restrictions.eq("calonSiswa", calonSiswa.getId())).setMaxResults(1).uniqueResult();

			if (fotocalonSiswa != null && fotocalonSiswa.getFoto() != null) {

				FotoSiswa fotoSiswa = (FotoSiswa) streamingSession.createCriteria(FotoSiswa.class)
						.addOrder(Order.desc("id")).add(Restrictions.eq("siswa", siswa.getId())).setMaxResults(1)
						.uniqueResult();

				if (fotoSiswa != null) {
					streamingSession.getTransaction().begin();
					streamingSession.delete(fotoSiswa);
					streamingSession.getTransaction().commit();
				}

				fotoSiswa = new FotoSiswa();
				fotoSiswa.setNama(fotocalonSiswa.getNama());
				fotoSiswa.setKeterangan(fotocalonSiswa.getKeterangan());
				fotoSiswa.setSiswa(siswa.getId());
				fotoSiswa.setFoto(fotocalonSiswa.getFoto());

				streamingSession.getTransaction().begin();
				streamingSession.save(fotoSiswa);
				streamingSession.getTransaction().commit();

			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:622");
			// TODO: handle exception
		}

	}

	public static void masukkanKelasLes(CalonSiswa calonSiswa, Siswa siswa) {
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<KelasLesSiswa> kelas = calonSiswa.ambilKelasLesSiswa();
			for (KelasLesSiswa kelasLesSiswa : kelas) {
				int count = ((Number) session.createCriteria(KelasLesSiswaPunyaSiswa.class)
						.setProjection(Projections.rowCount()).add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa))
						.add(Restrictions.eq("siswa", siswa)).uniqueResult()).intValue();

				if (count == 0) {
					KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = new KelasLesSiswaPunyaSiswa();
					kelasLesSiswaPunyaSiswa.setCalonSiswa(calonSiswa);
					kelasLesSiswaPunyaSiswa.setSiswa(siswa);
					kelasLesSiswaPunyaSiswa.setKelasLesSiswa(kelasLesSiswa);
					session.getTransaction().begin();
					session.save(kelasLesSiswaPunyaSiswa);
					session.getTransaction().commit();
				}
			}

			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPSB.java:654");
		}
		HibernateUtil.closeSession();
	}

	public static void masukkanKelas(CalonSiswa calonSiswa, Siswa siswa) {
		if (calonSiswa.getKelasSiswa() != null) {
			Session session = HibernateUtil.currentNativeSession();
			try {
				int count = ((Number) session.createCriteria(KelasSiswaPunyaSiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("kelasSiswa", calonSiswa.getKelasSiswa()))
						.add(Restrictions.eq("siswa", siswa)).uniqueResult()).intValue();
				if (count == 0) {
					KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = new KelasSiswaPunyaSiswa();
					kelasSiswaPunyaSiswa.setCalonSiswa(calonSiswa);
					kelasSiswaPunyaSiswa.setSiswa(siswa);
					kelasSiswaPunyaSiswa.setKelasSiswa(calonSiswa.getKelasSiswa());
					session.getTransaction().begin();
					session.save(kelasSiswaPunyaSiswa);
					session.getTransaction().commit();
				}
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPSB.java:682");
			}
			HibernateUtil.closeSession();
		}
	}

	public static Siswa saveSiswa(Session session, CalonSiswa calonSiswa, String nim, boolean commitMaual) {

		if (calonSiswa.getTelahDiterima()) {

			// FIX: calonSiswa bisa detached (session lama sudah ditutup di onGenerateNis).
			// Muat ulang dalam session aktif agar lazy-load gelombangPendaftaranPsb→sekolah
			// tidak melempar LazyInitializationException yang ditelan diam-diam di loop
			// property copy di bawah — akibatnya siswa.sekolah tetap null dan tersaring
			// oleh SiswaAction.initCriteria() Restrictions.isNotNull("sekolah").
			CalonSiswa calonSiswaForCopy = calonSiswa;
			if (calonSiswa.getId() != null && !session.contains(calonSiswa)) {
				try {
					CalonSiswa freshCalon = (CalonSiswa) session.get(CalonSiswa.class, calonSiswa.getId());
					if (freshCalon != null) {
						calonSiswaForCopy = freshCalon;
					}
				} catch (Exception eFresh) { ais.common.ErrorAuditUtil.record(eFresh, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:704");
					// biarkan calonSiswaForCopy = calonSiswa jika gagal
				}
			}

			System.out.println("[saveSiswa] mulai: calonSiswa=" + calonSiswa.getId() + " nim_input=" + nim + " telahDiterima=" + calonSiswa.getTelahDiterima());

			Siswa siswa = calonSiswa.getSiswa();
			System.out.println("[saveSiswa] getSiswa() via asosiasi = " + (siswa != null ? siswa.getId() + " nomorInduk=" + siswa.getNomorInduk() : "null"));

			if (siswa == null && calonSiswaForCopy != null) {
				siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
						.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
						.add(Restrictions.ilike("nama", calonSiswaForCopy.getNama(), MatchMode.EXACT))
						.add(Restrictions.eq("tanggalLahir", calonSiswaForCopy.getTanggalLahir()))
						.add(Restrictions.eq("tahunMasuk", calonSiswaForCopy.getTahunMasuk()))
						.add(Restrictions.eq("sekolah", calonSiswaForCopy.getSekolah())).setMaxResults(1).uniqueResult();
				System.out.println("[saveSiswa] cari siswa via nama/tgl/sekolah: " + (siswa != null ? siswa.getId() + " nomorInduk=" + siswa.getNomorInduk() : "tidak ditemukan -> buat baru"));
			}
			if (siswa == null) {
				siswa = new Siswa();
				siswa.setPass(Common.desEncrypter.get().encrypt(nim));
				siswa.setIs_encripted(true);
				siswa.setKelasLesDipilih(calonSiswa.getKelasLesDipilih());
				System.out.println("[saveSiswa] siswa BARU dibuat, nim=" + nim);
			} else {
				// Gunakan NIS lama hanya jika tidak ada NIS baru yang diberikan
				if (nim == null || nim.trim().isEmpty()) {
					nim = siswa.getNomorInduk();
					System.out.println("[saveSiswa] nim dari input kosong, pakai NIS lama=" + nim);
				} else {
					System.out.println("[saveSiswa] siswa EXISTING id=" + siswa.getId() + " nomorInduk lama=" + siswa.getNomorInduk() + " -> akan diganti nim=" + nim);
				}
			}
			siswa.setCalonSiswa(calonSiswa.getId());

			ClassMetadata classMetadata = HibernateUtil.getClassMetadata(CalonSiswa.class);
			ClassMetadata classMetadataSiswa = HibernateUtil.getClassMetadata(Siswa.class);
			// Set properti Siswa (target) sekali saja utk verifikasi cepat "properti
			// ada/tidak" -- CalonSiswa punya field spesifik PSB (mis. alamatSekolahAsal)
			// yang MEMANG tidak ada di Siswa (setelah diterima, info sekolah asal tak
			// relevan lagi). Ini skenario WAJAR, bukan bug -- skip proaktif SEBELUM
			// setPropertyValue supaya tidak selalu tercatat ERROR via ErrorAuditUtil
			// utk sesuatu yang memang seharusnya dilewati (pola sama spt fix
			// CommonComboInsertHelper.adaProperti()).
			java.util.Set<String> propertiSiswa = new java.util.HashSet<String>(
					java.util.Arrays.asList(classMetadataSiswa.getPropertyNames()));
			for (String p : classMetadata.getPropertyNames()) {
				if (!propertiSiswa.contains(p)) {
					continue;
				}
				try {
					Object val = classMetadata.getPropertyValue(calonSiswaForCopy, p, EntityMode.POJO);
					classMetadataSiswa.setPropertyValue(siswa, p, val, EntityMode.POJO);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPSB.java:734");

				}
			}

			siswa.setNomorInduk(nim);
			System.out.println("[saveSiswa] setNomorInduk=" + nim + " pada siswa id=" + siswa.getId() + " (null=baru)");

			// Safety net: pastikan sekolah tidak null setelah loop copy
			if (siswa.getSekolah() == null && calonSiswaForCopy.getSekolah() != null) {
				siswa.setSekolah(calonSiswaForCopy.getSekolah());
			}
			if (siswa.getYayasan() == null && calonSiswaForCopy.getYayasan() != null) {
				siswa.setYayasan(calonSiswaForCopy.getYayasan());
			}

			if (commitMaual) {
				session.getTransaction().begin();
			}
			// Bersihkan siswa lama yang masih ber-calonsiswa=calonSiswa.getId()
			// DALAM transaksi ini agar atomik — mencegah "siswa_calonsiswa_key" duplikat.
			// session.clear() HARUS selalu jalan (di luar try) agar L1 cache tidak
			// tulis-balik nilai lama bahkan ketika SQL di atas gagal.
			try {
				session.createSQLQuery("UPDATE sekolah.siswa SET calonsiswa=NULL WHERE calonsiswa=:id")
						.setParameter("id", calonSiswa.getId())
						.executeUpdate();
			} catch (Exception eClear) {
				System.err.println("[saveSiswa] clear calonsiswa gagal calonSiswa=" + calonSiswa.getId() + ": " + eClear.getClass().getSimpleName() + ": " + eClear.getMessage());
				ais.common.ErrorAuditUtil.record(eClear, "saveSiswa-clearCalonsiswa calonSiswa=" + calonSiswa.getId());
			}
			session.clear();
			Common.refreshSaveOrUpdate(session, siswa);
			System.out.println("[saveSiswa] refreshSaveOrUpdate selesai: siswa.id=" + siswa.getId() + " siswa.nomorInduk=" + siswa.getNomorInduk());
			calonSiswa.setSiswa(siswa);
			// FIX: persist siswa_id ke calon_siswa agar FK tersimpan di DB
			// (calonSiswa.setSiswa di atas hanya in-memory, tidak otomatis di-flush
			// karena calonSiswa detached dari session ini)
			if (siswa.getId() != null && calonSiswa.getId() != null) {
				try {
					session.createSQLQuery(
							"UPDATE sekolah.calon_siswa SET siswa_id=:siswaId WHERE id=:calonId")
							.setParameter("siswaId", siswa.getId())
							.setParameter("calonId", calonSiswa.getId())
							.executeUpdate();
				} catch (Exception eFk) {
					System.err.println("[saveSiswa] Gagal update siswa_id di calon_siswa siswaId=" + siswa.getId() + " calonId=" + calonSiswa.getId() + ": " + eFk.getClass().getSimpleName() + ": " + eFk.getMessage());
					ais.common.ErrorAuditUtil.record(eFk, "saveSiswa-updateCalonSiswaFk siswaId=" + siswa.getId() + " calonId=" + calonSiswa.getId());
				}
			}
			if (commitMaual) {
				session.getTransaction().commit();
			}

			return siswa;
		} else {
			return null;
		}
	}
}
