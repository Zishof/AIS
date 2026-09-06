package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;

import ais.action.ws.util.CommonUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PembagianKuotaPerkuliahanBerdasarkantahunAngkatan;
import ais.database.model.Perkuliahan;

/**
 * Kumpulan utilitas statis seputar KRS (Kartu Rencana Studi) mahasiswa: penyimpanan
 * detail KRS anti-duplikat, pencarian pembagian kuota perkuliahan berdasarkan tahun
 * angkatan, penghitungan total SKS yang telah diambil mahasiswa, penghitungan jumlah
 * peserta perkuliahan, dan ringkasan status penilaian satu perkuliahan. Kelas ini tidak
 * memiliki state (murni method statis) dan dipakai dari berbagai action/helper terkait
 * perkuliahan.
 */
public class KrsUtilHelper {

	/**
	 * Memastikan satu kelas benar-benar boleh ditawarkan pada KRS mahasiswa. Selain rentang
	 * angkatan kurikulum, relasi kelas ke baris kurikulum (bila sudah tersedia) harus konsisten:
	 * kurikulum dan mata kuliah pada {@link Perkuliahan} wajib sama dengan yang tersimpan pada
	 * {@link KurikulumPunyaMatakuliah}. Relasi yang masih kosong tetap diterima untuk menjaga
	 * kompatibilitas jadwal lama. Pemeriksaan ini mencegah mata kuliah dari kurikulum lama atau
	 * jadwal yang salah taut muncul sebagai pilihan KRS.
	 *
	 * @param perkuliahan kelas yang akan ditawarkan
	 * @param mahasiswa mahasiswa yang mengisi KRS
	 * @return {@code true} bila rentang angkatan sesuai dan relasi yang tersedia konsisten
	 */
	public static boolean bolehDitawarkanUntukKrs(Perkuliahan perkuliahan, Mahasiswa mahasiswa) {
		if (perkuliahan == null || mahasiswa == null || perkuliahan.getMatakuliah() == null
				|| perkuliahan.getMatakuliah().getId() == null) {
			return false;
		}
		Kurikulum kurikulum = perkuliahan.getKurikulum();
		if (kurikulum == null || kurikulum.getId() == null || !kurikulum.bolehAmbil(mahasiswa)) {
			return false;
		}
		KurikulumPunyaMatakuliah relasi = perkuliahan.getKurikulumPunyaMatakuliahTanpaSinkronisasiDosen();
		if (relasi == null) {
			return true;
		}
		if (relasi.getKurikulum() == null || relasi.getKurikulum().getId() == null
				|| relasi.getMatakuliah() == null || relasi.getMatakuliah().getId() == null) {
			return false;
		}
		return kurikulum.getId().equals(relasi.getKurikulum().getId())
				&& perkuliahan.getMatakuliah().getId().equals(relasi.getMatakuliah().getId());
	}

