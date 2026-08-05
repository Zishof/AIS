package ais.action.report.helper;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.StatusPertemuan;

/**
 * <h1>DiagnosaJadwalUjianHelper — Penjelas "Mengapa Jadwal Ujian Kosong" yang Dapat Dipakai Ulang</h1>
 *
 * <p><b>Untuk apa kelas ini?</b> Beberapa laporan ujian (Jadwal Ujian, Jadwal Pengawas Ujian, Kartu
 * Ujian) sama-sama menarik data dari entitas {@link ais.database.model.Pertemuan} yang bertindak
 * sebagai <i>sesi ujian</i>: sebuah Pertemuan dianggap sesi ujian bila kolom {@code statusPertemuan}
 * berisi jenis ujian (mis. UTS/UAS). Bila hasilnya kosong, pengguna hanya melihat tabel kosong atau
 * tulisan "Belum dijadwal" tanpa tahu penyebabnya. Kelas ini menyediakan SATU tempat untuk menyusun
 * kalimat penjelasan yang sederhana dan tepat sasaran, sehingga tiap laporan cukup memanggil
 * {@link #alasan} tanpa menyalin logikanya berulang.</p>
 *
 * <h2>Cara kerja — penelusuran bertingkat</h2>
 * <p>Alasan disusun dengan menelusuri kondisi dari yang paling mendasar ke yang paling spesifik,
 * sehingga pesan yang muncul benar-benar sesuai keadaan data:</p>
 * <ol>
 *   <li><b>Tidak ada sesi ujian untuk jenis yang dipilih?</b> Cek dulu apakah ada <i>mata kuliah/kelas</i>
 *       sama sekali untuk kombinasi filter (Fakultas/Prodi/Program/Tahun Akademik/Semester/Kelas). Bila
 *       nol → kemungkinan filter salah atau perkuliahan belum dibuat.</li>
 *   <li>Bila mata kuliah ADA tetapi <b>belum ada sesi ujian apa pun</b> → jadwal ujian memang belum
 *       dibuat/di-generate.</li>
 *   <li>Bila sudah ada sesi ujian (jenis lain) tetapi <b>belum untuk jenis yang dipilih</b> (mis. baru
 *       UTS, UAS belum) → arahkan agar memilih jenis ujian yang benar atau membuat jadwalnya.</li>
 *   <li><b>Sesi ujian ADA tetapi belum ada tanggalnya</b> (semua {@code tanggal} kosong) → inilah yang
 *       muncul sebagai "Belum dijadwal"; artinya jadwal sesi sudah ada tetapi hari/jam belum ditetapkan.</li>
 * </ol>
 *
 * <h2>Kontrak &amp; efisiensi</h2>
 * <p>Pemanggil menyerahkan {@code jumlahSesiJenisIni} — yaitu jumlah baris hasil laporan itu sendiri
 * (yang sudah dihitung) — agar tidak diulang. Bila &gt; 0 dan sudah ada yang bertanggal, method
 * langsung mengembalikan string kosong {@code ""} (tanpa query tambahan) yang menandakan "jadwal
 * sudah benar, tidak perlu penjelasan". Query hitung memakai {@code Projections.rowCount()} sehingga
 * ringan (tidak menarik baris/kolom besar). Semua query memakai {@link Session} yang <b>sudah</b>
 * disediakan pemanggil — kelas ini tidak pernah membuka atau menutup session sendiri, sesuai aturan:
 * session dari {@code currentSession()} ditutup otomatis kerangka kerja; session dari
 * {@code openSession()}/{@code currentNativeSession()} ditutup di {@code finally} milik pemanggil.</p>
 *
 * <h2>Konsistensi filter</h2>
 * <p>Restriksi filter (fakultas, jurusan, masa perkuliahan, tahun ajaran, semester genap/ganjil atau
 * semester pendek, program, kelas berdasarkan nama, dan penanda ekstrakurikuler pada mata kuliah)
 * disusun identik dengan query utama laporan supaya angka yang dihitung benar-benar mewakili apa yang
 * dilihat pengguna. Nilai {@code null} pada sebuah filter berarti "semua" (tidak membatasi).</p>
 *
 * <h2>Kompatibilitas</h2>
 * <p>Java 1.7 (tanpa lambda/stream); blok {@code try/catch} bergaya 1.6 (menangkap {@code Exception}).
 * Bila terjadi galat query, method mengembalikan pesan umum yang tetap membantu pengguna memeriksa
 * filternya, alih-alih melempar dan menggagalkan laporan.</p>
 *
 * @author AIS
 */
public final class DiagnosaJadwalUjianHelper {

	private DiagnosaJadwalUjianHelper() {
	}

