package ais.common;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;

public class CommonPenilaian {

	public static Konfigurasi getKonfigurasi(String tahunAkademik, String jenisSemester, Integer semesterPendek) {
		if (jenisSemester == null || jenisSemester.trim().isEmpty()) {
			jenisSemester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		Konfigurasi konfigurasi = (Konfigurasi) ConstantValues
				.simpleObject(
						HibernateUtil.currentSession().createCriteria(Konfigurasi.class).addOrder(Order.desc("id"))
								.add(Restrictions.eq("info1", jenisSemester))
								.add(Restrictions.eq("nama",
										semesterPendek == null ? Konfigurasi.PENILAIAN : Konfigurasi.PENILAIAN_SP))
								.add(Restrictions.eq("tahunAkademik", tahunAkademik)).setMaxResults(1),
						Konfigurasi.class);
		if (konfigurasi == null) {
			konfigurasi = new Konfigurasi();
			konfigurasi.setKeterangan("Digunakan untuk mengaktifkan / tidak mengaktifkan penilaian");
			konfigurasi.setNama(semesterPendek == null ? Konfigurasi.PENILAIAN : Konfigurasi.PENILAIAN_SP);
			konfigurasi.setTahunAkademik(tahunAkademik);
			konfigurasi.setNilai(Konfigurasi.AKTIF);
			konfigurasi.setInfo1(jenisSemester);
			HibernateUtil.currentSession().save(konfigurasi);
		}
		return konfigurasi;
	}

	public static Konfigurasi getKonfigurasiPersetujuanKrsOlehDosen(String tahunAkademik, String jenisSemester,
			Integer semesterPendek) {
		if (jenisSemester == null || jenisSemester.trim().isEmpty()) {
			jenisSemester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		Konfigurasi konfigurasi = (Konfigurasi) ConstantValues.simpleObject(HibernateUtil.currentSession()
				.createCriteria(Konfigurasi.class).addOrder(Order.desc("id")).add(Restrictions.eq("info1", jenisSemester))
				.add(Restrictions.eq("nama",
						semesterPendek == null ? "aktivasi_persetujuan_KRS_oleh_dosen"
								: "aktivasi_persetujuan_KRS_sp_oleh_dosen"))
				.add(Restrictions.eq("tahunAkademik", tahunAkademik)).setMaxResults(1), Konfigurasi.class);
		if (konfigurasi == null) {
			konfigurasi = new Konfigurasi();
			konfigurasi.setKeterangan("Digunakan untuk mengaktifkan / tidak mengaktifkan persetujuan KRS oleh dosen");
			konfigurasi.setNama(semesterPendek == null ? "aktivasi_persetujuan_KRS_oleh_dosen"
					: "aktivasi_persetujuan_KRS_sp_oleh_dosen");
			konfigurasi.setTahunAkademik(tahunAkademik);
			konfigurasi.setNilai(Konfigurasi.AKTIF);
			konfigurasi.setInfo1(jenisSemester);
			HibernateUtil.currentSession().save(konfigurasi);
		}
		return konfigurasi;
	}

}
