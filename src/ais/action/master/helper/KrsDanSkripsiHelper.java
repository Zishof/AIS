package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.ws.util.CommonUtil;
import ais.common.Common;
import ais.database.hibernate.AuditListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;

/**
 * Kumpulan method statis (bukan helper berstate/instance) seputar KRS (Kartu Rencana Studi) milik
 * {@link Mahasiswa} — khususnya perhitungan/singkronisasi KRS aktif dan pengecekan syarat
 * pengambilan mata kuliah seminar/skripsi. Entity utama yang dibaca/ditulis: {@link KrsMahasiswa}
 * (satu baris per kombinasi mahasiswa-semester-tahapan-semesterPendek), {@link Detailperkuliahan}
 * (riwayat pengambilan mata kuliah per semester berikut nilainya), dan {@link Matakuliah} (dicocokkan
 * lewat kode atau nama saat mencari mata kuliah seminar/skripsi).
 *
 * <p><b>{@link #ambilKrsMahasiswaTanpaSinkronisasi} vs {@link #singkronkanKrsMahasiswa}</b> — dua
 * jalur yang sengaja dipisah:</p>
 * <ul>
 * <li><b>Baca saja</b> ({@code ambilKrsMahasiswaTanpaSinkronisasi}): tidak menghitung ulang SKS/IPK
 * dan tidak menulis apa pun ke database, hanya mengambil baris {@link KrsMahasiswa} yang sudah ada.
 * Dipakai untuk render layar/status (mis. membuka komentar/catatan KRS) supaya pembukaan halaman
 * tidak ikut membuka transaksi tulis yang bisa berebut lock {@code krs_mahasiswa} dengan proses
 * simpan KRS yang sedang berjalan (lihat r78511 di riwayat svn file ini).</li>
 * <li><b>Hitung ulang &amp; simpan</b> ({@code singkronkanKrsMahasiswa}): menghitung ulang SKS/IPK/IPS
 * dari {@link Detailperkuliahan} terbaru mahasiswa, lalu melakukan {@code saveOrUpdate} ke baris
 * {@link KrsMahasiswa} yang sesuai. Dipanggil setelah ada perubahan nilai/KRS, termasuk dari worker
 * pembayaran setelah konversi calon mahasiswa menjadi mahasiswa aktif.</li>
 * </ul>
 *
 * <p><b>Cache JSON sementara:</b> {@code singkronkanKrsMahasiswa} memakai
 * {@link CommonUtil#ambilTemporary}/{@link CommonUtil#simpanTemporary} dengan kunci
 * {@code "KrsMahasiswa_" + idMahasiswa + "-" + semester + "-" + tahapan + "-" + semesterPendek} untuk
 * menghindari perhitungan ulang yang mahal (query {@link Detailperkuliahan} satu per satu) pada
 * pemanggilan berturut-turut dengan parameter yang sama; parameter {@code keDatabase=true} melewati
 * cache ini secara paksa.</p>
 *
 * <p><b>Pengecekan syarat seminar/skripsi</b> — {@link #checkApakahSudahMengambilKrsSeminarSkripsiDan}
 * (semua kode wajib ditemukan, logika AND) dan {@link #checkApakahSudahMengambilKrsSeminarSkripsi}
 * (kode manapun cukup, logika OR, dengan fase pencarian semester spesifik lalu jatuh ke pencarian
 * bebas semester) membaca daftar {@link Detailperkuliahan} mahasiswa dan mencocokkan kode/nama mata
 * kuliah terhadap parameter yang berasal dari konfigurasi label seminar/skripsi (dipisah koma).
 * {@link #kodeMatakuliahDanEfektif} adalah utilitas normalisasi murni (tanpa akses database) yang
 * dipanggil sebelum pengecekan AND: menghapus dari daftar "DAN" (prasyarat wajib) kode apa pun yang
 * juga muncul di daftar "ATAU" (kandidat utama), supaya satu daftar kandidat yang salah tersalin ke
 * kedua kolom konfigurasi tidak berubah menjadi kewajiban mengambil semua mata kuliah sekaligus.</p>
 *
 * <p><b>Manajemen session:</b> setiap method yang mengakses database membuka
 * {@link HibernateUtil#getSessionFactory()}{@code .openSession()} sendiri (bukan
 * {@code currentSession()} milik thread request) dan menutupnya secara eksplisit di blok
 * {@code finally} ({@code clear()}/{@code disconnect()}/{@code close()} berurutan, masing-masing
 * dibungkus try-catch terpisah agar kegagalan satu langkah cleanup tidak menghalangi langkah
 * berikutnya) — pola ini konsisten dipakai supaya helper aman dipanggil dari luar konteks request web
 * biasa (mis. dari worker/thread background pembayaran).</p>
 */
