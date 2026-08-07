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
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PembagianKuotaPerkuliahanBerdasarkantahunAngkatan;
import ais.database.model.Perkuliahan;

public class KrsUtilHelper {

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

	public static Integer ambilJumlahDetailperkuliahan(Session session, Perkuliahan perkuliahan, Boolean reload) {

		if (perkuliahan == null) {
			return 0;
		}
		if (!perkuliahan.udah("detailperkulaiahan") || Boolean.TRUE.equals(reload)) {
			perkuliahan.reInitDetailperkuliahan(HibernateUtil.ensureOpenSession(session));
		}

		return perkuliahan.ambilJumlahDetailperkuliahan();
	}

	public static Integer ambilJumlahDetailperkuliahan(Session session, Perkuliahan perkuliahan, Mahasiswa mahasiswa,
			Boolean reload) {

		// if (!perkuliahan.getUdah() || reload) {
		// perkuliahan.reInitDetailperkuliahan(session);
		// perkuliahan.setUdah(true);
		// }

		Long detailperkuliahan = perkuliahan.ambilDetailperkuliahan(mahasiswa);
		return detailperkuliahan == null ? 0 : 1;
	}

	public static String[] rubahStatusPenilaian(Perkuliahan perkuliahan, Boolean reload) {

		// if (!perkuliahan.getUdah() || reload) {
		// perkuliahan.reInitDetailperkuliahan(HibernateUtil.currentSession());
		// perkuliahan.setUdah(true);
		// }

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
