package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.master.RencanaTahunAkademikAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.RencanaTahunAkademik;

/**
 * Satu-satunya resolver dan pembangun bukti untuk angka "semester berjalan" pada daftar
 * mahasiswa. Helper ini dibentuk karena {@link Mahasiswa#currentSemester()} mempunyai dua sifat
 * yang tidak aman untuk layar administrasi massal: Rencana Tahun Akademik (RTA) dipilih memakai
 * konteks user yang sedang login, bukan konteks mahasiswa pada baris, dan ketika RTA tidak
 * ditemukan method umum berpindah ke kalender/tanggal server. Admin pusat dapat mempunyai scope
 * RTA yang berbeda dari prodi mahasiswa; jeda satu atau dua hari antara RTA lama dan baru juga
 * cukup untuk memajukan tahun akademik walaupun kampus belum membuka periode berikutnya.
 *
 * <p>Resolver ini memakai urutan bukti yang eksplisit. Pertama, cari RTA yang rentang tanggal dan
 * scope fakultas, prodi, program, status awal, serta angkatannya cocok dengan mahasiswa. Kedua,
 * bila tidak ada RTA yang meliputi hari ini, pertahankan RTA lampau terbaru yang masih cocok.
 * Pilihan kedua disengaja sebagai fail-closed: ketiadaan konfigurasi periode baru tidak boleh
 * diam-diam menaikkan semester, membuat kepala KRS kosong, atau mengubah status akademik. Hanya
 * bila institusi sama sekali belum memiliki RTA yang cocok, resolver memakai fallback tanggal
 * server agar instalasi lama tetap berfungsi.</p>
 *
 * <p>Perhitungan nomor semester tetap memakai rumus domain yang sama dengan sistem:
 * {@code Common.getSemester(angkatan, tahunAkademik, ganjilGenap, semesterMasukPindahan,
 * semesterMulai)}. Jadi helper ini tidak menciptakan rumus baru; yang diperbaiki adalah pemilihan
 * periode referensinya. Nilai {@code semesterMasukPindahan} nol akan dinormalisasi menjadi satu
 * oleh rumus lama. Untuk mahasiswa alih prodi, getter Mahasiswa sudah mengembalikan semester masuk
 * prodi, sehingga offset yang ditampilkan analisis sama dengan offset yang benar-benar dihitung.</p>
 *
 * <p>{@link #ringkas(Mahasiswa)} aman dipanggil oleh renderer: hanya membaca cache RTA dan tidak
 * membuka transaksi atau menulis KRS. Pemeriksaan KRS historis yang lebih mahal sengaja ditunda
 * ke {@link #analisisLengkap(Mahasiswa, RingkasanSemester)} dan hanya dijalankan setelah pengguna
 * mengklik angka semester. Pemisahan ini penting pada grid ribuan mahasiswa. Jangan mengganti
 * jalur render dengan {@code Common.singkronkanKrsMahasiswa}; membuka daftar bukan perintah bisnis
 * untuk membuat atau menghitung ulang KRS.</p>
 */
public final class SemesterMahasiswaAnalisisHelper {

	private SemesterMahasiswaAnalisisHelper() {
	}