public class KrsDanSkripsiHelper {

	// =========================================================================
	// 1. SINGKRONISASI KRS MAHASISWA
	// =========================================================================

	/**
	 * Mengambil KRS yang sudah ada tanpa menghitung ulang dan tanpa menulis ke
	 * database. Jalur ini khusus untuk render layar/status agar pembukaan halaman
	 * tidak membuat transaksi baru yang dapat bersaing dengan transaksi simpan KRS.
	 */
	public static KrsMahasiswa ambilKrsMahasiswaTanpaSinkronisasi(Mahasiswa mahasiswa, Integer semester,
			Integer tahapan, Integer semesterPendek) {
		KrsMahasiswa hasil = new KrsMahasiswa();
		if (mahasiswa == null || mahasiswa.getId() == null) {
			return hasil;
		}

		if (tahapan != null && tahapan.intValue() == 0) {
			tahapan = null;
		}
		if (semester == null) {
			semester = mahasiswa.currentSemester();
		}
		if (semester == null) {
			semester = Integer.valueOf(1);
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Mahasiswa mahasiswaDatabase = (Mahasiswa) session.get(Mahasiswa.class, mahasiswa.getId());
			if (mahasiswaDatabase != null) {
				hasil = mahasiswaDatabase.ambilDefaultKrsMahasiswa(semester, tahapan, semesterPendek, session);
				if (hasil != null) {
					// Dosen PA dipakai sesudah sesi baca ditutup (mis. saat membuka
					// komentar/catatan). Inisialisasi relasi ini tanpa melakukan write agar
					// jalur read-only tidak menimbulkan LazyInitializationException.
					if (hasil.getDosenPa() != null) {
						Hibernate.initialize(hasil.getDosenPa());
					}
					// Gunakan object mahasiswa milik layar supaya hasil tetap aman setelah
					// session baca ditutup dan tidak membawa proxy session lama.
					hasil.setMahasiswa(mahasiswa);
					return hasil;
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"ambil KRS tanpa sinkronisasi gagal; render dilanjutkan dengan data minimal");
		} finally {
			Common.closeNativeSessionQuietly(session);
		}

		hasil = new KrsMahasiswa();
		hasil.setMahasiswa(mahasiswa);
		hasil.setSemester(semester);
		hasil.setTahapan(tahapan);
		hasil.setSemesterPendek(semesterPendek);
		return hasil;
	}

	/**
	 * Menghitung ulang KRS aktif seorang mahasiswa (SKS diambil, SKS konversi, IPK, IPS, dosen PA
	 * default, kelas) dari riwayat {@link Detailperkuliahan}-nya, lalu menyimpan hasilnya sebagai
	 * baris {@link KrsMahasiswa} (insert/update via {@link Common#refreshSaveOrUpdate}) kecuali
	 * bila {@code jikaTidakAdaKembali} diminta secara eksplisit atau data cache JSON sementara
	 * masih valid dan {@code keDatabase} bernilai {@code false}.
	 *
	 * <p>Alur ringkas: normalisasi parameter (tahapan 0 dianggap null, semester kosong diisi dari
	 * {@link Mahasiswa#currentSemester()} lalu dibatasi tidak melebihi semester lulus bila
	 * mahasiswa berstatus keluar) &rarr; cek cache JSON kecuali {@code keDatabase=true} &rarr; buka
	 * transaksi baru, muat ulang {@link Mahasiswa} yang managed pada session ini dan tandai
	 * {@code setReadOnly(true)} agar Hibernate tidak mencoba meng-UPDATE baris mahasiswa saat flush
	 * (nilai {@code nimKey} lama kadang tidak identik dengan hasil normalisasi
	 * {@code getNimKey()}, sehingga dirty-check bisa menabrak constraint unik) &rarr; hitung
	 * kumpulan {@link Detailperkuliahan} yang diambil semester ini serta kumpulan "sampai semester
	 * ini" (dari {@link KrsDetailHelper#ambilDetailperkuliahanSampai}), saring yang lulus, lalu
	 * hitung SKS/IPK/IPS &rarr; {@code commit} transaksi &rarr; tulis hasil ke cache JSON &rarr;
	 * panggil {@link AuditListener#prosesUntukElearning} untuk menyinkronkan data ke modul
	 * e-learning.</p>
	 *
	 * <p>Efek samping: membuka dan meng-commit satu transaksi Hibernate, dapat melakukan
	 * insert/update pada tabel {@code krs_mahasiswa}, menulis/menghapus entri cache JSON sementara
	 * lewat {@link CommonUtil}, dan memicu proses sinkronisasi e-learning. Kegagalan di tengah
	 * proses melakukan rollback dan mengembalikan objek {@link KrsMahasiswa} kosong sebagai
	 * fallback (bukan melempar exception ke pemanggil).</p>
	 *
	 * @param mahasiswa mahasiswa target; harus punya {@code id} tersimpan, kalau tidak langsung
	 *            mengembalikan {@link KrsMahasiswa} kosong
	 * @param semester semester KRS yang dihitung; bila {@code null} diisi otomatis dari
	 *            {@link Mahasiswa#currentSemester()}
	 * @param tahapan kode tahapan studi (bernilai 0 diperlakukan sama dengan {@code null})
	 * @param semesterPendek penanda semester pendek/reguler, diteruskan apa adanya ke query
	 *            {@link Detailperkuliahan}
	 * @param keDatabase {@code true} untuk memaksa hitung ulang dari database dan melewati cache
	 *            JSON sementara
	 * @param dosenPaDefault {@code true} agar dosen PA hasil KRS mengikuti dosen PA mahasiswa saat
	 *            ini bila belum ada atau semester yang dihitung adalah semester berjalan
	 * @param jikaTidakAdaKembali {@code true} untuk langsung mengembalikan {@link KrsMahasiswa}
	 *            kosong tanpa membaca cache maupun database (dipakai saat pemanggil hanya perlu
	 *            memaksa reset tanpa hasil baru)
	 * @return {@link KrsMahasiswa} hasil hitung ulang yang sudah tersimpan, hasil dari cache, atau
	 *         objek kosong sebagai fallback bila terjadi kegagalan/precondition tidak terpenuhi
	 */
	public static KrsMahasiswa singkronkanKrsMahasiswa(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean keDatabase, boolean dosenPaDefault, boolean jikaTidakAdaKembali) {

		if (mahasiswa == null || mahasiswa.getId() == null) {
			return new KrsMahasiswa();
		}

		if (tahapan != null && tahapan == 0) {
			tahapan = null;
		}

		// PROTEKSI NULL: Jika semester kosong, ambil semester aktif saat ini
		if (semester == null) {
			semester = mahasiswa.currentSemester();
		}
		
		// PROTEKSI LAPIS 2: Jika currentSemester juga null (data mahasiswa tidak wajar), berikan fallback
		if (semester == null) {
			semester = 1;
		}

		Integer smtLulus = mahasiswa.getSemesterLulus();
		
		// PROTEKSI UNBOXING: Pastikan smtLulus tidak null sebelum dibandingkan dengan operator >
		if (mahasiswa.getStatusKeluar() != null && smtLulus != null && smtLulus > 1 && semester > smtLulus) {
			semester = smtLulus;
		}

		String key = KrsMahasiswa.class.getSimpleName() + "_" + mahasiswa.getId() + "-" + semester + "-" + tahapan + "-"
				+ semesterPendek;

		// 1. CEK DARI CACHE (JSON) JIKA TIDAK MEMAKSA KE DATABASE
		if (!keDatabase) {
			try {
				JSONArray array = CommonUtil.ambilTemporary(key);
				if (array != null && array.length() > 0) {
					Object cachePertama = array.opt(0);
					String d = cachePertama == null ? "" : cachePertama.toString();
					KrsMahasiswa k = null;
					if (cachePertama instanceof JSONObject) {
						k = (KrsMahasiswa) Common.convertToObject((JSONObject) cachePertama);
					}

					if (k != null) {
						k.setMahasiswa(mahasiswa);
						if (mahasiswa.getDosen() == null && k.getDosenPa() != null) {
							mahasiswa.setDosen(k.getDosenPa().getId());
						}
						return k;
					}
				}
			} catch (Exception e) {
				// Cache lama boleh berisi scalar/string yang bukan JSON. Anggap cache miss dan
				// lanjutkan ke database; ini bukan error aplikasi yang perlu masuk audit.
			}
		}

		if (jikaTidakAdaKembali) {
			return new KrsMahasiswa();
		}

		CommonUtil.reset(key);

		Session session = null;
		try {
				session = HibernateUtil.getSessionFactory().openSession();
				session.getTransaction().begin();

				// Sinkronisasi ini sering dipanggil dari worker pembayaran setelah konversi calon
				// mahasiswa. Jangan pernah menyimpan KRS dengan referensi Mahasiswa transient/stale:
				// Hibernate dapat mencoba cascade INSERT Mahasiswa kedua (NIM ganda), atau KRS gagal
				// pada FK krs_mahasiswa bila baris induknya belum commit/sudah dihapus.
				Mahasiswa mahasiswaDb = mahasiswa == null || mahasiswa.getId() == null ? null
						: (Mahasiswa) session.get(Mahasiswa.class, mahasiswa.getId());
				if (mahasiswaDb == null) {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
					KrsMahasiswa belumTersimpan = new KrsMahasiswa();
					belumTersimpan.setMahasiswa(mahasiswa);
					belumTersimpan.setSemester(semester);
					belumTersimpan.setTahapan(tahapan);
					return belumTersimpan;
				}
				// Mahasiswa hanya menjadi referensi dan sumber kalkulasi pada transaksi ini.
				// Jangan ikut dirty-check saat KRS di-flush: beberapa data lama memiliki
				// nimkey yang tidak identik dengan NIM, sedangkan getNimKey() menormalkan
				// nilai tersebut. Tanpa read-only Hibernate mencoba UPDATE Mahasiswa dan
				// dapat menabrak mahasiswa_nimkey_key milik baris lain.
				session.setReadOnly(mahasiswaDb, true);
				// Semua lazy association dan kalkulasi berikutnya harus memakai instance
				// managed dari session ini. Instance parameter dapat berasal dari session
				// lain yang sudah ditutup oleh proses pembayaran/KRS sebelumnya.
				mahasiswa = mahasiswaDb;

			// Karena pemanggilan mahasiswa memerlukan session yg sama jika tidak di detach,
			// di sini diasumsikan method bawaan sudah menangani isolated session,
			// jika tidak, gunakan session parameter dari pemanggil
			KrsMahasiswa krsMahasiswa = mahasiswa.ambilDefaultKrsMahasiswa(semester, tahapan, semesterPendek, session);
			Integer komentars = Common.loadKomentarUkuran(mahasiswa, semester, tahapan, semesterPendek);
			Dosen dosenPa = krsMahasiswa != null ? krsMahasiswa.getDosenPa() : null;

			try {
				if (mahasiswa.getDosen() != null) {
					if (dosenPa == null || (dosenPaDefault && semester.equals(mahasiswa.currentSemester()))) {
						dosenPa = new Dosen(mahasiswa.getDosen());
					}

				}
			}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:113");
				// TODO: handle exception
			}

			if (!dosenPaDefault && dosenPa == null) {
				dosenPa = new Dosen(mahasiswa.getDosen());
			}

			String kelas = krsMahasiswa != null ? krsMahasiswa.getKelas() : null;
			// Pengecekan aman karena semester sudah dijamin tidak null di baris atas
			if (kelas == null || kelas.trim().isEmpty() || semester.equals(mahasiswa.currentSemester())) {
				kelas = mahasiswa.getKelas();
			}

			boolean dihitungDariSp = Konfigurasi.AKTIF
					.equals(Common.getKonfigurasi("ips_juga_dihitung_dari_sp", Konfigurasi.TIDAK_AKTIF).getNilai());

			// PENGAMBILAN DATA NILAI/DETAIL PERKULIAHAN
			Collection<Long> det = dihitungDariSp
					? mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, false, true, null)
					: mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek);

