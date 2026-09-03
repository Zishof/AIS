package ais.action.master.payroll.util;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.payroll.JenisShiftPunyaPegawai;
import ais.database.model.payroll.LiburNasional;
import ais.database.model.payroll.LiburRutin;
import ais.database.model.sekolah.Siswa;

/** Utilitas modul payroll/presensi untuk resolusi status kehadiran harian dan pencocokan shift kerja pegawai berdasarkan jam masuk aktual. */
public class CommonPayroll {

	/**
	 * Mengambil (atau membuat bila belum ada) baris {@link StatuskehadiranKaryawanHarian} untuk
	 * satu tanggal dan satu subjek (pegawai/mahasiswa/siswa — tepat satu yang diisi, diprioritaskan
	 * mahasiswa lalu siswa lalu pegawai). Baris baru diinisialisasi berstatus
	 * {@link ConstantValues#BELUM_ABSEN} tanpa jam masuk/pulang; baris yang sudah ada maupun baru
	 * selalu diperbarui dengan info libur nasional ({@link LiburNasional#ambilLiburNasional}) dan
	 * libur rutin (berdasarkan hari dalam seminggu) terkini.
	 */
	public static StatuskehadiranKaryawanHarian getDefaultStatuskehadiranKaryawanHarian(Date tanggal, Pegawai pegawai,
			Mahasiswa mahasiswa, Siswa siswa) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal);
		final Integer bln = calendar.get(Calendar.MONTH) + 1;
		final Integer thn = calendar.get(Calendar.YEAR);
		final Integer tgl = calendar.get(Calendar.DATE);
		final Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

		Session session = HibernateUtil.currentSession();
		LiburNasional liburNasional = LiburNasional.ambilLiburNasional(tanggal);

		LiburRutin liburRutin = (LiburRutin) session.createCriteria(LiburRutin.class).add(Restrictions.eq("hari", hari))
				.setMaxResults(1).uniqueResult();

		StatuskehadiranKaryawanHarian karyawanHarian = (StatuskehadiranKaryawanHarian) session
				.createCriteria(StatuskehadiranKaryawanHarian.class).add(Restrictions.eq("tanggal", tanggal))
				.add(mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa)
						: siswa != null ? Restrictions.eq("siswa", siswa) : Restrictions.eq("pegawai", pegawai))
				.setMaxResults(1).uniqueResult();

		StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian;

		if (karyawanHarian == null) {
			statuskehadiranKaryawanHarian = new StatuskehadiranKaryawanHarian();
			statuskehadiranKaryawanHarian.setBulan(bln);
			statuskehadiranKaryawanHarian.setTahun(thn);
			statuskehadiranKaryawanHarian.setTgl(tgl);
			statuskehadiranKaryawanHarian.setPegawai(pegawai);
			statuskehadiranKaryawanHarian.setMahasiswa(mahasiswa);
			statuskehadiranKaryawanHarian.setSiswa(siswa);
			statuskehadiranKaryawanHarian.setKeterangan("");
			statuskehadiranKaryawanHarian.setMasukjam(null);
			statuskehadiranKaryawanHarian.setPulangJam(null);
			statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.BELUM_ABSEN);
			statuskehadiranKaryawanHarian.setTanggal(tanggal);
			statuskehadiranKaryawanHarian.setMinggu(hari);
			statuskehadiranKaryawanHarian.setLiburNasional(liburNasional);
			statuskehadiranKaryawanHarian.setLiburRutin(liburRutin);
			session.save(statuskehadiranKaryawanHarian);
			session.flush();
		} else {
			statuskehadiranKaryawanHarian = karyawanHarian;
			statuskehadiranKaryawanHarian.setLiburNasional(liburNasional);
			statuskehadiranKaryawanHarian.setLiburRutin(liburRutin);
		}

		return statuskehadiranKaryawanHarian;
	}

	/**
	 * Mencocokkan jam masuk aktual ({@code mulai}) pegawai ke {@link DetailJenisShiftPegawai}
	 * (detail slot shift) yang paling mendekati, di antara jenis shift yang berlaku untuk pegawai
	 * tersebut pada {@code tanggal} (dicari dari {@link JenisShiftPunyaPegawai} yang masih berlaku
	 * berdasarkan rentang {@code berlakuMulai}/{@code berlakuSampai}). Pencarian dilakukan dua
	 * arah: kandidat shift dengan jam mulai sebelum ({@code next}) dan sesudah/sama ({@code back})
	 * jam masuk aktual, masing-masing diprioritaskan cocok dengan {@code hari} spesifik lalu jatuh
	 * kembali ke shift tanpa hari spesifik (berlaku semua hari) bila tidak ada. Di antara kedua
	 * kandidat, yang dipilih adalah yang selisih jaraknya (dalam menit sejak tengah malam) ke jam
	 * masuk aktual paling kecil.
	 *
	 * @return detail shift yang paling cocok dengan jam masuk aktual, atau {@code null} bila {@code mulai} kosong atau tidak ada kandidat yang cocok
	 */
	public static DetailJenisShiftPegawai getDetailJenisShiftPegawai(Pegawai pegawai, Date mulai, Date tanggal,
			String hari) {

		DetailJenisShiftPegawai detailJenisShiftPegawai = null;

		if (mulai != null) {

			try {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(mulai);
				Double jarakMulai = (double) (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE));

				Session session = HibernateUtil.currentNativeSession();
				@SuppressWarnings("unchecked")
				List<Long> ids = session.createCriteria(JenisShiftPunyaPegawai.class)
						.add(Restrictions.eq("pegawai", pegawai)).createAlias("jenisShiftPegawai", "jenisShiftPegawai")
						.add(Restrictions.or(Restrictions.isNull("jenisShiftPegawai.aktif"),
								Restrictions.eq("jenisShiftPegawai.aktif", true)))

						.add(Restrictions.le("jenisShiftPegawai.berlakuMulai", tanggal))
						.addOrder(Order.desc("jenisShiftPegawai.berlakuMulai"))
						.add(Restrictions.or(Restrictions.isNull("jenisShiftPegawai.berlakuSampai"),
								Restrictions.ge("jenisShiftPegawai.berlakuSampai", tanggal)))
						.setProjection(Projections.groupProperty("jenisShiftPegawai.id")).setMaxResults(1).list();

				DetailJenisShiftPegawai next = (DetailJenisShiftPegawai) ConstantValues.simpleObject(session
						.createCriteria(DetailJenisShiftPegawai.class)

						.createAlias("jenisShiftPegawai", "jenisShiftPegawai")
						.add(Restrictions.or(Restrictions.isNull("jenisShiftPegawai.aktif"),
								Restrictions.eq("jenisShiftPegawai.aktif", true)))

						.add(Restrictions.sqlRestriction("ke<=jumlahshift")).add(Restrictions.eq("hari", hari))
						.add(ids.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.in("jenisShiftPegawai.id", ids))
						.add(Restrictions.sqlRestriction("mulai <= time '" + Common.timeFormat.get().format(mulai) + "'"))
						.addOrder(Order.desc("mulai")).setMaxResults(1), DetailJenisShiftPegawai.class);
				if (next == null) {
					next = (DetailJenisShiftPegawai) ConstantValues
							.simpleObject(
									session.createCriteria(DetailJenisShiftPegawai.class)
											.createAlias("jenisShiftPegawai", "jenisShiftPegawai")
											.add(Restrictions.or(Restrictions.isNull("jenisShiftPegawai.aktif"),
													Restrictions.eq("jenisShiftPegawai.aktif", true)))

											.add(Restrictions.sqlRestriction("ke<=jumlahshift"))
											.add(Restrictions.isNull("hari"))
											.add(ids.isEmpty() ? Restrictions.sqlRestriction("false")
													: Restrictions.in("jenisShiftPegawai.id", ids))
											.add(Restrictions.sqlRestriction(
													"mulai <= time '" + Common.timeFormat.get().format(mulai) + "'"))
											.addOrder(Order.desc("mulai")).setMaxResults(1),
									DetailJenisShiftPegawai.class);
				}

				DetailJenisShiftPegawai back = (DetailJenisShiftPegawai) ConstantValues.simpleObject(session
						.createCriteria(DetailJenisShiftPegawai.class)
						.createAlias("jenisShiftPegawai", "jenisShiftPegawai")
						.add(Restrictions.or(Restrictions.isNull("jenisShiftPegawai.aktif"),
								Restrictions.eq("jenisShiftPegawai.aktif", true)))

						.add(Restrictions.sqlRestriction("ke<=jumlahshift")).add(Restrictions.eq("hari", hari))
						.add(ids.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.in("jenisShiftPegawai.id", ids))
						.add(Restrictions.sqlRestriction("mulai >= time '" + Common.timeFormat.get().format(mulai) + "'"))
						.addOrder(Order.asc("mulai")).setMaxResults(1), DetailJenisShiftPegawai.class);
				if (back == null) {
					back = (DetailJenisShiftPegawai) ConstantValues
							.simpleObject(
									session.createCriteria(DetailJenisShiftPegawai.class)
											.createAlias("jenisShiftPegawai", "jenisShiftPegawai")
											.add(Restrictions.or(Restrictions.isNull("jenisShiftPegawai.aktif"),
													Restrictions.eq("jenisShiftPegawai.aktif", true)))

											.add(Restrictions.sqlRestriction("ke<=jumlahshift"))
											.add(Restrictions.isNull("hari"))
											.add(ids.isEmpty() ? Restrictions.sqlRestriction("false")
													: Restrictions.in("jenisShiftPegawai.id", ids))
											.add(Restrictions.sqlRestriction(
													"mulai >= time '" + Common.timeFormat.get().format(mulai) + "'"))
											.addOrder(Order.asc("mulai")).setMaxResults(1),
									DetailJenisShiftPegawai.class);
				}

				if (next != null) {
//					System.out.println("1. jenisShiftPegawai ids = " + ids + " mulai = " + Common.timeFormat.get().format(mulai)
//							+ ", next = " + next + ", next jarak mulai = " + next.getJarakMulai()
//							+ ", current jarak mulai = " + jarakMulai);
				}

				if (back != null) {
//					System.out.println("2. jenisShiftPegawai ids = " + ids + " mulai = " + Common.timeFormat.get().format(mulai)
//							+ ", back = " + back + ", back jarak mulai = " + back.getJarakMulai()
//							+ ", current jarak mulai = " + jarakMulai);
				}

				if (next != null && back != null) {
					Double selisihNext = Math.abs(next.getJarakMulai() - jarakMulai);
					Double selisihBack = Math.abs(back.getJarakMulai() - jarakMulai);

					if (selisihNext <= selisihBack) {
						detailJenisShiftPegawai = next;
					} else {
						detailJenisShiftPegawai = back;
					}

//					System.out.println("3. selisihNext = " + selisihNext + " selisihBack = " + selisihBack
//							+ ", selisihNext <= selisihBack = " + (selisihNext <= selisihBack));
				} else if (next != null && back == null) {
					detailJenisShiftPegawai = next;
				} else if (next == null && back != null) {
					detailJenisShiftPegawai = back;
				}

				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/CommonPayroll.java:197");
			}
			HibernateUtil.closeSession();
		}

		return detailJenisShiftPegawai;

	}

}