	/**
	 * Menentukan semester efektif untuk satu baris mahasiswa tanpa query KRS dan tanpa write.
	 * Snapshot juga menyimpan hasil algoritma lama agar popup dapat menjelaskan selisih secara
	 * faktual, bukan menebak dari angka yang tampak.
	 */
	public static RingkasanSemester ringkas(Mahasiswa mahasiswa) {
		RingkasanSemester hasil = new RingkasanSemester();
		hasil.waktuAnalisis = ais.ui.util.WaktuUtil.getDate();
		if (mahasiswa == null) {
			hasil.semesterEfektif = Integer.valueOf(1);
			hasil.sumberPeriode = "Data mahasiswa tidak tersedia";
			return hasil;
		}

		hasil.semesterLama = mahasiswa.currentSemester();
		hasil.rtaKonteksUser = RencanaTahunAkademikAction
				.getCurrentRencanaTahunAkademik(Common.getCurrentUser(), hasil.waktuAnalisis);
		hasil.rtaKonteksMahasiswa = cariRtaSaatIni(mahasiswa, hasil.waktuAnalisis);

		RencanaTahunAkademik rtaEfektif = hasil.rtaKonteksMahasiswa;
		if (rtaEfektif != null) {
			hasil.sumberPeriode = "Rencana Tahun Akademik aktif yang cocok dengan konteks mahasiswa";
		} else {
			rtaEfektif = cariRtaLampauTerakhir(mahasiswa, hasil.waktuAnalisis);
			if (rtaEfektif != null) {
				hasil.menahanPeriodeTerakhir = true;
				hasil.sumberPeriode = "Rencana Tahun Akademik terakhir yang cocok (periode baru belum tersedia)";
			} else {
				hasil.memakaiFallbackTanggal = true;
				hasil.sumberPeriode = "Fallback tanggal server karena tidak ada Rencana Tahun Akademik yang cocok";
			}
		}

		hasil.rtaEfektif = rtaEfektif;
		if (rtaEfektif != null) {
			hasil.tahunAkademik = rtaEfektif.getNama();
			hasil.jenisSemester = rtaEfektif.getSemester();
		} else {
			isiPeriodeFallbackTanggal(hasil);
		}

		hasil.semesterEfektif = Common.getSemester(mahasiswa.getTahunangkatan(), hasil.tahunAkademik,
				hasil.jenisSemester, mahasiswa.getPindahKeKampusIniMasukSemester(),
				mahasiswa.getSemesterMulai());
		if (hasil.semesterEfektif == null || hasil.semesterEfektif.intValue() < 1) {
			hasil.semesterEfektif = Integer.valueOf(1);
		}
		return hasil;
	}

	/**
	 * Memperkaya snapshot sesudah pengguna mengklik link. Query ini read-only dan dibatasi 100
	 * kepala KRS terbaru. Kepala KRS kosong dibedakan dari KRS yang benar-benar mempunyai SKS,
	 * karena renderer versi lama dapat membuat kepala KRS tanpa matakuliah dan bukti tersebut tidak
	 * boleh dianggap sebagai aktivitas studi mahasiswa.
	 */
	@SuppressWarnings("unchecked")
	public static AnalisisSemester analisisLengkap(Mahasiswa mahasiswa, RingkasanSemester ringkasan) {
		AnalisisSemester hasil = new AnalisisSemester(ringkasan == null ? ringkas(mahasiswa) : ringkasan);
		if (mahasiswa == null) {
			hasil.penghambat.add("Data mahasiswa tidak tersedia pada komponen yang diklik.");
			hasil.saran.add("Muat ulang daftar mahasiswa dan pilih kembali baris yang akan diperiksa.");
			return hasil;
		}

		isiFaktaMahasiswa(hasil, mahasiswa);
		isiKesimpulanPeriode(hasil, mahasiswa);
		bacaRiwayatKrs(hasil, mahasiswa);
		isiSaran(hasil, mahasiswa);
		return hasil;
	}

	private static RencanaTahunAkademik cariRtaSaatIni(Mahasiswa mahasiswa, Date tanggal) {
		return RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(
				ambilFakultas(mahasiswa), mahasiswa.getJurusan(), null, null,
				mahasiswa.getStatusAwalMahasiswa(), mahasiswa.getTahunangkatan(), mahasiswa.getProgram(),
				tanggal, null, null);
	}

