package ais.common;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;

import ais.action.master.RencanaTahunAkademikAction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmuser;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.payroll.JenisShiftPegawai;
import ais.database.model.payroll.JenisShiftPunyaPegawai;
import ais.database.model.payroll.LiburNasional;
import ais.database.model.payroll.LiburRutin;
import ais.database.model.sekolah.AbsenGuruPiket;
import ais.database.model.sekolah.AbsenPiket;
import ais.database.model.sekolah.AbsenPiketDetail;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Siswa;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.WaktuUtil;

public class CommonPayroll {

	public static StatuskehadiranKaryawanHarian getDefaultStatuskehadiranKaryawanHarian(Date tanggal, Pegawai pegawai,
			Mahasiswa mahasiswa, Siswa siswa, Double lat, Double lng) {
		Session session = HibernateUtil.currentSession();
		return getDefaultStatuskehadiranKaryawanHarian(tanggal, pegawai, mahasiswa, siswa, lat, lng, session, false);
	}

	public static StatuskehadiranKaryawanHarian getDefaultStatuskehadiranKaryawanHarian(Date tanggal, Pegawai pegawai,
			Mahasiswa mahasiswa, Siswa siswa, String lat, String lng, Session session, boolean baru) {
		return getDefaultStatuskehadiranKaryawanHarian(tanggal, pegawai, mahasiswa, siswa,
				lat == null || lat.trim().isEmpty() || !Common.isNumber(lat) ? null : Double.parseDouble(lat),
				lng == null || lng.trim().isEmpty() || !Common.isNumber(lng) ? null : Double.parseDouble(lng), session,
				baru);
	}