	/**
	 * Menyimpan satu baris {@link Detailperkuliahan} (entri KRS) hanya jika mahasiswa
	 * belum mengambil mata kuliah yang sama pada kombinasi
	 * semester+tahun akademik+status semester pendek yang sama — mencegah KRS ganda
	 * saat beberapa permintaan simpan datang hampir bersamaan (mis. klik ganda / race
	 * condition antar-request).
	 *
	 * <p>
	 * Mekanisme anti-duplikat: (1) WAJIB dipanggil di dalam transaksi Hibernate yang
	 * sedang aktif (dicek via {@code session.getTransaction().isActive()}, dilempar
	 * {@link IllegalStateException} bila tidak); (2) mengambil
	 * <b>PostgreSQL advisory transaction lock</b> ({@code pg_advisory_xact_lock}) dengan
	 * kunci numerik hasil hash dari kombinasi mahasiswa+semester+tahun
	 * akademik+status semester pendek+kode/id mata kuliah, sehingga permintaan simpan KRS
	 * untuk kombinasi identik yang datang bersamaan diserialisasi oleh database (lock
	 * otomatis dilepas saat transaksi commit/rollback); (3) setelah lock didapat, mengecek
	 * ulang (di dalam SQL native, menggabungkan {@code matakuliah_konversi} bila ada)
	 * apakah entri serupa sudah ada — bila ya, mengembalikan {@code false} tanpa menyimpan;
	 * (4) baris {@link Perkuliahan} yang direferensikan dimuat ulang dalam sesi yang sama
	 * sebelum dipakai, untuk menghindari referensi basi bila baris perkuliahan sudah
	 * dihapus operator lain di antrean sebelumnya (mengembalikan {@code false} bila
	 * ternyata sudah tidak ada, alih-alih menyebabkan pelanggaran foreign key).
	 * </p>
	 *
	 * @param session          sesi Hibernate dengan transaksi AKTIF (wajib)
	 * @param detailperkuliahan entri KRS yang akan disimpan; harus sudah punya mahasiswa
	 *                           dan (perkuliahan ATAU mata kuliah konversi) yang valid
	 * @return {@code true} bila baris berhasil disimpan (belum ada duplikat); {@code false}
	 *         bila entri serupa sudah ada, atau perkuliahan yang direferensikan sudah
	 *         terhapus
	 * @throws IllegalArgumentException bila parameter wajib kosong/tidak valid
	 * @throws IllegalStateException    bila dipanggil di luar transaksi aktif
	 * @throws org.hibernate.HibernateException bila advisory lock gagal diambil
	 */
	public static boolean simpanKrsJikaBelumAda(Session session, Detailperkuliahan detailperkuliahan) {
		if (session == null || detailperkuliahan == null || detailperkuliahan.getMahasiswa() == null
				|| detailperkuliahan.getMahasiswa().getId() == null) {
			throw new IllegalArgumentException("Session, detail KRS, dan mahasiswa wajib diisi");
		}
		if (session.getTransaction() == null || !session.getTransaction().isActive()) {
			throw new IllegalStateException("Pencegahan KRS double wajib dijalankan di dalam transaksi aktif");
		}

		Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
		if (perkuliahan != null) {
			if (perkuliahan.getId() == null) {
				throw new IllegalArgumentException("Perkuliahan pada detail KRS belum tersimpan");
			}
			/*
			 * Daftar kelas dapat tetap terbuka ketika baris perkuliahan sudah dihapus oleh
			 * operator lain. Jangan meneruskan object/proxy basi ke INSERT karena hasilnya
			 * FK violation dan seluruh transaksi menjadi aborted. Muat ulang pada transaksi
			 * yang sama; bila sudah tidak ada, anggap pilihan tidak lagi tersedia.
			 */
			Perkuliahan perkuliahanAktif = (Perkuliahan) session.get(Perkuliahan.class, perkuliahan.getId());
			if (perkuliahanAktif == null) {
				return false;
			}
			perkuliahan = perkuliahanAktif;
			detailperkuliahan.setPerkuliahan(perkuliahanAktif);
		}
		Matakuliah matakuliah = perkuliahan == null ? detailperkuliahan.getMatakuliahKonversi()
				: perkuliahan.getMatakuliah();
		if (matakuliah == null || matakuliah.getId() == null) {
			throw new IllegalArgumentException("Mata kuliah pada detail KRS wajib diisi");
		}

		Integer semester = detailperkuliahan.getSemester();
		String tahunAkademik = detailperkuliahan.getTahunAkademik();
		Integer semesterPendek = perkuliahan == null ? null : perkuliahan.getStatusSemesterPendek();
		String kode = matakuliah.getKode() == null ? "" : matakuliah.getKode().trim().toLowerCase();

		long kunci = 17L;
		kunci = (31L * kunci) + detailperkuliahan.getMahasiswa().getId().longValue();
		kunci = (31L * kunci) + (semester == null ? 0L : semester.longValue());
		kunci = (31L * kunci) + (tahunAkademik == null ? 0L : tahunAkademik.hashCode());
		kunci = (31L * kunci) + (semesterPendek == null ? 0L : semesterPendek.longValue());
		kunci = (31L * kunci) + (kode.length() == 0 ? matakuliah.getId().longValue() : kode.hashCode());
		java.sql.PreparedStatement psLock = null;
		try {
			psLock = session.connection().prepareStatement("select pg_advisory_xact_lock(?)");
			psLock.setLong(1, kunci);
			psLock.execute();
		} catch (java.sql.SQLException e) {
			throw new org.hibernate.HibernateException("Gagal mengunci proses simpan KRS agar tidak double", e);
		} finally {
			try {
				if (psLock != null) {
					psLock.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "KrsUtilHelper.close-advisory-lock-statement");
			}
		}

		StringBuilder sql = new StringBuilder();
		sql.append("select count(d.id) as jumlah from detailperkuliahan d ");
		sql.append("left join perkuliahan p on p.id=d.perkuliahan ");
		sql.append("left join matakuliah m on m.id=coalesce(d.matakuliah_konversi,p.matakuliah) ");
		sql.append("where d.mahasiswa=:mahasiswa and d.semester=:semester ");
		sql.append("and coalesce(d.tahunakademik,p.tahun_ajaran,'')=:tahunAkademik ");
		sql.append("and (m.id=:matakuliah");
		if (kode.length() > 0) {
			sql.append(" or lower(trim(m.kode))=:kode");
		}
		sql.append(") ");
		if (semesterPendek == null) {
			sql.append("and p.status_semesterpendek is null ");
		} else {
			sql.append("and p.status_semesterpendek=:semesterPendek ");
		}
		if (detailperkuliahan.getId() != null) {
			sql.append("and d.id<>:id ");
		}

		org.hibernate.SQLQuery query = session.createSQLQuery(sql.toString())
				.addScalar("jumlah", org.hibernate.Hibernate.LONG);
		query.setLong("mahasiswa", detailperkuliahan.getMahasiswa().getId());
		query.setInteger("semester", semester == null ? 0 : semester.intValue());
		query.setString("tahunAkademik", tahunAkademik == null ? "" : tahunAkademik);
		query.setLong("matakuliah", matakuliah.getId());
		if (kode.length() > 0) {
			query.setString("kode", kode);
		}
		if (semesterPendek != null) {
			query.setInteger("semesterPendek", semesterPendek);
		}
		if (detailperkuliahan.getId() != null) {
			query.setLong("id", detailperkuliahan.getId());
		}

		Number jumlah = (Number) query.uniqueResult();
		if (jumlah != null && jumlah.longValue() > 0L) {
			return false;
		}
		session.save(detailperkuliahan);
		return true;
	}