	/**
	 * Menyusun kalimat sederhana yang menjelaskan mengapa jadwal ujian kosong / belum dijadwal.
	 *
	 * @param session            session Hibernate yang sedang aktif (tidak dibuka/ditutup di sini).
	 * @param jumlahSesiJenisIni  jumlah sesi ujian jenis terpilih yang sudah dihitung laporan (hasilnya).
	 * @param fak                 filter fakultas (null = semua).
	 * @param jur                 filter jurusan/prodi (null = semua).
	 * @param masa                filter masa perkuliahan (null = semua).
	 * @param ta                  nilai tahun ajaran terpilih (null = semua).
	 * @param gg                  nilai semester Genap/Ganjil/SP terpilih (null = semua).
	 * @param prog                nilai program terpilih (null = semua).
	 * @param namaKelas           nama kelas terpilih (null = semua).
	 * @param ekstra              true bila hanya mata kuliah ekstrakurikuler.
	 * @param jenis               jenis ujian terpilih (StatusPertemuan); null berarti tak diketahui.
	 * @return kalimat alasan, atau {@code ""} bila jadwal sudah benar (tidak perlu ditampilkan).
	 */
	public static String alasan(Session session, int jumlahSesiJenisIni, Fakultas fak, Jurusan jur,
			MasaPerkuliahan masa, Object ta, Object gg, Object prog, String namaKelas, boolean ekstra,
			StatusPertemuan jenis) {
		try {
			if (jumlahSesiJenisIni <= 0) {
				long jmlMk = hitungPerkuliahan(session, fak, jur, masa, ta, gg, prog, namaKelas, ekstra);
				if (jmlMk == 0) {
					return "Belum ada mata kuliah/kelas untuk kombinasi filter ini. Silakan periksa Prodi, Program, "
							+ "Tahun Akademik, dan Semester — atau pastikan perkuliahan sudah dibuat.";
				}
				long jmlSesiSemua = hitungSesiUjian(session, fak, jur, masa, ta, gg, prog, namaKelas, ekstra, null, false);
				if (jmlSesiSemua == 0) {
					return "Sudah ada " + jmlMk + " mata kuliah untuk filter ini, tetapi belum ada satu pun sesi ujian "
							+ "yang dibuat. Silakan buat/generate jadwal ujian terlebih dahulu.";
				}
				String namaJenis = jenis == null ? "-" : jenis.getNama();
				return "Sudah ada sesi ujian untuk filter ini, tetapi belum ada untuk jenis ujian \"" + namaJenis
						+ "\". Pastikan pilihan Jenis Ujian sudah benar, atau buat jadwal untuk jenis ujian tersebut.";
			}

			long bertanggal = hitungSesiUjian(session, fak, jur, masa, ta, gg, prog, namaKelas, ekstra, jenis, true);
			if (bertanggal == 0) {
				return "Sesi ujian sudah dibuat (" + jumlahSesiJenisIni + " sesi), tetapi tanggal & jam ujian belum "
						+ "ditetapkan. Silakan tetapkan jadwal (tanggal/jam) pada menu Penjadwalan Ujian.";
			}
			return "";
		} catch (Exception e) {
			return "Jadwal ujian tidak ditemukan untuk filter yang dipilih. Periksa kembali Fakultas, Prodi, "
					+ "Program, Tahun Akademik, Semester, dan Jenis Ujian.";
		}
	}

	/** Menghitung jumlah perkuliahan (mata kuliah/kelas) yang cocok dengan filter akademik. */
	public static long hitungPerkuliahan(Session session, Fakultas fak, Jurusan jur, MasaPerkuliahan masa,
			Object ta, Object gg, Object prog, String namaKelas, boolean ekstra) {
		Criteria c = session.createCriteria(Perkuliahan.class).createAlias("jurusan", "j")
				.createAlias("matakuliah", "mk")
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("j.fakultas", fak))
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jur))
				.add(masa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("masaPerkuliahan", masa))
				.add(ta == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("tahunAjaran", ta))
				.add(gg == null ? Restrictions.sqlRestriction("true")
						: gg.equals(Perkuliahan.SP) ? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
								: Restrictions.in("semester", gg.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
				.add(prog == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("program", prog))
				.add(namaKelas == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("kelas", namaKelas))
				.add(!ekstra ? Restrictions.sqlRestriction("true") : Restrictions.eq("mk.extraKulikuler", true))
				.setProjection(Projections.rowCount());
		Object o = c.uniqueResult();
		return o instanceof Number ? ((Number) o).longValue() : 0L;
	}

	/**
	 * Menghitung sesi ujian (Pertemuan) yang cocok filter. {@code jenis} null = semua jenis ujian
	 * (statusPertemuan tidak kosong). Bila {@code bertanggalSaja} true, hanya sesi yang sudah punya
	 * {@code tanggal} — untuk membedakan "sesi belum dibuat" dari "sesi ada tapi belum dijadwalkan".
	 */
	public static long hitungSesiUjian(Session session, Fakultas fak, Jurusan jur, MasaPerkuliahan masa,
			Object ta, Object gg, Object prog, String namaKelas, boolean ekstra, StatusPertemuan jenis,
			boolean bertanggalSaja) {
		Criteria c = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(jenis == null ? Restrictions.isNotNull("statusPertemuan") : Restrictions.eq("statusPertemuan", jenis))
				.add(bertanggalSaja ? Restrictions.isNotNull("tanggal") : Restrictions.sqlRestriction("true"))
				.createAlias("perkuliahan", "perkuliahan").createAlias("perkuliahan.jurusan", "j")
				.createAlias("perkuliahan.matakuliah", "mk")
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("j.fakultas", fak))
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("perkuliahan.jurusan", jur))
				.add(masa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("perkuliahan.masaPerkuliahan", masa))
				.add(ta == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("perkuliahan.tahunAjaran", ta))
				.add(gg == null ? Restrictions.sqlRestriction("true")
						: gg.equals(Perkuliahan.SP)
								? Restrictions.eq("perkuliahan.statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
								: Restrictions.in("perkuliahan.semester", gg.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
				.add(prog == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("perkuliahan.program", prog))
				.add(namaKelas == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("perkuliahan.kelas", namaKelas))
				.add(!ekstra ? Restrictions.sqlRestriction("true") : Restrictions.eq("mk.extraKulikuler", true))
				.setProjection(Projections.rowCount());
		Object o = c.uniqueResult();
		return o instanceof Number ? ((Number) o).longValue() : 0L;
	}
}
