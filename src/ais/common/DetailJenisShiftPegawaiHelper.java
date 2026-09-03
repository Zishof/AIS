package ais.common;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.payroll.JenisShiftPegawai;
import ais.database.model.payroll.JenisShiftPunyaPegawai;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;

/**
 * Helper terfokus untuk detail jenis shift pegawai. Tipe ini membungkus satu variasi kecil dari
 * alur yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getJenisShiftPunyaPegawai()}, {@code
 * getDetailJenisShiftPegawai()}, {@code findDetailShift()}, {@code getJspIds()}, {@code getDefaultShiftIds()});
 * operasi domain lain ({@code shiftDetail()}, {@code applyOwnerRestriction()}, {@code
 * applyActiveAndDateRestrictions()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class DetailJenisShiftPegawaiHelper {

	public static JenisShiftPunyaPegawai getJenisShiftPunyaPegawai(Session session, Pegawai pegawai,
			Mahasiswa mahasiswa, Siswa siswa, Date tanggal) {
		// 1. Cek spesifik di JenisShiftPunyaPegawai
		Criteria c1 = session.createCriteria(JenisShiftPunyaPegawai.class)
				.add(Restrictions.isNotNull("detailJenisShiftPegawai"));
		applyOwnerRestriction(c1, pegawai, mahasiswa, siswa);
		applyActiveAndDateRestrictions(c1.createAlias("jenisShiftPegawai", "jenisShiftPegawai"), "jenisShiftPegawai.",
				tanggal);
		c1.setMaxResults(1);

		JenisShiftPunyaPegawai jsp = (JenisShiftPunyaPegawai) ConstantValues.simpleObject(c1,
				JenisShiftPunyaPegawai.class);

		if (jsp == null) {
			c1 = session.createCriteria(JenisShiftPunyaPegawai.class);
			applyOwnerRestriction(c1, pegawai, mahasiswa, siswa);
			applyActiveAndDateRestrictions(c1.createAlias("jenisShiftPegawai", "jenisShiftPegawai"),
					"jenisShiftPegawai.", tanggal);
			c1.setMaxResults(1);

			jsp = (JenisShiftPunyaPegawai) ConstantValues.simpleObject(c1, JenisShiftPunyaPegawai.class);
		}

//		System.out.println(
//				"getJenisShiftPunyaPegawai pegawai " + pegawai + " mahasiswa " + mahasiswa + " siswa " + siswa);

		return jsp;
	}

	public static DetailJenisShiftPegawai getDetailJenisShiftPegawai(Pegawai pegawai, Mahasiswa mahasiswa, Siswa siswa,
			Date mulai, Date tanggal, String hari, boolean liburNasional) {

		if (mulai == null) {
			return null;
		}

		DetailJenisShiftPegawai detailJenisShiftPegawai = null;
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {

			JenisShiftPunyaPegawai jsp = getJenisShiftPunyaPegawai(session, pegawai, mahasiswa, siswa, tanggal);
			if (jsp != null && jsp.getDetailJenisShiftPegawai() != null) {
				detailJenisShiftPegawai = jsp.getDetailJenisShiftPegawai();
				detailJenisShiftPegawai.setJenisShiftPunyaPegawai(jsp);
				return detailJenisShiftPegawai; // Early return untuk performa optimal
			}

			// 2. Cari ID JenisShiftPegawai berdasarkan hierarki
			List<Long> ids = null;
			Guru guru = (pegawai != null) ? pegawai.getGuru() : null;
			if (guru != null || siswa != null) {
				Sekolah sekolah = (guru != null) ? guru.getSekolah() : siswa.getSekolah();
				Yayasan yayasan = (guru != null) ? guru.getYayasan() : siswa.getYayasan();

				ids = getJspIds(session, pegawai, mahasiswa, siswa, tanggal, "sekolah", sekolah);
				if (ids == null || ids.isEmpty()) {
					ids = getJspIds(session, pegawai, mahasiswa, siswa, tanggal, "yayasan", yayasan);
				}
				if (ids == null || ids.isEmpty()) {
					ids = getJspIds(session, pegawai, mahasiswa, siswa, tanggal, null, null);
				}
			} else {
				ids = getJspIds(session, pegawai, mahasiswa, siswa, tanggal, null, null);
			}

			// 3. Fallback ke JenisShiftPegawai (Default)
			if (ids == null || ids.isEmpty()) {
				if (siswa != null) {
					Sekolah sekolah = siswa.getSekolah();
					Yayasan yayasan = siswa.getYayasan();

					ids = getDefaultShiftIds(session, "defaultSiswa", "sekolah", sekolah, "yayasan", yayasan, tanggal);
					if (ids.isEmpty())
						ids = getDefaultShiftIds(session, "defaultSiswa", "sekolah", null, "yayasan", yayasan, tanggal);
					if (ids.isEmpty())
						ids = getDefaultShiftIds(session, "defaultSiswa", "sekolah", null, "yayasan", null, tanggal);
				}

				if ((ids == null || ids.isEmpty()) && mahasiswa != null) {
					Jurusan jurusan = mahasiswa.getJurusan();
					Fakultas fakultas = (jurusan != null) ? jurusan.getFakultas() : null;

					ids = getDefaultShiftIds(session, "defaultMahasiswa", "jurusan", jurusan, "fakultas", fakultas,
							tanggal);
					if (ids.isEmpty())
						ids = getDefaultShiftIds(session, "defaultMahasiswa", "jurusan", jurusan, "fakultas", null,
								tanggal);
					if (ids.isEmpty())
						ids = getDefaultShiftIds(session, "defaultMahasiswa", "jurusan", null, "fakultas", null,
								tanggal);
				}

				if ((ids == null || ids.isEmpty()) && pegawai != null && pegawai.getDosen() != null) {
					ids = getDefaultShiftIds(session, "defaultAbsenDosen", null, null, null, null, tanggal);
				}

				if ((ids == null || ids.isEmpty()) && pegawai != null && guru != null) {
					Object sekolah = guru.getSekolah();
					Object yayasan = guru.getYayasan();

					ids = getDefaultShiftIds(session, "defaultAbsenGuru", "sekolah", sekolah, "yayasan", yayasan,
							tanggal);
					if (ids.isEmpty())
						ids = getDefaultShiftIds(session, "defaultAbsenGuru", "sekolah", null, "yayasan", yayasan,
								tanggal);
					if (ids.isEmpty())
						ids = getDefaultShiftIds(session, "defaultAbsenGuru", "sekolah", null, "yayasan", null,
								tanggal);
				}

				if ((ids == null || ids.isEmpty()) && pegawai != null) {
					ids = getDefaultShiftIds(session, "defaultAbsenPegawai", null, null, null, null, tanggal);
				}
			}

			// 4. Proses mendapatkan detail spesifik menggunakan external method
			if (ids != null && !ids.isEmpty()) {
				detailJenisShiftPegawai = shiftDetail(session, tanggal, mulai, hari, liburNasional, ids, null);
			}

			// 5. Fallback Paling Terakhir: jadikanDefault = true
			if (detailJenisShiftPegawai == null || detailJenisShiftPegawai.getId() == null) {
				Criteria cDefault = session.createCriteria(DetailJenisShiftPegawai.class)
						.add(Restrictions.eq("jadikanDefault", true)).setMaxResults(1).addOrder(Order.asc("id"));
				detailJenisShiftPegawai = (DetailJenisShiftPegawai) ConstantValues.simpleObject(cDefault,
						DetailJenisShiftPegawai.class);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DetailJenisShiftPegawaiHelper.java:159");
		} finally {

			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DetailJenisShiftPegawaiHelper.java:164");
				// TODO: handle exception
			}

			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DetailJenisShiftPegawaiHelper.java:170");
				// TODO: handle exception
			}

		}

		return detailJenisShiftPegawai;
	}

	public static DetailJenisShiftPegawai shiftDetail(Session session, Date tanggal, Date mulai, String hari,
			boolean liburNasional, List<Long> ids, JenisShiftPegawai jenisShiftPegawai) {

		if (ids != null && ids.size() == 1) {
			jenisShiftPegawai = (JenisShiftPegawai) ConstantValues.ambil(JenisShiftPegawai.class.getName(), ids.get(0));
		}

		DetailJenisShiftPegawai detailJenisShiftPegawai = null;

		if (jenisShiftPegawai != null && Integer.parseInt(Common.dateFormat8.get().format(tanggal)) < Integer
				.parseInt(Common.dateFormat8.get().format(jenisShiftPegawai.getBerlakuMulai()))) {
			return null;
		}

		if (jenisShiftPegawai != null && jenisShiftPegawai.getBerotasi()) {

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			Calendar calendar1 = Calendar.getInstance();

			calendar1.setTime(tanggal);
			calendar1.set(Calendar.HOUR_OF_DAY, 0);
			calendar1.set(Calendar.MINUTE, 0);
			calendar1.set(Calendar.SECOND, 1);

			calendar.setTime(jenisShiftPegawai.getBerlakuMulai());
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
			calendar.set(Calendar.HOUR_OF_DAY, 1);
			calendar.set(Calendar.MINUTE, 1);
			calendar.set(Calendar.SECOND, 1);

			long diffInMillies = Math.abs(calendar.getTime().getTime() - calendar1.getTime().getTime());
			int diff = (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
			int hasil = 0;
			try {
				if (!jenisShiftPegawai.getJumlahHariSamaDenganJumlahShift()) {
					hasil = diff % jenisShiftPegawai.getJumlahHari();
					detailJenisShiftPegawai = (DetailJenisShiftPegawai) ConstantValues
							.simpleObject(session.createCriteria(DetailJenisShiftPegawai.class)
									.add(Restrictions.eq("jenisShiftPegawai", jenisShiftPegawai))
									.add(Restrictions.eq("ke", hasil + 1)).setMaxResults(1)
									.add(mulai == null ? Restrictions.sqlRestriction("true")
											: Restrictions.sqlRestriction(
													"mulai <= time '" + Common.timeFormat.get().format(mulai) + "'"))
									.addOrder(Order.desc("ke")), DetailJenisShiftPegawai.class);
					
					if(detailJenisShiftPegawai == null) {
						detailJenisShiftPegawai = (DetailJenisShiftPegawai) ConstantValues
								.simpleObject(session.createCriteria(DetailJenisShiftPegawai.class)
										.add(Restrictions.eq("jenisShiftPegawai", jenisShiftPegawai))
										.add(Restrictions.eq("ke", hasil + 1)).setMaxResults(1)
										.addOrder(Order.desc("ke")), DetailJenisShiftPegawai.class);
					}
					
				} else {
					hasil = diff % jenisShiftPegawai.getJumlahShift();
					detailJenisShiftPegawai = (DetailJenisShiftPegawai) ConstantValues.simpleObject(
							session.createCriteria(DetailJenisShiftPegawai.class)
									.add(Restrictions.eq("jenisShiftPegawai", jenisShiftPegawai))
									.add(Restrictions.eq("ke", hasil + 1)).setMaxResults(1),
							DetailJenisShiftPegawai.class);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DetailJenisShiftPegawaiHelper.java:241"); // Mengganti catch kosong agar potential error terlacak
			}

		

		} else {
			// Refactoring: Penggunaan fungsi helper untuk mencari secara efisien tanpa
			// duplikasi kode masif
			DetailJenisShiftPegawai next = findDetailShift(session, hari, true, liburNasional, mulai, true,
					jenisShiftPegawai, ids);
			if (next == null)
				next = findDetailShift(session, null, true, liburNasional, mulai, true, jenisShiftPegawai, ids);
			if (next == null)
				next = findDetailShift(session, hari, false, liburNasional, mulai, true, jenisShiftPegawai, ids);
			if (next == null)
				next = findDetailShift(session, null, false, liburNasional, mulai, true, jenisShiftPegawai, ids);

			DetailJenisShiftPegawai back = findDetailShift(session, hari, true, liburNasional, mulai, false,
					jenisShiftPegawai, ids);
			if (back == null)
				back = findDetailShift(session, null, true, liburNasional, mulai, false, jenisShiftPegawai, ids);
			if (back == null)
				back = findDetailShift(session, hari, false, liburNasional, mulai, false, jenisShiftPegawai, ids);
			if (back == null)
				back = findDetailShift(session, null, false, liburNasional, mulai, false, jenisShiftPegawai, ids);

			if (next != null && back != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				if (mulai != null) {
					calendar.setTime(mulai);
				}
				Double jarakMulai = (double) (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE));

				Double selisihNext = Math.abs(next.getJarakMulai() - jarakMulai);
				Double selisihBack = Math.abs(back.getJarakMulai() - jarakMulai);

				detailJenisShiftPegawai = (selisihNext <= selisihBack) ? next : back;

			} else if (next != null) {
				detailJenisShiftPegawai = next;
			} else if (back != null) {
				detailJenisShiftPegawai = back;
			}
		}

		return detailJenisShiftPegawai;
	}

	// =========================================================================
	// HELPER METHODS UNTUK KEBERSIHAN KODE
	// =========================================================================

	/**
	 * Memusatkan logika pencarian shift yang sebelumnya ditulis berulang kali
	 */
	private static DetailJenisShiftPegawai findDetailShift(Session session, String hari, boolean applyLiburFilter,
			boolean liburNasional, Date mulai, boolean isNext, JenisShiftPegawai jenisShiftPegawai, List<Long> ids) {

		Criteria c = session.createCriteria(DetailJenisShiftPegawai.class);

		if (hari != null) {
			c.add(Restrictions.eq("hari", hari));
		} else {
			c.add(Restrictions.isNull("hari"));
		}

		if (applyLiburFilter) {
			if (liburNasional) {
				c.add(Restrictions.eq("khususBuatHariLibur", true));
			} else {
				c.add(Restrictions.or(Restrictions.isNull("khususBuatHariLibur"),
						Restrictions.eq("khususBuatHariLibur", false)));
			}
		}

		c.createAlias("jenisShiftPegawai", "jenisShiftPegawai");
		c.add(Restrictions.or(Restrictions.isNull("jenisShiftPegawai.aktif"),
				Restrictions.eq("jenisShiftPegawai.aktif", true)));
		c.add(Restrictions.sqlRestriction("ke<=jumlahshift"));

		if (jenisShiftPegawai != null && jenisShiftPegawai.getId() != null) {
			c.add(Restrictions.eq("jenisShiftPegawai", jenisShiftPegawai));
		} else if (ids != null && !ids.isEmpty()) {
			c.add(Restrictions.in("jenisShiftPegawai.id", ids));
		} else {
			c.add(Restrictions.sqlRestriction("false")); // Mencegah load data secara keliru jika filter tidak ada
		}

		if (mulai != null) {
			String operator = isNext ? "<=" : ">=";
			c.add(Restrictions.sqlRestriction("mulai " + operator + " time '" + Common.timeFormat.get().format(mulai) + "'"));
		}

		if (isNext) {
			c.addOrder(Order.desc("ke"));
		} else {
			c.addOrder(Order.asc("ke"));
		}

		c.setMaxResults(1);
		return (DetailJenisShiftPegawai) ConstantValues.simpleObject(c, DetailJenisShiftPegawai.class);
	}

	private static void applyOwnerRestriction(Criteria criteria, Pegawai pegawai, Mahasiswa mahasiswa, Siswa siswa) {
		if (mahasiswa != null && mahasiswa.getId() != null) {
			criteria.add(Restrictions.eq("mahasiswa", mahasiswa));
		} else if (siswa != null && siswa.getId() != null) {
			criteria.add(Restrictions.eq("siswa", siswa));
		} else if (pegawai != null && pegawai.getId() != null) {
			criteria.add(Restrictions.eq("pegawai", pegawai));
		}
	}

	private static void applyActiveAndDateRestrictions(Criteria criteria, String prefix, Date tanggal) {
		String p = (prefix != null) ? prefix : "";

		// 1. Buat batas Awal Hari (00:00:00.000)
		Calendar calAwal = Calendar.getInstance();
		calAwal.setTime(tanggal);
		calAwal.set(Calendar.HOUR_OF_DAY, 0);
		calAwal.set(Calendar.MINUTE, 0);
		calAwal.set(Calendar.SECOND, 0);
		calAwal.set(Calendar.MILLISECOND, 0);
		Date awalHari = calAwal.getTime();

		// 2. Buat batas Akhir Hari (23:59:59.999)
		Calendar calAkhir = Calendar.getInstance();
		calAkhir.setTime(tanggal);
		calAkhir.set(Calendar.HOUR_OF_DAY, 23);
		calAkhir.set(Calendar.MINUTE, 59);
		calAkhir.set(Calendar.SECOND, 59);
		calAkhir.set(Calendar.MILLISECOND, 999);
		Date akhirHari = calAkhir.getTime();

		// 3. Terapkan pada Restrictions
		criteria.add(Restrictions.or(Restrictions.isNull(p + "aktif"), Restrictions.eq(p + "aktif", true)))
				// Tanggal mulai harus terjadi SEBELUM atau SAMA DENGAN akhir hari tersebut
				.add(Restrictions.le(p + "berlakuMulai", akhirHari)).addOrder(Order.desc(p + "berlakuMulai"))
				// Tanggal sampai harus terjadi SESUDAH atau SAMA DENGAN awal hari tersebut
				// (atau null)
				.add(Restrictions.or(Restrictions.isNull(p + "berlakuSampai"),
						Restrictions.ge(p + "berlakuSampai", awalHari)));
	}

	@SuppressWarnings("unchecked")
	private static List<Long> getJspIds(Session session, Pegawai pegawai, Mahasiswa mahasiswa, Siswa siswa,
			Date tanggal, String filterField, Object filterValue) {

		Criteria criteria = session.createCriteria(JenisShiftPunyaPegawai.class);
		applyOwnerRestriction(criteria, pegawai, mahasiswa, siswa);
		criteria.createAlias("jenisShiftPegawai", "jenisShiftPegawai");

		if (filterField != null) {
			if (filterValue != null) {
				criteria.add(Restrictions.eq("jenisShiftPegawai." + filterField, filterValue));
			} else {
				criteria.add(Restrictions.isNull("jenisShiftPegawai." + filterField));
			}
		}

		applyActiveAndDateRestrictions(criteria, "jenisShiftPegawai.", tanggal);
		criteria.setProjection(Projections.groupProperty("jenisShiftPegawai.id"));
		criteria.setMaxResults(1);

		return criteria.list();
	}

	@SuppressWarnings("unchecked")
	private static List<Long> getDefaultShiftIds(Session session, String defaultField, String field1Name,
			Object field1Val, String field2Name, Object field2Val, Date tanggal) {

		Criteria criteria = session.createCriteria(JenisShiftPegawai.class).add(Restrictions.eq(defaultField, true));

		if (field1Name != null) {
			if (field1Val != null)
				criteria.add(Restrictions.eq(field1Name, field1Val));
			else
				criteria.add(Restrictions.isNull(field1Name));
		}

		if (field2Name != null) {
			if (field2Val != null)
				criteria.add(Restrictions.eq(field2Name, field2Val));
			else
				criteria.add(Restrictions.isNull(field2Name));
		}

		applyActiveAndDateRestrictions(criteria, null, tanggal);
		criteria.setProjection(Projections.groupProperty("id"));
		criteria.setMaxResults(1);

		return criteria.list();
	}

}