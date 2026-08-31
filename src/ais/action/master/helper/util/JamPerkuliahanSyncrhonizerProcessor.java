package ais.action.master.helper.util;

import java.util.List;
import java.util.TimerTask;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.ChecklistBaruPenilaianDosenOlehMahasiswa;
import ais.database.model.ChecklistPenilaianDosenOlehMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.DosenPembimbingAkademikTemporary;
import ais.database.model.KelasPunyaMahasiswaTemporary;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MatakuliahEkivalen;
import ais.database.model.NilaiTemporary;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.Statuskehadiran_old;

/**
 * Tugas terjadwal ({@link TimerTask}) yang, meski namanya menyiratkan sinkronisasi jam
 * perkuliahan, sebenarnya berfungsi sebagai <b>kumpulan proses migrasi/sinkronisasi data batch</b>
 * dari berbagai tabel "staging"/temporary ke entitas permanen: nilai temporary ke
 * {@link Detailperkuliahan}, absensi lama ({@link Statuskehadiran_old}) ke {@link Pertemuan},
 * checklist penilaian dosen lama ke {@link ChecklistBaruPenilaianDosenOlehMahasiswa}, dosen
 * pembimbing akademik temporary ke {@link Mahasiswa}, kelas temporary ke {@link Mahasiswa}, dan
 * populasi silang {@link MatakuliahEkivalen}. Setiap proses migrasi bersifat idempoten —
 * memproses hanya baris yang belum ditandai selesai ({@code udah}/{@code udahMasuk} bernilai
 * {@code false}/{@code null}) dan menandainya {@code true} setelah berhasil — sehingga aman
 * dijalankan berulang kali tanpa memproses ulang data yang sama.
 *
 * <p>
 * {@link #run()} (dipanggil timer) hanya menjalankan sebagian kecil proses yang tersedia di kelas
 * ini ({@link #prosesMigrasiNilaiTanpaTahunAkademik()}, {@link #prosesMigrasiNilai()},
 * {@link #processMigrasiAbsensi()}, {@link #processChecklistPenilaianDosenOlehMahasiswa(Mahasiswa)})
 * di dalam satu thread terpisah, agar tidak memblokir thread timer. Method statis lainnya
 * ({@link #processMigrasiCicilan()}, {@link #procesDosenPa()}, {@link #procesKelas()},
 * {@link #processMigrasiEkivalen()}) tampaknya dipanggil dari tempat lain (mis. tombol admin)
 * atau merupakan proses migrasi historis yang sudah tidak lagi dijadwalkan otomatis (lihat juga
 * blok besar logika penghitungan {@code JamPerkuliahan} otomatis yang dikomentari nonaktif di
 * {@link #doProcess()} — riwayat migrasi sekali-jalan yang sengaja dimatikan, dibiarkan apa
 * adanya sesuai instruksi untuk tidak mengubah kode fungsional).
 * </p>
 *
 * <p>
 * Pola umum setiap method migrasi: ambil daftar id kandidat lewat query proyeksi ringan (sesi
 * ditutup segera setelahnya), lalu iterasi satu-per-satu dengan sesi Hibernate native BARU per
 * baris (dibuka dan ditutup di setiap iterasi) — menghindari satu transaksi raksasa yang menahan
 * koneksi/kunci lama saat volume data besar, dengan progres dicetak ke konsol setiap baris.
 * </p>
 */
public class JamPerkuliahanSyncrhonizerProcessor extends TimerTask {

	/** Dipanggil oleh timer/scheduler; mendelegasikan ke {@link #doProcess()}. */
	@Override
	public void run() {
		doProcess();
	}

