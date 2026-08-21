package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;

public class StainBatusangkarNimGenerator implements NimGenerator {

	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		Integer tahun = calonMahasiswa.getTahun();
		String digitPertama = tahun.toString().substring(2);

		String digitKedua = calonMahasiswa == null || calonMahasiswa.getJenjang() == null ? ""
				: calonMahasiswa.getJenjang().getKode();

		String digitKetiga = ambilKodeProdi(calonMahasiswa == null ? null : calonMahasiswa.getProdiLulus());
		String digitKetiga1 = calonMahasiswa.getJenisKelamin() == null ? "0"
				: calonMahasiswa.getJenisKelamin().equals("Laki-laki") ? "1" : "2";
		validasiKomponenNim(digitPertama, "tahun", calonMahasiswa);
		validasiKomponenNim(digitKedua, "jenjang", calonMahasiswa);
		validasiKomponenNim(digitKetiga, "prodi", calonMahasiswa);
		validasiKomponenNim(digitKetiga1, "jenis kelamin", calonMahasiswa);

		Session session = HibernateUtil.currentNativeSession();
		Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahunangkatan", tahun))
				.add(Restrictions.eq("jenjang", calonMahasiswa.getJenjang()))
				.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
				.longValue();

		jumlah += jumlahPengecualian.size();
		String digitKeempat = "000000000000" + (jumlah + 1);
		digitKeempat = digitKeempat.substring(digitKeempat.length() - 4);

		System.out.println("digit pertama (kode prodi) = " + digitPertama);
		System.out.println("digit kedua (kode tahun) = " + digitKedua);
		System.out.println("digit ketiga (tahun sememster) = " + digitKetiga);
		System.out.println("digit ketiga (jk) = " + digitKetiga1);
		System.out.println("digit keempat (urutan) = " + digitKeempat);

		String nim = digitPertama + digitKedua + digitKetiga + digitKetiga1 + digitKeempat;
		validasiNim(nim, calonMahasiswa);

		Integer count = ((Number) session.createCriteria(Mahasiswa.class)
				.add(Restrictions.eq("nim", nim)).setProjection(Projections.count("nim")).uniqueResult()).intValue();
		org.hibernate.Criteria calonCriteria = session.createCriteria(BiodataCalonMahasiswa.class)
				.add(Restrictions.eq("nim", nim)).setProjection(Projections.count("nim"));
		if (calonMahasiswa.getId() != null) {
			calonCriteria.add(Restrictions.ne("id", calonMahasiswa.getId()));
		}
		Integer countCalon = ((Number) calonCriteria.uniqueResult()).intValue();

		HibernateUtil.closeSession();

		if (!count.equals(0) || !countCalon.equals(0)) {
			jumlahPengecualian.add(nim);
			return generateNim(calonMahasiswa, jumlahPengecualian);
		}

		return nim;
	}

	private String ambilKodeProdi(Jurusan prodi) {
		String kode = prodi == null ? "" : prodi.getKode();
		if (kode != null) {
			kode = kode.trim();
		}
		if (kode == null || kode.length() == 0 || kode.replace("-", "").replace("_", "").trim().length() == 0) {
			kode = prodi == null || prodi.getId() == null ? "" : prodi.getId().toString();
		}
		kode = kode == null ? "" : kode.replaceAll("[^A-Za-z0-9]", "");
		if (kode.length() == 1) {
			kode = "0" + kode;
		}
		if (kode.length() == 0) {
			throw new IllegalArgumentException("Kode prodi untuk generate NIM belum tersedia.");
		}
		return kode;
	}

	private void validasiNim(String nim, BiodataCalonMahasiswa calonMahasiswa) {
		if (nim == null || nim.trim().isEmpty() || nim.indexOf('-') >= 0 || nim.indexOf('_') >= 0) {
			throw new IllegalArgumentException("Format NIM tidak valid untuk "
					+ (calonMahasiswa == null ? "" : calonMahasiswa.getNama()) + ": " + nim);
		}
	}

	private void validasiKomponenNim(String nilai, String label, BiodataCalonMahasiswa calonMahasiswa) {
		if (nilai == null || nilai.trim().isEmpty() || nilai.indexOf('-') >= 0 || nilai.indexOf('_') >= 0) {
			throw new IllegalArgumentException("Komponen " + label + " untuk generate NIM belum valid"
					+ (calonMahasiswa == null ? "" : " pada " + calonMahasiswa.getNama()) + ": " + nilai);
		}
	}

}
