package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.ws.util.CommonUtil;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.JenisTinggalMahasiswa;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Paket;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;

public class PembayaranUtilHelper {

	private static final String SQL_TRUE = "1=1";
	private static final String SQL_FALSE = "1=0";

	private static boolean jenjangCocok(String jenjangAngsuranJson, String key,
			Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		if (jenjangAngsuranJson == null || jenjangAngsuranJson.trim().isEmpty()) return true;
		try {
			JSONArray arr = new JSONObject(jenjangAngsuranJson).optJSONArray(key);
			if (arr == null || arr.length() == 0) return true;
			Jenjang jenjang = mahasiswa != null ? mahasiswa.getJenjang()
					: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getJenjang() : null);
			if (jenjang == null || jenjang.getId() == null) return false;
			String jenjangId = String.valueOf(jenjang.getId());
			for (int i = 0; i < arr.length(); i++) {
				if (jenjangId.equals(arr.getString(i))) return true;
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:72");}
		return false;
	}

	private static void closeOpenedSession(Session session) {
		if (session != null) {
			try {
				if (session.isOpen()) {
					session.clear();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:82");
			}
			try {
				if (session.isOpen()) {
					session.disconnect();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:88");
			}
			try {
				if (session.isOpen()) {
					session.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:94");
			}
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public static Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, Integer semester, JenisKegiatan jenisKegiatan,
			boolean reload) {
		return getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan, null, reload);
	}

	@SuppressWarnings({ "rawtypes" })
	public static Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, Integer semester, JenisKegiatan jenisKegiatan,
			String bulan, boolean reload) {
		return getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan, bulan, false, reload);
	}

	@SuppressWarnings({ "rawtypes" })
	public static Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, Integer semester, JenisKegiatan jenisKegiatan,
			String bulan, Boolean untukBulananTampilkanMeskipunSudahDibayar, boolean reload) {
		Collection d = getDetailBiayaMahasiswadariDatabase(mahasiswa, semester, jenisKegiatan, bulan,
				untukBulananTampilkanMeskipunSudahDibayar, reload);
		return d;
	}

	@SuppressWarnings("rawtypes")
	public static Collection getDetailBiayaMahasiswaBerdasarkanJenisKegiatan(Mahasiswa mahasiswa, JenisKegiatan jenisKegiatan,
			String bulan, boolean reload) {
		Boolean ganjil = CommonUtil.isNowSemensterGanjil();
		Integer semester = CommonUtil.getSemester(mahasiswa.getTahunangkatan(), ganjil,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
		return PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan, bulan, reload);
	}

	@SuppressWarnings("rawtypes")
	public static Collection getDetailBiayaMahasiswa(Mahasiswa mahasiswa, JadwalPembayaran jadwalPembayaran, String bulan,
			boolean reload) {
		Boolean ganjil = jadwalPembayaran.getGanjil() == null ? Common.isNowSemensterGanjil() : jadwalPembayaran.getGanjil();
		Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), jadwalPembayaran.getTahunAkademik(),
				Boolean.TRUE.equals(ganjil) ? Perkuliahan.GANJIL : Perkuliahan.GENAP, mahasiswa.getPindahKeKampusIniMasukSemester(),
				mahasiswa.getSemesterMulai());
		return PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester, jadwalPembayaran.getJenisKegiatan(), bulan, reload);
	}

	@SuppressWarnings("unchecked")
	public static List<DetailKegiatan> getDetailKegiatanMahasiswa(Mahasiswa mahasiswa, BiodataCalonMahasiswa calonMahasiswa,
			JenisKegiatan jenisKegiatan) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Criteria criteria = session.createCriteria(DetailKegiatan.class).createCriteria("kegiatan");
			if (jenisKegiatan != null) {
				criteria.add(Restrictions.eq("jenisKegiatan", jenisKegiatan));
			}
			if (mahasiswa != null && calonMahasiswa != null) {
				criteria.add(Restrictions.or(Restrictions.eq("mahasiswa", mahasiswa),
						Restrictions.eq("calonMahasiswa", calonMahasiswa)));
			} else if (mahasiswa != null) {
				criteria.add(Restrictions.eq("mahasiswa", mahasiswa));
			} else if (calonMahasiswa != null) {
				criteria.add(Restrictions.eq("calonMahasiswa", calonMahasiswa));
			}
			List<DetailKegiatan> detailBiaya = criteria.list();
			return detailBiaya;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:159");
			return new ArrayList<DetailKegiatan>();
		} finally {
			closeOpenedSession(session);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Collection getDetailBiayaMahasiswadariDatabase(Mahasiswa mahasiswa, Integer semester,
			JenisKegiatan jenisKegiatan, String bulan, boolean untukBulananTampilkanMeskipunSudahDibayar,
			boolean reload) {

		if (mahasiswa != null && mahasiswa.getTidakAdaTagihan() != null && mahasiswa.getTidakAdaTagihan()) {
			return new TreeSet();
		}

		if (mahasiswa != null && mahasiswa.getPindahKeKampusIniMasukSemester() != null 
				&& mahasiswa.getPindahKeKampusIniMasukSemester() > 0
				&& semester != null && mahasiswa.getPindahKeKampusIniMasukSemester() > semester) {
			return new TreeSet();
		}

		if (semester != null && mahasiswa != null && mahasiswa.getStatusKeluar() != null
				&& ((mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() < semester))) {
			if (jenisKegiatan != null && !Boolean.TRUE.equals(jenisKegiatan.getTagihanJugaUntukAlumni())) {
				return new TreeSet();
			}
		}

		String bulanKey = (bulan == null || bulan.trim().isEmpty()) ? "" : "_" + bulan.trim();
		String key = "tagihan_mhs_" + (mahasiswa != null ? mahasiswa.getId() : "null") + "_" 
				+ (jenisKegiatan != null ? jenisKegiatan.getId() : "null") + "_" + semester
				+ bulanKey + "_" + (untukBulananTampilkanMeskipunSudahDibayar ? "semua" : "belum_dibayar")
				+ "_aktif_tagihan_v2";

		if (!reload && mahasiswa != null) {
			try {
				String s = mahasiswa.retreive(key);
				JSONObject data = s == null || s.trim().isEmpty() ? null : new JSONObject(s);
				if (data != null) {
					boolean smtSalah = false;
					List d = new ArrayList();
					Iterator<String> iter = data.keys();
					while (iter.hasNext()) {
						try {
							String keyIter = iter.next();
							String value = data.get(keyIter).toString();
							if (value.equalsIgnoreCase("1")) {
								DetailBiaya detailBiaya1 = (DetailBiaya) GeneralValueObject.ambilData(DetailBiaya.class, keyIter, true);
								
								if (detailBiaya1 != null) {
									detailBiaya1.updateKeterangan(mahasiswa, semester);
									d.add(detailBiaya1);

									if (detailBiaya1.getSemester() != null && !detailBiaya1.getSemester().equals(semester)) {
										smtSalah = true;
										break;
									}
								}
							} else if (value.equalsIgnoreCase("2")) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) GeneralValueObject
										.ambilData(PengaturanPembayaranBulanan.class, keyIter, true);

								if (pengaturanPembayaranBulanan != null) {
									d.add(pengaturanPembayaranBulanan);
									if (pengaturanPembayaranBulanan.getDetailBiaya() != null 
											&& pengaturanPembayaranBulanan.getDetailBiaya().getSemester() != null
											&& !pengaturanPembayaranBulanan.getDetailBiaya().getSemester().equals(semester)) {
										smtSalah = true;
										break;
									}
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:232");}
					}

					try {
						Collections.sort(d);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:237");}

					if (!smtSalah) {
						return d;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:243");}
		}

		if (mahasiswa == null) return new TreeSet();

		Jurusan jurusan = mahasiswa.getJurusan();
		Jenjang jenjang = jurusan != null ? jurusan.getJenjang() : mahasiswa.getJenjang();

		Integer angkatan = mahasiswa.getTahunangkatan();
		String warganegara = mahasiswa.getWarganegara();

		Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
		Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai, mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		Integer tahap = PengaturanPembayaranBulanan.hitungTahap(mahasiswa, semester, Common.BULAN[ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH)]);
		String mulaiBelajarDiSemester = mahasiswa.getSemesterMulai();
		
		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahap,
				jenisKegiatan != null && Boolean.TRUE.equals(jenisKegiatan.getUntukBayarSP()) ? Perkuliahan.SEMESTER_PENDEK : null, reload);

		StatusMahasiswa statusMahasiswa = null;
		if (ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa) != null) {
			statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa).getStatusMahasiswa();
		}

		HistoryStatusMahasiswa tempHistoryStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa, reload);
		String program = tempHistoryStatusMahasiswa != null ? tempHistoryStatusMahasiswa.getProgram() : mahasiswa.getProgram();
		String kelamin = mahasiswa.getKelamin();
		StatusAwalMahasiswa statusAwalMahasiswa = tempHistoryStatusMahasiswa != null ? tempHistoryStatusMahasiswa.getStatusAwalMahasiswa() : null;

		if (((statusMahasiswa != null && ConstantValues.LULUS != null && ConstantValues.LULUS.getId().equals(statusMahasiswa.getId())) || mahasiswa.getStatusKeluar() != null)
				&& mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus().equals(semester)) {
			statusMahasiswa = ConstantValues.AKTIF;
		}

		PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
		if (pendaftaranCutiMahasiswa != null && Boolean.TRUE.equals(pendaftaranCutiMahasiswa.getPersetujuan())) {
			statusMahasiswa = ConstantValues.CUTI;
		}

		try {
			Konfigurasi k1 = Common.getKonfigurasi("mahasiswa_dengan_status_non_aktif_bisa_melakukan_pembayaran_seperti_status_aktif", Konfigurasi.AKTIF);
			if (k1 != null && Konfigurasi.AKTIF.equals(k1.getNilai())) {
				if (statusMahasiswa == null || statusMahasiswa.getId().equals(ConstantValues.TIDAK_AKTIF.getId())) {
					statusMahasiswa = ConstantValues.AKTIF;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:292");}

		try {
			Konfigurasi k2 = Common.getKonfigurasi("mahasiswa_dengan_status_non_lulus_bisa_melakukan_pembayaran_seperti_status_aktif", Konfigurasi.TIDAK_AKTIF);
			if (k2 != null && Konfigurasi.AKTIF.equals(k2.getNilai())) {
				if (statusMahasiswa == null || statusMahasiswa.getId().equals(ConstantValues.LULUS.getId())) {
					statusMahasiswa = ConstantValues.AKTIF;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:301");}

		try {
			Konfigurasi k3 = Common.getKonfigurasi("mahasiswa_dengan_status_kampus_merdeka_bisa_melakukan_pembayaran_seperti_status_aktif", Konfigurasi.AKTIF);
			if (k3 != null && Konfigurasi.AKTIF.equals(k3.getNilai())) {
				if (ConstantValues.KAMPUS_MERDEKA != null && (statusMahasiswa == null || statusMahasiswa.getId().equals(ConstantValues.KAMPUS_MERDEKA.getId()))) {
					statusMahasiswa = ConstantValues.AKTIF;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:310");}

		String filterKelas = "TIDAK AKTIF";
		try {
			Konfigurasi fk = Common.getKonfigurasi("tampilkan_filter_kelas_pada_billing_pembayaran", Konfigurasi.TIDAK_AKTIF);
			if (fk != null) filterKelas = fk.getNilai();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:316");}

		String filterJenisTempatTinggalMahasiswa = "TIDAK AKTIF";
		try {
			Konfigurasi fj = Common.getKonfigurasi("tampilkan_filter_jenis_tempat_tinggal_mahasiswa_pada_billing_pembayaran", Konfigurasi.TIDAK_AKTIF);
			if (fj != null) filterJenisTempatTinggalMahasiswa = fj.getNilai();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:322");}

		String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
				+ (semester == null ? "0" : (semester % 2 == 0) ? "2" : "1");
		Integer ta = 0;
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:329");}

		Session session = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// Fresh reload untuk hindari stale cache dari combobox lama
			if (jenisKegiatan != null && jenisKegiatan.getId() != null) {
				try {
					JenisKegiatan freshJk = (JenisKegiatan) session.get(JenisKegiatan.class, jenisKegiatan.getId());
					if (freshJk != null) jenisKegiatan = freshJk;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:341");}
			}

			/*
			 * Setting biaya khusus mahasiswa harus diperiksa SEBELUM guard mode
			 * bulanan/angsuran. Sebelumnya guard langsung mengembalikan koleksi kosong,
			 * sehingga setting khusus (yang memang memakai nilai/default tanggal pada
			 * Setting Biaya) tidak pernah sempat dibaca untuk jenis kegiatan bulanan.
			 */
			List<DetailBiaya> biayaDefault = SetingBiayaHelper.getDetailBiayaDefault(session,
					mahasiswa, jenisKegiatan, semester, ta);
			if (PengecualianTagihanList.adalah(biayaDefault)) {
				return PengecualianTagihanList.kosong();
			}

			if (bulan == null && jenisKegiatan != null) {
				// Per-jenjang PER-SEMESTER (dan per-angkatan bila diisi format TAHUN:SMT):
				// aturan angsuran hanya mengenai semester/angkatan yang masuk daftar
				// "Berlaku di smt" (kosong = semua) pada form Jenis Kegiatan.
				Boolean modeAngsuran = jenisKegiatan.modeAngsuranUntukJenjang(jenjang, semester,
						mahasiswa == null ? null : mahasiswa.getTahunangkatan());
				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][getDetailBiaya] mhs=" + (mahasiswa != null ? mahasiswa.getNim() : "null")
						+ " jk=" + jenisKegiatan.getNama() + " jenjang=" + (jenjang != null ? jenjang.getNama() : "null")
						+ " bulan=" + bulan + " modeAngsuran=" + modeAngsuran);
				if (Boolean.TRUE.equals(modeAngsuran)
						&& (biayaDefault == null || biayaDefault.isEmpty())) {
					if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
							"[DEBUG-ANGSURAN][getDetailBiaya] → TRUE tanpa setting khusus: return empty (lanjut jalur angsuran bulanan)");
					/*
					 * Ini bukan pengecualian NIM. Koleksi kosong biasa memberi tahu pemanggil
					 * agar tagihan dilayani oleh jalur angsuran/bulanan. Sentinel
					 * PengecualianTagihanList hanya boleh dipakai untuk NIM yang benar-benar
					 * tercantum pada daftar pengecualian Setting Biaya.
					 */
					return new TreeSet();
				}
				if (Boolean.TRUE.equals(modeAngsuran) && biayaDefault != null && !biayaDefault.isEmpty()
						&& JenisKegiatan.DEBUG_MODE_ANGSURAN) {
					System.out.println("[DEBUG-ANGSURAN][getDetailBiaya] → TRUE tetapi setting khusus mahasiswa ditemukan: proses setting khusus");
				}
				// FALSE (bukan angsuran) atau null → lanjut query billing reguler
				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][getDetailBiaya] → " + modeAngsuran + ": lanjut query billing reguler");
			}

			AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = null;

			if (biayaDefault == null || biayaDefault.isEmpty()) {
				Paket paket = null;
				try {
					BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();
					if (biodataCalonMahasiswa != null) {
						paket = biodataCalonMahasiswa.getPaket();
						afiliasiCalonMahasiswa = biodataCalonMahasiswa.getAfiliasiCalonMahasiswa();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:371");}

				biayaDefault = SetingBiayaHelper.getDetailBiayaDefault(session, angkatan, jenjang, semester, jenisKegiatan,
						statusAwalMahasiswa, statusMahasiswa, mahasiswa.getJenisSeleksi(),
						mahasiswa.getGelombangPendaftaran(), paket, jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
						mahasiswa.getNim());
				if (PengecualianTagihanList.adalah(biayaDefault)) {
					return new TreeSet();
				}
			}

			if (JenisKegiatan.DEBUG_MODE_ANGSURAN) {
				System.out.println("[DEBUG-ANGSURAN][getDetailBiaya] biayaDefault size=" + (biayaDefault != null ? biayaDefault.size() : 0)
						+ " mhs=" + mahasiswa.getNim() + " jenjang=" + (jenjang != null ? jenjang.getNama() : "null"));
				if (biayaDefault != null) {
					for (DetailBiaya db : biayaDefault) {
						System.out.println("[DEBUG-ANGSURAN][getDetailBiaya]   biayaDefault item id=" + db.getId()
								+ " item=" + (db.getItemBiaya() != null ? db.getItemBiaya().getNama() : "null")
								+ " nilaibiaya=" + db.getNilaiBiaya()
								+ " jenjang=" + (db.getJenjang() != null ? db.getJenjang().getNama() : "null")
								+ " jurusan=" + (db.getJurusan() != null ? db.getJurusan().getNama() : "null"));
					}
				}
			}

			if (biayaDefault != null && !biayaDefault.isEmpty()) {
				for (DetailBiaya detailBiaya : biayaDefault) {
					detailBiaya.updateKeterangan(mahasiswa, semester);
				}

				JSONObject data = new JSONObject();
				for (DetailBiaya detailBiaya : biayaDefault) {
					try {
						data.put(detailBiaya.getId().toString(), "1");
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:401");}
					GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya);
				}
				mahasiswa.put(data.toString(), key);

				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][getDetailBiaya] EARLY RETURN via biayaDefault (" + biayaDefault.size() + " items)");
				return biayaDefault;
			}

			String kelasStr = null;
			if (Konfigurasi.AKTIF.equals(filterKelas)) {
				kelasStr = mahasiswa.getKelas();
			}

			JenisTinggalMahasiswa jenisTinggalMahasiswa = null;
			if (Konfigurasi.AKTIF.equals(filterJenisTempatTinggalMahasiswa)) {
				jenisTinggalMahasiswa = (JenisTinggalMahasiswa) session.createCriteria(BiodataMahasiswa.class)
						.setProjection(Projections.property("jenisTinggalMahasiswa"))
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.addOrder(Order.desc("id"))
						.setMaxResults(1)
						.uniqueResult();
			}

			Paket paket = null;
			try {
				BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();
				if (biodataCalonMahasiswa != null) {
					paket = biodataCalonMahasiswa.getPaket();
					afiliasiCalonMahasiswa = biodataCalonMahasiswa.getAfiliasiCalonMahasiswa();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:433");}

			List<ItemBiaya> detailSettingBiayas = SetingBiayaHelper.getItemBiaya(session, angkatan, jenjang, semester,
					jenisKegiatan, statusAwalMahasiswa, statusMahasiswa, mahasiswa.getJenisSeleksi(),
					mahasiswa.getGelombangPendaftaran(), paket, jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
					mahasiswa.getNim());
			if (detailSettingBiayas == null) {
				return PengecualianTagihanList.kosong();
			}

			if (JenisKegiatan.DEBUG_MODE_ANGSURAN) {
				System.out.println("[DEBUG-ANGSURAN][getDetailBiaya] getItemBiaya size="
						+ (detailSettingBiayas != null ? detailSettingBiayas.size() : 0)
						+ " angkatan=" + angkatan + " jenjang=" + (jenjang != null ? jenjang.getNama() : "null")
						+ " smt=" + semester + " statusMhs=" + (statusMahasiswa != null ? statusMahasiswa.getNama() : "null")
						+ " statusAwal=" + (statusAwalMahasiswa != null ? statusAwalMahasiswa.getNama() : "null")
						+ " mulaiBelajar=" + mulaiBelajarDiSemester + " ta=" + ta + " jurusan=" + (jurusan != null ? jurusan.getNama() : "null"));
				if (detailSettingBiayas != null) {
					for (ItemBiaya ib : detailSettingBiayas) {
						System.out.println("[DEBUG-ANGSURAN][getDetailBiaya]   itemBiaya id=" + ib.getId() + " nama=" + ib.getNama());
					}
				}
			}

			Criteria criteria = session.createCriteria(DetailBiaya.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

			if (bulan != null && !bulan.trim().isEmpty() && Common.isNumber(bulan)) {

				List<PengaturanPembayaranBulanan> yangSudahDibayarBulanans = untukBulananTampilkanMeskipunSudahDibayar
						? null
						: session.createCriteria(CicilanPembayaran.class).createAlias("kegiatan", "kegiatan")
								.add(Restrictions.eq("kegiatan.mahasiswa", mahasiswa))
								.add(Restrictions.eq("kegiatan.jenisKegiatan", jenisKegiatan))
								.add(Restrictions.eq("kegiatan.semster", semester))
								.setProjection(Projections.groupProperty("pengaturanPembayaranBulanan"))
								.add(Restrictions.isNotNull("pengaturanPembayaranBulanan")).list();

				StringBuilder sqlQueryBuilder = new StringBuilder();
				sqlQueryBuilder.append("(realbulan,item_biaya) not in (");
				
				StringBuilder sqlBuilder = new StringBuilder();
				if (yangSudahDibayarBulanans != null) {
					boolean isFirst = true;
					for (PengaturanPembayaranBulanan p : yangSudahDibayarBulanans) {
						if (p != null && p.getDetailBiaya() != null && p.getDetailBiaya().getItemBiaya() != null) {
							if (!isFirst) {
								sqlBuilder.append(",");
							}
							sqlBuilder.append("(").append(p.getRealBulan()).append(",").append(p.getDetailBiaya().getItemBiaya().getId()).append(")");
							isFirst = false;
						}
					}
					sqlQueryBuilder.append(sqlBuilder);
				}
				sqlQueryBuilder.append(")");
				
				String sql = sqlBuilder.toString();
				String sqlQuery = sqlQueryBuilder.toString();

				Konfigurasi tagihanKonfig = Common.getKonfigurasi("tagihan_pembayaran_host_to_host_per_bulan_dihitung_berdasarkan_akumulasi_bulanan_yg_belum_dibayar", Konfigurasi.TIDAK_AKTIF);
				if (tagihanKonfig != null && Konfigurasi.AKTIF.equalsIgnoreCase(tagihanKonfig.getNilai())) {

					Integer bln = bulan.trim().equals("-1") ? null
							: (Integer) session.createCriteria(PengaturanPembayaranBulanan.class)
									.add(Restrictions.eq("aktif", true))
									.add(Restrictions.eq("realBulan", Integer.parseInt(bulan.trim())))
									.setProjection(Projections.property("bulan")).setMaxResults(1)
									.addOrder(Order.desc("id")).uniqueResult();

					criteria = session.createCriteria(PengaturanPembayaranBulanan.class)
							.add(Restrictions.eq("aktif", true))
					.add(sql.trim().isEmpty() ? Restrictions.sqlRestriction(SQL_TRUE) : Restrictions.sqlRestriction(sqlQuery))
							.add(bulan.trim().equals("-1") ? Restrictions.sqlRestriction(SQL_TRUE)
									: bln != null ? Restrictions.le("bulan", bln)
											: Restrictions.eq("realBulan", Integer.parseInt(bulan.trim())))
							.createCriteria("detailBiaya");

				} else {
					criteria = session.createCriteria(PengaturanPembayaranBulanan.class)
							.add(Restrictions.eq("aktif", true))
							.add(sql.trim().isEmpty() ? Restrictions.sqlRestriction(SQL_TRUE) : Restrictions.sqlRestriction(sqlQuery))
							.add(bulan.trim().equals("-1") ? Restrictions.sqlRestriction(SQL_TRUE)
									: Restrictions.eq("realBulan", Integer.parseInt(bulan.trim())))
							.createCriteria("detailBiaya");
				}
			}

			filterCriteriaDenganNilaiTambahan(criteria, session, mahasiswa, null);

			if (kelasStr != null) {
				criteria.createAlias("kelas", "kelas").add(Restrictions.eq("kelas.nama", kelasStr));
			} else {
				criteria.add(Restrictions.isNull("kelas"));
			}

			Collection detailBiaya = criteria
					.add(detailSettingBiayas == null || detailSettingBiayas.isEmpty() ? Restrictions.sqlRestriction(SQL_FALSE) : Restrictions.in("itemBiaya", detailSettingBiayas))
					.add(Restrictions.or(Restrictions.eq("merupakanPembayaran", false), Restrictions.isNull("merupakanPembayaran")))
					.addOrder(Order.desc("id"))
					.add(jenisTinggalMahasiswa == null ? Restrictions.isNull("jenisTinggalMahasiswa") : Restrictions.eq("jenisTinggalMahasiswa", jenisTinggalMahasiswa))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.eq("statusMahasiswa", statusMahasiswa))
					.add(Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))
					.add(Restrictions.eq("mulaiBelajarDiSemester", mulaiBelajarDiSemester))
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
					.add(warganegara != null ? Restrictions.ilike("wnaAtauWni", warganegara, MatchMode.EXACT) : Restrictions.sqlRestriction(SQL_TRUE))
					.add(Restrictions.eq("jenjang", jenjang))
					.add(Restrictions.eq("jurusan", jurusan))
					.add(program != null ? Restrictions.ilike("program", program, MatchMode.EXACT) : Restrictions.sqlRestriction(SQL_TRUE))
					.add(Restrictions.eq("semester", semester))
					.add(jenisKegiatan != null ? Restrictions.between("semester", jenisKegiatan.getMinSmt(), jenisKegiatan.getMaxSmt()) : Restrictions.sqlRestriction(SQL_TRUE))
					.add(Restrictions.eq("angkatan", angkatan)).list();

			if (JenisKegiatan.DEBUG_MODE_ANGSURAN) {
				System.out.println("[DEBUG-ANGSURAN][getDetailBiaya] criteria query result size=" + detailBiaya.size()
						+ " tahunAkademik=" + tahunAkademik + " statusMhs=" + (statusMahasiswa != null ? statusMahasiswa.getNama() : "null")
						+ " statusAwal=" + (statusAwalMahasiswa != null ? statusAwalMahasiswa.getNama() : "null")
						+ " mulaiBelajar=" + mulaiBelajarDiSemester + " jurusan=" + (jurusan != null ? jurusan.getNama() : "null")
						+ " angkatan=" + angkatan + " smt=" + semester);
				for (Object item : detailBiaya) {
					if (item instanceof DetailBiaya) {
						DetailBiaya db = (DetailBiaya) item;
						System.out.println("[DEBUG-ANGSURAN][getDetailBiaya]   result item=" + (db.getItemBiaya() != null ? db.getItemBiaya().getNama() : "null")
								+ " nilaibiaya=" + db.getNilaiBiaya() + " jenjang=" + (db.getJenjang() != null ? db.getJenjang().getNama() : "null"));
					}
				}
			}

			List<DetailBiaya> biayaDefaultBiaya = SetingBiayaHelper.getDetailBiayaBukanDefaultBiaya(session, angkatan,
					jenjang, semester, jenisKegiatan, statusAwalMahasiswa, statusMahasiswa, mahasiswa.getJenisSeleksi(),
					mahasiswa.getGelombangPendaftaran(), paket, jurusan, program, kelamin, afiliasiCalonMahasiswa, ta);

			if (biayaDefaultBiaya != null && !biayaDefaultBiaya.isEmpty()) {
				for (DetailBiaya detailBiayaDefault : biayaDefaultBiaya) {
					detailBiayaDefault.updateKeterangan(mahasiswa, semester);
					detailBiaya.add(detailBiayaDefault);
				}
			}


			boolean nolMasukFilter = false;
			try {
				Konfigurasi knol = Common.getKonfigurasi("nol_masuk_filter_pembayaran", Konfigurasi.TIDAK_AKTIF);
				nolMasukFilter = (knol != null && Konfigurasi.AKTIF.equals(knol.getNilai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:574");}

			if (bulan != null && !bulan.trim().isEmpty() && Common.isNumber(bulan)) {

				List detailBiayaList = (detailBiaya instanceof List) ? (List) detailBiaya : new ArrayList(detailBiaya);
				List<PengaturanPembayaranBulanan> d = saringPengaturanPembayaranBulanan(detailBiayaList, nolMasukFilter, mahasiswa, semester);

				if (d != null) {
					JSONObject data = new JSONObject();
					try {
						for (PengaturanPembayaranBulanan p : d) {
							GeneralValueObject.masukkanData(PengaturanPembayaranBulanan.class, p);
							data.put(p.getId().toString(), "2");
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:588");}
					mahasiswa.put(data.toString(), key);
				}
				
				return d != null ? d : new ArrayList();
				
			} else {

				Map<Long, Long> ids = new HashMap<Long, Long>();
				Map<Long, Object> maps = new HashMap<Long, Object>();
				
				for (Object o : detailBiaya) {
					try {
						if (o instanceof DetailBiaya) {
							DetailBiaya biaya = (DetailBiaya) o;
							Long value = ids.get(biaya.getItemBiaya().getId());
							if (value == null || value < biaya.getId()) {
								ids.put(biaya.getItemBiaya().getId(), biaya.getId());
								maps.put(biaya.getItemBiaya().getId(), biaya);
							}
						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
							if (biaya.getNominal() != null && biaya.getNominal().intValue() != 0 && !maps.containsKey(biaya.getId())) {
								maps.put(biaya.getId(), biaya);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:614");}
				}

				for (Object o : detailBiaya) {
					try {
						if (o instanceof DetailBiaya) {
							DetailBiaya biaya = (DetailBiaya) o;
							if (!maps.containsKey(biaya.getItemBiaya().getId())
									|| (nolMasukFilter && ((DetailBiaya) maps.get(biaya.getItemBiaya().getId())).getNilaiBiaya().intValue() == 0)) {
								maps.put(biaya.getItemBiaya().getId(), biaya);
							}
						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
							if (!maps.containsKey(biaya.getId())
									|| (nolMasukFilter && ((PengaturanPembayaranBulanan) maps.get(biaya.getId())).getNominal().intValue() == 0)) {
								maps.put(biaya.getId(), biaya);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:632");}
				}

				TreeSet treeSet = new TreeSet(maps.values());

				JSONObject data = new JSONObject();
				try {
					for (Object o : treeSet) {
						if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya1 = (DetailBiaya) o;
							detailBiaya1.updateKeterangan(mahasiswa, semester);
							data.put(detailBiaya1.getId().toString(), "1");
							GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya1);
						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
							data.put(biaya.getId().toString(), "2");
							GeneralValueObject.masukkanData(PengaturanPembayaranBulanan.class, biaya);
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:651");}

				mahasiswa.put(data.toString(), key);
				
				return treeSet;
			}
			
		} catch(Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:659");
			return new TreeSet();
		} finally {
			closeOpenedSession(session);
		}
	}

	public static Collection<DetailBiaya> getDetailBiayaCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, Jurusan jurusan, boolean reload) {
		return getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, jurusan, null, reload);
	}

	/**
	 * Nilai jenis seleksi lama bisa tidak lagi termasuk pada gelombang yang dipilih
	 * (misalnya tersimpan Genap, sedangkan gelombang hanya menyediakan Ganjil).
	 * Gunakan pilihan yang masih sah agar pencarian billing tidak terkunci pada data
	 * lama yang sudah tidak konsisten.
	 */
	private static JenisSeleksi jenisSeleksiSesuaiGelombang(BiodataCalonMahasiswa calonMahasiswa) {
		JenisSeleksi tersimpan = calonMahasiswa == null ? null : calonMahasiswa.getJenisSeleksi();
		GelombangPendaftaran gelombang = calonMahasiswa == null ? null
				: calonMahasiswa.getGelombangPendaftaran();
		if (gelombang == null) {
			return tersimpan;
		}

		List<JenisSeleksi> pilihan = gelombang.ambilJenisSeleksi();
		if (pilihan == null || pilihan.isEmpty()) {
			return tersimpan;
		}
		for (JenisSeleksi item : pilihan) {
			if (item != null && tersimpan != null && item.getId() != null
					&& item.getId().equals(tersimpan.getId())) {
				return item;
			}
		}

		JenisSeleksi bawaanGelombang = gelombang.getJenisSeleksi();
		if (bawaanGelombang != null) {
			for (JenisSeleksi item : pilihan) {
				if (item != null && item.getId() != null && bawaanGelombang.getId() != null
						&& item.getId().equals(bawaanGelombang.getId())) {
					return item;
				}
			}
		}
		// Jika nilai lama tidak termasuk pilihan gelombang, gunakan urutan pertama yang
		// dikonfigurasi admin. Mempertahankan nilai lama membuat jenis seleksi di biodata dan
		// sumber tagihan berbeda (mis. Genap pada gelombang Ganjil).
		return pilihan.get(0);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Collection<DetailBiaya> getDetailBiayaCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, Jurusan jurusan, Integer semester, boolean reload) {

		if (biodataCalonMahasiswa == null || jenisKegiatan == null) {
			return new TreeSet<DetailBiaya>();
		}

		String key = "tagihan_cal_mhs_" + biodataCalonMahasiswa.getId() + "_" + jenisKegiatan.getId() + "_" + semester;

		if (!reload) {
			try {
				String s = biodataCalonMahasiswa.retreive(key);
				JSONObject data = s == null || s.trim().isEmpty() ? null : new JSONObject(s);
				if (data != null) {
					List d = new ArrayList();
					Iterator<String> iter = data.keys();
					while (iter.hasNext()) {
						String keyIter = iter.next();
						String value = data.get(keyIter).toString();

						if (value.equalsIgnoreCase("1")) {
							DetailBiaya detailBiaya1 = (DetailBiaya) GeneralValueObject.ambilData(DetailBiaya.class, keyIter, true);

							if (detailBiaya1 != null && biodataCalonMahasiswa.getMahasiswa() != null) {
								detailBiaya1.updateKeterangan(biodataCalonMahasiswa.getMahasiswa(), semester);
							}
							if (detailBiaya1 != null) {
								d.add(detailBiaya1);
							}
						} else if (value.equalsIgnoreCase("2")) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) GeneralValueObject
									.ambilData(PengaturanPembayaranBulanan.class, keyIter, true);
							if (pengaturanPembayaranBulanan != null) {
								d.add(pengaturanPembayaranBulanan);
							}
						}
					}

					try {
						Collections.sort(d);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:713");
					}

					return d;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:719");
			}
		}

		Jenjang jenjang = jurusan != null ? jurusan.getJenjang() : biodataCalonMahasiswa.getJenjang();
		JenisSeleksi jenisSeleksi = jenisSeleksiSesuaiGelombang(biodataCalonMahasiswa);
		String program = biodataCalonMahasiswa.getProgram();
		Integer angkatan = biodataCalonMahasiswa.getTahun();
		Paket paket = biodataCalonMahasiswa.getPaket();
		GelombangPendaftaran gelombangPendaftaran = biodataCalonMahasiswa.getGelombangPendaftaran();
		String warganegara = biodataCalonMahasiswa.getKewarganegaraan();
		String kelamin = biodataCalonMahasiswa.getJenisKelamin();
		AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = biodataCalonMahasiswa.getAfiliasiCalonMahasiswa();

		String tahunAkademik;
		try {
			Integer tahunAngkatanMhs = biodataCalonMahasiswa.getTahun();
			Integer semesterMulai = 0;
			Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai,
					biodataCalonMahasiswa.getSemesterMulai());
			tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		} catch (Exception e) {
			tahunAkademik = biodataCalonMahasiswa.getTahunAkademik();
		}

		String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
				+ (semester == null ? "0" : (semester % 2 == 0) ? "2" : "1");
		Integer ta = 0;
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:749");}

		Session session = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// Fresh reload untuk hindari stale cache dari combobox lama (calon mahasiswa path)
			if (jenisKegiatan != null && jenisKegiatan.getId() != null) {
				try {
					JenisKegiatan freshJk = (JenisKegiatan) session.get(JenisKegiatan.class, jenisKegiatan.getId());
					if (freshJk != null) jenisKegiatan = freshJk;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:761");}
			}

			List<DetailBiaya> biayaDefault = SetingBiayaHelper.getDetailBiayaDefault(session, biodataCalonMahasiswa,
					jenisKegiatan, semester, ta);
			if (PengecualianTagihanList.adalah(biayaDefault)) {
				return PengecualianTagihanList.kosong();
			}

			if (biayaDefault == null || biayaDefault.isEmpty()) {
				biayaDefault = SetingBiayaHelper.getDetailBiayaDefault(session, angkatan, jenjang, semester, jenisKegiatan,
						biodataCalonMahasiswa.getStatusAwalMahasiswa(), ConstantValues.AKTIF,
						jenisSeleksi, biodataCalonMahasiswa.getGelombangPendaftaran(),
						biodataCalonMahasiswa.getPaket(), jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
						biodataCalonMahasiswa.getNim());
				if (PengecualianTagihanList.adalah(biayaDefault)) {
					return PengecualianTagihanList.kosong();
				}
			}
			
			if (biayaDefault != null && !biayaDefault.isEmpty()) {
				if (biodataCalonMahasiswa.getMahasiswa() != null) {
					for (DetailBiaya detailBiaya : biayaDefault) {
						detailBiaya.updateKeterangan(biodataCalonMahasiswa.getMahasiswa(), semester);
					}
				}

				JSONObject data = new JSONObject();
				for (DetailBiaya detailBiaya : biayaDefault) {
					try {
						data.put(detailBiaya.getId().toString(), "1");
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:785");}
					GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya);
				}
				biodataCalonMahasiswa.put(data.toString(), key);
				
				return biayaDefault;
			}

			if (jurusan == null) {
				jurusan = (Jurusan) session.createCriteria(Jurusan.class)
						.add(Restrictions.eq("aktif", true))
						.add(Restrictions.eq("jenjang", jenjang))
						.setMaxResults(1)
						.uniqueResult();
			}

			List<ItemBiaya> detailSettingBiayas = SetingBiayaHelper.getItemBiaya(session, angkatan, jenjang, semester,
					jenisKegiatan, biodataCalonMahasiswa.getStatusAwalMahasiswa(), ConstantValues.AKTIF,
					jenisSeleksi, biodataCalonMahasiswa.getGelombangPendaftaran(),
					biodataCalonMahasiswa.getPaket(), jurusan, program, kelamin, afiliasiCalonMahasiswa, ta,
					biodataCalonMahasiswa.getNim());
			if (detailSettingBiayas == null) {
				return PengecualianTagihanList.kosong();
			}

			// Cek apakah jenjang calon mhs ini masuk mode angsuran — terpusat via
			// modeAngsuranUntukJenjang(jenjang, semester, angkatan) sehingga aturan
			// per-jenjang SEKALIGUS per-semester dan per-angkatan ("Berlaku di smt",
			// format TAHUN:SMT) terhormati. Lalu verifikasi terhadap kenyataan billing:
			// bila kombinasi ini tidak punya baris bulanan sama sekali, JANGAN paksa
			// jalur angsuran — kuerinya akan kosong dan tagihan reguler ikut lenyap
			// (total 0 di layar admin & inquiry bank error 07).
			boolean isHarusAngsuranForJenjang = Boolean.TRUE
					.equals(jenisKegiatan.modeAngsuranUntukJenjang(jenjang, semester, angkatan));
			if (isHarusAngsuranForJenjang) {
				int barisBulanan = ais.action.ws.util.PembayaranUtil.hitungBarisBulananSemester(session,
						jenisKegiatan, jenjang, semester, angkatan, null);
				if (barisBulanan == 0) {
					isHarusAngsuranForJenjang = false;
				}
			}

			Criteria criteria = isHarusAngsuranForJenjang
					? session.createCriteria(PengaturanPembayaranBulanan.class)
							.add(Restrictions.eq("aktif", true))
							.setProjection(Projections.property("detailBiaya")).createCriteria("detailBiaya")
					: session.createCriteria(DetailBiaya.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			
			filterCriteriaDenganNilaiTambahan(criteria, session, null, biodataCalonMahasiswa);

			criteria = criteria
					.add(paket == null ? Restrictions.isNull("paket") : Restrictions.eq("paket", paket))
					.add(detailSettingBiayas == null || detailSettingBiayas.isEmpty() ? Restrictions.sqlRestriction(SQL_FALSE) : Restrictions.in("itemBiaya", detailSettingBiayas))
					.add(Restrictions.or(Restrictions.eq("merupakanPembayaran", false), Restrictions.isNull("merupakanPembayaran")));
					
			if (paket != null && Boolean.TRUE.equals(paket.getBiayaPendaftaranSemuaGelombangSama())) {
				criteria.add(Restrictions.isNull("gelombangPendaftaran"));
			} else {
				if (jenisKegiatan.getNamaKegiatan() != null && jenisKegiatan.getNamaKegiatan().equalsIgnoreCase(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
					criteria.add(Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran));
				} else {
					criteria.add(Restrictions.or(Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran), Restrictions.isNull("gelombangPendaftaran")));
				}
			}

			criteria.add(semester == null ? Restrictions.in("semester", new Integer[] { 0, 1 }) : Restrictions.eq("semester", semester))
					.add(Restrictions.ge("semester", jenisKegiatan.getMinSmt()))
					.add(Restrictions.le("semester", jenisKegiatan.getMaxSmt()))
					.add(Restrictions.eq("statusAwalMahasiswa", biodataCalonMahasiswa.getStatusAwalMahasiswa()))
					.add(Restrictions.eq("statusMahasiswa", ConstantValues.AKTIF))
					.add(warganegara != null ? Restrictions.ilike("wnaAtauWni", warganegara, MatchMode.EXACT) : Restrictions.sqlRestriction(SQL_TRUE))
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
					.add(Restrictions.eq("jenisSeleksi", jenisSeleksi))
					.add(Restrictions.eq("jenjang", jenjang))
					.add(Restrictions.eq("jurusan", jurusan))
					.add(program != null ? Restrictions.ilike("program", program, MatchMode.EXACT) : Restrictions.sqlRestriction(SQL_TRUE))
					.add(Restrictions.eq("angkatan", angkatan))
					.add(biodataCalonMahasiswa.getSemesterMulai() != null ? Restrictions.ilike("mulaiBelajarDiSemester", biodataCalonMahasiswa.getSemesterMulai(), MatchMode.EXACT) : Restrictions.sqlRestriction(SQL_TRUE));

			criteria.addOrder(Order.desc("id"));

			List<DetailBiaya> detailBiaya = criteria.list();

			if (isHarusAngsuranForJenjang) {
				List<DetailBiaya> biayaDefaultBiaya = SetingBiayaHelper.getDetailBiayaBukanDefaultBiaya(session, angkatan,
						jenjang, semester, jenisKegiatan, biodataCalonMahasiswa.getStatusAwalMahasiswa(),
						ConstantValues.AKTIF, jenisSeleksi,
						biodataCalonMahasiswa.getGelombangPendaftaran(), biodataCalonMahasiswa.getPaket(), jurusan, program,
						kelamin, afiliasiCalonMahasiswa, ta);
				if (biayaDefaultBiaya != null && !biayaDefaultBiaya.isEmpty()) {
					for (DetailBiaya detailBiayaDefault : biayaDefaultBiaya) {
						detailBiaya.add(detailBiayaDefault);
					}
				}
			}


			Map<Long, DetailBiaya> maps = new HashMap<Long, DetailBiaya>();
			Map<Long, Long> ids = new HashMap<Long, Long>();

			for (DetailBiaya biaya : detailBiaya) {
				if (biaya != null && biaya.getItemBiaya() != null) {
					Long value = ids.get(biaya.getItemBiaya().getId());
					if (value == null || value < biaya.getId()) {
						ids.put(biaya.getItemBiaya().getId(), biaya.getId());
						maps.put(biaya.getItemBiaya().getId(), biaya);
					}
				}
			}

			TreeSet d = new TreeSet(maps.values());

			JSONObject data = new JSONObject();
			try {
				for (Object o : d) {
					try {
						if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya1 = (DetailBiaya) o;
							data.put(detailBiaya1.getId().toString(), "1");

							if (biodataCalonMahasiswa.getMahasiswa() != null) {
								detailBiaya1.updateKeterangan(biodataCalonMahasiswa.getMahasiswa(), semester);
							}

							GeneralValueObject.masukkanData(DetailBiaya.class, detailBiaya1);
						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
							data.put(biaya.getId().toString(), "2");
							GeneralValueObject.masukkanData(PengaturanPembayaranBulanan.class, biaya);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:901");}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:903");}

			biodataCalonMahasiswa.put(data.toString(), key);

			return d;
			
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:910");
			return new TreeSet<DetailBiaya>();
		} finally {
			closeOpenedSession(session);
		}
	}

	public static Collection<DetailBiaya> getDetailBiayaMahasiswaBaru(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan) {
		Jurusan jurusan = null;
		if (biodataCalonMahasiswa != null) {
			jurusan = biodataCalonMahasiswa.getProdiLulus();
		}
		return getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, jurusan, false);
	}

	public static void filterCriteriaDenganNilaiTambahan(Criteria criteria, Session session, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa) {
		if (criteria == null || session == null) {
			return;
		}
		
		Konfigurasi konfigurasiTambahan1 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_1_paramater_tambahan", Konfigurasi.TIDAK_AKTIF, "-1", "", "");
		Konfigurasi konfigurasiTambahan2 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_2_paramater_tambahan", Konfigurasi.TIDAK_AKTIF, "-1", "", "");
		Konfigurasi konfigurasiTambahan3 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_3_paramater_tambahan", Konfigurasi.TIDAK_AKTIF, "-1", "", "");

		List<String> nilaiTambahan = null;
		
		boolean isAktif1 = konfigurasiTambahan1 != null && Konfigurasi.AKTIF.equals(konfigurasiTambahan1.getNilai());
		boolean isAktif2 = konfigurasiTambahan2 != null && Konfigurasi.AKTIF.equals(konfigurasiTambahan2.getNilai());
		boolean isAktif3 = konfigurasiTambahan3 != null && Konfigurasi.AKTIF.equals(konfigurasiTambahan3.getNilai());

		if (isAktif1 || isAktif2 || isAktif3) {

			String parameterTambahanInds = null;

			if (mahasiswa != null && mahasiswa.getId() != null) {
				parameterTambahanInds = (String) session.createCriteria(BiodataMahasiswa.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.addOrder(Order.desc("id"))
						.setMaxResults(1)
						.setProjection(Projections.property("parameterTambahanInds"))
						.uniqueResult();
			} else if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getParameterTambahanInds() != null) {
				parameterTambahanInds = biodataCalonMahasiswa.getParameterTambahanInds();
			}

			if (parameterTambahanInds != null && !parameterTambahanInds.trim().isEmpty()) {
				nilaiTambahan = new ArrayList<String>();
				String[] spl = parameterTambahanInds.split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					String lbl = value.length > 0 ? value[0].trim() : "";
					String val = value.length > 1 ? value[1].trim() : "";
					if (!val.isEmpty()) {
						try {
							nilaiTambahan.add(lbl.split("->")[1].trim() + "<=>" + val);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:967");}
					}
				}
			}
		}

		if (nilaiTambahan != null && !nilaiTambahan.isEmpty()) {
			if (isAktif1) {
				criteria.add(Restrictions.in("nilaiTambahan1", nilaiTambahan));
			}
			if (isAktif2) {
				criteria.add(Restrictions.in("nilaiTambahan2", nilaiTambahan));
			}
			if (isAktif3) {
				criteria.add(Restrictions.in("nilaiTambahan3", nilaiTambahan));
			}
		}
	}


	private static boolean tampilkanPengaturanBulananNolNilaiBisaDiubah() {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(
					"tampilkan_pengaturan_bulanan_nol_nilai_bisa_diubah", Konfigurasi.TIDAK_AKTIF);
			return konfigurasi != null && Konfigurasi.AKTIF.equals(konfigurasi.getNilai());
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean isAktifPengaturanBulanan(PengaturanPembayaranBulanan pembayaranBulanan) {
		try {
			return pembayaranBulanan != null && Boolean.TRUE.equals(pembayaranBulanan.getAktif());
		} catch (Exception e) {
			return false;
		}
	}

	private static Double ambilNominalPengaturanBulananAman(PengaturanPembayaranBulanan pembayaranBulanan,
			Mahasiswa mahasiswa, Integer semester) {
		Double nominal = Double.valueOf(0.0);
		try {
			if (pembayaranBulanan == null) {
				return nominal;
			}
			try {
				Double nominalModifikasi = PembayaranNominalModifikasiHelper.ambilNominalModifikasi(pembayaranBulanan,
						mahasiswa, semester);
				if (nominalModifikasi != null) {
					nominal = nominalModifikasi;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1018");
			}
			if (Math.abs(nominal.doubleValue()) > 0.01) {
				return nominal;
			}
			try {
				Double nominalAsli = pembayaranBulanan.getNominal();
				if (nominalAsli != null) {
					nominal = nominalAsli;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1028");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1030");
		}
		return nominal == null ? Double.valueOf(0.0) : nominal;
	}

	private static boolean isPengaturanBulananLayakDitampilkan(PengaturanPembayaranBulanan pembayaranBulanan,
			boolean tampilkanNolNilaiBisaDiubah, Mahasiswa mahasiswa, Integer semester) {
		try {
			if (!isAktifPengaturanBulanan(pembayaranBulanan)) {
				return false;
			}
			Double nominal = ambilNominalPengaturanBulananAman(pembayaranBulanan, mahasiswa, semester);
			if (nominal != null && Math.abs(nominal.doubleValue()) > 0.01) {
				return true;
			}
			DetailBiaya detailBiaya = pembayaranBulanan.getDetailBiaya();
			ItemBiaya itemBiaya = detailBiaya == null ? null : detailBiaya.getItemBiaya();
			if (itemBiaya != null && ItemBiaya.DIKALI_NILAI_MINUS.equals(itemBiaya.getPenghitungan())) {
				return true;
			}
			if (Boolean.TRUE.equals(pembayaranBulanan.getTetapDitampilkanWalaupunNol())) {
				return true;
			}
			if (tampilkanNolNilaiBisaDiubah && itemBiaya != null && Boolean.TRUE.equals(itemBiaya.getNilaiBisaDiubah())) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1056");
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes" })
	public static List<PengaturanPembayaranBulanan> saringPengaturanPembayaranBulanan(List pengaturanPembayaranBulanans,
			boolean nolMasukFilter) {
		return saringPengaturanPembayaranBulanan(pengaturanPembayaranBulanans, nolMasukFilter, null, null);
	}

	@SuppressWarnings({ "rawtypes" })
	public static List<PengaturanPembayaranBulanan> saringPengaturanPembayaranBulanan(List pengaturanPembayaranBulanans,
			boolean nolMasukFilter, Mahasiswa mahasiswa, Integer semester) {
		Map<String, PengaturanPembayaranBulanan> map = new java.util.HashMap<String, PengaturanPembayaranBulanan>(
				pengaturanPembayaranBulanans == null ? 16 : Math.max(16, pengaturanPembayaranBulanans.size()));
		
		boolean tampilkanNolNilaiBisaDiubah = tampilkanPengaturanBulananNolNilaiBisaDiubah();
		if (pengaturanPembayaranBulanans != null) {
			for (Object valueObject : pengaturanPembayaranBulanans) {
				try {
					if (valueObject instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan pembayaranBulanan = (PengaturanPembayaranBulanan) valueObject;

						if (!isPengaturanBulananLayakDitampilkan(pembayaranBulanan, tampilkanNolNilaiBisaDiubah,
								mahasiswa, semester)) {
							continue;
						}
						
						if (pembayaranBulanan.getDetailBiaya() != null
								&& pembayaranBulanan.getDetailBiaya().getItemBiaya() != null
								&& pembayaranBulanan.getRealBulan() != null) {
							String bulan = pembayaranBulanan.getRealBulan() + "-" + pembayaranBulanan.getDetailBiaya().getItemBiaya().getId();
							
							boolean isZeroFilter = false;
							if (nolMasukFilter && map.containsKey(bulan)) {
								PengaturanPembayaranBulanan existingP = map.get(bulan);
								Double existingNominal = ambilNominalPengaturanBulananAman(existingP, mahasiswa, semester);
								if (existingNominal != null && Math.abs(existingNominal.doubleValue()) <= 0.01) {
									isZeroFilter = true;
								}
							}
							
							if (!map.containsKey(bulan) || isZeroFilter) {
								map.put(bulan, pembayaranBulanan);
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1104");}
			}
		}
		
		List<PengaturanPembayaranBulanan> bulanans = new ArrayList<PengaturanPembayaranBulanan>(map.values());
		try {
			Collections.sort(bulanans);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1111");}
		
		return bulanans;
	}

	@SuppressWarnings("rawtypes")
	public static int countBulanan(Session session, Mahasiswa mahasiswa, JenisKegiatan jenisKegiatan, Integer semester,
			Collection detailBiayas, boolean reload, boolean comitManual) {
		return countBulanan(session, mahasiswa, null, jenisKegiatan, semester, detailBiayas, reload, comitManual);
	}

	@SuppressWarnings("rawtypes")
	public static int countBulanan(Session session, BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan,
			Integer semester, Collection detailBiayas, boolean reload, boolean comitManual) {
		return countBulanan(session, null, biodataCalonMahasiswa, jenisKegiatan, semester, detailBiayas, reload,
				comitManual);
	}

	@SuppressWarnings({ "rawtypes" })
	public static int countBulanan(Session session, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, Integer semester, Collection detailBiayas, boolean reload,
			boolean comitManual) {
		if (PengecualianTagihanList.adalah(detailBiayas)) {
			return 0;
		}

		// Fresh reload untuk hindari stale cache dari combobox lama
		if (jenisKegiatan != null && jenisKegiatan.getId() != null) {
			Session refreshSession = session;
			boolean isLocalRefreshSession = false;
			try {
				if (refreshSession == null || !refreshSession.isOpen()) {
					refreshSession = HibernateUtil.getSessionFactory().openSession();
					isLocalRefreshSession = true;
				}
				JenisKegiatan freshJk = (JenisKegiatan) refreshSession.get(JenisKegiatan.class, jenisKegiatan.getId());
				if (freshJk != null) jenisKegiatan = freshJk;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1145");
			} finally {
				if (isLocalRefreshSession && refreshSession != null && refreshSession.isOpen()) {
					try { refreshSession.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1148");}
				}
			}
		}

		if (jenisKegiatan != null) {
			Jenjang mhsJenjang = null;
			if (mahasiswa != null) {
				mhsJenjang = mahasiswa.getJurusan() != null
						? mahasiswa.getJurusan().getJenjang() : mahasiswa.getJenjang();
			} else if (biodataCalonMahasiswa != null) {
				mhsJenjang = biodataCalonMahasiswa.getJenjang();
			}
			Integer angkatanMhs = mahasiswa != null ? mahasiswa.getTahunangkatan()
					: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getTahun() : null);
			Boolean modeAngsuran = jenisKegiatan.modeAngsuranUntukJenjang(mhsJenjang, semester, angkatanMhs);
			if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
					"[DEBUG-ANGSURAN][countBulanan] mhs="
					+ (mahasiswa != null ? mahasiswa.getNim() : (biodataCalonMahasiswa != null ? "cln-" + biodataCalonMahasiswa.getId() : "null"))
					+ " jk=" + jenisKegiatan.getNama()
					+ " jenjang=" + (mhsJenjang != null ? mhsJenjang.getNama() : "null")
					+ " detailBiayas=" + (detailBiayas != null ? detailBiayas.size() : 0)
					+ " modeAngsuran=" + modeAngsuran);
			if (Boolean.TRUE.equals(modeAngsuran)) {
				// PER-SEMESTER: aturan per-jenjang tidak boleh menimpa kenyataan billing.
				// Konfigurasi bulanan dibuat per semester (contoh nyata: S2 smt 1-3 bulanan,
				// smt 4 sekali tagih) — hitung baris bulanan yang BENAR-BENAR ada untuk
				// semester ini; 0 berarti semester ini bukan bulanan meski jenjang ditandai
				// harus angsuran. -1 = pengecekan gagal → pertahankan perilaku lama (paksa 1).
				// CATATAN (revert 07-17): status "Tagihan Default" di SettingBiaya TIDAK lagi
				// memaksa mode menjadi bukan-bulanan — mode murni mengikuti aturan jenjang/
				// semester dan keberadaan baris bulanan di billing.
				int nyata = ais.action.ws.util.PembayaranUtil.hitungBarisBulananSemester(session,
						jenisKegiatan, mhsJenjang, semester, angkatanMhs, detailBiayas);
				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][countBulanan] → TRUE: baris bulanan nyata semester ini=" + nyata);
				if (nyata >= 0) {
					return nyata;
				}
				return 1;
			} else if (Boolean.FALSE.equals(modeAngsuran)) {
				if (detailBiayas != null && !detailBiayas.isEmpty()) {
					if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
							"[DEBUG-ANGSURAN][countBulanan] → FALSE + ada billing reguler (" + detailBiayas.size() + "): return 0");
					return 0;
				}
				// Billing reguler kosong → cek PPB agar bisa fallback ke angsuran
				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][countBulanan] → FALSE + billing reguler KOSONG: cek PPB untuk jenjang="
						+ (mhsJenjang != null ? mhsJenjang.getNama() : "null"));
				if (mhsJenjang != null) {
					Session ppbSession = session;
					boolean isLocalPpb = false;
					try {
						if (ppbSession == null || !ppbSession.isOpen()) {
							ppbSession = HibernateUtil.getSessionFactory().openSession();
							isLocalPpb = true;
						}
						@SuppressWarnings("unchecked")
						java.util.List<PengaturanPembayaranBulanan> ppbList = ppbSession
								.createCriteria(PengaturanPembayaranBulanan.class)
								.createAlias("detailBiaya", "db")
								.createAlias("db.itemBiaya", "dbItem")
								.add(Restrictions.eq("aktif", true))
								.add(Restrictions.eq("db.jenisKegiatan", jenisKegiatan))
								.add(Restrictions.eq("db.jenjang", mhsJenjang))
								// WAJIB samakan semester dengan yang sedang diminta -- tanpa filter ini baris
								// bulanan milik semester LAIN (mis. DetailBiaya.semester=2) ikut terhitung saat
								// mahasiswa sedang di semester 1, sehingga tagihan semester lain bocor tampil
								// (lihat juga getDetailBiayaMahasiswadariDatabase yang sudah pakai eq("semester",..) ketat).
								.add(Restrictions.eq("db.semester", semester))
								.add(Restrictions.or(
										Restrictions.eq("dbItem.penghitungan", ItemBiaya.DIKALI_NILAI_MINUS),
										Restrictions.gt("nominal", 0.01)))
								.list();
						// (revert 07-17) Status "Tagihan Default" TIDAK lagi menyaring hitungan —
						// seluruh baris bulanan aktif dihitung apa adanya.
						int ppbCount = ppbList == null ? 0 : ppbList.size();
						if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
								"[DEBUG-ANGSURAN][countBulanan] → PPB count=" + ppbCount);
						if (ppbCount > 0) {
							if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
									"[DEBUG-ANGSURAN][countBulanan] → return " + ppbCount + " (PPB fallback)");
							return ppbCount;
						}
					} catch (Exception e) {
						if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
								"[DEBUG-ANGSURAN][countBulanan] → PPB query ERROR: " + e.getMessage());
					} finally {
						if (isLocalPpb && ppbSession != null && ppbSession.isOpen()) {
							try { ppbSession.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1249");}
						}
					}
				}
				if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
						"[DEBUG-ANGSURAN][countBulanan] → FALSE + PPB kosong: return 0");
				return 0;
			}
			// modeAngsuran == null → tidak ada aturan per-jenjang, ikuti DB count di bawah
			if (JenisKegiatan.DEBUG_MODE_ANGSURAN) System.out.println(
					"[DEBUG-ANGSURAN][countBulanan] → null: tidak ada aturan angsuran, lanjut DB count");
		}

		String key = (biodataCalonMahasiswa != null ? "cln_mhs_" + biodataCalonMahasiswa.getId()
				: "mhs_" + (mahasiswa != null ? mahasiswa.getId() : "null")) + "_" 
				+ (jenisKegiatan != null ? jenisKegiatan.getId() : "null") + "_" + semester + "_aktif_tagihan_v2";

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = Common.getJSONTemporary(biodataCalonMahasiswa != null ? biodataCalonMahasiswa : mahasiswa, key);
			if (!reload && jsonObject != null) {
				if (jsonObject.has(key) && !jsonObject.isNull(key)) {
					return jsonObject.getInt(key);
				}
			}
		} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1274");}

		if (jsonObject == null) {
			jsonObject = new JSONObject();
		}

		int countPengaturanBulanan = 0;

		// (revert 07-17) Status "Tagihan Default" di SettingBiaya TIDAK lagi menyaring
		// penghitung mode angsuran — seluruh DetailBiaya dihitung apa adanya.
		if (detailBiayas != null && !detailBiayas.isEmpty()) {
			Session activeSession = session;
			boolean isLocalSession = false;
			boolean isLocalTransaction = false;

			try {
				if (activeSession == null || !activeSession.isOpen()) {
					activeSession = HibernateUtil.getSessionFactory().openSession();
					isLocalSession = true;
				}

				Transaction activeTransaction = activeSession.getTransaction();
				if (activeTransaction == null || !activeTransaction.isActive()) {
					activeTransaction = activeSession.beginTransaction();
					isLocalTransaction = true;
				}

				Number count = (Number) activeSession.createCriteria(PengaturanPembayaranBulanan.class)
								.createAlias("detailBiaya", "detailBiaya")
								.createAlias("detailBiaya.itemBiaya", "itemBiaya")
								.add(Restrictions.eq("aktif", true))
								.add(Restrictions.in("detailBiaya", detailBiayas))
								.add(Restrictions.or(
										Restrictions.eq("itemBiaya.penghitungan", ItemBiaya.DIKALI_NILAI_MINUS),
										Restrictions.gt("nominal", 0.01)))
								.setProjection(Projections.rowCount()).uniqueResult();

				countPengaturanBulanan = count != null ? count.intValue() : 0;

				if (isLocalTransaction) {
					activeTransaction.commit();
				}

			} catch (Exception e) {
				if (isLocalTransaction && activeSession != null && activeSession.getTransaction() != null && activeSession.getTransaction().isActive()) {
					try { activeSession.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1333");}
				}
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PembayaranUtilHelper.java:1335");
			} finally {
				if (isLocalSession && activeSession != null) {
					try { if (activeSession.isOpen()) activeSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1338");}
					try { activeSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1339");}
					try { if (activeSession.isOpen()) activeSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1340");}
				}
			}
		}

		try {
			jsonObject.put(key, countPengaturanBulanan);
			Common.setJSONTemporary(biodataCalonMahasiswa != null ? biodataCalonMahasiswa : mahasiswa, key, jsonObject);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1348");}

		return countPengaturanBulanan;
	}

	/**
	 * Mengisi dataTagihanData dari riwayat CicilanPembayaran jika kosong,
	 * sekaligus memperbaiki DetailBiaya/PengaturanPembayaranBulanan yang
	 * nilaiBiaya/nominal = 0. Dipanggil dari DaftarUlangMahasiswa*Action.
	 * student boleh Mahasiswa atau BiodataCalonMahasiswa.
	 *
	 * @param semester semester yang SEDANG diminta layar ini; baris cicilan yang DetailBiaya-nya
	 *                 punya semester lain (non-null, tidak sama) DILEWATI -- tanpa ini, riwayat
	 *                 cicilan/PPB dari semester LAIN (mis. semester berubah lewat Excel upload)
	 *                 ikut bocor tampil di semester yang sedang dibuka (lihat catatan di countBulanan).
	 */
	@SuppressWarnings("unchecked")
	public static void fallbackTagihanDariCicilan(Object student, JenisKegiatan jenisKegiatan,
			List dataTagihanData, Map<Long, DetailBiaya> itemBiayas, Integer semester) {
		Object studentIdForLog = null;
		try {
			if (student instanceof Mahasiswa) {
				studentIdForLog = ((Mahasiswa) student).getId() + "-" + ((Mahasiswa) student).getNim();
			} else if (student instanceof BiodataCalonMahasiswa) {
				studentIdForLog = ((BiodataCalonMahasiswa) student).getId() + "-"
						+ ((BiodataCalonMahasiswa) student).getNoRegistrasi();
			}
		} catch (Exception ignoredLog) { ais.common.ErrorAuditUtil.record(ignoredLog, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:fallbackTagihanDariCicilan:log"); }
		System.out.println("[TAGIHAN-DEBUG] ==> fallbackTagihanDariCicilan student=" + studentIdForLog
				+ " jenisKegiatan=" + (jenisKegiatan == null ? "null" : jenisKegiatan.getId() + "-" + jenisKegiatan.getNamaKegiatan())
				+ " semester=" + semester + " dataTagihanData.isEmpty()="
				+ (dataTagihanData == null ? "null" : dataTagihanData.isEmpty()));

		if (dataTagihanData == null || !dataTagihanData.isEmpty() || student == null) {
			System.out.println(
					"[TAGIHAN-DEBUG] fallbackTagihanDariCicilan: DIBATALKAN lebih awal (dataTagihanData sudah berisi data lain, atau student null) -> fallback TIDAK dijalankan.");
			return;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Criteria fallbackCrit = session.createCriteria(CicilanPembayaran.class);
			Criteria kegCrit = fallbackCrit.createCriteria("kegiatan");
			if (student instanceof Mahasiswa) {
				kegCrit.add(Restrictions.eq("mahasiswa", student));
			} else {
				kegCrit.add(Restrictions.eq("calonMahasiswa", student));
			}
			if (jenisKegiatan != null && jenisKegiatan.getId() != null)
				kegCrit.add(Restrictions.eq("jenisKegiatan", jenisKegiatan));
			List<CicilanPembayaran> fallbackList = fallbackCrit.list();

			System.out.println("[TAGIHAN-DEBUG] fallbackTagihanDariCicilan: jumlah CicilanPembayaran ditemukan (semua jenisKegiatan+semester, belum difilter semester) = "
					+ (fallbackList == null ? 0 : fallbackList.size()));
			if (fallbackList != null) {
				for (CicilanPembayaran cpLog : fallbackList) {
					if (cpLog == null) {
						continue;
					}
					System.out.println("[TAGIHAN-DEBUG]   - cicilanId=" + cpLog.getId() + " ke=" + cpLog.getKe()
							+ " nilai=" + cpLog.getNilai() + " itemBiaya="
							+ (cpLog.getItemBiaya() == null ? "null" : cpLog.getItemBiaya().getId() + "-" + cpLog.getItemBiaya().getNama())
							+ " bayarKe=" + cpLog.getBayarKe() + " detailBiayaId="
							+ (cpLog.getDetailBiaya() == null ? "null (BELUM ber-FK ke DetailBiaya manapun)" : cpLog.getDetailBiaya().getId())
							+ " detailBiayaSemester="
							+ (cpLog.getDetailBiaya() == null ? "-" : cpLog.getDetailBiaya().getSemester())
							+ " detailBiayaSettingBiayaId="
							+ (cpLog.getDetailBiaya() == null || cpLog.getDetailBiaya().getSettingBiaya() == null ? "null"
									: cpLog.getDetailBiaya().getSettingBiaya().getId())
							+ " pengaturanPembayaranBulananId="
							+ (cpLog.getPengaturanPembayaranBulanan() == null ? "null" : cpLog.getPengaturanPembayaranBulanan().getId()));
				}
			}

			if (fallbackList == null || fallbackList.isEmpty()) {
				System.out.println("[TAGIHAN-DEBUG] fallbackTagihanDariCicilan: TIDAK ADA riwayat cicilan sama sekali -> keluar tanpa mengisi apa pun.");
				return;
			}

			// Tentukan mode angsuran: dari flag jenisKegiatan atau ada cicilan ber-ppb
			boolean isAngsuranMode = jenisKegiatan != null
					&& Boolean.TRUE.equals(jenisKegiatan.getHanyaBerupaAngsuran());
			if (!isAngsuranMode) {
				for (CicilanPembayaran cp : fallbackList) {
					if (cp.getPengaturanPembayaranBulanan() != null) {
						isAngsuranMode = true;
						break;
					}
				}
			}
			System.out.println("[TAGIHAN-DEBUG] fallbackTagihanDariCicilan: mode = " + (isAngsuranMode ? "ANGSURAN (PPB)" : "NON-ANGSURAN (DetailBiaya langsung)"));

			if (isAngsuranMode) {
				// === MODE ANGSURAN: kumpulkan PPB (hanya cicilan ber-ppb) ===
				Map<Long, PengaturanPembayaranBulanan> ppbMap = new HashMap<Long, PengaturanPembayaranBulanan>();
				for (CicilanPembayaran cp : fallbackList) {
					PengaturanPembayaranBulanan ppb = cp.getPengaturanPembayaranBulanan();
					if (ppb == null || ppb.getId() == null)
						continue;
					Integer dbSemester = ppb.getDetailBiaya() == null ? null : ppb.getDetailBiaya().getSemester();
					// PERBAIKAN "tagihan semester lain ikut muncul walau tidak relevan": SEBELUMNYA
					// dbSemester==null diperlakukan sbg wildcard (cocok semua semester), padahal
					// baris legacy tanpa semester (biasanya data lama sebelum field ini konsisten
					// diisi) jadi ikut nongol di SETIAP semester yg pernah dibuka -- terbukti dari
					// laporan nyata (baris yg sama muncul di smt 6 & smt 8). Kalau kita SEDANG minta
					// semester tertentu, baris yg semesternya tidak diketahui (null) TIDAK BOLEH
					// dianggap cocok -- lebih aman kosong drpd salah semester.
					if (semester != null && !semester.equals(dbSemester))
						continue;
					if (!ppbMap.containsKey(ppb.getId())) {
						ppbMap.put(ppb.getId(), ppb);
						dataTagihanData.add(ppb);
						if (itemBiayas != null && ppb.getDetailBiaya() != null
								&& ppb.getDetailBiaya().getId() != null)
							itemBiayas.put(ppb.getDetailBiaya().getId(), ppb.getDetailBiaya());
					}
				}
				// Perbaiki DetailBiaya.nilaiBiaya=0 dan PPB.nominal=0 dari sum nilaiAsli cicilan
				if (!ppbMap.isEmpty()) {
					Map<Long, Double> sumPerDb = new HashMap<Long, Double>();
					for (CicilanPembayaran cp : fallbackList) {
						PengaturanPembayaranBulanan ppb = cp.getPengaturanPembayaranBulanan();
						if (ppb == null || ppb.getDetailBiaya() == null || ppb.getDetailBiaya().getId() == null)
							continue;
						if (cp.getNilaiAsli() == null || cp.getNilaiAsli() < 0.001)
							continue;
						Long dbId = ppb.getDetailBiaya().getId();
						Double cur = sumPerDb.containsKey(dbId) ? sumPerDb.get(dbId) : 0.0;
						sumPerDb.put(dbId, cur + cp.getNilaiAsli());
					}
					if (!sumPerDb.isEmpty()) {
						Transaction txFix = null;
						try {
							txFix = session.beginTransaction();
							for (Map.Entry<Long, Double> entry : sumPerDb.entrySet()) {
								Long dbId = entry.getKey();
								double totalNilai = entry.getValue();
								DetailBiaya db = (DetailBiaya) session.get(DetailBiaya.class, dbId);
								if (db != null) {
									if (db.getNilaiBiaya() == null || db.getNilaiBiaya() < 0.01) {
										db.setNilaiBiaya(totalNilai);
										session.saveOrUpdate(db);
										if (itemBiayas != null) itemBiayas.put(dbId, db);
									}
									List<PengaturanPembayaranBulanan> ppbList = session
											.createCriteria(PengaturanPembayaranBulanan.class)
											.add(Restrictions.eq("detailBiaya", db)).list();
									for (PengaturanPembayaranBulanan ppbFix : ppbList) {
										if ((ppbFix.getNominal() == null || ppbFix.getNominal() < 0.01)
												&& ppbFix.getPersentase() != null && ppbFix.getPersentase() > 0) {
											ppbFix.setNominal(totalNilai * ppbFix.getPersentase() / 100.0);
											session.saveOrUpdate(ppbFix);
										}
									}
								}
							}
							txFix.commit();
						} catch (Exception eTx) {
							if (txFix != null)
								try { txFix.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1449");}
							Common.tampilErrorJikaAdmin(eTx);
						}
					}
				}
			} else {
				// === MODE NON-ANGSURAN: kumpulkan DetailBiaya (hanya cicilan tanpa ppb) ===
				Map<Long, DetailBiaya> fallbackMap = new HashMap<Long, DetailBiaya>();
				// PERBAIKAN "item tagihan tampil dobel di layar mahasiswa lama": dedup di sini
				// SEBELUMNYA memakai raw detail_biaya id sbg kunci -- kalau riwayat cicilan
				// mahasiswa ternyata terpecah di ANTARA DUA DetailBiaya berbeda utk Item Biaya
				// + bayarKe yg SAMA (mis. akibat duplikasi DetailBiaya lama sebelum "jaring
				// pengaman terakhir" di SetingBiayaHelper dipasang), method ini ikut menambahkan
				// KEDUA baris ke tampilan -- padahal DetailPembayaranMahasiswaRenderer sendiri
				// menjumlah "Dibayar" per (itemBiaya, bayarKe), BUKAN per detail_biaya id, jadi
				// menampilkan >1 baris utk kombinasi item+bayarKe yg sama murni duplikat tampilan
				// (masing-masing baris akan menampilkan TOTAL PEMBAYARAN YANG SAMA, seolah lunas
				// dobel), bukan mewakili tagihan yg benar-benar berbeda. Dedup sekarang JUGA per
				// (itemBiaya id + bayarKe) -- baris PERTAMA yg ditemukan (urutan dari fallbackList)
				// yang dipakai, sisanya dilewati & dicatat ke audit log agar admin/pengembang bisa
				// menelusuri baris DetailBiaya duplikat yg sebenarnya di database (idealnya baris
				// duplikat itu sendiri dibersihkan permanen, bukan cuma disembunyikan di sini).
				java.util.Set<String> itemBayarKeSudahAda = new java.util.HashSet<String>();
				for (CicilanPembayaran cp : fallbackList) {
					if (cp.getPengaturanPembayaranBulanan() != null) {
						System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] cicilanId=" + cp.getId()
								+ " DILEWATI (punya pengaturanPembayaranBulanan, seharusnya masuk mode angsuran)");
						continue;
					}
					if (cp.getDetailBiaya() == null || cp.getDetailBiaya().getId() == null) {
						System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] cicilanId=" + cp.getId()
								+ " DILEWATI (detailBiaya null/tanpa id -> cicilan ini TIDAK bisa dipetakan ke tagihan manapun)");
						continue;
					}
					Integer dbSemester = cp.getDetailBiaya().getSemester();
					// PERBAIKAN "tagihan semester lain ikut muncul walau tidak relevan" (lihat
					// komentar sama di mode angsuran di atas): dbSemester==null TIDAK LAGI
					// dianggap cocok utk semester tertentu -- baris legacy tanpa semester yg
					// jelas semestinya TIDAK ditampilkan sama sekali drpd salah semester.
					if (semester != null && !semester.equals(dbSemester)) {
						System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] cicilanId=" + cp.getId() + " detailBiayaId="
								+ cp.getDetailBiaya().getId() + " DILEWATI (semester DetailBiaya=" + dbSemester
								+ " != semester yg dicari=" + semester + ")");
						continue;
					}
					Long dbId = cp.getDetailBiaya().getId();
					if (fallbackMap.containsKey(dbId)) {
						System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] cicilanId=" + cp.getId() + " detailBiayaId="
								+ dbId + " -- DetailBiaya ini sudah diproses sebelumnya (cicilan lain miliknya), lanjut jumlahkan saja.");
						continue;
					}
					ItemBiaya itemBiayaCp = cp.getDetailBiaya().getItemBiaya();
					String itemBayarKeKey = (itemBiayaCp == null || itemBiayaCp.getId() == null ? "null"
							: itemBiayaCp.getId().toString()) + "_" + cp.getDetailBiaya().getBayarKe();
					System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] cicilanId=" + cp.getId() + " detailBiayaId=" + dbId
							+ " itemBiaya=" + (itemBiayaCp == null ? "null" : itemBiayaCp.getId() + "-" + itemBiayaCp.getNama())
							+ " bayarKe=" + cp.getDetailBiaya().getBayarKe() + " -> kunci dedup=\"" + itemBayarKeKey + "\""
							+ " | kunci ini " + (itemBayarKeSudahAda.contains(itemBayarKeKey) ? "SUDAH ADA sebelumnya" : "BELUM ADA (baris baru)"));
					if (itemBayarKeSudahAda.contains(itemBayarKeKey)) {
						System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] => DUPLIKAT TERDETEKSI: detailBiayaId=" + dbId
								+ " (kunci=" + itemBayarKeKey + ") DILEWATI dari tampilan, sudah ada baris lain utk item+bayarKe yg sama. INI KEMUNGKINAN BESAR PENYEBAB DOBEL YANG DILAPORKAN.");
						ais.common.ErrorAuditUtil.record(
								new Exception(
										"Duplikat DetailBiaya terdeteksi di fallbackTagihanDariCicilan (item+bayarKe="
												+ itemBayarKeKey + "): detailBiayaId=" + dbId
												+ " DILEWATI dari tampilan karena sudah ada baris lain utk item+bayarKe yg sama -- perlu dicek manual apakah baris DetailBiaya ini benar duplikat & sebaiknya dibersihkan."),
								"auto-audit(fallback-dedup-item-bayarke) src/ais/action/master/helper/PembayaranUtilHelper.java:fallbackTagihanDariCicilan");
						continue;
					}
					itemBayarKeSudahAda.add(itemBayarKeKey);
					fallbackMap.put(dbId, cp.getDetailBiaya());
					dataTagihanData.add(cp.getDetailBiaya());
					System.out.println("[TAGIHAN-DEBUG]   [non-angsuran] => DetailBiaya id=" + dbId
							+ " DITAMBAHKAN ke dataTagihanData (nilaiBiaya saat ini=" + cp.getDetailBiaya().getNilaiBiaya() + ")");
					if (itemBiayas != null && cp.getDetailBiaya().getItemBiaya() != null)
						itemBiayas.put(dbId, cp.getDetailBiaya());
				}
				System.out.println("[TAGIHAN-DEBUG] fallbackTagihanDariCicilan: SELESAI mode non-angsuran -> dataTagihanData berisi "
						+ dataTagihanData.size() + " baris DetailBiaya.");
				// Perbaiki nilaiBiaya=0 dan PengaturanBulanan.nominal=0
				if (!fallbackMap.isEmpty()) {
					Map<Long, Double> sumNilaiMap = new HashMap<Long, Double>();
					for (CicilanPembayaran cp : fallbackList) {
						if (cp.getPengaturanPembayaranBulanan() != null)
							continue;
						if (cp.getDetailBiaya() != null && cp.getDetailBiaya().getId() != null
								&& cp.getNilaiAsli() != null && cp.getNilaiAsli() > 0.001) {
							Long dbId = cp.getDetailBiaya().getId();
							Double cur = sumNilaiMap.containsKey(dbId) ? sumNilaiMap.get(dbId) : 0.0;
							sumNilaiMap.put(dbId, cur + cp.getNilaiAsli());
						}
					}
					Transaction txFix = null;
					try {
						txFix = session.beginTransaction();
						for (Map.Entry<Long, DetailBiaya> entry : fallbackMap.entrySet()) {
							Long dbId = entry.getKey();
							DetailBiaya db = entry.getValue();
							if ((db.getNilaiBiaya() == null || db.getNilaiBiaya() < 0.01)
									&& sumNilaiMap.containsKey(dbId)) {
								double totalNilai = sumNilaiMap.get(dbId);
								db.setNilaiBiaya(totalNilai);
								session.saveOrUpdate(db);
								List<PengaturanPembayaranBulanan> ppbList = session
										.createCriteria(PengaturanPembayaranBulanan.class)
										.add(Restrictions.eq("detailBiaya", db)).list();
								for (PengaturanPembayaranBulanan ppb : ppbList) {
									if ((ppb.getNominal() == null || ppb.getNominal() < 0.01)
											&& ppb.getPersentase() != null && ppb.getPersentase() > 0) {
										ppb.setNominal(totalNilai * ppb.getPersentase() / 100.0);
										session.saveOrUpdate(ppb);
									}
								}
							}
						}
						txFix.commit();
					} catch (Exception eTx) {
						if (txFix != null)
							try { txFix.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1509");}
						Common.tampilErrorJikaAdmin(eTx);
					}
				}
			}
		} catch (Exception eFb) {
			Common.tampilErrorJikaAdmin(eFb);
		} finally {
			try { if (session != null) session.clear(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1517");}
			try { if (session != null) session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1518");}
			try { if (session != null) session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranUtilHelper.java:1519");}
		}
	}
}