	/**
	 * Memindahkan nilai dari {@link NilaiTemporary} (baris dengan {@code jumlah > 0.1} dan belum
	 * {@code udahMasuk}) ke {@link Detailperkuliahan} terkait lewat
	 * {@link Detailperkuliahan#populateDetailNilai}, hanya dijalankan bila konfigurasi
	 * {@code aktifkan_proses_migrasi_nilai} aktif.
	 */
	@SuppressWarnings("unchecked")
	public static void prosesMigrasiNilai() {
		Konfigurasi aktifkan_proses_migrasi_nilai = Common.getKonfigurasi("aktifkan_proses_migrasi_nilai",
				Konfigurasi.TIDAK_AKTIF);

		if (aktifkan_proses_migrasi_nilai.getNilai().equals(Konfigurasi.AKTIF)) {
			Session session = HibernateUtil.currentNativeSession();
			List<Long> indsNilaiTemporary = session.createCriteria(NilaiTemporary.class)
					.setProjection(Projections.property("id")).add(Restrictions.gt("jumlah", 0.1))
					.add(Restrictions.or(Restrictions.eq("udahMasuk", false), Restrictions.isNull("udahMasuk"))).list();
			HibernateUtil.closeSession();

			int size = indsNilaiTemporary.size();
			System.out.println("indsNilaiTemporary => " + size);

			int index = 0;
			for (Long id : indsNilaiTemporary) {
				index++;
				session = HibernateUtil.currentNativeSession();
				NilaiTemporary nilaiTemporary = (NilaiTemporary) session.createCriteria(NilaiTemporary.class)
						.add(Restrictions.idEq(id)).uniqueResult();
				if (nilaiTemporary != null) {
					System.out.println("ubah nilai => " + nilaiTemporary.getDetailperkuliahan().getId() + " "
							+ nilaiTemporary.getFormatNilai().getStatusPertemuan().getNama() + " "
							+ nilaiTemporary.getJumlah() + " => " + ((index * 100.0) / size) + " %");
					Detailperkuliahan detailperkuliahan = nilaiTemporary.getDetailperkuliahan();
					detailperkuliahan.populateDetailNilai(nilaiTemporary.getFormatNilai(), null,
							nilaiTemporary.getJumlah(), true, null);
					nilaiTemporary.setUdahMasuk(true);
					session.getTransaction().begin();
					Common.refreshUpdate(session, nilaiTemporary);
					Common.refreshUpdate(session, detailperkuliahan);
					session.getTransaction().commit();
				}
				HibernateUtil.closeSession();
			}
		}
	}

