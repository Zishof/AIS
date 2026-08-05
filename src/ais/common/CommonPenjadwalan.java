package ais.common;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.KonfigurasiKalenderAkademik;

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
		Konfigurasi konfigurasi = getKonfigurasi(tahunAkademik, jenisSemester, semesterPendek);
		if (konfigurasi == null || !Konfigurasi.TIDAK_AKTIF.equals(konfigurasi.getNilai())) {
			// Aktif (atau default aktif) → tidak diblokir.
			return false;
		}
		// Nilai konfigurasi TIDAK_AKTIF; beri kesempatan Kalender Akademik membukanya.
		if (konfigurasi.getId() != null && dibukaOlehKalenderAkademik(konfigurasi)) {
			return false;
		}
		return true;
	}

	/**
	 * Apakah ada kegiatan Kalender Akademik yang sedang berlangsung hari ini dan
	 * menaut ke konfigurasi penjadwalan {@code konfigurasi} dengan aksi mulai = AKTIF.
	 * Batas tanggal mengikuti pola {@code KonfigurasiKalenderAkademikProcessor} agar
	 * konsisten (tanggalMulai &le; hari ini &le; tanggalSelesai).
	 */
	private static boolean dibukaOlehKalenderAkademik(Konfigurasi konfigurasi) {
		try {
			Calendar c = ais.ui.util.WaktuUtil.getCalendar();
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			Date awalHariIni = c.getTime();

			c.set(Calendar.HOUR_OF_DAY, 23);
			c.set(Calendar.MINUTE, 59);
			c.set(Calendar.SECOND, 59);
			c.set(Calendar.MILLISECOND, 999);
			Date akhirHariIni = c.getTime();

			Object jml = HibernateUtil.currentSession().createCriteria(KonfigurasiKalenderAkademik.class)
					.add(Restrictions.eq("konfigurasi", konfigurasi))
					.add(Restrictions.eq("padaSaatMulaiBerubahMenjadi", Konfigurasi.AKTIF))
					.createAlias("kalenderAkademik", "ka")
					.add(Restrictions.le("ka.tanggalMulai", awalHariIni))
					.add(Restrictions.ge("ka.tanggalSelesai", akhirHariIni))
					.add(Restrictions.or(Restrictions.isNull("ka.aktif"), Restrictions.eq("ka.aktif", Boolean.TRUE)))
					.setProjection(Projections.rowCount()).uniqueResult();
			return jml != null && ((Number) jml).longValue() > 0;
		} catch (Exception e) {
			// Jangan sampai kendala pengecekan kalender menghentikan gate; ikuti nilai konfigurasi apa adanya.
			return false;
		}
	}

}