	private static RencanaTahunAkademik cariRtaLampauTerakhir(Mahasiswa mahasiswa, Date tanggal) {
		// Pemanggilan ini juga memastikan cache RTA dimuat bila aplikasi baru restart.
		cariRtaSaatIni(mahasiswa, tanggal);
		RencanaTahunAkademik terbaru = null;
		Set<String> periodeSudahDiperiksa = new HashSet<String>();
		if (Common.rencanaTahunAkademiks == null) return null;

		List<RencanaTahunAkademik> snapshot = new ArrayList<RencanaTahunAkademik>(
				Common.rencanaTahunAkademiks);
		for (RencanaTahunAkademik item : snapshot) {
			if (item == null || item.getNama() == null || item.getSemester() == null) continue;
			String key = item.getNama() + "|" + item.getSemester();
			if (!periodeSudahDiperiksa.add(key)) continue;

			RencanaTahunAkademik kandidat = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(
					ambilFakultas(mahasiswa), mahasiswa.getJurusan(), null, null,
					mahasiswa.getStatusAwalMahasiswa(), mahasiswa.getTahunangkatan(), mahasiswa.getProgram(),
					null, item.getNama(), item.getSemester());
			if (kandidat == null || kandidat.getTanggalMulai() == null
					|| kandidat.getTanggalMulai().after(tanggal)) continue;
			if (terbaru == null || tanggalPembanding(kandidat).after(tanggalPembanding(terbaru))) {
				terbaru = kandidat;
			}
		}
		return terbaru;
	}

	private static Date tanggalPembanding(RencanaTahunAkademik rta) {
		return rta.getTanggalSampai() == null ? rta.getTanggalMulai() : rta.getTanggalSampai();
	}

	private static Fakultas ambilFakultas(Mahasiswa mahasiswa) {
		return mahasiswa == null || mahasiswa.getJurusan() == null ? null
				: mahasiswa.getJurusan().getFakultas();
	}

	private static void isiPeriodeFallbackTanggal(RingkasanSemester hasil) {
		Calendar kalender = ais.ui.util.WaktuUtil.getCalendar();
		kalender.setTime(hasil.waktuAnalisis);
		int tahun = kalender.get(Calendar.YEAR);
		int bulan = kalender.get(Calendar.MONTH);
		hasil.jenisSemester = bulan >= Calendar.JUNE ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		// Pertahankan persis batas warisan Common: tahun akademik berganti setelah bulan Juni.
		hasil.tahunAkademik = bulan > Calendar.JUNE ? tahun + "/" + (tahun + 1)
				: (tahun - 1) + "/" + tahun;
	}

	private static void isiFaktaMahasiswa(AnalisisSemester hasil, Mahasiswa mahasiswa) {
		RingkasanSemester r = hasil.ringkasan;
		hasil.fakta.put("Mahasiswa", aman(mahasiswa.getNim()) + " - " + aman(mahasiswa.getNama()));
		hasil.fakta.put("Program studi", mahasiswa.getJurusan() == null ? "-" : aman(mahasiswa.getJurusan().getNama()));
		hasil.fakta.put("Program / status awal", aman(mahasiswa.getProgram()) + " / "
				+ (mahasiswa.getStatusAwalMahasiswa() == null ? "-" : aman(mahasiswa.getStatusAwalMahasiswa().getNama())));
		hasil.fakta.put("Waktu analisis", Common.dateFormat4.get().format(r.waktuAnalisis));
		hasil.fakta.put("Tahun angkatan", aman(mahasiswa.getTahunangkatan()));
		hasil.fakta.put("Mulai pada", aman(mahasiswa.getSemesterMulai()));
		hasil.fakta.put("Semester masuk pindahan/alih prodi",
				String.valueOf(mahasiswa.getPindahKeKampusIniMasukSemester()));
		hasil.fakta.put("Periode efektif", aman(r.tahunAkademik) + " " + aman(r.jenisSemester));
		hasil.fakta.put("Sumber periode efektif", aman(r.sumberPeriode));
		hasil.fakta.put("RTA cocok untuk mahasiswa", deskripsiRta(r.rtaKonteksMahasiswa));
		hasil.fakta.put("RTA yang terbaca dari konteks user/login", deskripsiRta(r.rtaKonteksUser));
		hasil.fakta.put("Semester hasil resolver lama", aman(r.semesterLama));
		hasil.fakta.put("Semester efektif pada daftar", aman(r.semesterEfektif));
		hasil.fakta.put("Rumus", "semester masuk efektif + selisih periode setengah-tahunan sejak angkatan");
		hasil.fakta.put("Efek saat membuka daftar", "Baca saja; tidak membuat dan tidak menyinkronkan KRS");
	}