	/**
	 * Memperbaiki baris {@link Detailperkuliahan} yang memiliki {@code semester > 0} tetapi
	 * {@code tahunAkademik} kosong, dengan memicu ulang {@code refreshUpdate} (yang diasumsikan
	 * menghitung/mengisi tahun akademik dari semester lewat logika pada entitas/listener).
	 * Kegagalan per-baris di-rollback dan dilewati tanpa menghentikan proses baris lain.
	 */
	@SuppressWarnings("unchecked")
	public static void prosesMigrasiNilaiTanpaTahunAkademik() {
		List<Long> longs = null;
		Session session = HibernateUtil.currentNativeSession();
		try {
			longs = session.createCriteria(Detailperkuliahan.class).setProjection(Projections.property("id"))
					.add(Restrictions.gt("semester", 0)).add(Restrictions.isNull("tahunAkademik")).list();
		} finally {
			HibernateUtil.closeSession();
		}
		if (longs == null || longs.isEmpty()) return;

		int size = longs.size();
		System.out.println("longs nilai tanpa TA => " + size);

		int index = 0;
		for (Long id : longs) {
			index++;
			session = HibernateUtil.currentNativeSession();
			try {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.idEq(id)).uniqueResult();
				if (detailperkuliahan != null) {
					System.out.println("ubah nilai => semester " + detailperkuliahan.getSemester() + " TA "
							+ detailperkuliahan.getTahunAkademik() + " Mahasiswa " + detailperkuliahan.getMahasiswa()
							+ " => " + ((index * 100.0) / size) + " %");

					session.getTransaction().begin();
					try {
						Common.refreshUpdate(session, detailperkuliahan);
						session.getTransaction().commit();
					} catch (Exception exTx) {
						try { if (session.getTransaction() != null && session.getTransaction().isActive()) session.getTransaction().rollback(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/util/JamPerkuliahanSyncrhonizerProcessor.java:105");}
					}
				}
			} finally {
				HibernateUtil.closeSession();
			}
		}
	}

	/**
	 * Memigrasikan data absensi lama ({@link Statuskehadiran_old}, model kehadiran versi
	 * sebelumnya) ke {@link Pertemuan#populate} pada model absensi baru, untuk pertemuan yang
	 * kolom {@code absensi}-nya masih kosong. Baris dengan referensi {@code statusabsensi}/
	 * {@code pertemuan} tidak lengkap (null) dilewati dengan aman, bukan menyebabkan
	 * {@link NullPointerException}.
	 */
	@SuppressWarnings("unchecked")
	public static void processMigrasiAbsensi() {
		System.out.println("=====> processMigrasiAbsensi ======");
		Session session1 = HibernateUtil.currentNativeSession();
		List<Object[]> datas = session1.createCriteria(Statuskehadiran_old.class)
				.add(Restrictions.or(Restrictions.isNotNull("mahasiswa"), Restrictions.isNotNull("dosen")))
				.setProjection(Projections.projectionList().add(Projections.property("statusabsensi.id"))
						.add(Projections.property("mahasiswa.id")).add(Projections.property("dosen.id"))
						.add(Projections.property("pertemuan.id")))
				.createAlias("pertemuan", "pertemuan").add(Restrictions.or(Restrictions.isNull("pertemuan.absensi"),
						Restrictions.eq("pertemuan.absensi", "")))
				.list();
		HibernateUtil.closeSession();
		int size = datas.size();
		int index = 1;
		for (Object[] id : datas) {
			double persen = ((index++) * 100.0 / size);
			System.out.println("memproses data absen, " + index + " dari " + size + ", "
					+ Common.numberFormat.get().format(persen) + " %");
			Session session = HibernateUtil.currentNativeSession();
			try {
				// id[0] (statusabsensi.id) dan id[3] (pertemuan.id) bisa null kalau data
				// Statuskehadiran_old belum lengkap (relasi statusabsensi/pertemuan kosong).
				// Sebelumnya langsung di-parseLong tanpa cek null -> NPE saat id[0]==null.
				if (id[0] == null || id[3] == null) {
					// data tidak lengkap, lewati baris ini
				} else {
					Statusabsensi statusabsensi = (Statusabsensi) session.createCriteria(Statusabsensi.class)
							.add(Restrictions.idEq(Long.parseLong(id[0].toString()))).uniqueResult();
					Object mhs = id[1] == null ? id[2] : id[1];
					Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.idEq(Long.parseLong(id[3].toString()))).uniqueResult();
					// statusabsensi/pertemuan bisa null (mis. pertemuan tidak aktif atau sudah
					// dihapus) dan mhs bisa null jika mahasiswa & dosen dua-duanya kosong ->
					// jaga agar tidak NPE saat dipakai di bawah.
					if (statusabsensi != null && pertemuan != null && mhs != null) {
						pertemuan.populate(Long.parseLong(mhs.toString()), statusabsensi, pertemuan.getWaktuMulai(),
								pertemuan.getWaktuSelesai(), "Mahasiswa");
						session.getTransaction().begin();
						session.update(pertemuan);
						session.getTransaction().commit();
					}
				}

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/JamPerkuliahanSyncrhonizerProcessor.java:147");
				// Common.tampilErrorJikaAdmin(e);
			}
			HibernateUtil.closeSession();
		}

	}