	/**
	 * Mencari konfigurasi {@link PembagianKuotaPerkuliahanBerdasarkantahunAngkatan}
	 * (kuota kelas perkuliahan yang dipecah berdasarkan rentang tahun angkatan mahasiswa)
	 * yang berlaku untuk {@code perkuliahan} dan {@code tahunangkatan} tertentu — yaitu
	 * baris dengan {@code tahunMulai <= tahunangkatan <= tahunSampai} dan kuota terbesar
	 * bila ada beberapa yang cocok. Hasil di-cache sementara (lewat
	 * {@link CommonUtil#simpanTemporary}/{@link CommonUtil#ambilTemporary}) berkunci
	 * {@code perkuliahan.id + tahunangkatan} untuk menghindari query berulang pada
	 * permintaan yang sama, kecuali {@code reload=true}.
	 *
	 * @param session       sesi Hibernate; boleh {@code null} (sesi baru dibuka via {@link HibernateUtil#ensureOpenSession})
	 * @param perkuliahan   perkuliahan yang dicari pembagian kuotanya
	 * @param tahunangkatan tahun angkatan mahasiswa yang dicek terhadap rentang kuota
	 * @param reload        paksa lewati cache dan query ulang ke database
	 * @return baris pembagian kuota yang cocok (kuota terbesar bila beberapa cocok), atau {@code null} bila tidak ada/parameter kosong
	 */
	public static PembagianKuotaPerkuliahanBerdasarkantahunAngkatan ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(
			Session session, Perkuliahan perkuliahan, Integer tahunangkatan, Boolean reload) {

		if (perkuliahan == null || perkuliahan.getId() == null || tahunangkatan == null) {
			return null;
		}

		String key = "ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan_" + perkuliahan.getId() + "_"
				+ tahunangkatan;
		if (!Boolean.TRUE.equals(reload)) {
			JSONArray array = CommonUtil.ambilTemporary(key);
			if (array.length() > 0) {
				try {
					PembagianKuotaPerkuliahanBerdasarkantahunAngkatan k = (PembagianKuotaPerkuliahanBerdasarkantahunAngkatan) Common
							.convertToObject(array.getJSONObject(0));
					k.setPerkuliahan(perkuliahan);
					return k;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsUtilHelper.java:42");
				}
			}
		}
		CommonUtil.reset(key);

		Session kerjaSession = HibernateUtil.ensureOpenSession(session);
		PembagianKuotaPerkuliahanBerdasarkantahunAngkatan pembagianKuotaPerkuliahanBerdasarkantahunAngkatan = ((PembagianKuotaPerkuliahanBerdasarkantahunAngkatan) kerjaSession
				.createCriteria(PembagianKuotaPerkuliahanBerdasarkantahunAngkatan.class)
				.add(Restrictions.eq("perkuliahan", perkuliahan)).add(Restrictions.le("tahunMulai", tahunangkatan))
				.add(Restrictions.ge("tahunSampai", tahunangkatan)).addOrder(Order.desc("kuota")).setMaxResults(1)
				.uniqueResult());

		if (pembagianKuotaPerkuliahanBerdasarkantahunAngkatan != null) {
			try {
				List<String> filePaths = new ArrayList<String>();
				filePaths.add(pembagianKuotaPerkuliahanBerdasarkantahunAngkatan.getOrCreateFileLocation());
				CommonUtil.simpanTemporary(key, filePaths);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsUtilHelper.java:61");
			}
		}

		return pembagianKuotaPerkuliahanBerdasarkantahunAngkatan;
	}

