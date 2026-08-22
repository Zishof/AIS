package ais.common;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KonfigurasiKalenderAkademik;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

public class KonfigurasiManager {

	/**
	 * Nama konfigurasi yang bentrok id-nya dan SUDAH dicatat ke audit. Dipakai agar
	 * kegagalan sequence hanya dilaporkan sekali per nama, bukan tiap pemanggilan.
	 */
	private static final java.util.Set<String> BENTROK_ID_TERCATAT =
		java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

	public static Konfigurasi konfigurasiKosong = new Konfigurasi("", "");

	// --- Public Methods ---

	public static Konfigurasi getKonfigurasi(String nama, String defaultValue) {
		return getKonfigurasi(nama, defaultValue, "", "", "");
	}

	public static Konfigurasi getKonfigurasi(Session session, String nama, String defaultValue) {
		return getKonfigurasi(session, nama, defaultValue, "", "", "");
	}

	public static Konfigurasi getKonfigurasi(String nama, String defaultValue, String info1, String info2,
			String info3) {
		if (nama == null || nama.trim().isEmpty()) {
			return konfigurasiKosong;
		}

		// Cek Memory DB / Cache.
		// Cache di-backing oleh MapDB; bila store sudah ditutup (mis. saat reset
		// cache / redeploy) akses map melempar java.lang.Error ("already closed"),
		// BUKAN Exception, sehingga harus ditangkap lewat Throwable. Tanpa guard ini
		// getter entity yang memanggil getKonfigurasi saat Hibernate flush ikut gagal
		// (PropertyAccessException). Bila gagal, reset referensi agar dimuat ulang.
		Konfigurasi konfigurasi = null;
		try {
			Map<String, Konfigurasi> cache = MemoryDbUtil.getKonfigurasi();
			konfigurasi = cache == null ? null : cache.get(nama);
		} catch (Throwable t) {
			konfigurasi = null;
			try {
				MemoryDbUtil.resetLocalReferences();
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KonfigurasiManager.java:57");
			}
		}

		if (konfigurasi != null) {
			return konfigurasi;
		}

		konfigurasi = konfigurasiKosong;
		Session session = null;

		try {
			// Menggunakan openSession() untuk isolasi thread yang aman
			session = HibernateUtil.getSessionFactory().openSession();

			// Ambil dari DB
			konfigurasi = (Konfigurasi) ConstantValues.simpleObject(session.createCriteria(Konfigurasi.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("nama", nama)).setMaxResults(1), Konfigurasi.class);

			// Jika di DB tidak ada, buat baru dalam koridor transaksi
			if (konfigurasi == null) {
				Transaction tx = null;
				try {
					tx = session.beginTransaction();
					konfigurasi = new Konfigurasi();
					konfigurasi.setNama(nama);
					konfigurasi.setNilai(defaultValue);
					konfigurasi.setInfo1(info1);
					konfigurasi.setInfo2(info2);
					konfigurasi.setInfo3(info3);
					Common.refreshSaveOrUpdate(session, konfigurasi);
					tx.commit();
				} catch (org.hibernate.exception.ConstraintViolationException cve) {
					// AKAR MASALAH: yang bentrok adalah PRIMARY KEY (konfigurasi_pkey), BUKAN
					// kolom nama. Ada dua sebab yang mungkin:
					//
					//   (a) Thread lain menyisipkan konfigurasi yang sama lebih dulu -- lazim
					//       karena getKonfigurasi dipanggil dari thread web DAN dari pembangun
					//       ringkasan di latar. Di sini cukup MEMBACA ULANG hasil thread itu.
					//   (b) Sequence id tabel konfigurasi TERTINGGAL di belakang max(id) --
					//       lazim setelah impor/restore data -- sehingga nextval mengembalikan
					//       id yang sudah terpakai. Ini TIDAK dapat diperbaiki dari kode;
					//       perlu setval pada sequence-nya oleh administrator basis data.
					//
					// Untuk kedua sebab, yang benar adalah TIDAK menjatuhkan pemanggil: sebelum
					// ini exception-nya dilempar ke atas dan membuat seluruh pembangunan
					// ringkasan kampus gagal hanya karena satu baris konfigurasi.
					if (tx != null && tx.isActive()) {
						tx.rollback();
					}
					Konfigurasi hasilThreadLain = null;
					try {
						session.clear();
						hasilThreadLain = (Konfigurasi) ConstantValues.simpleObject(
								session.createCriteria(Konfigurasi.class).addOrder(Order.desc("id"))
										.add(Restrictions.eq("nama", nama)).setMaxResults(1),
								Konfigurasi.class);
					} catch (Exception eBacaUlang) {
						ais.common.ErrorAuditUtil.record(eBacaUlang, "KonfigurasiManager.bacaUlangSetelahBentrok");
					}
					if (hasilThreadLain != null) {
						konfigurasi = hasilThreadLain;
					} else {
						// Sebab (b): dicatat SEKALI per nama supaya audit tidak dibanjiri --
						// getKonfigurasi termasuk jalur yang sangat sering dipanggil.
						if (BENTROK_ID_TERCATAT.add(nama)) {
							ais.common.ErrorAuditUtil.record(cve,
									"KonfigurasiManager: id konfigurasi bentrok utk '" + nama
										+ "'. Periksa sequence id tabel konfigurasi (kemungkinan tertinggal di belakang max(id)).");
						}
						konfigurasi = konfigurasiKosong;
					}
				} catch (Exception txEx) {
					if (tx != null && tx.isActive()) {
						tx.rollback();
					}
					throw txEx;
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/KonfigurasiManager.java:98");
		} finally {
			// Blok pembersihan ketat untuk mencegah connection leak & memory leak
			if (session != null) {
				try {
					session.clear();
					session.disconnect();
					session.close();
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/KonfigurasiManager.java:107");
				}
			}
		}

		// Simpan hasil ke Memory DB jika valid. Dibungkus Throwable: MapDB yang
		// sudah ditutup bisa melempar Error saat put.
		if (konfigurasi != null && konfigurasi != konfigurasiKosong) {
			try {
				Map<String, Konfigurasi> cacheSimpan = MemoryDbUtil.getKonfigurasi();
				if (cacheSimpan != null) {
					cacheSimpan.put(nama, konfigurasi);
				}
			} catch (Throwable t) {
				try {
					MemoryDbUtil.resetLocalReferences();
				} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KonfigurasiManager.java:123");
				}
			}
		}

		return konfigurasi;
	}

	/**
	 * Mengembalikan himpunan SEMUA nama konfigurasi yang ada di basis data (satu kali query).
	 *
	 * <p><b>Tujuan.</b> Mendeteksi apakah sebuah kunci berasal dari konfigurasi <b>tanpa</b>
	 * memanggil {@link #getKonfigurasi(String, String)} — yang akan MEMBUAT entri baru bila kunci
	 * belum ada (lihat method itu). Mis. dipakai penampil parameter laporan untuk menentukan
	 * parameter mana yang boleh disunting.</p>
	 *
	 * @return himpunan nama konfigurasi; himpunan kosong bila gagal (tidak pernah {@code null}).
	 */
	@SuppressWarnings("unchecked")
	public static java.util.Set<String> kumpulanNamaKonfigurasi() {
		java.util.Set<String> hasil = new java.util.HashSet<String>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			java.util.List<String> namas = session.createCriteria(Konfigurasi.class)
					.setProjection(org.hibernate.criterion.Projections.distinct(
							org.hibernate.criterion.Projections.property("nama")))
					.list();
			for (String n : namas) {
				if (n != null) {
					hasil.add(n);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/KonfigurasiManager.java:157");
		} finally {
			if (session != null) {
				try {
					session.clear();
					session.disconnect();
					session.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/KonfigurasiManager.java:164");
				}
			}
		}
		return hasil;
	}

	/**
	 * Mencari konfigurasi bernama {@code nama} secara READ-ONLY: cache dulu, lalu DB.
	 * BERBEDA dengan {@link #getKonfigurasi(String, String)}, metode ini TIDAK pernah membuat
	 * entri baru bila tidak ditemukan.
	 *
	 * @param nama kunci konfigurasi
	 * @return entitas {@link Konfigurasi} bila ada; {@code null} bila tidak ada / gagal.
	 */
	public static Konfigurasi cariKonfigurasi(String nama) {
		if (nama == null) {
			return null;
		}
		try {
			Map<String, Konfigurasi> cache = MemoryDbUtil.getKonfigurasi();
			Konfigurasi c = cache == null ? null : cache.get(nama);
			if (c != null && c != konfigurasiKosong && c.getId() != null) {
				return c;
			}
		} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KonfigurasiManager.java:189");
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			return (Konfigurasi) ConstantValues.simpleObject(session.createCriteria(Konfigurasi.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("nama", nama)).setMaxResults(1), Konfigurasi.class);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/KonfigurasiManager.java:197");
			return null;
		} finally {
			if (session != null) {
				try {
					session.clear();
					session.disconnect();
					session.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/KonfigurasiManager.java:205");
				}
			}
		}
	}

	/**
	 * Menyimpan NILAI baru untuk konfigurasi bernama {@code nama} ke basis data DAN memperbarui
	 * cache (MapDB), sehingga pembacaan berikutnya lewat {@link #getKonfigurasi(String, String)}
	 * langsung memakai nilai terbaru tanpa perlu mereset cache.
	 *
	 * <p>Bila konfigurasi belum ada, entri baru dibuat. AMAN: kegagalan di-rollback dan dicatat
	 * (tidak mematikan aplikasi / tidak shutdown).</p>
	 *
	 * @param nama  kunci konfigurasi
	 * @param nilai nilai baru
	 * @return entitas {@link Konfigurasi} tersimpan, atau {@code null} bila gagal.
	 */
	public static Konfigurasi simpanKonfigurasi(String nama, String nilai) {
		if (nama == null) {
			return null;
		}
		Session session = null;
		Transaction tx = null;
		Konfigurasi konfigurasi = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			konfigurasi = (Konfigurasi) ConstantValues.simpleObject(session.createCriteria(Konfigurasi.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("nama", nama)).setMaxResults(1), Konfigurasi.class);
			tx = session.beginTransaction();
			if (konfigurasi == null) {
				konfigurasi = new Konfigurasi();
				konfigurasi.setNama(nama);
			}
			konfigurasi.setNilai(nilai);
			Common.refreshSaveOrUpdate(session, konfigurasi);
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/KonfigurasiManager.java:246");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/KonfigurasiManager.java:249");
			return null;
		} finally {
			if (session != null) {
				try {
					session.clear();
					session.disconnect();
					session.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/KonfigurasiManager.java:257");
				}
			}
		}
		if (konfigurasi != null) {
			try {
				Map<String, Konfigurasi> cache = MemoryDbUtil.getKonfigurasi();
				if (cache != null) {
					cache.put(nama, konfigurasi);
				}
			} catch (Throwable t) {
				try {
					MemoryDbUtil.resetLocalReferences();
				} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/KonfigurasiManager.java:270");
				}
			}
		}
		return konfigurasi;
	}

	public static Konfigurasi getKonfigurasi(Session session, String nama, String defaultValue, String info1,
			String info2, String info3) {
		Konfigurasi konfigurasi = new Konfigurasi();
		konfigurasi.setNama("");
		konfigurasi.setNilai("");

		try {
			konfigurasi = (Konfigurasi) ConstantValues.simpleObject(session.createCriteria(Konfigurasi.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("nama", nama)).setMaxResults(1), Konfigurasi.class);

			if (konfigurasi == null) {
				konfigurasi = new Konfigurasi();
				konfigurasi.setNama(nama);
				konfigurasi.setNilai(defaultValue);
				konfigurasi.setInfo1(info1);
				konfigurasi.setInfo2(info2);
				konfigurasi.setInfo3(info3);
				Common.refreshSaveOrUpdate(session, konfigurasi);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return konfigurasi;
	}

	public static Konfigurasi prosesKonfigurasi(String nama, String defaultValue, Integer semester, Integer angkatan,
			Jurusan jurusan, String pro, String stsAwl) {

		String smt = semester == null ? "_smt:0" : "_smt:" + semester;
		String ang = angkatan == null ? "_ang:0" : "_ang:" + angkatan;
		String jur = jurusan == null ? "_jur:0" : "_jur:" + jurusan.getId();

		String[] kombinasiSuffix = new String[] { smt + ang + jur + pro + stsAwl, "_smt:0" + ang + jur + pro + stsAwl,
				smt + "_ang:0" + jur + pro + stsAwl, smt + ang + "_jur:0" + pro + stsAwl,
				"_smt:0_ang:0" + jur + pro + stsAwl, smt + "_ang:0_jur:0" + pro + stsAwl,
				"_smt:0_ang:0" + jur + pro + stsAwl, "_smt:0" + ang + "_jur:0" + pro + stsAwl,
				"_smt:0" + ang + "_jur:0" + pro + stsAwl, smt + "_ang:0_jur:0" + pro + stsAwl,
				"_smt:0_ang:0_jur:0" + pro + stsAwl };

		for (int i = 0; i < kombinasiSuffix.length; i++) {
			Konfigurasi k = cekKetersediaanKonfigurasi(nama + kombinasiSuffix[i]);
			if (k != null) {
				return k;
			}
		}

		return null;
	}

	public static Konfigurasi getKonfigurasi(String nama, String defaultValue, Integer semester, Integer angkatan,
			Jurusan jurusan, String program, StatusAwalMahasiswa statusAwalMahasiswa) {

		String pro = program == null ? "_pro:0" : "_pro:" + program;
		String stsAwl = statusAwalMahasiswa == null ? "" : "_statusAwal:" + statusAwalMahasiswa.getId();

		String[][] params = { { pro, stsAwl }, { pro, "" }, { "", stsAwl }, { "", "" } };

		for (int i = 0; i < params.length; i++) {
			Konfigurasi k = KonfigurasiManager.prosesKonfigurasi(nama, defaultValue, semester, angkatan, jurusan,
					params[i][0], params[i][1]);
			if (k != null && k.getNilai() != null && !k.getNilai().trim().isEmpty()) {
				return k;
			}
		}

		return KonfigurasiManager.getKonfigurasi(nama, defaultValue);
	}

	public static Konfigurasi getKonfigurasi(String nama, String defaultValue, String program, Jurusan jurusan,
			String custom) {

		String prog = program == null ? "_prog:0" : "_prog:" + program;
		String cust = custom == null ? "_cust:0" : "_cust:" + custom;
		String jur = jurusan == null ? "_jur:0" : "_jur:" + jurusan.getId();

		String[] kombinasiSuffix = new String[] { prog + cust + jur, "_prog:0" + cust + jur, prog + "_cust:0" + jur,
				prog + cust + "_jur:0", "_prog:0_cust:0" + jur, prog + "_cust:0_jur:0", "_prog:0_cust:0" + jur,
				"_prog:0" + cust + "_jur:0", "_prog:0" + cust + "_jur:0", prog + "_cust:0_jur:0" };

		for (int i = 0; i < kombinasiSuffix.length; i++) {
			Konfigurasi k = cekKetersediaanKonfigurasi(nama + kombinasiSuffix[i]);
			if (k != null) {
				return k;
			}
		}

		return KonfigurasiManager.getKonfigurasi(nama, defaultValue);
	}

	public static Konfigurasi getKonfigurasi(String jenisKonfigurasi, String tahunAkademik, String info1) {
		return getKonfigurasi(jenisKonfigurasi, tahunAkademik, info1, null, null, null, null);
	}

	// -------------------------------------------------------------------------------------
	// 1. PUBLIC METHODS (Dipertahankan agar kompatibel dengan pemanggilan dari
	// kelas lain)
	// -------------------------------------------------------------------------------------

	public static Konfigurasi checkKonfigurasiDenganKalenderAkademik(Session session, String jenisKonfigurasi,
			String tahunAkademik, String smt, String masukDiSmt, Fakultas fakultas, Jurusan jurusan, String program) {

		return fetchKonfigurasiKalender(session, jenisKonfigurasi, tahunAkademik, smt, masukDiSmt, fakultas, jurusan,
				null, null, program, false);
	}

	public static Konfigurasi checkKonfigurasiDenganKalenderAkademikAktif(Session session, String jenisKonfigurasi,
			String masukDiSmt, Fakultas fakultas, Jurusan jurusan, String program) {

		return fetchKonfigurasiKalender(session, jenisKonfigurasi, null, null, masukDiSmt, fakultas, jurusan, null,
				null, program, true);
	}

	public static Konfigurasi checkKonfigurasiDenganKalenderAkademik(Session session, String jenisKonfigurasi,
			String tahunAkademik, String smt, String masukDiSmt, Yayasan yayasan, Sekolah sekolah, String program) {

		return fetchKonfigurasiKalender(session, jenisKonfigurasi, tahunAkademik, smt, masukDiSmt, null, null, yayasan,
				sekolah, program, false);
	}

	// -------------------------------------------------------------------------------------
	// 2. PRIVATE HELPER METHOD (Pusat logika query untuk memaksimalkan reuse)
	// -------------------------------------------------------------------------------------

	private static Konfigurasi fetchKonfigurasiKalender(Session session, String jenisKonfigurasi, String tahunAkademik,
			String smt, String masukDiSmt, Fakultas fakultas, Jurusan jurusan, Yayasan yayasan, Sekolah sekolah,
			String program, boolean isAktifOnly) {

		if (masukDiSmt != null && masukDiSmt.trim().isEmpty()) {
			masukDiSmt = null;
		}

		Date[] range = getStartEndOfDay();

		// A. Kriteria Dasar yang berlaku untuk semua tipe
		Criteria criteria = session.createCriteria(KonfigurasiKalenderAkademik.class)
				.createAlias("kalenderAkademik", "kalenderAkademik").createAlias("konfigurasi", "konfigurasi")
				.add(Restrictions.eq("konfigurasi.nama", jenisKonfigurasi))
				.add(Restrictions.le("kalenderAkademik.tanggalMulai", range[0]))
				.add(Restrictions.ge("kalenderAkademik.tanggalSelesai", range[1]))
				.add(masukDiSmt == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("kalenderAkademik.masukDiSmt"),
								Restrictions.eq("kalenderAkademik.masukDiSmt", masukDiSmt)))
				.add(Restrictions.or(Restrictions.isNull("kalenderAkademik.program"),
						Restrictions.eq("kalenderAkademik.program", program)));

		// B. Kriteria Scope: Yayasan/Sekolah ATAU Fakultas/Jurusan
		if (yayasan != null || sekolah != null) {
			criteria.add(Restrictions.or(Restrictions.isNull("kalenderAkademik.yayasan"),
					Restrictions.eq("kalenderAkademik.yayasan", yayasan)))
					.add(Restrictions.or(Restrictions.isNull("kalenderAkademik.sekolah"),
							Restrictions.eq("kalenderAkademik.sekolah", sekolah)));
		} else {
			criteria.add(Restrictions.or(Restrictions.isNull("kalenderAkademik.fakultas"),
					Restrictions.eq("kalenderAkademik.fakultas", fakultas)))
					.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.isNull("kalenderAkademik.jenjang"),
									Restrictions.eq("kalenderAkademik.jenjang", jurusan.getJenjang())))
					.add(Restrictions.or(Restrictions.isNull("kalenderAkademik.jurusan"),
							Restrictions.eq("kalenderAkademik.jurusan", jurusan)));
		}

		// C. Kriteria Mode: Konfigurasi AKTIF saja ATAU Spesifik Tahun/Smt
		if (isAktifOnly) {
			criteria.add(Restrictions.eq("konfigurasi.nilai", Konfigurasi.AKTIF))
					.addOrder(Order.desc("kalenderAkademik.tanggalMulai"))
					.addOrder(Order.desc("konfigurasi.tahunAkademik")).addOrder(Order.desc("id"));
		} else {
			criteria.add(Restrictions.eq("konfigurasi.tahunAkademik", tahunAkademik))
					.add(jenisKonfigurasi.equals(Konfigurasi.KRS_SP)
							? Restrictions.or(Restrictions.isNull("kalenderAkademik.ganjilGenap"),
									Restrictions.eq("kalenderAkademik.ganjilGenap", Perkuliahan.SP))
							: Restrictions.or(Restrictions.isNull("kalenderAkademik.ganjilGenap"),
									Restrictions.eq("kalenderAkademik.ganjilGenap", smt)))
					.addOrder(Order.desc("kalenderAkademik.tanggalMulai")).addOrder(Order.desc("id"));
		}

		// D. Eksekusi
		criteria.setMaxResults(1);

		KonfigurasiKalenderAkademik kka = (KonfigurasiKalenderAkademik) ConstantValues.simpleObject(criteria,
				KonfigurasiKalenderAkademik.class);

		return kka == null ? null : kka.getKonfigurasi();
	}

	public static Konfigurasi getKonfigurasi(String jenisKonfigurasi, String tahunAkademik, String info1,
			String masukDiSmt, Fakultas fakultas, Jurusan jurusan, String program) {
		return getKonfigurasi(jenisKonfigurasi, tahunAkademik, info1, masukDiSmt, fakultas, jurusan, program,
				Konfigurasi.TIDAK_AKTIF);
	}

	public static Konfigurasi getKonfigurasi(String jenisKonfigurasi, String tahunAkademik, String info1,
			String masukDiSmt, Fakultas fakultas, Jurusan jurusan, String program, String defaultNilai) {

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			Konfigurasi konfigurasi = checkKonfigurasiDenganKalenderAkademik(session, jenisKonfigurasi, tahunAkademik,
					info1, masukDiSmt, fakultas, jurusan, program);

			if (konfigurasi != null) {
				return konfigurasi;
			}

			return getOrInsertKonfigurasi(session, jenisKonfigurasi, tahunAkademik, info1, defaultNilai);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/KonfigurasiManager.java:486");
			return null;
		} finally {
			if (session != null) {
				try {
					session.clear();
					session.disconnect();
					session.close();
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/KonfigurasiManager.java:495");
				}
			}
		}
	}

	public static Konfigurasi getKonfigurasi(String jenisKonfigurasi, String tahunAkademik, String info1,
			String masukDiSmt, Yayasan yayasan, Sekolah sekolah, String program, String defaultNilai) {

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			Konfigurasi konfigurasi = checkKonfigurasiDenganKalenderAkademik(session, jenisKonfigurasi, tahunAkademik,
					info1, masukDiSmt, yayasan, sekolah, program);

			if (konfigurasi != null) {
				return konfigurasi;
			}

			return getOrInsertKonfigurasi(session, jenisKonfigurasi, tahunAkademik, info1, defaultNilai);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/KonfigurasiManager.java:518");
			return null;
		} finally {
			if (session != null) {
				try {
					session.clear();
					session.disconnect();
					session.close();
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/KonfigurasiManager.java:527");
				}
			}
		}
	}

	// --- Private Helper Methods ---

	private static Konfigurasi cekKetersediaanKonfigurasi(String key) {
		Konfigurasi konfigurasi = KonfigurasiManager.getKonfigurasi(key, "");
		if (konfigurasi != null && konfigurasi.getNilai() != null && !konfigurasi.getNilai().trim().isEmpty()) {
			return konfigurasi;
		}
		return null;
	}

	private static Date[] getStartEndOfDay() {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.HOUR_OF_DAY, 1);
		Date dateStart = calendar.getTime();

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		Date dateEnd = calendar.getTime();

		return new Date[] { dateStart, dateEnd };
	}

	private static Konfigurasi getOrInsertKonfigurasi(Session session, String jenisKonfigurasi, String tahunAkademik,
			String info1, String defaultNilai) {
		String key = tahunAkademik + "-" + info1 + "-" + jenisKonfigurasi;
		Map<String, Konfigurasi> konfigurasis = MemoryDbUtil.getKonfigurasi();

		if (konfigurasis.containsKey(key)) {
			return konfigurasis.get(key);
		}

		Konfigurasi konfigurasi = (Konfigurasi) ConstantValues.simpleObject(session.createCriteria(Konfigurasi.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(Restrictions.eq("info1", info1)).add(Restrictions.eq("nama", jenisKonfigurasi)).setMaxResults(1),
				Konfigurasi.class);

		if (konfigurasi == null) {
			konfigurasi = new Konfigurasi();
			konfigurasi.setNama(jenisKonfigurasi);
			konfigurasi.setKeterangan("Digunakan untuk mengaktifkan / tidak mengaktifkan " + jenisKonfigurasi);
			konfigurasi.setTahunAkademik(tahunAkademik);
			konfigurasi.setInfo1(info1);
			konfigurasi.setNilai(defaultNilai);

			Transaction tx = null;
			try {
				tx = session.beginTransaction();
				session.save(konfigurasi);
				tx.commit();
			} catch (Exception e) {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/KonfigurasiManager.java:586");
			}
		}

		konfigurasis.put(key, konfigurasi);
		return konfigurasi;
	}
}