	/**
	 * Menandai {@code udah=true} pada baris {@link BiodataCalonMahasiswa} aktif yang belum
	 * ditandai selesai (hingga 10.000 baris terurut id menurun per pemanggilan). Blok migrasi
	 * {@code Perkuliahan} (memicu {@code reInitDetailperkuliahan}/{@code reInitPertemuan}) di atas
	 * kode aktif method ini dikomentari nonaktif — dibiarkan apa adanya sesuai instruksi.
	 */
	@SuppressWarnings("unchecked")
	public static void processMigrasiCicilan() {
		System.out.println("=====> processMigrasiCicilan ======");

//		Session session1 = HibernateUtil.currentNativeSession();
//		List<Long> datas = session1.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.property("id"))
////				.add(Restrictions.or(Restrictions.isNull("udah"), Restrictions.eq("udah", false)))
//				.addOrder(Order.desc("id")).setMaxResults(10000).list();
//		HibernateUtil.closeSession();
//		int size = datas.size();
//		int index = 1;
//		for (Long id : datas) {
//			double persen = ((index++) * 100.0 / size);
//			System.out.println("memproses data perkuliahan, " + index + " dari " + size + ", "
//					+ Common.numberFormat.get().format(persen) + " %");
//			Session session = HibernateUtil.currentNativeSession();
//			try {
//
//				Perkuliahan perkuliahan = (Perkuliahan) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//						.add(Restrictions.idEq(id)).uniqueResult();
//				perkuliahan.reInitDetailperkuliahan(session);
//				perkuliahan.reInitPertemuan(session);
////				perkuliahan.setUdah(true);
//				session.getTransaction().begin();
//				session.update(perkuliahan);
//				session.getTransaction().commit();
//
//			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/JamPerkuliahanSyncrhonizerProcessor.java:182");
//				HibernateUtil.rollbackTransaction();
//			}
//			HibernateUtil.closeSession();
//		}

		Session session1 = HibernateUtil.currentNativeSession();
		List<Long> datas = session1.createCriteria(BiodataCalonMahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.property("id")).addOrder(Order.desc("id")).setMaxResults(10000)
				.add(Restrictions.or(Restrictions.isNull("udah"), Restrictions.eq("udah", false))).list();
		HibernateUtil.closeSession();
		int size = datas.size();
		int index = 1;
		for (Long id : datas) {
			double persen = ((index++) * 100.0 / size);
			System.out.println("memproses data cicilan calon mahasiswa, " + index + " dari " + size + ", "
					+ Common.numberFormat.get().format(persen) + " %");
			Session session = HibernateUtil.currentNativeSession();
			try {

				BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
						.createCriteria(BiodataCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(id)).uniqueResult();

				biodataCalonMahasiswa.setUdah(true);
				session.getTransaction().begin();
				session.update(biodataCalonMahasiswa);
				session.getTransaction().commit();

			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();
			}
			HibernateUtil.closeSession();
		}

		

	}

