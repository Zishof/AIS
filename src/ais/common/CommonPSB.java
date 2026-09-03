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
 * #generateCode(FormatNis, CalonSiswa)}/{@link #ambilNomorUrutOtomatis(FormatNis, CalonSiswa)}
 * (nomor agenda/surat berformat kustom lewat {@link FormatNis}, dengan nomor urut otomatis
 * dipersistensikan per sekolah lewat {@link ais.database.model.sekolah.NisCounter} atau indeks
 * manual dikunci baris lewat {@link FormatNis#ambilLaluNaikkanNomorIndex(FormatNis)}). Pola
 * generator yang dapat diganti lewat konfigurasi kelas (reflection
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

	/** Jumlah maksimal percobaan ulang saat NIS hasil rakitan bentrok dengan yang sudah dipakai. */
	private static final int MAKS_PERCOBAAN_NIS_BENTROK = 1000;

	/**
	 * Membangkitkan satu string nomor (mis. NIS, nomor surat/agenda) berformat kustom dari
	 * {@link FormatNis}, dengan pemeriksaan bentrok + percobaan ulang (pola yang sama dengan
	 * generator cadangan {@code DefaultNisGenerator}).
	 *
	 * <p><b>Sumber nomor urut:</b> {@link FormatNis#ambilLaluNaikkanNomorIndex(FormatNis)} (dibaca
	 * DAN dinaikkan atomik dalam satu transaksi terkunci) bila {@code formatNis} dikonfigurasi
	 * memakai indeks manual ({@code gunakanIndexUrut=true}), atau
	 * {@link #ambilNomorUrutOtomatis(FormatNis, CalonSiswa)} (pencacah {@link
	 * ais.database.model.sekolah.NisCounter} dipersistensikan per sekolah, juga dikunci atomik)
	 * bila tidak. Kedua sumber TIDAK bisa lagi menghasilkan nomor kembar akibat race condition
	 * lintas request/node seperti sebelumnya.</p>
	 *
	 * <p><b>Lapisan pertahanan terakhir:</b> setelah {@link FormatNis#format(Long, Integer)}
	 * merakit string NIS, method ini memeriksa apakah NIS tersebut SUDAH dipakai siswa lain di
	 * sekolah yang sama (mis. sisa data lama sebelum unique constraint dipasang, lihat
	 * {@code ais.common.InitIndex#initNisCounterDanKeunikanSiswa()}). Bila bentrok, nomor urut
	 * berikutnya dicoba (mengonsumsi satu nilai pencacah tanpa dipakai — aman, hanya berarti ada
	 * lubang di deret nomor), dibatasi {@link #MAKS_PERCOBAAN_NIS_BENTROK} kali agar tidak
	 * berputar tanpa batas seperti rekursi tak terbatas pada {@code DefaultNisGenerator}.</p>
	 *
	 * @param formatNis  konfigurasi format nomor yang dipakai
	 * @param calonSiswa calon siswa yang datanya (tahun masuk, sekolah) dipakai untuk memformat
	 *                   dan memeriksa bentrok nomor
	 * @return nomor hasil format sesuai {@link FormatNis#format(Long, Integer)}, dijamin belum
	 *         dipakai siswa lain di sekolah yang sama pada saat pemeriksaan
	 * @throws Exception diteruskan dari kegagalan penguncian/transaksi basis data, atau
	 *                    {@code IllegalStateException} bila {@link #MAKS_PERCOBAAN_NIS_BENTROK}
	 *                    percobaan berturut-turut semuanya bentrok
	 */
	public static String generateCode(FormatNis formatNis, CalonSiswa calonSiswa) throws Exception {
		for (int percobaan = 0; percobaan < MAKS_PERCOBAAN_NIS_BENTROK; percobaan++) {
			Long index = formatNis.getGunakanIndexUrut() ? FormatNis.ambilLaluNaikkanNomorIndex(formatNis)
					: ambilNomorUrutOtomatis(formatNis, calonSiswa);
			String noAgenda = formatNis.format(index, calonSiswa.getTahunMasuk());
			if (!nomorIndukSudahDipakai(calonSiswa.getSekolah(), noAgenda)) {
				return noAgenda;
			}
		}
		throw new IllegalStateException("Gagal membangkitkan NIS unik untuk calon siswa id=" + calonSiswa.getId()
				+ " setelah " + MAKS_PERCOBAAN_NIS_BENTROK + " percobaan; setiap nomor urut yang dicoba sudah "
				+ "dipakai siswa lain di sekolah yang sama. Periksa data NIS duplikat sekolah ini.");
	}

	/**
	 * {@code true} bila {@code nomorInduk} sudah dipakai baris {@link Siswa} lain pada
	 * {@code sekolah} yang sama &mdash; lapisan pertahanan terakhir yang dipanggil
	 * {@link #generateCode(FormatNis, CalonSiswa)} setelah NIS dirakit, sebelum dikembalikan ke
	 * pemanggil.
	 *
	 * @param sekolah    sekolah yang jadi lingkup pemeriksaan; {@code null}/tanpa id membuat method
	 *                   langsung mengembalikan {@code false} (tidak ada lingkup untuk diperiksa)
	 * @param nomorInduk NIS yang akan diperiksa; {@code null}/kosong membuat method mengembalikan
	 *                   {@code false}
	 * @return {@code true} bila sudah ada siswa lain di sekolah yang sama dengan NIS ini
	 */
	private static boolean nomorIndukSudahDipakai(Sekolah sekolah, String nomorInduk) {
		if (sekolah == null || sekolah.getId() == null || nomorInduk == null || nomorInduk.trim().isEmpty()) {
			return false;
		}
		Session session = HibernateUtil.currentSession();
		Number jumlah = (Number) session.createCriteria(Siswa.class).add(Restrictions.eq("sekolah", sekolah))
				.add(Restrictions.eq("nomorInduk", nomorInduk)).setProjection(Projections.rowCount())
				.uniqueResult();
		return jumlah != null && jumlah.longValue() > 0;
	}

	/**
	 * Mengembalikan nomor urut berikutnya untuk {@link FormatNis} bertipe indeks OTOMATIS (bukan
	 * manual), diambil dari pencacah dipersistensikan {@link ais.database.model.sekolah.NisCounter}
	 * milik {@code calonSiswa.getSekolah()}, dinaikkan ATOMIK (dikunci baris database, aman lintas
	 * thread maupun lintas node aplikasi lewat
	 * {@code ais.database.hibernate.KunciEntityHelper#jalankanDenganKunci}).
	 *
	 * <p><b>Perbaikan dibanding {@code getindex(FormatNis, CalonSiswa)} yang lama:</b> versi lama
	 * MENGHITUNG ULANG jumlah baris {@link CalonSiswa} yang sudah punya
	 * {@code gelombangPendaftaranPsb} setiap kali dipanggil (bukan jumlah NIS yang SUDAH terbit),
	 * dan query-nya tidak menyaring {@code sekolah} sama sekali &mdash; dua cacat yang bersama-sama
	 * membuat mode ini menghasilkan NIS kembar deterministik dan bocor volume pendaftaran antar
	 * sekolah. Sekarang nomor urut adalah nilai pencacah yang dipersistensikan per SEKOLAH ini
	 * saja, dinaikkan satu setiap dipanggil &mdash; tidak pernah dihitung ulang dari data lain.</p>
	 *
	 * <p><b>Lingkup pencacah</b> (baca javadoc {@link ais.database.model.sekolah.NisCounter} untuk
	 * detail): kunci baris pencacah adalah (sekolah, tahun masuk ATAU sentinel bila
	 * {@code formatNis.getResetUrutanTiapTahun()} tidak aktif, epoch reset kustom bila
	 * {@code formatNis.getResetTiap()} sudah terlampaui).</p>
	 *
	 * @param formatNis  konfigurasi format yang menentukan aturan reset nomor urut
	 * @param calonSiswa calon siswa yang sekolah, tahun masuk, &amp; tanggal pendaftarannya dipakai
	 *                   sebagai acuan lingkup pencacah; WAJIB memiliki sekolah (lihat
	 *                   {@code throws})
	 * @return nomor urut berikutnya (mulai dari 1 untuk pencacah baru)
	 * @throws Exception diteruskan dari kegagalan penguncian/transaksi basis data
	 * @throws IllegalStateException bila {@code calonSiswa} tidak memiliki sekolah &mdash; pencacah
	 *                    otomatis tidak dapat dibangkitkan tanpa lingkup sekolah (dahulu, versi
	 *                    lama diam-diam menghitung LINTAS SEKOLAH dalam kasus ini)
	 */
	public static Long ambilNomorUrutOtomatis(final FormatNis formatNis, final CalonSiswa calonSiswa)
			throws Exception {
		Sekolah sekolahCalon = calonSiswa.getSekolah();
		if (sekolahCalon == null || sekolahCalon.getId() == null) {
			throw new IllegalStateException("Calon siswa id=" + calonSiswa.getId()
					+ " tidak memiliki sekolah; NIS otomatis tidak dapat dibangkitkan tanpa lingkup sekolah.");
		}
		final Long sekolahId = sekolahCalon.getId();
		final int tahunKunci = formatNis.getResetUrutanTiapTahun() ? calonSiswa.getTahunMasuk()
				: ais.database.model.sekolah.NisCounter.TAHUN_TANPA_RESET;
		Date resetTiap = formatNis.getResetTiap();
		Date sekarang = calonSiswa.getTanggalPendaftaran();
		final String epoch = (resetTiap != null && sekarang != null && !resetTiap.after(sekarang))
				? Common.dateFormat8.get().format(resetTiap)
				: ais.database.model.sekolah.NisCounter.EPOCH_AWAL;

		Long counterId = cariAtauBuatNisCounterId(sekolahId, tahunKunci, epoch);

		final Long[] hasil = new Long[1];
		boolean adaBaris = ais.database.hibernate.KunciEntityHelper.jalankanDenganKunci(
				ais.database.model.sekolah.NisCounter.class, counterId,
				new ais.database.hibernate.KunciEntityHelper.PekerjaanTransaksi() {
					@Override
					public void kerjakan(Session session, Object entityTerkunci) throws Exception {
						ais.database.model.sekolah.NisCounter counter = (ais.database.model.sekolah.NisCounter) entityTerkunci;
						Long nilaiBaru = counter.getNilai() + 1L;
						counter.setNilai(nilaiBaru);
						session.update(counter);
						hasil[0] = nilaiBaru;
					}
				});
		if (!adaBaris) {
			// Baris pencacah dihapus tepat di antara pencarian dan penguncian (sangat jarang):
			// buat ulang lalu coba sekali lagi.
			return ambilNomorUrutOtomatis(formatNis, calonSiswa);
		}
		return hasil[0];
	}

	/**
	 * Mencari id baris {@link ais.database.model.sekolah.NisCounter} untuk kunci
	 * (sekolah, tahun, epoch) yang diberikan, membuatnya bila belum ada.
	 *
	 * <p>Pencarian &amp; penyisipan di sini TIDAK dikunci &mdash; penguncian sungguhan terjadi
	 * belakangan lewat {@code KunciEntityHelper.jalankanDenganKunci} pada id yang dikembalikan.
	 * Race saat penyisipan (dua permintaan sama-sama tidak menemukan baris, sama-sama mencoba
	 * INSERT) ditangani lewat unique constraint {@code uq_nis_counter_sekolah_tahun_epoch}: yang
	 * kalah menerima pelanggaran constraint (ditangkap &amp; diabaikan di sini), lalu SELECT ulang
	 * menemukan baris milik pemenang.</p>
	 */
	private static Long cariAtauBuatNisCounterId(Long sekolahId, int tahun, String epoch) {
		Session session = HibernateUtil.currentNativeSession();
		Number id = (Number) session
				.createSQLQuery("SELECT id FROM sekolah.nis_counter WHERE sekolah_id=:sekolahId AND tahun=:tahun"
						+ " AND epoch=:epoch")
				.setParameter("sekolahId", sekolahId).setParameter("tahun", tahun).setParameter("epoch", epoch)
				.uniqueResult();
		if (id != null) {
			return id.longValue();
		}
		try {
			session.createSQLQuery("INSERT INTO sekolah.nis_counter (sekolah_id, tahun, epoch, nilai)"
					+ " VALUES (:sekolahId, :tahun, :epoch, 0)").setParameter("sekolahId", sekolahId)
					.setParameter("tahun", tahun).setParameter("epoch", epoch).executeUpdate();
		} catch (RuntimeException race) {
			// Baris sudah dibuat proses lain di antara SELECT dan INSERT di atas (unique
			// constraint) -- abaikan, SELECT ulang di bawah akan menemukannya.
		}
		Number idBaru = (Number) session
				.createSQLQuery("SELECT id FROM sekolah.nis_counter WHERE sekolah_id=:sekolahId AND tahun=:tahun"
						+ " AND epoch=:epoch")
				.setParameter("sekolahId", sekolahId).setParameter("tahun", tahun).setParameter("epoch", epoch)
				.uniqueResult();
		if (idBaru == null) {
			throw new IllegalStateException("Gagal mencari/membuat baris sekolah.nis_counter untuk sekolah="
					+ sekolahId + " tahun=" + tahun + " epoch=" + epoch);
		}
		return idBaru.longValue();
	}

	/** Seperti {@link #onGenerateNis(CalonSiswa, NisGenerator, boolean)} dengan {@code cetak=true} (mencetak bukti PDF setelah NIS dibangkitkan). */
	public static String onGenerateNis(CalonSiswa calonSiswa, NisGenerator nimGenerator) throws Exception {
		return onGenerateNis(calonSiswa, nimGenerator, true);
	}

	/**
	 * Titik masuk konversi INDIVIDUAL satu calon siswa (yang sudah dinyatakan
	 * {@code telahDiterima}) menjadi siswa resmi dengan Nomor Induk Siswa (NIS).
	 *
	 * <p>
	 * Alur kerja: (1) menolak proses dengan pesan peringatan bila calon siswa belum dinyatakan
	 * diterima; (2) bila calon siswa SUDAH memiliki {@link Siswa} terkait (sudah pernah
	 * dikonversi), hanya menampilkan info NIS yang sudah ada (bila {@code cetak=true}) tanpa
	 * membuat data baru; (3) bila belum, menentukan NIS: memakai {@link FormatNis} aktif milik
	 * sekolah calon siswa (lewat {@link #generateCode(FormatNis, CalonSiswa)}) bila ada, atau
	 * {@code nimGenerator} sebagai fallback; (4) membersihkan tautan {@code calonsiswa} pada
	 * baris {@code sekolah.siswa} lama (SQL native langsung) untuk mencegah duplikasi tautan;
	 * (5) memanggil {@link #saveSiswa(Session, CalonSiswa, String, boolean)} untuk membuat/
	 * memperbarui record {@link Siswa}; (6) bila {@code cetak=true}, menampilkan dialog info
	 * NIS yang lalu (saat pengguna menutup dialog, via {@link EventListener}) mencetak bukti
	 * PDF ({@code sekolah/Biodata_Nis}) dan menyalin lampiran/keanggotaan kelas — bila
	 * pencetakan PDF gagal, penyalinan lampiran/kelas TETAP dijalankan lewat blok
	 * {@code catch}; bila {@code cetak=false}, penyalinan lampiran/kelas dijalankan langsung
	 * tanpa dialog/cetak.
	 * </p>
	 *
	 * @param calonSiswa   calon siswa yang akan dikonversi menjadi siswa
	 * @param nimGenerator generator NIS fallback, dipakai bila sekolah tidak memiliki
	 *                     {@link FormatNis} aktif
	 * @param cetak        {@code true} untuk menampilkan dialog info dan mencetak bukti PDF
	 *                     setelah proses; {@code false} untuk proses senyap (dipakai alur batch
	 *                     seperti {@link #uploadKelulusan})
	 * @return NIS yang dibangkitkan/dipakai, atau string kosong bila calon siswa belum diterima
	 *         atau sudah memiliki siswa terkait
	 * @throws Exception diteruskan dari kegagalan operasi database atau pembangkitan laporan PDF
	 */
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

	/**
	 * Titik masuk konversi MASSAL banyak calon siswa sekaligus menjadi siswa resmi, berdasarkan
	 * berkas Excel ({@code .xlsx}) berisi daftar id/nomor registrasi calon siswa, NIS (opsional),
	 * dan status diterima per baris. Seluruh proses dijalankan di THREAD TERPISAH (bukan thread
	 * request ZK) agar antarmuka tetap responsif, dengan progres dilaporkan lewat komponen
	 * {@link Label} yang di-polling setiap 200ms oleh {@link Timer} ZK di sisi UI.
	 *
	 * <p>
	 * Format kolom Excel yang dibaca per baris (indeks kolom, 0-based): kolom 0 = id
	 * {@link CalonSiswa} (angka; bila gagal diparsing, dicari lewat kolom 1 sebagai nomor
	 * registrasi), kolom 1 = nomor registrasi (fallback pencarian bila kolom 0 gagal/kosong),
	 * kolom 3 = NIS (bila kosong, dibangkitkan otomatis lewat {@code nisGenerator} yang
	 * dikonfigurasi {@code class_untuk_generate_nis}, kecuali siswa sudah punya NIS
	 * sebelumnya — NIS lama dipertahankan), kolom 6 = status diterima ({@code "true"}/lainnya).
	 * </p>
	 *
	 * <p>
	 * Untuk setiap baris: (1) mencari {@link CalonSiswa} berdasarkan id atau nomor registrasi;
	 * (2) mengecek kuota gelombang pendaftaran ({@code kuotaDiterima}) — bila jumlah yang sudah
	 * diterima pada gelombang tersebut MELEBIHI/MENYAMAI kuota, SELURUH PROSES DIHENTIKAN
	 * (bukan hanya baris ini) dan label diset {@code "Penuh"} sehingga timer UI menampilkan
	 * peringatan kuota penuh; (3) memperbarui status {@code telahDiterima} calon siswa; (4)
	 * membersihkan tautan {@code calonsiswa} lama pada {@code sekolah.siswa}; (5) memanggil
	 * {@link #saveSiswa} untuk membuat/memperbarui {@link Siswa}, lalu menyalin lampiran/
	 * keanggotaan kelas; (6) mencatat hasil baris (sukses/gagal) ke {@code report} untuk
	 * laporan akhir. Kegagalan pada satu baris (baik saat menyimpan status diterima maupun
	 * kegagalan lain apa pun) DITANGKAP secara terpisah, transaksi terkait di-rollback, sesi
	 * dibersihkan (dan dibuka ulang bila ternyata tertutup oleh {@link #masukkanKelas}/
	 * {@link #masukkanKelasLes}), lalu proses BERLANJUT ke baris berikutnya — satu baris
	 * bermasalah tidak menggagalkan seluruh upload.
	 * </p>
	 *
	 * <p>
	 * Setelah seluruh baris diproses, laporan ringkasan disimpan ke berkas lewat
	 * {@link ais.common.UploadReportHelper#simpanLaporan()} dan path-nya diteruskan ke UI untuk
	 * diunduh pengguna ({@code Filedownload.save}) bersamaan dengan dialog pemberitahuan hasil
	 * akhir (jumlah sukses/gagal via {@code report.getRingkasan()}).
	 * </p>
	 *
	 * @param file          berkas Excel {@code .xlsx} berisi daftar calon siswa yang akan
	 *                      dikonversi
	 * @param eventListener listener yang dipanggil setelah dialog hasil akhir (sukses maupun
	 *                      kuota penuh) ditutup pengguna
	 * @throws Exception diteruskan dari kegagalan inisialisasi awal (mis. pembuatan komponen
	 *                    {@link Timer}); kegagalan per-baris di dalam thread pemrosesan
	 *                    ditangani secara internal dan tidak dilempar ke pemanggil method ini
	 */
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
													? Restrictions.eq("sekolah", sekolah)
													: Restrictions.sqlRestriction("true"))
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
												? Restrictions.eq("sekolah", sekolah)
												: Restrictions.sqlRestriction("true"))
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

	/**
	 * Menyalin foto calon siswa ({@link FotoCalonSiswa}) menjadi foto siswa resmi
	 * ({@link FotoSiswa}) setelah konversi ke {@link Siswa} berhasil, memakai sesi streaming
	 * khusus ({@link StreamingHibernateUtil}) yang cocok untuk data biner besar (foto). Bila
	 * siswa tujuan sudah memiliki foto sebelumnya (mis. dari proses konversi ulang), foto lama
	 * DIHAPUS lebih dulu sebelum foto baru disimpan (bukan ditambahkan sebagai foto ke sekian).
	 * Tidak melakukan apa pun bila calon siswa tidak memiliki foto tersimpan.
	 *
	 * @param calonSiswa calon siswa sumber foto
	 * @param siswa      siswa tujuan yang akan menerima salinan foto
	 */
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

	/**
	 * Menyalin keanggotaan kelas les ({@link KelasLesSiswa}) yang sudah dipilih calon siswa
	 * (lewat {@link CalonSiswa#ambilKelasLesSiswa()}) menjadi record keanggotaan
	 * {@link KelasLesSiswaPunyaSiswa} untuk siswa hasil konversi, satu record per kelas les,
	 * hanya bila belum ada record keanggotaan yang sama (dicek lewat hitung baris agar tidak
	 * terjadi duplikasi saat method dipanggil berulang, mis. pada retry setelah kegagalan
	 * sebagian). Sesi thread-local ditutup di akhir method (baik sukses maupun gagal).
	 *
	 * @param calonSiswa calon siswa sumber daftar kelas les
	 * @param siswa      siswa tujuan yang akan didaftarkan ke kelas les yang sama
	 */
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

	/**
	 * Seperti {@link #masukkanKelasLes(CalonSiswa, Siswa)}, namun untuk keanggotaan kelas
	 * reguler ({@code calonSiswa.getKelasSiswa()} → record {@link KelasSiswaPunyaSiswa}).
	 * Tidak melakukan apa pun bila calon siswa tidak memiliki kelas siswa yang dipilih. Sesi
	 * thread-local ditutup di akhir method.
	 *
	 * @param calonSiswa calon siswa sumber data kelas
	 * @param siswa      siswa tujuan yang akan didaftarkan ke kelas yang sama
	 */
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

	/**
	 * Implementasi INTI pembuatan atau pembaruan record {@link Siswa} dari data
	 * {@link CalonSiswa} yang sudah dinyatakan diterima, dipakai bersama oleh
	 * {@link #onGenerateNis} dan alur upload massal {@link #uploadKelulusan}.
	 *
	 * <p>
	 * Tidak melakukan apa pun (mengembalikan {@code null}) bila {@code calonSiswa} belum
	 * dinyatakan {@code telahDiterima}. Bila diterima, alur kerjanya:
	 * </p>
	 * <ol>
	 * <li>Memuat ulang {@code calonSiswa} dalam sesi AKTIF bila objek yang diberikan detached
	 * ({@code !session.contains(calonSiswa)}) — penting agar relasi lazy-load (mis.
	 * {@code gelombangPendaftaranPsb.sekolah}) tidak melempar {@code LazyInitializationException}
	 * yang bisa tertelan diam-diam saat penyalinan properti di bawah, yang akhirnya membuat
	 * {@code siswa.sekolah} tetap {@code null} dan siswa tersaring dari pencarian yang mensyaratkan
	 * sekolah tidak kosong.</li>
	 * <li>Mencari {@link Siswa} yang sudah ada terkait {@code calonSiswa} lewat asosiasi
	 * langsung, atau bila tidak ada, mencari berdasarkan kecocokan nama+tanggal lahir+tahun
	 * masuk+sekolah (menangani kasus data siswa sudah ada dari jalur lain sebelum konversi
	 * PSB); bila tetap tidak ditemukan, membuat {@link Siswa} BARU dengan kata sandi awal =
	 * NIS terenkripsi (lihat catatan keamanan pada javadoc kelas).</li>
	 * <li>Bila {@link Siswa} sudah ada dan {@code nim} yang diberikan kosong, NIS LAMA
	 * dipertahankan (tidak ditimpa kosong); bila {@code nim} diberikan, NIS lama akan diganti.</li>
	 * <li>Menyalin SELURUH properti yang namanya sama persis di kedua entitas (irisan nama
	 * properti {@link CalonSiswa} dan {@link Siswa}) lewat metadata Hibernate
	 * ({@link ClassMetadata}) — properti PSB spesifik yang tidak ada padanannya di
	 * {@link Siswa} (mis. alamat sekolah asal) dilewati secara proaktif (bukan dianggap error)
	 * karena memang tidak relevan lagi setelah diterima.</li>
	 * <li>Menetapkan NIS final, dan sebagai jaring pengaman memastikan {@code sekolah}/
	 * {@code yayasan} pada siswa tidak kosong bila tersedia pada calon siswa (mengantisipasi
	 * properti yang gagal tersalin di langkah sebelumnya).</li>
	 * <li>Membersihkan tautan {@code calonsiswa} pada baris {@code sekolah.siswa} LAIN yang
	 * mungkin masih merujuk ke {@code calonSiswa} ini (SQL native, DALAM transaksi yang sama
	 * agar atomik, mencegah pelanggaran constraint unik {@code siswa_calonsiswa_key|});
	 * {@code session.clear()} SELALU dijalankan setelahnya (di luar try-catch pembersihan)
	 * agar cache level-1 Hibernate tidak menulis-balik nilai lama meskipun SQL pembersihan
	 * gagal.</li>
	 * <li>Menyimpan {@link Siswa} lewat {@link Common#refreshSaveOrUpdate(Session, Object)},
	 * menautkan {@code calonSiswa.setSiswa(siswa)} secara in-memory, DAN secara terpisah
	 * meng-update kolom {@code siswa_id} pada {@code sekolah.calon_siswa} lewat SQL native
	 * (karena {@code calonSiswa} yang detached dari sesi ini tidak otomatis ter-flush oleh
	 * setter in-memory saja).</li>
	 * </ol>
	 *
	 * @param session     sesi Hibernate aktif tempat operasi dijalankan
	 * @param calonSiswa  calon siswa sumber data, sudah harus {@code telahDiterima}
	 * @param nim         NIS yang akan dipakai; boleh kosong bila siswa sudah ada dan NIS lama
	 *                    hendak dipertahankan
	 * @param commitMaual {@code true} agar method ini SENDIRI membuka dan meng-commit
	 *                    transaksi ({@code session.getTransaction().begin()/commit()});
	 *                    {@code false} bila pemanggil sudah mengelola transaksi di luar method
	 *                    ini (mis. transaksi sudah dibuka sebelum memanggil method ini)
	 * @return record {@link Siswa} yang berhasil dibuat/diperbarui, atau {@code null} bila
	 *         {@code calonSiswa} belum dinyatakan diterima
	 */
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