			Collection<Long> det1 = KrsDetailHelper.ambilDetailperkuliahanSampai(mahasiswa, semester, tahapan, semesterPendek, true,
					false);

			if (semesterPendek == null && !dihitungDariSp) {
				Collection<Long> detSP = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
						Perkuliahan.SEMESTER_PENDEK);
				if (detSP != null && !detSP.isEmpty()) {
					det1.removeAll(detSP);
				}
			}
			det1 = mahasiswa.saringBerdasarNilai(det1);

			Set<Long> detLulus = new HashSet<Long>();
			Set<Long> detLulus1 = new HashSet<Long>();

			for (Long detailperkuliahanid : det) {
				Detailperkuliahan detailperkuliahan = detailperkuliahanid == null ? null
						: (Detailperkuliahan) session.get(Detailperkuliahan.class, detailperkuliahanid);
				if (detailperkuliahan != null && detailperkuliahan.getNilaiHuruf() != null
						&& !detailperkuliahan.getNilaiHuruf().isEmpty() && detailperkuliahan.getTotalNilai() > 10.0
						&& detailperkuliahan.getLulus() != null && detailperkuliahan.getLulus()) {
					detLulus.add(detailperkuliahanid);
				}
			}

			for (Long detailperkuliahanid : det1) {
				Detailperkuliahan detailperkuliahan = detailperkuliahanid == null ? null
						: (Detailperkuliahan) session.get(Detailperkuliahan.class, detailperkuliahanid);
				if (detailperkuliahan != null && detailperkuliahan.getNilaiHuruf() != null
						&& !detailperkuliahan.getNilaiHuruf().isEmpty() && detailperkuliahan.getTotalNilai() > 10.0
						&& detailperkuliahan.getLulus() != null && detailperkuliahan.getLulus()) {
					detLulus1.add(detailperkuliahanid);
				}
			}

			if (krsMahasiswa == null) {
				krsMahasiswa = new KrsMahasiswa();
			}

			krsMahasiswa.setAktif(true);
				krsMahasiswa.setMahasiswa(mahasiswaDb);
			krsMahasiswa.setSemester(semester);

			// OPTIMASI MEMORI: Menggunakan StringBuilder daripada Concatenation S +=
			StringBuilder sbDet = new StringBuilder();
			for (Long l : det) {
				if (sbDet.length() > 0)
					sbDet.append(",");
				sbDet.append(l);
			}
			krsMahasiswa.setSksYangDiambilS(sbDet.toString());
			krsMahasiswa.setSksYangDiambil(mahasiswa.prosesHitungSks(det, null, true));

			StringBuilder sbDet1 = new StringBuilder();
			for (Long l : det1) {
				if (sbDet1.length() > 0)
					sbDet1.append(",");
				sbDet1.append(l);
			}
			krsMahasiswa.setSkskS(sbDet1.toString());
			krsMahasiswa.setSksk(mahasiswa.prosesHitungSks(det1, null, true));

			krsMahasiswa.setSksYangDiambilLulus(mahasiswa.prosesHitungSks(detLulus, null, true));
			krsMahasiswa.setSkskLulus(mahasiswa.prosesHitungSks(detLulus1, null, true));

			krsMahasiswa.setSksBukanKonversi(mahasiswa.prosesHitungSks(det, true, true));
			krsMahasiswa.setSksKonversi(mahasiswa.prosesHitungSks(det, false, true));

			krsMahasiswa.setTahapan(tahapan);

			Double ipk = mahasiswa.prosesHitungIpk(det1);
			krsMahasiswa.setIpk(ipk != null ? ipk : 0.0);

			boolean tidakMenghitungNilaiKonversi = Konfigurasi.AKTIF
					.equals(Common.getKonfigurasi("ips_tidak_menghitung_nilai_konversi", Konfigurasi.AKTIF).getNilai());
			Double ips = mahasiswa.prosesHitungIpk(det, tidakMenghitungNilaiKonversi);
			krsMahasiswa.setIps(ips != null ? ips : 0.0);

			krsMahasiswa.setKomentars(komentars);
			krsMahasiswa.setSemesterPendek(semesterPendek);
			krsMahasiswa.setDosenPa(dosenPa);
			krsMahasiswa.setKelas(kelas == null ? "" : kelas.trim());

			// Menyimpan KRS ke DB
			Common.refreshSaveOrUpdate(session, krsMahasiswa);
			session.getTransaction().commit();

			try {
				List<String> filePaths = new ArrayList<String>();
				filePaths.add(krsMahasiswa.getId().toString());
				CommonUtil.simpanTemporary(key, filePaths);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:226");
				// Ignored
			}

			mahasiswa.populateDefaultKrsMahasiswa(krsMahasiswa);
			AuditListener.prosesUntukElearning(krsMahasiswa, "", krsMahasiswa.getId());

			return krsMahasiswa;

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsDanSkripsiHelper.java:236");
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				try {
					session.getTransaction().rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:240");
				}
			}

			KrsMahasiswa krsFallback = new KrsMahasiswa();
			krsFallback.setMahasiswa(mahasiswa);
			krsFallback.setSemester(semester);
			krsFallback.setTahapan(tahapan);
			return krsFallback;
		} finally {
			// MANAJEMEN SESSION KETAT
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:254");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:258");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:262");
				}
			}
		}
	}

	// =========================================================================
	// 2. CEK MENGAMBIL KRS SEMINAR / SKRIPSI (DAN)
	// =========================================================================

	/**
	 * Menghapus kode pada kelompok DAN yang sudah tercantum pada kelompok ATAU.
	 * Kode ATAU adalah kandidat matakuliah utama skripsi, sedangkan kode DAN hanya
	 * boleh berisi prasyarat tambahan. Tanpa normalisasi ini satu daftar kandidat
	 * yang tersalin ke kedua kolom berubah menjadi kewajiban mengambil semua MK.
	 */
	public static String kodeMatakuliahDanEfektif(String kodeDan, String kodeAtau) {
		Set<String> alternatif = new HashSet<String>();
		for (String kode : (kodeAtau == null ? "" : kodeAtau).split(",")) {
			if (kode != null && !kode.trim().isEmpty()) {
				alternatif.add(kode.trim().toLowerCase(java.util.Locale.ENGLISH));
			}
		}

		Set<String> sudahDitambahkan = new HashSet<String>();
		StringBuilder hasil = new StringBuilder();
		for (String kode : (kodeDan == null ? "" : kodeDan).split(",")) {
			String bersih = kode == null ? "" : kode.trim();
			String pembanding = bersih.toLowerCase(java.util.Locale.ENGLISH);
			if (!bersih.isEmpty() && !alternatif.contains(pembanding)
					&& sudahDitambahkan.add(pembanding)) {
				if (hasil.length() > 0) {
					hasil.append(",");
				}
				hasil.append(bersih);
			}
		}
		return hasil.toString();
	}

	/**
	 * Mengecek apakah mahasiswa sudah mengambil (dan disetujui) SEMUA kode/nama mata kuliah pada
	 * {@code label_seminar_skripsi} (logika AND, dipisah koma) — dipakai untuk syarat gabungan
	 * seperti "seminar proposal DAN metodologi penelitian" yang harus dipenuhi sekaligus.
	 * Panggil {@link #kodeMatakuliahDanEfektif} terlebih dulu di sisi pemanggil bila daftar ini
	 * perlu dinormalisasi terhadap daftar kandidat OR.
	 *
	 * <p>Membaca seluruh {@link Detailperkuliahan} milik mahasiswa (lewat
	 * {@link Mahasiswa#ambilDetailperkuliahan()}, tanpa filter semester), mengabaikan baris yang
	 * belum disetujui ({@link Detailperkuliahan#BELUM_DISETUJUI}), lalu mencocokkan kode ATAU nama
	 * {@link Matakuliah} (memakai matakuliah konversi bila ada, kalau tidak memakai matakuliah asli
	 * dari {@link Perkuliahan}) case-insensitive terhadap tiap parameter. Berhenti dini begitu satu
	 * parameter dipastikan tidak ditemukan pada mata kuliah manapun (short-circuit AND).</p>
	 *
	 * @param mahasiswa mahasiswa yang dicek; {@code null} langsung menghasilkan {@code null}
	 * @param label_seminar_skripsi daftar kode/nama mata kuliah dipisah koma yang SEMUANYA wajib
	 *            ditemukan; kosong/{@code null} langsung menghasilkan {@code null}
	 * @return {@link Detailperkuliahan} terakhir yang cocok (dari parameter manapun yang ditemukan
	 *         terakhir) bila SEMUA parameter ditemukan, atau {@code null} bila ada satu saja yang
	 *         tidak ditemukan atau terjadi kegagalan akses data
	 */
	public static Detailperkuliahan checkApakahSudahMengambilKrsSeminarSkripsiDan(Mahasiswa mahasiswa,
			String label_seminar_skripsi) {
		if (mahasiswa == null || label_seminar_skripsi == null || label_seminar_skripsi.trim().isEmpty()) {
			return null;
		}

		Detailperkuliahan dMatch = null;
		boolean semuaKetemu = true;

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan();
			if (detailperkuliahans == null || detailperkuliahans.isEmpty()) {
				return null;
			}

			String[] paramLabels = label_seminar_skripsi.split(",");

			for (String param : paramLabels) {
				if (param == null || param.trim().isEmpty())
					continue;

				boolean paramIniKetemu = false;

				for (Long detailperkuliahanid : detailperkuliahans) {
					Detailperkuliahan detailperkuliahan = detailperkuliahanid == null ? null
							: (Detailperkuliahan) session.get(Detailperkuliahan.class, detailperkuliahanid);
					if (detailperkuliahan == null)
						continue;

					if (Detailperkuliahan.BELUM_DISETUJUI.equals(detailperkuliahan.getPersetujuan())) {
						continue;
					}

					Matakuliah matakuliah = null;
					if (detailperkuliahan.getMatakuliahKonversi() != null) {
						matakuliah = detailperkuliahan.getMatakuliahKonversi();
					} else if (detailperkuliahan.getPerkuliahan() != null) {
						matakuliah = detailperkuliahan.getPerkuliahan().getMatakuliah();
					}

					if (matakuliah == null)
						continue;

					String p = param.trim();
					String mkKode = matakuliah.getKode() == null ? "" : matakuliah.getKode().trim();
					String mkNama = matakuliah.getNama() == null ? "" : matakuliah.getNama().trim();
					if (!p.isEmpty() && (p.equalsIgnoreCase(mkKode) || p.equalsIgnoreCase(mkNama))) {
						dMatch = detailperkuliahan;
						paramIniKetemu = true;
						break; // Jika param sudah ketemu, lanjut ke param berikutnya
					}
				}

				semuaKetemu = semuaKetemu && paramIniKetemu;
				if (!semuaKetemu)
					break; // Logical AND: Jika 1 saja tidak ketemu, auto false, tidak perlu cek sisanya
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsDanSkripsiHelper.java:331");
			semuaKetemu = false;
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:337");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:341");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:345");
				}
			}
		}

		return semuaKetemu ? dMatch : null;
	}

	// =========================================================================
	// 3. CEK MENGAMBIL KRS SEMINAR / SKRIPSI (OR / REGULER)
	// =========================================================================

	/**
	 * Mengecek apakah mahasiswa sudah mengambil (dan disetujui) SALAH SATU kode/nama mata kuliah
	 * pada {@code label_seminar_skripsi} (logika OR, dipisah koma) — dipakai untuk syarat mata
	 * kuliah seminar/skripsi yang kandidatnya bisa lebih dari satu (mis. berbagai variasi nama mata
	 * kuliah skripsi antar kurikulum).
	 *
	 * <p>Pencarian dilakukan dua fase: <b>Fase 1</b> mencocokkan hanya pada
	 * {@link Detailperkuliahan} yang {@code semester}-nya sama persis dengan parameter (bila
	 * {@code semester} diisi); bila tidak ada yang cocok, <b>Fase 2</b> mengulang pencarian yang
	 * sama tanpa filter semester (bebas semester manapun). Baris yang belum disetujui
	 * ({@link Detailperkuliahan#BELUM_DISETUJUI}) selalu diabaikan di kedua fase. Berhenti begitu
	 * kecocokan pertama ditemukan.</p>
	 *
	 * @param mahasiswa mahasiswa yang dicek; {@code null} langsung menghasilkan {@code null}
	 * @param semester semester yang diprioritaskan pada Fase 1; {@code null} berarti langsung
	 *            mencari bebas semester
	 * @param label_seminar_skripsi daftar kode/nama mata kuliah dipisah koma, kandidat mana pun
	 *            yang cocok sudah dianggap memenuhi syarat; kosong/{@code null} langsung
	 *            menghasilkan {@code null}
	 * @return {@link Detailperkuliahan} pertama yang cocok, atau {@code null} bila tidak ada satu
	 *         pun kandidat yang ditemukan atau terjadi kegagalan akses data
	 */
	public static Detailperkuliahan checkApakahSudahMengambilKrsSeminarSkripsi(Mahasiswa mahasiswa, Integer semester,
			String label_seminar_skripsi) {
		if (mahasiswa == null || label_seminar_skripsi == null || label_seminar_skripsi.trim().isEmpty()) {
			return null;
		}

		Detailperkuliahan dMatch = null;
		Session session = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();

			List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan();
			if (detailperkuliahans == null || detailperkuliahans.isEmpty()) {
				return null;
			}

			String[] paramLabels = label_seminar_skripsi.split(",");

			// FASE 1: Cari berdasarkan Semester spesifik (Jika diberikan)
			for (String param : paramLabels) {
				if (param == null || param.trim().isEmpty() || dMatch != null)
					continue;

				for (Long detailperkuliahanid : detailperkuliahans) {
					Detailperkuliahan detailperkuliahan = detailperkuliahanid == null ? null
							: (Detailperkuliahan) session.get(Detailperkuliahan.class, detailperkuliahanid);

					if (detailperkuliahan == null
							|| Detailperkuliahan.BELUM_DISETUJUI.equals(detailperkuliahan.getPersetujuan())) {
						continue;
					}

					if (semester != null && !semester.equals(detailperkuliahan.getSemester())) {
						continue;
					}

					Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() != null
							? detailperkuliahan.getMatakuliahKonversi()
							: (detailperkuliahan.getPerkuliahan() != null
									? detailperkuliahan.getPerkuliahan().getMatakuliah()
									: null);

					String p = param.trim();
					String mkKode = matakuliah == null || matakuliah.getKode() == null ? "" : matakuliah.getKode().trim();
					String mkNama = matakuliah == null || matakuliah.getNama() == null ? "" : matakuliah.getNama().trim();
					if (matakuliah != null && !p.isEmpty()
							&& (p.equalsIgnoreCase(mkKode) || p.equalsIgnoreCase(mkNama))) {
						dMatch = detailperkuliahan;
						break;
					}
				}
			}

			// FASE 2: Jika Fase 1 gagal, cari bebas di seluruh semester
			if (dMatch == null) {
				for (String param : paramLabels) {
					if (param == null || param.trim().isEmpty() || dMatch != null)
						continue;

					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = detailperkuliahanid == null ? null
								: (Detailperkuliahan) session.get(Detailperkuliahan.class, detailperkuliahanid);

						if (detailperkuliahan == null
								|| Detailperkuliahan.BELUM_DISETUJUI.equals(detailperkuliahan.getPersetujuan())) {
							continue;
						}

						Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() != null
								? detailperkuliahan.getMatakuliahKonversi()
								: (detailperkuliahan.getPerkuliahan() != null
										? detailperkuliahan.getPerkuliahan().getMatakuliah()
										: null);

						String p = param.trim();
						String mkKode = matakuliah == null || matakuliah.getKode() == null ? "" : matakuliah.getKode().trim();
						String mkNama = matakuliah == null || matakuliah.getNama() == null ? "" : matakuliah.getNama().trim();
						if (matakuliah != null && !p.isEmpty()
								&& (p.equalsIgnoreCase(mkKode) || p.equalsIgnoreCase(mkNama))) {
							dMatch = detailperkuliahan;
							break;
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsDanSkripsiHelper.java:439");
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:444");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:448");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsDanSkripsiHelper.java:452");
				}
			}
		}

		return dMatch;
	}

}