	public static StatuskehadiranKaryawanHarian getDefaultStatuskehadiranKaryawanHarian(Date tanggal, Pegawai pegawai,
			Mahasiswa mahasiswa, Siswa siswa, Double lat, Double lng, Session session, boolean baru) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal);
		Integer bln = calendar.get(Calendar.MONTH) + 1;
		Integer thn = calendar.get(Calendar.YEAR);
		Integer tgl = calendar.get(Calendar.DATE);
		Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

		LiburNasional liburNasional = LiburNasional.ambilLiburNasional(tanggal);

		LiburRutin liburRutin = (LiburRutin) ConstantValues.simpleObject(
				session.createCriteria(LiburRutin.class).add(Restrictions.eq("hari", hari)).setMaxResults(1),
				LiburRutin.class);

		StatuskehadiranKaryawanHarian karyawanHarian = (StatuskehadiranKaryawanHarian) session
				.createCriteria(StatuskehadiranKaryawanHarian.class).add(Restrictions.eq("tanggal", tanggal))

				.add(mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa)
						: siswa != null ? Restrictions.eq("siswa", siswa)
								: Restrictions.or(Restrictions.eq("pegawai.id", pegawai.getId()),

										Restrictions.or(

												pegawai.getGuru() == null || pegawai.getGuru().getId() == null
														? Restrictions.sqlRestriction("false")
														: Restrictions.eq("guru.id", pegawai.getGuru().getId())

												,
												pegawai.getDosen() == null || pegawai.getDosen().getId() == null
														? Restrictions.sqlRestriction("false")
														: Restrictions.eq("dosen.id", pegawai.getDosen().getId())))

				)

				.setMaxResults(1).uniqueResult();

		StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian;
		if (karyawanHarian == null) {
			statuskehadiranKaryawanHarian = new StatuskehadiranKaryawanHarian(lng, lat);
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

			JenisShiftPunyaPegawai jsp = DetailJenisShiftPegawaiHelper.getJenisShiftPunyaPegawai(session, pegawai,
					mahasiswa, siswa, tanggal);
			// System.out.println("1. jsp -> " + jsp);
			statuskehadiranKaryawanHarian.setJenisShiftPunyaPegawai(jsp);

			/* ROOT CAUSE (pola sama seperti Kegiatan.kodeunik null): sebelumnya kode di sini SELALU
			 * memulai & meng-commit transaksinya sendiri ketika baru==true, walau pemanggil (mis.
			 * AbsensiKehadiranPegawaiHarianHelper background thread) sudah membuka transaksi sendiri
			 * lebih dulu pada session yang SAMA (session.beginTransaction()). Karena
			 * session.getTransaction() mengembalikan objek Transaction yang SAMA untuk satu session,
			 * commit() di sini DIAM-DIAM mengakhiri transaksi milik pemanggil. Saat pemanggil lalu
			 * memanggil tx.commit() lagi di akhir loop-nya, Hibernate melempar
			 * "TransactionException: Transaction not successfully started" -- gejala jauh dari akar
			 * masalah aslinya. Fix: hanya begin/commit transaksi sendiri bila BELUM ada transaksi aktif
			 * pada session tsb; kalau pemanggil sudah punya transaksi aktif, method ini numpang di
			 * transaksi itu (tidak begin/commit) supaya kontrol commit/rollback tetap di tangan pemanggil. */
			boolean transaksiSudahAktifSebelumnya = session != null && session.getTransaction() != null
					&& session.getTransaction().isActive();
			if (baru && !transaksiSudahAktifSebelumnya) {
				/* Callee (getJenisShiftPunyaPegawai dkk) bisa menutup session
				 * thread-local -> "Session is closed!" saat begin(). */
				if (session == null || !session.isOpen()) {
					session = ais.database.hibernate.HibernateUtil.currentNativeSession();
				}
				session.getTransaction().begin();
			}
			session.save(statuskehadiranKaryawanHarian);
			if (baru && !transaksiSudahAktifSebelumnya) {
				session.getTransaction().commit();
			}
		} else {
			statuskehadiranKaryawanHarian = karyawanHarian;
			statuskehadiranKaryawanHarian.setLiburNasional(liburNasional);
			statuskehadiranKaryawanHarian.setLiburRutin(liburRutin);

			if (statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() == null) {
				try {
					JenisShiftPunyaPegawai jsp = DetailJenisShiftPegawaiHelper.getJenisShiftPunyaPegawai(session,
							pegawai, mahasiswa, siswa, tanggal);
					// System.out.println("2. jsp -> " + jsp);
					statuskehadiranKaryawanHarian.setJenisShiftPunyaPegawai(jsp);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPayroll.java:144");
				}
			}

			if (lng != null) {
				statuskehadiranKaryawanHarian.setLng(lng);
			}
			if (lat != null) {
				statuskehadiranKaryawanHarian.setLat(lat);
			}
		}

		return statuskehadiranKaryawanHarian;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, StatuskehadiranKaryawanHarian> getDefaultStatuskehadiranKaryawanHarian(
			List<CutiDanIzin> cutiDanIzinsSemua, Date mulai, Date sampai, List<Pegawai> pegawais, Session session,
			boolean baru) {

		List<Long> pegawaisId = new ArrayList<Long>();
		List<Long> dosensId = new ArrayList<Long>();
		List<Long> gurusId = new ArrayList<Long>();
		for (Pegawai pegawai : pegawais) {
			pegawaisId.add(pegawai.getId());
			if (pegawai.getDosen() != null) {
				dosensId.add(pegawai.getDosen().getId());
			}
			if (pegawai.getGuru() != null) {
				gurusId.add(pegawai.getGuru().getId());
			}
		}

		Date sekarang = WaktuUtil.getDate();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(mulai);
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 7);

		Calendar s = ais.ui.util.WaktuUtil.getCalendar();
		s.setTime(sampai);
		s.set(Calendar.DATE, s.get(Calendar.DATE) + 7);

		List<StatuskehadiranKaryawanHarian> karyawanHarians = session
				.createCriteria(StatuskehadiranKaryawanHarian.class).add(Restrictions.isNotNull("tanggal"))
				.add(Restrictions.between("tanggal", calendar.getTime(), s.getTime()))

				.add(Restrictions.or(
						pegawaisId.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.in("pegawai.id", pegawaisId),

						Restrictions.or(

								gurusId.isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.in("guru.id", gurusId)

								, dosensId.isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.in("dosen.id", dosensId)))

				)

				.list();

		// System.out.println("ckaryawanHarians size " + karyawanHarians.size() + "
		// pegawaisId -> " + pegawaisId);

		List<LiburNasional> liburNasionals = LiburNasional.ambilLiburNasional(mulai, sampai);

		List<LiburRutin> liburRutins = ConstantValues.simpleList(session.createCriteria(LiburRutin.class),
				LiburRutin.class);

		Map<String, StatuskehadiranKaryawanHarian> statuskehadiranKaryawanHarians = new HashMap<String, StatuskehadiranKaryawanHarian>();

		while (calendar.getTime().before(s.getTime())) {

			Date tanggal = calendar.getTime();

			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
			Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

			LiburNasional liburNasional = null;
			for (LiburNasional d : liburNasionals) {
				if (Common.dateFormat83.get().format(tanggal).equalsIgnoreCase(Common.dateFormat83.get().format(d.getTanggal()))) {
					liburNasional = d;
					break;
				}
			}

			LiburRutin liburRutin = null;
			for (LiburRutin d : liburRutins) {
				if (d.getHari() != null && hari.equals(d.getHari())) {
					liburRutin = d;
					break;
				}
			}

			for (Pegawai pegawai : pegawais) {

				StatuskehadiranKaryawanHarian karyawanHarian = null;
				for (StatuskehadiranKaryawanHarian d : karyawanHarians) {
					if (

					(

					(d.getPegawai() != null && pegawai != null && d.getPegawai().getId().equals(pegawai.getId()))

							|| (d.getDosen() != null && pegawai != null && pegawai.getDosen() != null
									&& d.getDosen().getId().equals(pegawai.getDosen().getId()))

							|| (d.getGuru() != null && pegawai != null && pegawai.getGuru() != null
									&& d.getGuru().getId().equals(pegawai.getGuru().getId()))

					)

							&& Common.dateFormat83.get().format(tanggal)
									.equalsIgnoreCase(Common.dateFormat83.get().format(d.getTanggal()))) {
						karyawanHarian = d;
						break;
					}
				}

//				System.out.println("karyawanHarian -> " + karyawanHarian);

				StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian;
				if (karyawanHarian == null) {
					statuskehadiranKaryawanHarian = new StatuskehadiranKaryawanHarian();
					statuskehadiranKaryawanHarian.setTanggal(tanggal);
					statuskehadiranKaryawanHarian.setPegawai(pegawai);
					statuskehadiranKaryawanHarian.setKeterangan("");
					statuskehadiranKaryawanHarian.setMasukjam(null);
					statuskehadiranKaryawanHarian.setPulangJam(null);
					if (tanggal.before(sekarang)) {
						statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.TIDAK_ADA_ALASAN);
					} else {
						statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.BELUM_ABSEN);
					}
					statuskehadiranKaryawanHarian.setMinggu(hari);
					statuskehadiranKaryawanHarian.setLiburNasional(liburNasional);
					statuskehadiranKaryawanHarian.setLiburRutin(liburRutin);
				} else {
					statuskehadiranKaryawanHarian = karyawanHarian;
					statuskehadiranKaryawanHarian.setLiburNasional(liburNasional);
					statuskehadiranKaryawanHarian.setLiburRutin(liburRutin);
				}

				List<CutiDanIzin> cutiDanIzins = new ArrayList<CutiDanIzin>();
				for (CutiDanIzin cutiDanIzin : cutiDanIzinsSemua) {
					if (cutiDanIzin.getPegawai() != null && pegawai != null
							&& cutiDanIzin.getPegawai().getId().equals(pegawai.getId())) {
						cutiDanIzins.add(cutiDanIzin);
					}
				}

				CommonPayroll.chekCuti(cutiDanIzins, session, tanggal, pegawai, statuskehadiranKaryawanHarian, baru);

				statuskehadiranKaryawanHarians.put(Common.dateFormat83.get().format(tanggal) + "_" + pegawai.getId(),
						statuskehadiranKaryawanHarian);
			}
		}

		return statuskehadiranKaryawanHarians;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, StatuskehadiranKaryawanHarian> getDefaultStatuskehadiranKaryawanHarian(
			List<CutiDanIzin> cutiDanIzins, Integer bulan, Integer tahun, Pegawai pegawai, Session session,
			boolean baru) {

		// Filter berdasarkan TANGGAL (rentang bulan), BUKAN hanya kolom denormalisasi bulan/tahun.
		// SEBAB: sebagian record StatuskehadiranKaryawanHarian punya "tanggal" terisi (mis. jam masuk
		// diisi oleh scan/mesin/API atau record dibuat path lama) TETAPI kolom bulan/tahun NULL/tak
		// konsisten. Akibatnya data tampil di modul Presensi (yang membaca via "tanggal") tetapi HILANG
		// di Rekap/Laporan Kehadiran (yang dulu memfilter eq(bulan)+eq(tahun)) sehingga kehadiran jadi
		// kosong. Filter rentang tanggal adalah SUPERSET aman: semua record yang dulu lolos (bulan/tahun
		// benar) pasti juga punya tanggal di rentang ini, plus menangkap record yang bulan/tahun-nya kosong.
		Calendar batasBulan = ais.ui.util.WaktuUtil.getCalendar();
		batasBulan.set(Calendar.YEAR, tahun);
		batasBulan.set(Calendar.MONTH, bulan - 1);
		batasBulan.set(Calendar.DATE, 1);
		batasBulan.set(Calendar.HOUR_OF_DAY, 0);
		batasBulan.set(Calendar.MINUTE, 0);
		batasBulan.set(Calendar.SECOND, 0);
		batasBulan.set(Calendar.MILLISECOND, 0);
		Date awalBulan = batasBulan.getTime();
		batasBulan.set(Calendar.DATE, batasBulan.getActualMaximum(Calendar.DAY_OF_MONTH));
		batasBulan.set(Calendar.HOUR_OF_DAY, 23);
		batasBulan.set(Calendar.MINUTE, 59);
		batasBulan.set(Calendar.SECOND, 59);
		batasBulan.set(Calendar.MILLISECOND, 999);
		Date akhirBulan = batasBulan.getTime();

		List<StatuskehadiranKaryawanHarian> karyawanHarians = session
				.createCriteria(StatuskehadiranKaryawanHarian.class).add(Restrictions.isNotNull("tanggal"))
				.add(Restrictions.between("tanggal", awalBulan, akhirBulan))

				.add(Restrictions.or(Restrictions.eq("pegawai.id", pegawai.getId()),

						Restrictions.or(

								pegawai.getGuru() == null || pegawai.getGuru().getId() == null
										? Restrictions.sqlRestriction("false")
										: Restrictions.eq("guru.id", pegawai.getGuru().getId())

								,
								pegawai.getDosen() == null || pegawai.getDosen().getId() == null
										? Restrictions.sqlRestriction("false")
										: Restrictions.eq("dosen.id", pegawai.getDosen().getId())))

				)

				.list();

		System.out.println("ckaryawanHarians size " + karyawanHarians.size());

		List<LiburNasional> liburNasionals = ConstantValues.simpleList(
				session.createCriteria(LiburNasional.class).add(Restrictions.eq("tahun", tahun)), LiburNasional.class);

		List<LiburRutin> liburRutins = ConstantValues.simpleList(session.createCriteria(LiburRutin.class),
				LiburRutin.class);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, bulan - 1);
		calendar.set(Calendar.YEAR, tahun);
		calendar.set(Calendar.DATE, 1);
		int jumlahHari = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

		Map<String, StatuskehadiranKaryawanHarian> statuskehadiranKaryawanHarians = new HashMap<String, StatuskehadiranKaryawanHarian>();
		StatuskehadiranKaryawanHarian back = null;
		for (int i = 1; i <= jumlahHari; i++) {

			calendar.set(Calendar.DATE, i);
			Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

			Date tanggal = calendar.getTime();

			LiburNasional liburNasional = null;
			for (LiburNasional d : liburNasionals) {
				if (Common.dateFormat83.get().format(tanggal).equalsIgnoreCase(Common.dateFormat83.get().format(d.getTanggal()))) {
					liburNasional = d;
					break;
				}
			}

			LiburRutin liburRutin = null;
			for (LiburRutin d : liburRutins) {
				if (d.getHari() != null && hari.equals(d.getHari())) {
					liburRutin = d;
					break;
				}
			}

			StatuskehadiranKaryawanHarian karyawanHarian = null;
			for (StatuskehadiranKaryawanHarian d : karyawanHarians) {
				if (Common.dateFormat83.get().format(tanggal).equalsIgnoreCase(Common.dateFormat83.get().format(d.getTanggal()))) {
					karyawanHarian = d;
					break;
				}
			}

			StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian;
			if (karyawanHarian == null) {
				statuskehadiranKaryawanHarian = new StatuskehadiranKaryawanHarian();
				statuskehadiranKaryawanHarian.setTanggal(tanggal);
				statuskehadiranKaryawanHarian.setPegawai(pegawai);
				statuskehadiranKaryawanHarian.setKeterangan("");
				statuskehadiranKaryawanHarian.setMasukjam(null);
				statuskehadiranKaryawanHarian.setPulangJam(null);
				statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.BELUM_ABSEN);
				statuskehadiranKaryawanHarian.setMinggu(hari);
				statuskehadiranKaryawanHarian.setLiburNasional(liburNasional);
				statuskehadiranKaryawanHarian.setLiburRutin(liburRutin);
			} else {
				statuskehadiranKaryawanHarian = karyawanHarian;
				statuskehadiranKaryawanHarian.setLiburNasional(liburNasional);
				statuskehadiranKaryawanHarian.setLiburRutin(liburRutin);
			}

			CommonPayroll.chekCuti(cutiDanIzins, session, tanggal, pegawai, statuskehadiranKaryawanHarian, baru);

			if (back != null) {
				statuskehadiranKaryawanHarian.setBack(back);
			}
			statuskehadiranKaryawanHarians.put(Common.dateFormat83.get().format(tanggal), statuskehadiranKaryawanHarian);

			if (back != null) {
				back.setNext(statuskehadiranKaryawanHarian);
				if (back.getTanggal() != null) {
					statuskehadiranKaryawanHarians.put(Common.dateFormat83.get().format(back.getTanggal()), back);
				}
			}

			back = statuskehadiranKaryawanHarian;
		}

		return statuskehadiranKaryawanHarians;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, StatuskehadiranKaryawanHarian> getDefaultStatuskehadiranKaryawanHarian(
			List<CutiDanIzin> cutiDanIzins, Integer tahun, Pegawai pegawai, Session session, boolean baru) {

		// Filter by TANGGAL (rentang tahun), bukan hanya kolom denormalisasi "tahun" yang bisa null.
		// Alasan sama dgn overload per-bulan: record yang jam masuknya diisi scan/mesin tapi kolom
		// tahun-nya kosong akan tetap terjaring lewat rentang tanggal (superset aman).
		Calendar batasTahun = ais.ui.util.WaktuUtil.getCalendar();
		batasTahun.set(Calendar.YEAR, tahun);
		batasTahun.set(Calendar.MONTH, Calendar.JANUARY);
		batasTahun.set(Calendar.DATE, 1);
		batasTahun.set(Calendar.HOUR_OF_DAY, 0);
		batasTahun.set(Calendar.MINUTE, 0);
		batasTahun.set(Calendar.SECOND, 0);
		batasTahun.set(Calendar.MILLISECOND, 0);
		Date awalTahun = batasTahun.getTime();
		batasTahun.set(Calendar.MONTH, Calendar.DECEMBER);
		batasTahun.set(Calendar.DATE, 31);
		batasTahun.set(Calendar.HOUR_OF_DAY, 23);
		batasTahun.set(Calendar.MINUTE, 59);
		batasTahun.set(Calendar.SECOND, 59);
		batasTahun.set(Calendar.MILLISECOND, 999);
		Date akhirTahun = batasTahun.getTime();

		List<StatuskehadiranKaryawanHarian> karyawanHarians = session
				.createCriteria(StatuskehadiranKaryawanHarian.class).add(Restrictions.isNotNull("tanggal"))
				.add(Restrictions.between("tanggal", awalTahun, akhirTahun))

				.add(Restrictions.or(Restrictions.eq("pegawai.id", pegawai.getId()),

						Restrictions.or(

								pegawai.getGuru() == null || pegawai.getGuru().getId() == null
										? Restrictions.sqlRestriction("false")
										: Restrictions.eq("guru.id", pegawai.getGuru().getId())

								,
								pegawai.getDosen() == null || pegawai.getDosen().getId() == null
										? Restrictions.sqlRestriction("false")
										: Restrictions.eq("dosen.id", pegawai.getDosen().getId())))

				)

				.list();

		System.out.println("ckaryawanHarians size " + karyawanHarians.size());

		List<LiburNasional> liburNasionals = ConstantValues.simpleList(
				session.createCriteria(LiburNasional.class).add(Restrictions.eq("tahun", tahun)), LiburNasional.class);

		List<LiburRutin> liburRutins = ConstantValues.simpleList(session.createCriteria(LiburRutin.class),
				LiburRutin.class);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, 0);
		calendar.set(Calendar.YEAR, tahun);
		calendar.set(Calendar.DATE, 1);

		Map<String, StatuskehadiranKaryawanHarian> statuskehadiranKaryawanHarians = new HashMap<String, StatuskehadiranKaryawanHarian>();
		StatuskehadiranKaryawanHarian back = null;
		for (int i = 1; i <= 356; i++) {

			calendar.set(Calendar.DATE, i);
			Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

			Date tanggal = calendar.getTime();

			LiburNasional liburNasional = null;
			for (LiburNasional d : liburNasionals) {
				if (Common.dateFormat83.get().format(tanggal).equalsIgnoreCase(Common.dateFormat83.get().format(d.getTanggal()))) {
					liburNasional = d;
					break;
				}
			}

			LiburRutin liburRutin = null;
			for (LiburRutin d : liburRutins) {
				if (d.getHari() != null && hari.equals(d.getHari())) {
					liburRutin = d;
					break;
				}
			}

			StatuskehadiranKaryawanHarian karyawanHarian = null;
			for (StatuskehadiranKaryawanHarian d : karyawanHarians) {
				if (Common.dateFormat83.get().format(tanggal).equalsIgnoreCase(Common.dateFormat83.get().format(d.getTanggal()))) {
					karyawanHarian = d;
					break;
				}
			}

			StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian;
			if (karyawanHarian == null) {
				statuskehadiranKaryawanHarian = new StatuskehadiranKaryawanHarian();
				statuskehadiranKaryawanHarian.setTanggal(tanggal);
				statuskehadiranKaryawanHarian.setPegawai(pegawai);
				statuskehadiranKaryawanHarian.setKeterangan("");
				statuskehadiranKaryawanHarian.setMasukjam(null);
				statuskehadiranKaryawanHarian.setPulangJam(null);
				statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.BELUM_ABSEN);
				statuskehadiranKaryawanHarian.setMinggu(hari);
				statuskehadiranKaryawanHarian.setLiburNasional(liburNasional);
				statuskehadiranKaryawanHarian.setLiburRutin(liburRutin);
			} else {
				statuskehadiranKaryawanHarian = karyawanHarian;
				statuskehadiranKaryawanHarian.setLiburNasional(liburNasional);
				statuskehadiranKaryawanHarian.setLiburRutin(liburRutin);
			}

			CommonPayroll.chekCuti(cutiDanIzins, session, tanggal, pegawai, statuskehadiranKaryawanHarian, baru);
			if (back != null) {
				statuskehadiranKaryawanHarian.setBack(back);
			}
			statuskehadiranKaryawanHarians.put(Common.dateFormat83.get().format(tanggal), statuskehadiranKaryawanHarian);

			if (back != null) {
				back.setNext(statuskehadiranKaryawanHarian);
				if (back.getTanggal() != null) {
					statuskehadiranKaryawanHarians.put(Common.dateFormat83.get().format(back.getTanggal()), back);
				}
			}

			back = statuskehadiranKaryawanHarian;
		}

		return statuskehadiranKaryawanHarians;
	}

	public static void chekCuti(List<CutiDanIzin> cutiDanIzins, Session session, Date tanggal, Pegawai pegawai,
			StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian, boolean baru) {
		CutiDanIzin cutiDanIzin = null;

		for (CutiDanIzin cuti : cutiDanIzins) {

			// 1. Cek apakah tanggal berada dalam rentang cuti (mulai s.d sampai)
			if ((tanggal.after(cuti.getMulai()) && tanggal.before(cuti.getSampai()))
					|| Common.dateFormat83.get().format(tanggal).equalsIgnoreCase(Common.dateFormat83.get().format(cuti.getMulai()))
					|| Common.dateFormat83.get().format(tanggal).equalsIgnoreCase(Common.dateFormat83.get().format(cuti.getSampai()))) {
				
				boolean isPengecualian = false;
				String kecualiStr = cuti.getKecualiTanggals();
				
				// 2. Cek apakah tanggal ini masuk ke dalam daftar pengecualian (kecualiTanggals)
				if (kecualiStr != null && !kecualiStr.trim().isEmpty()) {
					try {
						org.json.JSONArray arr = new org.json.JSONArray(kecualiStr);
						for (int i = 0; i < arr.length(); i++) {
							String tglStr = arr.getString(i);
							
							// Parsing string menjadi Date sesuai standar format penyimpanan (dateFormat4)
							Date tglCb = Common.dateFormat4.get().parse(tglStr);
							
							// Membandingkan tanggal dengan format baku (dateFormat83) agar jam/menit diabaikan
							if (Common.dateFormat83.get().format(tanggal).equalsIgnoreCase(Common.dateFormat83.get().format(tglCb))) {
								isPengecualian = true;
								break; // Tanggal ditemukan di pengecualian, hentikan pencarian array
							}
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonPayroll.java:599");
						// Abaikan error secara aman agar jika format JSON/Tanggal rusak, tidak menghancurkan proses absen.
					}
				}

				// 3. Jika tanggal BUKAN pengecualian, maka sah dianggap sedang cuti/izin
				if (!isPengecualian) {
					cutiDanIzin = cuti;
					break; // Cuti yang tepat sudah ditemukan, keluar dari perulangan
				}

			}

		}

		statuskehadiranKaryawanHarian.setCutiDanIzin(cutiDanIzin);

	}

	public static void simpanDetail(Session session, final StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian,
			boolean baru) {

		try {
			if (statuskehadiranKaryawanHarian != null && statuskehadiranKaryawanHarian.getStatusabsensi() != null) {
				Date tanggal = statuskehadiranKaryawanHarian.getTanggal();

				try {
					if (baru) {
						CommonPayroll.simpanAbsenPertemuan(session, statuskehadiranKaryawanHarian.getPegawai(),
								statuskehadiranKaryawanHarian.getDosen(), statuskehadiranKaryawanHarian.getGuru(),
								statuskehadiranKaryawanHarian.getMahasiswa(), statuskehadiranKaryawanHarian.getSiswa(),
								statuskehadiranKaryawanHarian.getKeterangan());
					} else {
						Session newSession = HibernateUtil.currentNativeSession();
						CommonPayroll.simpanAbsenPertemuan(newSession, statuskehadiranKaryawanHarian.getPegawai(),
								statuskehadiranKaryawanHarian.getDosen(), statuskehadiranKaryawanHarian.getGuru(),
								statuskehadiranKaryawanHarian.getMahasiswa(), statuskehadiranKaryawanHarian.getSiswa(),
								statuskehadiranKaryawanHarian.getKeterangan());
						// newSession.disconnect();
						if (newSession.isOpen()) {
							newSession.disconnect();
							newSession.close();
						}
						HibernateUtil.closeSession();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPayroll.java:645");
				}

				Guru guru = statuskehadiranKaryawanHarian.getGuru();
				Siswa siswa = statuskehadiranKaryawanHarian.getSiswa();

				if (statuskehadiranKaryawanHarian.getGuru() == null && statuskehadiranKaryawanHarian.getSiswa() == null
						&& statuskehadiranKaryawanHarian.getPegawai() != null) {

					if (statuskehadiranKaryawanHarian.getStatusabsensi().getKode() != null
							&& statuskehadiranKaryawanHarian.getStatusabsensi().getKode().equals("M")) {
						new Thread(new Runnable() {

							@SuppressWarnings("unchecked")
							@Override
							public void run() {
								try {

								Session session = HibernateUtil.currentNativeSession();
								List<String> usernames = session.createCriteria(Tbmuser.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("pegawai", statuskehadiranKaryawanHarian.getPegawai()))
										.setProjection(Projections.property("userId")).list();
								// session.disconnect();
								if (session.isOpen()) {
									session.disconnect();
									session.close();
								}
								HibernateUtil.closeSession();
								if (!usernames.isEmpty()) {
									JSONArray userIds = new JSONArray();
									for (String s : usernames) {
										userIds.put(s);
									}

									String waktu = "";
									int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
									if (jam >= 10 && jam < 15) {
										waktu = "Siang";
									} else if (jam >= 15 && jam < 18) {
										waktu = "Sore";
									} else if (jam >= 18 && jam <= 24) {
										waktu = "Malam";
									} else {
										waktu = "Pagi";
									}

									String nama = statuskehadiranKaryawanHarian.getPegawai().getNama();

									String ket = "Selamat " + waktu
											+ " bapak/ibu. Kami ingin mengucapkan selamat kepada " + nama
											+ " yang telah berhasil melakukan absen kehadiran pada "
											+ Common.dateFormat.get().format(statuskehadiranKaryawanHarian.getTanggal())
											+ ". Kehadiran Anda sangat kami hargai dan semoga Anda selalu bersemangat dalam mengikuti kegiatan pada hari ini"
											+ ". Terima kasih.";

									String recipientsTemp = null;
									MailSender.simpanNotif(userIds, recipientsTemp, ket,
											statuskehadiranKaryawanHarian.getKeterangan(),
											statuskehadiranKaryawanHarian);
								}

															} finally {
									ais.database.hibernate.HibernateUtil.closeSession();
								}
							}
						}).start();
					}

				}

				if (guru != null) {

					RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction
							.getCurrentRencanaTahunAkademik(
									statuskehadiranKaryawanHarian.getDosen() == null ? null
											: statuskehadiranKaryawanHarian.getDosen().getFakultas(),
									statuskehadiranKaryawanHarian.getDosen() == null ? null
											: statuskehadiranKaryawanHarian.getDosen().getJurusan(),
									guru == null ? null : guru.getYayasan(), guru == null ? null : guru.getSekolah(),
									null, null, null, tanggal, null, null);
					Integer semester = Common.isNowSemensterGanjil() ? 1 : 2;
					String ta = Common.getCurrentTahunAkademik();
					if (rencanaTahunAkademik != null) {
						semester = rencanaTahunAkademik.getSemester().equals(Perkuliahan.GANJIL) ? 1 : 2;
						ta = rencanaTahunAkademik.getNama();
					}

					Integer jamke = AbsenGuruPiket.jamKe(guru.getSekolah() == null ? 0L : guru.getSekolah().getId());

					AbsenGuruPiket absenGuruPiket;
					if (baru) {
						absenGuruPiket = (AbsenGuruPiket) session.createCriteria(AbsenGuruPiket.class)
								.add(Restrictions.or(Restrictions.isNull("jamke"), Restrictions.eq("jamke", jamke)))
								.add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.eq("semester", semester))
								.add(Restrictions.sqlRestriction("date(this_.tanggal)=date('"
										+ Common.databaseDateFormat.get().format(tanggal) + "')"))
								.setMaxResults(1).add(Restrictions.eq("sekolah", guru.getSekolah())).uniqueResult();
					} else {
						Session newSession = HibernateUtil.currentNativeSession();
						absenGuruPiket = (AbsenGuruPiket) newSession.createCriteria(AbsenGuruPiket.class)
								.add(Restrictions.or(Restrictions.isNull("jamke"), Restrictions.eq("jamke", jamke)))
								.add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.eq("semester", semester))
								.add(Restrictions.sqlRestriction("date(this_.tanggal)=date('"
										+ Common.databaseDateFormat.get().format(tanggal) + "')"))
								.setMaxResults(1).add(Restrictions.eq("sekolah", guru.getSekolah())).uniqueResult();
						// newSession.disconnect();
						if (newSession.isOpen()) {
							newSession.disconnect();
							newSession.close();
						}
					}

					if (absenGuruPiket == null) {
						absenGuruPiket = new AbsenGuruPiket();
						absenGuruPiket.setTanggal(WaktuUtil.getDate());
					}
					absenGuruPiket.setJamke(jamke);
					absenGuruPiket.setTahunAjaran(ta);
					absenGuruPiket.setSemester(semester);
					absenGuruPiket.setSekolah(guru.getSekolah());

					Statusabsensi statusabsensi = statuskehadiranKaryawanHarian.getStatusabsensi();

					String waktu = "";
					int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
					if (jam >= 10 && jam < 15) {
						waktu = "Siang";
					} else if (jam >= 15 && jam < 18) {
						waktu = "Sore";
					} else if (jam >= 18 && jam <= 24) {
						waktu = "Malam";
					} else {
						waktu = "Pagi";
					}

					String ket = "Absensi per " + Common.dateFormat.get().format(statuskehadiranKaryawanHarian.getTanggal());

					if (statusabsensi != null && statusabsensi.getKode() != null
							&& statusabsensi.getKode().equals("M")) {
						ket = "Selamat " + waktu + " Yth. Bapak/Ibu guru "
								+ (guru.getSekolah() == null ? "" : guru.getSekolah().getNama())
								+ ". Kami ingin mengucapkan selamat kepada guru atas nama " + guru.getNama()
								+ " yang telah berhasil melakukan absen kehadiran pada "
								+ Common.dateFormat.get().format(statuskehadiranKaryawanHarian.getTanggal())
								+ (ta == null ? "" : " tahun pelajaran " + ta)
								+ (semester == null ? "" : " semester " + (semester.equals(1) ? "ganjil" : "genap"))
								+ ". Kehadiran Anda sangat kami hargai dan semoga Anda selalu bersemangat dalam mengikuti kegiatan pada hari ini"
								+ ". Terima kasih.";
					}

					absenGuruPiket.populate(guru.getId() + "", statusabsensi, ket,
							statuskehadiranKaryawanHarian.ambilMasukjam() == null ? ""
									: Common.timeFormat2.get().format(statuskehadiranKaryawanHarian.ambilMasukjam()),
							statuskehadiranKaryawanHarian.ambilPulangjam() == null ? ""
									: Common.timeFormat2.get().format(statuskehadiranKaryawanHarian.ambilPulangjam()),
							"AbsenGuruPiket");
					if (baru) {
						session.getTransaction().begin();
						session.saveOrUpdate(absenGuruPiket);
						session.getTransaction().commit();
					} else {
						Session newSession = HibernateUtil.currentNativeSession();
						newSession.getTransaction().begin();
						newSession.saveOrUpdate(absenGuruPiket);
						newSession.getTransaction().commit();
						// newSession.disconnect();
						if (newSession.isOpen()) {
							newSession.disconnect();
							newSession.close();
						}
						HibernateUtil.closeSession();
					}
				}

				else if (siswa != null) {

					KelasSiswa kelasSiswa = siswa.getKelas();
					if (kelasSiswa != null) {
						RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction
								.getCurrentRencanaTahunAkademik(
										statuskehadiranKaryawanHarian.getDosen() == null ? null
												: statuskehadiranKaryawanHarian.getDosen().getFakultas(),
										statuskehadiranKaryawanHarian.getDosen() == null ? null
												: statuskehadiranKaryawanHarian.getDosen().getJurusan(),
										siswa == null ? null : siswa.getYayasan(),
										siswa == null ? null : siswa.getSekolah(), null, null, null, tanggal, null,
										null);
						Integer semester = Common.isNowSemensterGanjil() ? 1 : 2;
						String ta = Common.getCurrentTahunAkademik();
						if (rencanaTahunAkademik != null) {
							semester = rencanaTahunAkademik.getSemester().equals(Perkuliahan.GANJIL) ? 1 : 2;
							ta = rencanaTahunAkademik.getNama();
						}

						AbsenPiket absenSiswaPiket;
						if (baru) {
							absenSiswaPiket = (AbsenPiket) session.createCriteria(AbsenPiket.class)
									.add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.eq("semester", semester))
									.add(Restrictions.sqlRestriction("date(this_.tanggal)=date('"
											+ Common.databaseDateFormat.get().format(tanggal) + "')"))
									.setMaxResults(1).add(Restrictions.eq("sekolah", siswa.getSekolah()))
									.uniqueResult();
						} else {
							Session newSession = HibernateUtil.currentNativeSession();
							absenSiswaPiket = (AbsenPiket) newSession.createCriteria(AbsenPiket.class)
									.add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.eq("semester", semester))
									.add(Restrictions.sqlRestriction("date(this_.tanggal)=date('"
											+ Common.databaseDateFormat.get().format(tanggal) + "')"))
									.setMaxResults(1).add(Restrictions.eq("sekolah", siswa.getSekolah()))
									.add(Restrictions.eq("kelas", kelasSiswa)).uniqueResult();
							// newSession.disconnect();
							if (newSession.isOpen()) {
								newSession.disconnect();
								newSession.close();
							}
						}

						if (absenSiswaPiket == null) {
							absenSiswaPiket = new AbsenPiket();
							absenSiswaPiket.setTanggal(WaktuUtil.getDate());
						}
						absenSiswaPiket.setTahunAjaran(ta);
						absenSiswaPiket.setSemester(semester);
						absenSiswaPiket.setSekolah(siswa.getSekolah());
						absenSiswaPiket.setKelas(kelasSiswa);
						if (baru) {
							session.getTransaction().begin();
							session.saveOrUpdate(absenSiswaPiket);
							session.getTransaction().commit();
						} else {
							Session newSession = HibernateUtil.currentNativeSession();
							newSession.getTransaction().begin();
							newSession.saveOrUpdate(absenSiswaPiket);
							newSession.getTransaction().commit();
							// newSession.disconnect();
							if (newSession.isOpen()) {
								newSession.disconnect();
								newSession.close();
							}
							HibernateUtil.closeSession();
						}

						AbsenPiketDetail absenPiketDetail = AbsenPiketDetail.ambil(null, siswa, absenSiswaPiket,
								kelasSiswa.getAbsensi(),
								AbsenPiketDetail.jamKe(siswa.getSekolah() == null ? 0L : siswa.getId()));

						Statusabsensi statusabsensi = statuskehadiranKaryawanHarian.getStatusabsensi();

						String waktu = "";
						int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
						if (jam >= 10 && jam < 15) {
							waktu = "Siang";
						} else if (jam >= 15 && jam < 18) {
							waktu = "Sore";
						} else if (jam >= 18 && jam <= 24) {
							waktu = "Malam";
						} else {
							waktu = "Pagi";
						}

						String ket = "Absensi per "
								+ Common.dateFormat.get().format(statuskehadiranKaryawanHarian.getTanggal());

						if (statusabsensi != null && statusabsensi.getKode() != null
								&& statusabsensi.getKode().equals("M")) {
							ket = "Selamat " + waktu + " siswa/i "
									+ (siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama())
									+ ". Kami ingin mengucapkan selamat kepada siswa atas nama " + siswa.getNama()
									+ " yang telah berhasil melakukan absen kehadiran pada "
									+ Common.dateFormat.get().format(statuskehadiranKaryawanHarian.getTanggal())
									+ (ta == null ? "" : " tahun pelajaran " + ta)
									+ (semester == null ? "" : " semester " + (semester.equals(1) ? "ganjil" : "genap"))
									+ ". Kehadiran Anda sangat kami hargai dan semoga Anda selalu bersemangat dalam mengikuti kegiatan pada hari ini"
									+ ". Terima kasih.";
						}

						absenPiketDetail.populate(siswa.getId() + "_" + absenSiswaPiket.getId(), statusabsensi, ket, "",
								"", "AbsenPiket");

						kelasSiswa.populate(siswa.getId() + "_" + absenSiswaPiket.getId(), statusabsensi,
								statuskehadiranKaryawanHarian.getKeterangan(), "", "", "AbsenPiket");
						if (baru) {
							session.getTransaction().begin();
							Common.refreshUpdate(session, absenPiketDetail);
							Common.refreshUpdate(session, kelasSiswa);
							session.getTransaction().commit();
						} else {
							Session newSession = HibernateUtil.currentNativeSession();
							newSession.getTransaction().begin();
							Common.refreshUpdate(newSession, absenPiketDetail);
							Common.refreshUpdate(newSession, kelasSiswa);
							newSession.getTransaction().commit();
							// newSession.disconnect();
							if (newSession.isOpen()) {
								newSession.disconnect();
								newSession.close();
							}
							HibernateUtil.closeSession();
						}

					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPayroll.java:952");
		}
	}

	@SuppressWarnings("unchecked")
	public static List<DetailJenisShiftPegawai> shiftRotasiHari(Session session, Date tanggal,
			JenisShiftPegawai jenisShiftPegawai) {

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
		System.out.println("hari -> " + diff + " " + Common.dateFormat4.get().format(tanggal));
		int hasil = diff % jenisShiftPegawai.getJumlahHari();

		return ConstantValues.simpleList(
				session.createCriteria(DetailJenisShiftPegawai.class)
						.add(Restrictions.eq("jenisShiftPegawai", jenisShiftPegawai))
						.add(Restrictions.eq("hariKe", hasil + 1)).addOrder(Order.asc("ke")),
				DetailJenisShiftPegawai.class);
	}

	public static DetailJenisShiftPegawai getDetailJenisShiftPegawai(Pegawai pegawai, Mahasiswa mahasiswa, Siswa siswa,
			Date mulai, Date tanggal, String hari, boolean liburNasional) {
		return DetailJenisShiftPegawaiHelper.getDetailJenisShiftPegawai(pegawai, mahasiswa, siswa, mulai, tanggal, hari,
				liburNasional);
	}

	public static DetailJenisShiftPegawai shiftDetail(Session session, Date tanggal, Date mulai, String hari,
			boolean liburNasional, List<Long> ids, JenisShiftPegawai jenisShiftPegawai) {
		return DetailJenisShiftPegawaiHelper.shiftDetail(session, tanggal, mulai, hari, liburNasional, ids,
				jenisShiftPegawai);
	}

	@SuppressWarnings("unchecked")
	public static Pertemuan simpanAbsenPertemuan(Session session, Pegawai pegawai, Dosen dosen, Guru guru,
			Mahasiswa mahasiswa, Siswa siswa, String s) {

		int toleransi_jam_masuk_perkuliahan_dalam_menit_sebelum = 60;
		try {
			toleransi_jam_masuk_perkuliahan_dalam_menit_sebelum = Integer.parseInt(
					Common.getKonfigurasi("toleransi_jam_masuk_perkuliahan_dalam_menit_sebelum", "60").getNilai());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPayroll.java:1007");
		}

		int toleransi_jam_masuk_perkuliahan_dalam_menit_setelah = 60;
		try {
			toleransi_jam_masuk_perkuliahan_dalam_menit_setelah = Integer.parseInt(
					Common.getKonfigurasi("toleransi_jam_masuk_perkuliahan_dalam_menit_setelah", "60").getNilai());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPayroll.java:1015");
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MINUTE,
				calendar.get(Calendar.MINUTE) + toleransi_jam_masuk_perkuliahan_dalam_menit_setelah);
		String waktuSekarangPlus1 = Common.timeFormat2.get().format(calendar.getTime());

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MINUTE,
				calendar.get(Calendar.MINUTE) - toleransi_jam_masuk_perkuliahan_dalam_menit_sebelum);
		String waktuSekarangMinus1 = Common.timeFormat2.get().format(calendar.getTime());

		boolean buatDosen = ConstantValues.ABSEN_DOSEN_TERINTEGRASI_DENGAN_FINGER_PRINT;
		boolean buatGuru = ConstantValues.ABSEN_GURU_TERINTEGRASI_DENGAN_FINGER_PRINT;
		boolean buatMahasiswa = ConstantValues.ABSEN_MAHASISWA_TERINTEGRASI_DENGAN_FINGER_PRINT;
		boolean buatSiswa = ConstantValues.ABSEN_SISWA_TERINTEGRASI_DENGAN_FINGER_PRINT;

//		System.out.println("buatDosen => " + buatDosen + ", buatGuru => " + buatGuru + ", buatMahasiswa => "
//				+ buatMahasiswa + ", buatSiswa => " + buatSiswa + ", waktuSekarangPlus1 => " + waktuSekarangPlus1
//				+ ", waktuSekarangMinus1 => " + waktuSekarangMinus1);

		Pertemuan pertemuanUtama = null;
		if (buatDosen) {
			if (dosen != null) {

				Criterion criterion = dosen == null ? Restrictions.sqlRestriction("1!=1")
						: Restrictions.or(Restrictions.eq("perkuliahan.dosen1", dosen),
								Restrictions.eq("perkuliahan.dosen2", dosen));

				criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen3", dosen));
				criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen4", dosen));
				criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen5", dosen));
				criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen6", dosen));
				criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen7", dosen));
				criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen8", dosen));
				criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen9", dosen));
				criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen10", dosen));

				List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("tanggal", ais.ui.util.WaktuUtil.getDate()))
						.add(Restrictions.between("waktuMulai", waktuSekarangMinus1, waktuSekarangPlus1))
						.createAlias("perkuliahan", "perkuliahan").add(criterion).addOrder(Order.asc("waktuMulai"))
						.list();

				System.out.println(
						"dosen => " + pegawai.getDosen() + ", pertemuans => " + pertemuans + ", waktuSekarangPlus1 => "
								+ waktuSekarangPlus1 + ", waktuSekarangMinus1 => " + waktuSekarangMinus1);

				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						Statusabsensi statusabsensi = ConstantValues.MASUK;

						String keterangan = "";
						keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

						pertemuan.populate(dosen.getId(), statusabsensi, keterangan, null,
								Common.timeFormat2.get().format(WaktuUtil.getDate()), pertemuan.getWaktuSelesai(), "Dosen");
						session.getTransaction().begin();
						Common.refreshUpdate(session, pertemuan);
						session.getTransaction().commit();

						pertemuanUtama = pertemuan;
					}
				}
			}

		}

		if (buatGuru) {
			if (guru != null) {

				Criterion criterion = guru == null ? Restrictions.sqlRestriction("1!=1")
						: Restrictions.or(Restrictions.eq("jadwalPelajaran.guru", guru),
								Restrictions.eq("jadwalPelajaran.guru2", guru));

				criterion = Restrictions.or(criterion, Restrictions.eq("jadwalPelajaran.guru3", guru));
				criterion = Restrictions.or(criterion, Restrictions.eq("jadwalPelajaran.guru4", guru));
				criterion = Restrictions.or(criterion, Restrictions.eq("jadwalPelajaran.guru5", guru));
				criterion = Restrictions.or(criterion, Restrictions.eq("jadwalPelajaran.guru6", guru));
				criterion = Restrictions.or(criterion, Restrictions.eq("jadwalPelajaran.guru7", guru));
				criterion = Restrictions.or(criterion, Restrictions.eq("jadwalPelajaran.guru8", guru));
				criterion = Restrictions.or(criterion, Restrictions.eq("jadwalPelajaran.guru9", guru));
				criterion = Restrictions.or(criterion, Restrictions.eq("jadwalPelajaran.guru10", guru));
				criterion = Restrictions.or(criterion, Restrictions.eq("jadwalPelajaran.guru11", guru));
				criterion = Restrictions.or(criterion, Restrictions.eq("jadwalPelajaran.guru12", guru));

				List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("tanggal", ais.ui.util.WaktuUtil.getDate()))
						.add(Restrictions.between("waktuMulai", waktuSekarangMinus1, waktuSekarangPlus1))
						.createAlias("jadwalPelajaran", "jadwalPelajaran").add(criterion)
						.addOrder(Order.asc("waktuMulai")).list();

				System.out.println(
						"guru => " + pegawai.getGuru() + ", pertemuans => " + pertemuans + ", waktuSekarangPlus1 => "
								+ waktuSekarangPlus1 + ", waktuSekarangMinus1 => " + waktuSekarangMinus1);

				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						Statusabsensi statusabsensi = ConstantValues.MASUK;

						String keterangan = "";
						keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

						pertemuan.populate(guru.getId(), statusabsensi, keterangan, null,
								Common.timeFormat2.get().format(WaktuUtil.getDate()), pertemuan.getWaktuSelesai(), "Guru");
						session.getTransaction().begin();
						Common.refreshUpdate(session, pertemuan);
						session.getTransaction().commit();

						pertemuanUtama = pertemuan;
					}

				}
			}

		}

		if (buatMahasiswa) {

			if (mahasiswa != null) {

				List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("tanggal", ais.ui.util.WaktuUtil.getDate()))
						.add(Restrictions.between("waktuMulai", waktuSekarangMinus1, waktuSekarangPlus1))
						.add(Restrictions.sqlRestriction(
								"this_.perkuliahan in (select perkuliahan from detailperkuliahan where mahasiswa="
										+ mahasiswa.getId() + ")"))
						.addOrder(Order.asc("waktuMulai")).list();

				System.out.println(
						"mahasiswa => " + mahasiswa + ", pertemuans => " + pertemuans + ", waktuSekarangPlus1 => "
								+ waktuSekarangPlus1 + ", waktuSekarangMinus1 => " + waktuSekarangMinus1);

				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						Statusabsensi statusabsensi = ConstantValues.MASUK;

						String keterangan = "";
						keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

						pertemuan.populate(mahasiswa.getId(), statusabsensi, keterangan, null,
								Common.timeFormat2.get().format(WaktuUtil.getDate()), pertemuan.getWaktuSelesai(),
								"Mahasiswa");
						session.getTransaction().begin();
						Common.refreshUpdate(session, pertemuan);
						session.getTransaction().commit();

						pertemuanUtama = pertemuan;
					}
				}

			}
		}

		if (buatSiswa) {

			if (siswa != null) {

				String sql1 = "kelas_id in (select kelas_id from sekolah.kelas_punya_siswa where siswa_id="
						+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";

				String sql2 = "kelas_les_siswa in (select kelas_id from sekolah.kelas_les_punya_siswa where siswa_id="
						+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";

				List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("tanggal", ais.ui.util.WaktuUtil.getDate()))
						.add(Restrictions.between("waktuMulai", waktuSekarangMinus1, waktuSekarangPlus1))
						.add(Restrictions.sqlRestriction(
								"this_.jadwal_pelajaran in (select id from sekolah.jadwal_pelajaran where " + sql1
										+ " or " + sql2 + ")"))
						.addOrder(Order.asc("waktuMulai")).list();

				System.out.println("siswa => " + siswa + ", pertemuans => " + pertemuans + ", waktuSekarangPlus1 => "
						+ waktuSekarangPlus1 + ", waktuSekarangMinus1 => " + waktuSekarangMinus1);

				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						Statusabsensi statusabsensi = ConstantValues.MASUK;

						String keterangan = "";
						keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

						pertemuan.populate(siswa.getId(), statusabsensi, keterangan, null,
								Common.timeFormat2.get().format(WaktuUtil.getDate()), pertemuan.getWaktuSelesai(), "Siswa");
						session.getTransaction().begin();
						Common.refreshUpdate(session, pertemuan);
						session.getTransaction().commit();

						pertemuanUtama = pertemuan;
					}
				}

			}
		}

		return pertemuanUtama;
	}

}