	/**
	 * Menghitung total SKS unik yang telah diambil mahasiswa untuk kombinasi
	 * tahapan/semester/semester-pendek tertentu: menggabungkan mata kuliah dari
	 * {@code hashMap} (perkuliahan yang sedang dipilih di layar, belum tentu tersimpan)
	 * dengan mata kuliah dari entri KRS yang sudah tersimpan di database
	 * ({@link Mahasiswa#ambilDetailperkuliahan}), lalu mendedupkan berdasarkan id mata
	 * kuliah sebelum menjumlahkan SKS-nya (satu mata kuliah tidak dihitung dobel walau
	 * diambil di beberapa kelas paralel). Entri hasil konversi mata kuliah diikutsertakan
	 * hanya bila konfigurasi
	 * {@code konversi_masuk_akumulasi_jumlah_sks_pengambilan_krs} aktif.
	 *
	 * @param hashMap        peta perkuliahan yang sedang dipilih (belum tentu tersimpan), boleh {@code null}
	 * @param mahasiswa      mahasiswa yang dihitung SKS-nya
	 * @param tahapan        tahapan KRS
	 * @param semester       semester akademik
	 * @param semesterPendek status semester pendek
	 * @return total SKS unik yang telah/akan diambil
	 */
	public static Integer hitungSksYangTelahDiambil(Map<Long, Perkuliahan> hashMap, Mahasiswa mahasiswa,
			Integer tahapan, Integer semester, Integer semesterPendek) {
		Map<Long, Matakuliah> map = new java.util.HashMap<Long, Matakuliah>();
		if (hashMap != null) {
			for (Perkuliahan perkuliahan : hashMap.values()) {
				if (perkuliahan.getMatakuliah() != null) {
					map.put(perkuliahan.getMatakuliah().getId(), perkuliahan.getMatakuliah());
				}
			}
		}

		Boolean termasukKonversi = Common.bolehKonfigurasi("konversi_masuk_akumulasi_jumlah_sks_pengambilan_krs", Konfigurasi.TIDAK_AKTIF);
		Integer persetujuan = null;
		List<Long> sudahDiambil = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, false, false,
				persetujuan);