	private static void isiKesimpulanPeriode(AnalisisSemester hasil, Mahasiswa mahasiswa) {
		RingkasanSemester r = hasil.ringkasan;
		boolean berbeda = r.semesterLama != null && !r.semesterLama.equals(r.semesterEfektif);
		if (berbeda) {
			hasil.keputusanUtama = "Semester " + r.semesterEfektif
					+ " dipakai karena periode dihitung untuk konteks mahasiswa. Perhitungan lama menghasilkan semester "
					+ r.semesterLama + " karena memakai konteks user/login atau fallback tanggal server.";
			hasil.penghambat.add("Resolver lama dan resolver konteks mahasiswa berbeda: " + r.semesterLama
					+ " berbanding " + r.semesterEfektif + ".");
		} else {
			hasil.keputusanUtama = "Semester " + r.semesterEfektif + " konsisten dengan periode "
					+ r.tahunAkademik + " " + r.jenisSemester + " dan data awal studi mahasiswa.";
			hasil.kondisiBenar.add("Resolver lama dan resolver konteks mahasiswa menghasilkan angka yang sama.");
		}

		if (r.menahanPeriodeTerakhir) {
			hasil.penghambat.add("Tidak ada Rencana Tahun Akademik yang rentang tanggalnya meliputi hari ini untuk scope mahasiswa.");
			hasil.kondisiBenar.add("Sistem menahan semester pada RTA terakhir yang cocok agar jeda konfigurasi tidak menaikkan semester secara otomatis.");
		} else if (r.memakaiFallbackTanggal) {
			hasil.penghambat.add("Tidak ditemukan RTA aktif maupun RTA lampau yang cocok; hasil terpaksa mengikuti tanggal server.");
		} else {
			hasil.kondisiBenar.add("Ditemukan RTA aktif dengan rentang tanggal dan scope yang cocok untuk mahasiswa ini.");
		}

		if (!samaPeriode(r.rtaKonteksUser, r.rtaKonteksMahasiswa)) {
			hasil.penghambat.add("Periode pada scope user/login berbeda dari periode pada prodi/angkatan mahasiswa. Ini penyebab utama angka antarhalaman dapat berbeda.");
		}
		if (mahasiswa.getPindahKeKampusIniMasukSemester() != null
				&& mahasiswa.getPindahKeKampusIniMasukSemester().intValue() > 1) {
			hasil.perhatian.add("Mahasiswa mempunyai offset semester masuk "
					+ mahasiswa.getPindahKeKampusIniMasukSemester()
					+ "; angka ini sengaja menggeser hasil dan perlu divalidasi bila mahasiswa bukan pindahan/alih prodi.");
		}
		if (r.memakaiFallbackTanggal) {
			Calendar c = ais.ui.util.WaktuUtil.getCalendar();
			c.setTime(r.waktuAnalisis);
			if (c.get(Calendar.MONTH) == Calendar.JUNE) {
				hasil.perhatian.add("Khusus bulan Juni, fallback warisan menandai semester Ganjil tetapi tahun akademik belum berganti. RTA eksplisit wajib dipakai untuk menghindari kombinasi ambigu ini.");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void bacaRiwayatKrs(AnalisisSemester hasil, Mahasiswa mahasiswa) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Mahasiswa db = (Mahasiswa) session.get(Mahasiswa.class, mahasiswa.getId());
			if (db == null) {
				hasil.perhatian.add("Mahasiswa tidak ditemukan lagi di database saat riwayat KRS dibaca.");
				return;
			}
			Criteria criteria = session.createCriteria(KrsMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", db))
					.add(Restrictions.isNull("semesterPendek"))
					.addOrder(Order.desc("semester")).addOrder(Order.desc("id")).setMaxResults(100);
			List<KrsMahasiswa> daftar = criteria.list();
			int berisiSks = 0;
			Integer semesterTertinggi = null;
			Integer semesterAktualTertinggi = null;
			for (KrsMahasiswa krs : daftar) {
				if (krs.getSemester() != null && (semesterTertinggi == null
						|| krs.getSemester().intValue() > semesterTertinggi.intValue())) {
					semesterTertinggi = krs.getSemester();
				}
				if (krs.getSksYangDiambil().intValue() > 0) {
					berisiSks++;
					if (semesterAktualTertinggi == null
							|| krs.getSemester().intValue() > semesterAktualTertinggi.intValue()) {
						semesterAktualTertinggi = krs.getSemester();
					}
				}
			}
			hasil.fakta.put("Kepala KRS reguler tersimpan", daftar.size() + " baris");
			hasil.fakta.put("KRS yang mempunyai SKS", berisiSks + " baris");
			hasil.fakta.put("Semester KRS tertinggi", aman(semesterTertinggi));
			hasil.fakta.put("Semester tertinggi dengan SKS", aman(semesterAktualTertinggi));

			if (daftar.isEmpty()) {
				hasil.perhatian.add("Belum ada kepala KRS reguler; angka semester sepenuhnya berasal dari periode dan data awal studi, bukan dari KRS.");
			} else if (berisiSks == 0) {
				hasil.perhatian.add("Ada kepala KRS, tetapi semuanya 0 SKS. Baris kosong bukan bukti mahasiswa pernah mengambil KRS.");
			}
			if (semesterAktualTertinggi != null && semesterAktualTertinggi.intValue() > hasil.ringkasan.semesterEfektif.intValue()) {
				hasil.penghambat.add("Ditemukan KRS berisi SKS pada semester " + semesterAktualTertinggi
						+ ", lebih tinggi dari semester efektif " + hasil.ringkasan.semesterEfektif
						+ ". Periksa periode KRS atau data angkatan/semester masuk sebelum mengoreksi angka.");
			}
		} catch (Exception e) {
			hasil.perhatian.add("Riwayat KRS tidak dapat dibaca saat analisis; kesimpulan periode tetap tersedia.");
			ais.common.ErrorAuditUtil.record(e, "analisis semester mahasiswa id=" + mahasiswa.getId());
		} finally {
			Common.closeNativeSessionQuietly(session);
		}
	}

	private static void isiSaran(AnalisisSemester hasil, Mahasiswa mahasiswa) {
		RingkasanSemester r = hasil.ringkasan;
		if (r.rtaKonteksMahasiswa == null) {
			hasil.saran.add("Buka master Rencana Tahun Akademik dan buat/perpanjang periode yang mencakup tanggal hari ini untuk prodi "
					+ (mahasiswa.getJurusan() == null ? "mahasiswa" : mahasiswa.getJurusan().getNama()) + ".");
			hasil.saran.add("Pastikan Tahun Akademik, Ganjil/Genap, tanggal mulai-sampai, program, status awal, dan angkatan tidak saling bertentangan.");
		}
		if (!samaPeriode(r.rtaKonteksUser, r.rtaKonteksMahasiswa)) {
			hasil.saran.add("Periksa scope RTA milik admin dan scope mahasiswa. Baris global boleh kosong pada dimensi scope; baris khusus harus cocok tepat dengan prodi/program/angkatan.");
		}
		hasil.saran.add("Validasi Tahun Angkatan, Semester Mulai, serta Semester Masuk Pindahan/Alih Prodi pada biodata mahasiswa.");
		hasil.saran.add("Setelah konfigurasi diperbaiki, klik Cari/Refresh pada daftar. Membuka daftar tidak lagi membuat KRS baru.");
		hasil.saran.add("Jangan mengubah nomor semester langsung hanya untuk menyamakan tampilan; koreksi sumber periode atau data awal studi agar seluruh modul tetap konsisten.");
	}

	private static boolean samaPeriode(RencanaTahunAkademik a, RencanaTahunAkademik b) {
		if (a == null || b == null) return a == b;
		return aman(a.getNama()).equalsIgnoreCase(aman(b.getNama()))
				&& aman(a.getSemester()).equalsIgnoreCase(aman(b.getSemester()));
	}

	private static String deskripsiRta(RencanaTahunAkademik rta) {
		if (rta == null) return "Tidak ditemukan";
		String tanggal = (rta.getTanggalMulai() == null ? "-" : Common.dateFormat2.get().format(rta.getTanggalMulai()))
				+ " s.d. "
				+ (rta.getTanggalSampai() == null ? "-" : Common.dateFormat2.get().format(rta.getTanggalSampai()));
		return aman(rta.getNama()) + " " + aman(rta.getSemester()) + " (" + tanggal + ", "
				+ deskripsiScope(rta) + ")";
	}

	private static String deskripsiScope(RencanaTahunAkademik rta) {
		List<String> scope = new ArrayList<String>();
		if (rta.getFakultas() != null) scope.add("fakultas=" + rta.getFakultas().getNama());
		if (rta.getJurusan() != null) scope.add("prodi=" + rta.getJurusan().getNama());
		if (rta.getProgram() != null) scope.add("program=" + rta.getProgram());
		if (rta.getStatusAwalMahasiswa() != null) scope.add("status awal=" + rta.getStatusAwalMahasiswa().getNama());
		if (rta.getTahunAngkatan() != null) scope.add("angkatan=" + rta.getTahunAngkatan());
		if (scope.isEmpty()) return "scope global";
		StringBuilder result = new StringBuilder();
		for (String value : scope) {
			if (result.length() > 0) result.append(", ");
			result.append(value);
		}
		return result.toString();
	}

	private static String aman(Object value) {
		return value == null || value.toString().trim().isEmpty() ? "-" : value.toString().trim();
	}

	/** Snapshot murah yang dipakai renderer dan kemudian diteruskan ke popup. */
	public static final class RingkasanSemester {
		private Date waktuAnalisis;
		private Integer semesterEfektif;
		private Integer semesterLama;
		private String tahunAkademik;
		private String jenisSemester;
		private String sumberPeriode;
		private boolean menahanPeriodeTerakhir;
		private boolean memakaiFallbackTanggal;
		private RencanaTahunAkademik rtaKonteksMahasiswa;
		private RencanaTahunAkademik rtaKonteksUser;
		private RencanaTahunAkademik rtaEfektif;

		public Integer getSemesterEfektif() { return semesterEfektif; }
		public Integer getSemesterLama() { return semesterLama; }
		public String getTahunAkademik() { return tahunAkademik; }
		public String getJenisSemester() { return jenisSemester; }
		public String getSumberPeriode() { return sumberPeriode; }
		public boolean isMenahanPeriodeTerakhir() { return menahanPeriodeTerakhir; }
		public boolean isMemakaiFallbackTanggal() { return memakaiFallbackTanggal; }
		public RencanaTahunAkademik getRtaEfektif() { return rtaEfektif; }
	}

	/** Data polos untuk popup; UI tidak ikut menentukan kesimpulan algoritma. */
	public static final class AnalisisSemester {
		private final RingkasanSemester ringkasan;
		private String keputusanUtama = "Data semester dianalisis dari periode, biodata awal studi, dan riwayat KRS.";
		private final LinkedHashMap<String, String> fakta = new LinkedHashMap<String, String>();
		private final List<String> penghambat = new ArrayList<String>();
		private final List<String> kondisiBenar = new ArrayList<String>();
		private final List<String> perhatian = new ArrayList<String>();
		private final List<String> saran = new ArrayList<String>();

		private AnalisisSemester(RingkasanSemester ringkasan) { this.ringkasan = ringkasan; }
		public RingkasanSemester getRingkasan() { return ringkasan; }
		public String getKeputusanUtama() { return keputusanUtama; }
		public LinkedHashMap<String, String> getFakta() { return fakta; }
		public List<String> getPenghambat() { return penghambat; }
		public List<String> getKondisiBenar() { return kondisiBenar; }
		public List<String> getPerhatian() { return perhatian; }
		public List<String> getSaran() { return saran; }
	}
}
