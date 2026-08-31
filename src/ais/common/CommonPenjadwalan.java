package ais.common;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KonfigurasiKalenderAkademik;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.database.model.Tbmuser;

/**
 * Utilitas bersama AIS untuk common penjadwalan. Kelas ini mengonsolidasikan operasi lintas
 * layar/service yang benar-benar satu domain agar pemanggil tidak membuat helper dengan fungsi
 * paralel.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getKonfigurasi()}); operasi domain
 * lain ({@code apakahPenjadwalanTidakAktif()}, {@code apakahPenjadwalanTidakAktif()}, {@code
 * apakahPenjadwalanTidakAktif()}, {@code statusLingkupKalenderAkademik()}, {@code jumlah()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class CommonPenjadwalan {

	public static Konfigurasi getKonfigurasi(String tahunAkademik, String jenisSemester, Integer semesterPendek) {
		if (jenisSemester == null || jenisSemester.trim().isEmpty() || tahunAkademik == null
				|| tahunAkademik.trim().isEmpty()) {
			return new Konfigurasi(semesterPendek == null ? Konfigurasi.PENJADWALAN : Konfigurasi.PENJADWALAN_SP,
					Konfigurasi.TIDAK_AKTIF);
		}

		Konfigurasi konfigurasi = (Konfigurasi) ConstantValues
				.simpleObject(
						HibernateUtil.currentSession().createCriteria(Konfigurasi.class).addOrder(Order.desc("id"))
								.add(Restrictions.eq("info1", jenisSemester))
								.add(Restrictions.eq("nama",
										semesterPendek == null ? Konfigurasi.PENJADWALAN : Konfigurasi.PENJADWALAN_SP))
								.add(Restrictions.eq("tahunAkademik", tahunAkademik)).setMaxResults(1),
						Konfigurasi.class);
		if (konfigurasi == null) {
			konfigurasi = new Konfigurasi();
			konfigurasi.setKeterangan("Digunakan untuk mengaktifkan / tidak mengaktifkan penjadwalan");
			konfigurasi.setNama(semesterPendek == null ? Konfigurasi.PENJADWALAN : Konfigurasi.PENJADWALAN_SP);
			konfigurasi.setTahunAkademik(tahunAkademik);
			konfigurasi.setNilai(Konfigurasi.AKTIF);
			konfigurasi.setInfo1(jenisSemester);
			HibernateUtil.currentSession().save(konfigurasi);
		}
		return konfigurasi;
	}

	/**
	 * Gerbang tunggal untuk mengecek apakah penjadwalan perkuliahan HARUS DIBLOKIR
	 * (belum aktif) untuk Tahun Akademik + Jenis Semester tertentu.
	 *
	 * <p>Selain membaca nilai konfigurasi {@code penjadwalan} / {@code penjadwalan_sp},
	 * method ini juga menghormati <b>Kalender Akademik</b>: apabila terdapat kegiatan
	 * pada Kalender Akademik yang menaut ke konfigurasi penjadwalan ini, sedang
	 * <b>BERLANGSUNG</b> (tanggal hari ini berada dalam rentang tanggal mulai s/d
	 * selesai), aktif, dan beraksi "pada saat mulai → AKTIF", maka penjadwalan dianggap
	 * DIBUKA oleh kalender — tidak bergantung pada apakah background processor
	 * ({@code KonfigurasiKalenderAkademikProcessor}) sudah sempat berjalan untuk
	 * memutakhirkan nilai konfigurasi. Dengan demikian, begitu kalender akademik
	 * penyusunan jadwal diaktifkan, penambahan/perubahan jadwal langsung diizinkan.
	 *
	 * @return {@code true} bila penjadwalan belum aktif (proses harus diblokir).
	 */
	public static boolean apakahPenjadwalanTidakAktif(String tahunAkademik, String jenisSemester,
			Integer semesterPendek) {
		Tbmuser pengguna = Common.getCurrentUser();
		Fakultas fakultas = null;
		Jurusan jurusan = null;
		String program = null;
		try {
			if (pengguna != null) {
				fakultas = pengguna.ambilFakultas();
				jurusan = pengguna.ambilJurusan();
				Program programPengguna = pengguna.ambilProgram();
				program = programPengguna == null ? null : programPengguna.getNama();
			}
		} catch (Exception e) {
			// Relasi pengguna yang belum lengkap diperlakukan tanpa lingkup, sehingga kalender
			// fakultas/prodi tidak pernah terbuka secara tidak sengaja.
		}
		return apakahPenjadwalanTidakAktif(tahunAkademik, jenisSemester, semesterPendek, fakultas, jurusan,
				program);
	}

	/**
	 * Memeriksa periode penjadwalan terhadap fakultas/prodi/program tujuan. Kalender
	 * yang dibatasi ke Fakultas A tidak boleh membuka konfigurasi global bagi
	 * Fakultas B. Bila ada kalender terbatas yang menaut ke konfigurasi, kalender
	 * tersebut menjadi sumber kebenaran meskipun processor lama sudah telanjur
	 * mengubah nilai konfigurasi global menjadi AKTIF.
	 */
	public static boolean apakahPenjadwalanTidakAktif(String tahunAkademik, String jenisSemester,
			Integer semesterPendek, Fakultas fakultas, Jurusan jurusan, String program) {
		Konfigurasi konfigurasi = getKonfigurasi(tahunAkademik, jenisSemester, semesterPendek);
		if (konfigurasi == null) {
			return true;
		}

		LingkupKalender lingkup = statusLingkupKalenderAkademik(konfigurasi, fakultas, jurusan, program);
		if (lingkup.gagalMembaca) {
			return true;
		}
		if (lingkup.dikelolaKalenderTerbatas) {
			return !lingkup.adaKalenderAktifYangSesuai;
		}
		if (lingkup.adaKalenderAktifYangSesuai) {
			return false;
		}
		return Konfigurasi.TIDAK_AKTIF.equals(konfigurasi.getNilai());
	}

	/** Memakai lingkup jurusan/program dari jadwal yang sedang ditambah atau diedit. */
	public static boolean apakahPenjadwalanTidakAktif(String tahunAkademik, String jenisSemester,
			Integer semesterPendek, Perkuliahan perkuliahan) {
		if (perkuliahan != null && perkuliahan.getId() != null) {
			// Form edit dapat menyimpan instance detached. Muat ulang pada currentSession
			// agar relasi Jurusan/Fakultas lazy tidak diakses dari session yang sudah tutup.
			Perkuliahan managed = (Perkuliahan) HibernateUtil.currentSession().get(Perkuliahan.class,
					perkuliahan.getId());
			if (managed != null) {
				perkuliahan = managed;
			}
		}
		Jurusan jurusan = perkuliahan == null ? null : perkuliahan.getJurusan();
		Fakultas fakultas = jurusan == null ? null : jurusan.getFakultas();
		String program = perkuliahan == null ? null : perkuliahan.getProgram();
		return apakahPenjadwalanTidakAktif(tahunAkademik, jenisSemester, semesterPendek, fakultas, jurusan,
				program);
	}

	/**
	 * Apakah ada kegiatan Kalender Akademik yang sedang berlangsung hari ini dan
	 * menaut ke konfigurasi penjadwalan {@code konfigurasi} dengan aksi mulai = AKTIF.
	 * Batas tanggal mengikuti pola {@code KonfigurasiKalenderAkademikProcessor} agar
	 * konsisten (tanggalMulai &le; hari ini &le; tanggalSelesai).
	 */
	private static LingkupKalender statusLingkupKalenderAkademik(Konfigurasi konfigurasi, Fakultas fakultas,
			Jurusan jurusan, String program) {
		LingkupKalender hasil = new LingkupKalender();
		if (konfigurasi == null || konfigurasi.getId() == null) {
			return hasil;
		}
		try {
			Calendar c = ais.ui.util.WaktuUtil.getCalendar();
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			Date awalHariIni = c.getTime();

			Object jmlTerbatas = HibernateUtil.currentSession().createCriteria(KonfigurasiKalenderAkademik.class)
					.add(Restrictions.eq("konfigurasi", konfigurasi))
					.add(Restrictions.eq("padaSaatMulaiBerubahMenjadi", Konfigurasi.AKTIF))
					.createAlias("kalenderAkademik", "ka")
					.add(Restrictions.disjunction().add(Restrictions.isNotNull("ka.fakultas"))
							.add(Restrictions.isNotNull("ka.jurusan")).add(Restrictions.isNotNull("ka.jenjang"))
							.add(Restrictions.isNotNull("ka.program")))
					.setProjection(Projections.rowCount()).uniqueResult();
			hasil.dikelolaKalenderTerbatas = jumlah(jmlTerbatas) > 0;

			org.hibernate.Criteria berlaku = HibernateUtil.currentSession()
					.createCriteria(KonfigurasiKalenderAkademik.class)
					.add(Restrictions.eq("konfigurasi", konfigurasi))
					.add(Restrictions.eq("padaSaatMulaiBerubahMenjadi", Konfigurasi.AKTIF))
					.createAlias("kalenderAkademik", "ka")
					.add(Restrictions.le("ka.tanggalMulai", awalHariIni))
					.add(Restrictions.ge("ka.tanggalSelesai", awalHariIni))
					.add(Restrictions.or(Restrictions.isNull("ka.aktif"), Restrictions.eq("ka.aktif", Boolean.TRUE)));

			berlaku.add(fakultas == null ? Restrictions.isNull("ka.fakultas")
					: Restrictions.or(Restrictions.isNull("ka.fakultas"), Restrictions.eq("ka.fakultas", fakultas)));
			berlaku.add(jurusan == null ? Restrictions.isNull("ka.jurusan")
					: Restrictions.or(Restrictions.isNull("ka.jurusan"), Restrictions.eq("ka.jurusan", jurusan)));
			berlaku.add(jurusan == null || jurusan.getJenjang() == null ? Restrictions.isNull("ka.jenjang")
					: Restrictions.or(Restrictions.isNull("ka.jenjang"),
							Restrictions.eq("ka.jenjang", jurusan.getJenjang())));
			berlaku.add(program == null || program.trim().isEmpty() ? Restrictions.isNull("ka.program")
					: Restrictions.or(Restrictions.isNull("ka.program"), Restrictions.eq("ka.program", program.trim())));

			Object jmlBerlaku = berlaku.setProjection(Projections.rowCount()).uniqueResult();
			hasil.adaKalenderAktifYangSesuai = jumlah(jmlBerlaku) > 0;
		} catch (Exception e) {
			// Fail closed untuk kalender terbatas: jangan membuka fakultas lain ketika relasi
			// lingkup gagal dibaca.
			hasil.gagalMembaca = true;
		}
		return hasil;
	}

	private static long jumlah(Object nilai) {
		return nilai instanceof Number ? ((Number) nilai).longValue() : 0L;
	}

	private static final class LingkupKalender {
		private boolean dikelolaKalenderTerbatas;
		private boolean adaKalenderAktifYangSesuai;
		private boolean gagalMembaca;
	}

}