		for (Long detailperkuliahanid : sudahDiambil) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {

				if (!termasukKonversi && detailperkuliahan.getMatakuliahKonversi() != null) {
					continue;
				}

				Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
						? detailperkuliahan.getMatakuliahKonversi()
						: detailperkuliahan.getPerkuliahan().getMatakuliah();
				if (matakuliah == null) {
					continue;
				}
				map.put(matakuliah.getId(), matakuliah);
			}
		}

		Integer jumlah = 0;
		for (Matakuliah d : map.values()) {
			jumlah += d.getSks();
		}
		return jumlah;
	}

	/**
	 * Menghitung jumlah total peserta (baris {@link Detailperkuliahan}) pada satu
	 * {@code perkuliahan}, memuat ulang daftar detail perkuliahan lebih dulu bila belum
	 * pernah dimuat atau {@code reload=true}.
	 *
	 * @param session     sesi Hibernate; boleh {@code null} (sesi baru dibuka via {@link HibernateUtil#ensureOpenSession})
	 * @param perkuliahan perkuliahan yang dihitung pesertanya
	 * @param reload      paksa muat ulang daftar detail perkuliahan dari database
	 * @return jumlah peserta, atau {@code 0} bila {@code perkuliahan} {@code null}
	 */
	public static Integer ambilJumlahDetailperkuliahan(Session session, Perkuliahan perkuliahan, Boolean reload) {

		if (perkuliahan == null) {
			return 0;
		}
		if (!perkuliahan.udah("detailperkulaiahan") || Boolean.TRUE.equals(reload)) {
			perkuliahan.reInitDetailperkuliahan(HibernateUtil.ensureOpenSession(session));
		}

		return perkuliahan.ambilJumlahDetailperkuliahan();
	}

	/**
	 * Memeriksa apakah {@code mahasiswa} tertentu terdaftar sebagai peserta
	 * {@code perkuliahan}.
	 *
	 * @param session     tidak digunakan langsung (parameter dipertahankan untuk kompatibilitas overload)
	 * @param perkuliahan perkuliahan yang dicek
	 * @param mahasiswa   mahasiswa yang dicek keikutsertaannya
	 * @param reload      tidak digunakan langsung (parameter dipertahankan untuk kompatibilitas overload)
	 * @return {@code 1} bila mahasiswa terdaftar, {@code 0} bila tidak
	 */
	public static Integer ambilJumlahDetailperkuliahan(Session session, Perkuliahan perkuliahan, Mahasiswa mahasiswa,
			Boolean reload) {

		Long detailperkuliahan = perkuliahan.ambilDetailperkuliahan(mahasiswa);
		return detailperkuliahan == null ? 0 : 1;
	}

	/**
	 * Menyusun ringkasan status penilaian satu {@code perkuliahan} dalam bentuk pasangan
	 * teks deskriptif dan kode status ({@link Perkuliahan#BELUM_ADA_MAHASISWA}/
	 * {@link Perkuliahan#BELUM_DINILAI}/{@link Perkuliahan#SUDAH_DINILAI}/
	 * {@link Perkuliahan#SEBAGIAN_BESAR_BELUM_DINILAI}/
	 * {@link Perkuliahan#SEBAGIAN_BESAR_SUDAH_DINILAI}), berdasarkan jumlah mahasiswa
	 * yang sudah vs belum dinilai ({@link Perkuliahan#ambilStatusPenilaian()}).
	 *
	 * @param perkuliahan perkuliahan yang dicek status penilaiannya
	 * @param reload      diteruskan tidak langsung; status dihitung ulang setiap panggilan
	 * @return array dua elemen: {@code [0]} teks status deskriptif, {@code [1]} kode status
	 */
	public static String[] rubahStatusPenilaian(Perkuliahan perkuliahan, Boolean reload) {

		String status = "";
		Integer[] s = perkuliahan.ambilStatusPenilaian();
		Integer countBelumDinilai = s[0];
		Integer countSudahDinilai = s[1];

		String kode = "";
		if (countSudahDinilai.equals(0) && countBelumDinilai.equals(0)) {
			status = ("Belum ada mahasiswa yang mengikuti perkuliahan ini");
			kode = Perkuliahan.BELUM_ADA_MAHASISWA.toString();
		} else if (countSudahDinilai.equals(0)) {
			status = ("Belum Dinilai, " + countBelumDinilai + " mahasiswa belum dinilai");
			kode = Perkuliahan.BELUM_DINILAI.toString();
		} else if (countBelumDinilai.equals(0)) {
			kode = Perkuliahan.SUDAH_DINILAI.toString();
			status = ("Sudah Dinilai, " + countSudahDinilai + " mahasiswa sudah dinilai");
		} else if (countBelumDinilai >= countSudahDinilai) {
			kode = Perkuliahan.SEBAGIAN_BESAR_BELUM_DINILAI.toString();
			status = ("Sebagian Besar Belum Dinilai, " + countBelumDinilai + " mahasiswa dari total "
					+ (countSudahDinilai + countBelumDinilai) + " mahasiswa belum dinilai");
		} else {
			kode = Perkuliahan.SEBAGIAN_BESAR_SUDAH_DINILAI.toString();
			status = ("Sebagian Besar Sudah Dinilai, " + countSudahDinilai + " mahasiswa dari total "
					+ (countSudahDinilai + countBelumDinilai) + " mahasiswa sudah dinilai");
		}

		return new String[] { status, kode };
	}
}