	/**
	 * Memigrasikan baris {@link ChecklistPenilaianDosenOlehMahasiswa} lama (yang belum tertaut ke
	 * {@link ChecklistBaruPenilaianDosenOlehMahasiswa}) ke model checklist baru, dengan
	 * deduplikasi berdasarkan kunci unik {@code mahasiswa_perkuliahan_dosen}: baris baru yang sudah
	 * ada untuk kombinasi tersebut diperbarui, bukan diduplikasi.
	 *
	 * @param m bila diberikan, migrasi dibatasi hanya untuk mahasiswa tersebut; bila {@code null},
	 *          seluruh baris yang belum dimigrasikan diproses
	 */
	@SuppressWarnings("unchecked")
	public static void processChecklistPenilaianDosenOlehMahasiswa(Mahasiswa m) {
		System.out.println("=====> processChecklistPenilaianDosenOlehMahasiswa ======");
		Session session1 = HibernateUtil.currentNativeSession();
		List<Long> datas = session1.createCriteria(ChecklistPenilaianDosenOlehMahasiswa.class)
				.add(m == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa", m))
				.setProjection(Projections.property("id"))
				.add(Restrictions.isNull("checklistBaruPenilaianDosenOlehMahasiswa")).list();
		HibernateUtil.closeSession();

		// int size = datas.size();
		// int index = 1;
		for (Long id : datas) {

			Session session = HibernateUtil.currentNativeSession();

			try {
				ChecklistPenilaianDosenOlehMahasiswa checklistPenilaianDosenOlehMahasiswa = (ChecklistPenilaianDosenOlehMahasiswa) session
						.createCriteria(ChecklistPenilaianDosenOlehMahasiswa.class).add(Restrictions.idEq(id))
						.uniqueResult();

				String kodeUnik = checklistPenilaianDosenOlehMahasiswa.getMahasiswa().getId() + "_"
						+ checklistPenilaianDosenOlehMahasiswa.getPerkuliahan().getId() + "_"
						+ checklistPenilaianDosenOlehMahasiswa.getDosen().getId();

				ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa = (ChecklistBaruPenilaianDosenOlehMahasiswa) session
						.createCriteria(ChecklistBaruPenilaianDosenOlehMahasiswa.class)
						.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
				if (checklistBaruPenilaianDosenOlehMahasiswa == null) {
					checklistBaruPenilaianDosenOlehMahasiswa = new ChecklistBaruPenilaianDosenOlehMahasiswa();
				}
				checklistBaruPenilaianDosenOlehMahasiswa.setValue(checklistPenilaianDosenOlehMahasiswa.getNilai(),
						checklistPenilaianDosenOlehMahasiswa.getMahasiswa(),
						checklistPenilaianDosenOlehMahasiswa.getDosen(),
						checklistPenilaianDosenOlehMahasiswa.getPerkuliahan(),
						checklistPenilaianDosenOlehMahasiswa.getChecklistPenilaianDosen(), "");
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, checklistBaruPenilaianDosenOlehMahasiswa);
				session.getTransaction().commit();

				session.getTransaction().begin();
				checklistPenilaianDosenOlehMahasiswa
						.setChecklistBaruPenilaianDosenOlehMahasiswa(checklistBaruPenilaianDosenOlehMahasiswa);
				Common.refreshSaveOrUpdate(session, checklistPenilaianDosenOlehMahasiswa);
				session.getTransaction().commit();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			HibernateUtil.closeSession();
		}

	}

	/** Menerapkan penugasan dosen Pembimbing Akademik dari baris {@link DosenPembimbingAkademikTemporary} yang belum diproses (`udah` null) ke field {@code dosen} pada {@link Mahasiswa} terkait, lalu menandai baris temporary tersebut selesai. */
	@SuppressWarnings("unchecked")
	public static void procesDosenPa() {
		System.out.println("=====> procesDosenPa ======");
		Session session1 = HibernateUtil.currentNativeSession();
		List<Long> datas = session1.createCriteria(DosenPembimbingAkademikTemporary.class)
				.setProjection(Projections.property("id")).add(Restrictions.isNull("udah")).list();
		HibernateUtil.closeSession();

		int size = datas.size();
		int index = 1;
		for (Long id : datas) {

			Session session = HibernateUtil.currentNativeSession();

			try {
				DosenPembimbingAkademikTemporary dosenPembimbingAkademikTemporary = (DosenPembimbingAkademikTemporary) session
						.createCriteria(DosenPembimbingAkademikTemporary.class).add(Restrictions.idEq(id))
						.uniqueResult();

				Mahasiswa mahasiswa = dosenPembimbingAkademikTemporary.getMahasiswa();

				double persen = ((index++) * 100.0 / size);
				System.out.println("procesDosenPa memproses data " + mahasiswa + " " + persen + "%");

				mahasiswa.setDosen(dosenPembimbingAkademikTemporary.getId());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, mahasiswa);
				session.getTransaction().commit();

				session.getTransaction().begin();
				dosenPembimbingAkademikTemporary.setUdah(true);
				Common.refreshSaveOrUpdate(session, dosenPembimbingAkademikTemporary);
				session.getTransaction().commit();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			HibernateUtil.closeSession();
		}

	}

	/** Menerapkan penugasan kelas dari baris {@link KelasPunyaMahasiswaTemporary} yang belum diproses ke field {@code kelas} pada {@link Mahasiswa} terkait, lalu menandai baris temporary tersebut selesai. */
	@SuppressWarnings("unchecked")
	public static void procesKelas() {
		System.out.println("=====> procesKelas ======");
		Session session1 = HibernateUtil.currentNativeSession();
		List<Long> datas = session1.createCriteria(KelasPunyaMahasiswaTemporary.class)
				.setProjection(Projections.property("id")).add(Restrictions.isNull("udah")).list();
		HibernateUtil.closeSession();

		int size = datas.size();
		int index = 1;
		for (Long id : datas) {

			Session session = HibernateUtil.currentNativeSession();

			try {
				KelasPunyaMahasiswaTemporary kelasPunyaMahasiswaTemporary = (KelasPunyaMahasiswaTemporary) session
						.createCriteria(KelasPunyaMahasiswaTemporary.class).add(Restrictions.idEq(id)).uniqueResult();

				Mahasiswa mahasiswa = kelasPunyaMahasiswaTemporary.getMahasiswa();

				double persen = ((index++) * 100.0 / size);
				System.out.println("procesKelas memproses data " + mahasiswa + " " + persen + "%");

				mahasiswa.setKelas(kelasPunyaMahasiswaTemporary.getNama());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, mahasiswa);
				session.getTransaction().commit();

				session.getTransaction().begin();
				kelasPunyaMahasiswaTemporary.setUdah(true);
				Common.refreshSaveOrUpdate(session, kelasPunyaMahasiswaTemporary);
				session.getTransaction().commit();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			HibernateUtil.closeSession();
		}

	}

	/**
	 * Untuk setiap {@link MatakuliahEkivalen} aktif yang belum diproses (hingga 10.000 baris
	 * terurut id menurun per pemanggilan), memanggil {@code populateEkivalen} pada KEDUA
	 * matakuliah yang terlibat (asal dan ekivalennya) agar indeks/cache ekivalensi keduanya
	 * konsisten, lalu menandai baris tersebut selesai.
	 */
	@SuppressWarnings("unchecked")
	public static void processMigrasiEkivalen() {
		System.out.println("=====> processMigrasiEkivalen ======");
		Session session1 = HibernateUtil.currentNativeSession();
		List<Long> datas = session1.createCriteria(MatakuliahEkivalen.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.property("id"))
				.add(Restrictions.or(Restrictions.isNull("udah"), Restrictions.eq("udah", false)))
				.addOrder(Order.desc("id")).setMaxResults(10000).list();
		HibernateUtil.closeSession();
		int size = datas.size();
		int index = 1;
		for (Long id : datas) {
			double persen = ((index++) * 100.0 / size);
			System.out.println("memproses data cicilan processMigrasiEkivalen, " + index + " dari " + size + ", "
					+ Common.numberFormat.get().format(persen) + " %");
			Session session = HibernateUtil.currentNativeSession();
			try {
				MatakuliahEkivalen matakuliahEkivalen = (MatakuliahEkivalen) session
						.createCriteria(MatakuliahEkivalen.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(id)).uniqueResult();
				matakuliahEkivalen.getMatakuliah().populateEkivalen(matakuliahEkivalen);
				matakuliahEkivalen.getMatakuliahEkivalen().populateEkivalen(matakuliahEkivalen);
				matakuliahEkivalen.setUdah(true);
				session.getTransaction().begin();
				session.update(matakuliahEkivalen);
				session.getTransaction().commit();

			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();
			}
			HibernateUtil.closeSession();
		}

	}

	/** Belum diimplementasikan — hanya menyusun string query tanpa menjalankannya. Dipanggil dari baris yang dikomentari nonaktif di {@link #doProcess()}. */
	@SuppressWarnings("unused")
	private void removeLargeObjectDiskusi() {
		String query = "select foto from lampiran_lain where date(tanggal_dirubah) < ";
	}

	/**
	 * Menjalankan subset proses migrasi (lihat javadoc kelas) di dalam satu thread terpisah agar
	 * tidak memblokir thread timer. Blok besar logika penghitungan otomatis {@code JamPerkuliahan}
	 * dari data jadwal perkuliahan lama di bawahnya dikomentari nonaktif — riwayat migrasi
	 * sekali-jalan yang sengaja dimatikan, dibiarkan apa adanya sesuai instruksi.
	 */
	private void doProcess() {

		new Thread(new Runnable() {

			@Override
			public void run() {
				// processMigrasiEkivalen();
				// processMigrasiCicilan();

//				removeLargeObjectDiskusi();

				prosesMigrasiNilaiTanpaTahunAkademik();

				prosesMigrasiNilai();
				processMigrasiAbsensi();
				processChecklistPenilaianDosenOlehMahasiswa(null);
			}
		}).start();

		// Konfigurasi jam_perkuliahan_syncrhonizer =
		// Common.getKonfigurasi("jam_perkuliahan_synchronizer",
		// Konfigurasi.TIDAK_AKTIF);
		//
		// if
		// (jam_perkuliahan_syncrhonizer.getNilai().equals(Konfigurasi.AKTIF)) {
		//
		// Integer minJam = 0;
		// Konfigurasi jam_perkuliahan_syncrhonizer_min_jam = Common
		// .getKonfigurasi("jam_perkuliahan_syncrhonizer_min_jam",
		// Konfigurasi.AKTIF, "6", "", "");
		//
		// if
		// (jam_perkuliahan_syncrhonizer_min_jam.getNilai().equals(Konfigurasi.AKTIF))
		// {
		// try {
		// minJam =
		// Integer.parseInt(jam_perkuliahan_syncrhonizer_min_jam.getInfo1().trim());
		// } catch (NumberFormatException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/JamPerkuliahanSyncrhonizerProcessor.java:438");
		// Common.tampilErrorJikaAdmin(e);
		// }
		// }
		//
		// Session session = HibernateUtil.currentNativeSession();
		// session.getTransaction().begin();
		// String sql = "update perkuliahan set jam_perkuliahan = null;";
		// System.out.println(sql);
		// session.createSQLQuery(sql).executeUpdate();
		//
		// session.getTransaction().commit();
		//
		// List<Jurusan> jurusans =
		// session.createCriteria(Jurusan.class).list();
		// HibernateUtil.closeSession();
		//
		// for (Jurusan jurusan : jurusans) {
		//
		// session = HibernateUtil.currentNativeSession();
		// try {
		// sql = "select cast(replace(replace(waktu_mulai,'.',':'),',',':') as
		// TIME) as mulai, "
		// + "cast(replace(replace(waktu_selesai,'.',':'),',',':') as TIME) as
		// sampai from perkuliahan a "
		// + "where a.jurusan = " + jurusan.getId()
		// + " and waktu_mulai is not null and waktu_selesai is not null and
		// trim(waktu_mulai) != '' and trim(waktu_selesai) != '' "
		// + " and
		// to_number(to_char(cast(replace(replace(waktu_mulai,'.',':'),',',':')
		// as TIME),'HH24'),'9999') > "
		// + minJam + " "
		// + " and
		// to_number(to_char(cast(replace(replace(waktu_selesai,'.',':'),',',':')
		// as TIME),'HH24'),'9999') > "
		// + minJam + " and waktu_selesai ~ '^[0-9\\.]+$' and waktu_mulai ~
		// '^[0-9\\.]+$' "
		// + "group by cast(replace(replace(waktu_mulai,'.',':'),',',':') as
		// TIME),cast(replace(replace(waktu_selesai,'.',':'),',',':') as TIME) "
		// + "order by mulai,sampai;";
		// System.out.println(sql);
		// List<Object[]> dates = session.createSQLQuery(sql).list();
		// int i = 1;
		// session.getTransaction().begin();
		// for (Object[] myDates : dates) {
		// JamPerkuliahan jamPerkuliahan = (JamPerkuliahan)
		// session.createCriteria(JamPerkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"),
		// Restrictions.eq("aktif", true)))
		// .add(Restrictions.eq("mulai",
		// myDates[0])).add(Restrictions.eq("sampai", myDates[1]))
		// .add(Restrictions.eq("jurusan",
		// jurusan)).setMaxResults(1).uniqueResult();
		//
		// if (jamPerkuliahan == null) {
		// jamPerkuliahan = new JamPerkuliahan();
		// jamPerkuliahan.setFakultas(jurusan.getFakultas());
		// jamPerkuliahan.setJurusan(jurusan);
		// jamPerkuliahan.setKeterangan("");
		// jamPerkuliahan.setMulai((Date) myDates[0]);
		// jamPerkuliahan.setNama("Jam ke " + i);
		// jamPerkuliahan.setSampai((Date) myDates[1]);
		// session.save(jamPerkuliahan);
		// }
		// i++;
		// }
		// session.getTransaction().commit();
		// } catch (Exception e) {
		//
		// }
		// HibernateUtil.closeSession();
		// }
		//
		// session = HibernateUtil.currentNativeSession();
		// sql = "update perkuliahan a set jam_perkuliahan = (select max(id)
		// from jam_perkuliahan where jurusan = a.jurusan and mulai =
		// cast(replace(replace(a.waktu_mulai,'.',':'),',',':') as TIME) and
		// sampai = cast(replace(replace(waktu_selesai,'.',':'),',',':') as
		// TIME) and waktu_mulai is not null and waktu_selesai is not null and
		// trim(waktu_mulai) != '' and trim(waktu_selesai) != '' and
		// waktu_selesai ~ '^[0-9\\.]+$' and waktu_mulai ~ '^[0-9\\.]+$' );";
		// session.getTransaction().begin();
		// System.out.println(sql);
		// session.createSQLQuery(sql).executeUpdate();
		// session.getTransaction().commit();
		//
		// HibernateUtil.closeSession();
		// }
	}
}
