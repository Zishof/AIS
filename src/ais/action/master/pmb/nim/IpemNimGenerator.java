package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;

/**
 * Pembangkit NIM khusus institusi IPEM dengan format {@code TAHUN+KODEPRODI+URUT}, mis.
 * {@code "2026TI0007"}. Bagian {@code URUT} (4 digit) dihitung dari jumlah mahasiswa aktif yang
 * sudah terdaftar pada tahun angkatan dan jurusan (prodi lulus) yang sama. Bila calon mahasiswa
 * belum memiliki prodi lulus, dikembalikan {@code "-"}. Bila nomor hasil bentrok dengan yang
 * sudah tersimpan, dibangkitkan ulang secara rekursif dengan nomor tersebut ditambahkan ke
 * daftar pengecualian.
 */
public class IpemNimGenerator implements NimGenerator {

	/** Membangkitkan NIM baru untuk {@code calonMahasiswa} tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan NIM berformat {@code TAHUN+KODEPRODI+URUT}, menghindari nomor pada
	 * {@code jumlahPengecualian} maupun yang sudah tersimpan; mengulang secara rekursif bila
	 * terjadi bentrok. Mengembalikan {@code "-"} bila calon mahasiswa belum memiliki prodi lulus.
	 *
	 * @param calonMahasiswa     data calon mahasiswa yang akan diberi NIM
	 * @param jumlahPengecualian daftar NIM yang harus dihindari, diperbarui di tempat saat
	 *                           terjadi bentrok
	 * @return NIM baru yang belum dipakai, atau {@code "-"} bila prodi lulus belum ditentukan
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = tahun.toString();

			String digitKedua = calonMahasiswa.getProdiLulus().getKode();

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();

			jumlah += jumlahPengecualian.size();
			String digitKetiga = "000000000000" + (jumlah + 1);
			digitKetiga = digitKetiga.substring(digitKetiga.length() - 4);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode prodi) = " + digitKedua);
			System.out.println("digit ketiga (urutan) = " + digitKetiga);

			nim = digitPertama + digitKedua + digitKetiga;

			Integer count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
					.setProjection(Projections.count("nim")).uniqueResult()).intValue();

			HibernateUtil.closeSessionQuietly(session);

			if (!count.equals(0)) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

		}

		return nim;
	}

}
