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
 * Pembangkit NIM khusus STAKN Palangkaraya dengan format
 * {@code YY+"02"+KODEPRODI+URUT}, mis. {@code "2602TI0007"} — digit kedua berupa kode tetap
 * {@code "02"} (bukan berasal dari data calon mahasiswa). Bagian {@code URUT} (4 digit)
 * dihitung dari 4 digit terakhir NIM tertinggi ({@code MAX(nim)}) yang sudah terdaftar untuk
 * jurusan (prodi lulus) yang sama, bukan dari jumlah baris seperti pada generator NIM lain.
 * Bila calon mahasiswa belum memiliki prodi lulus, dikembalikan {@code "-"}. Bila nomor hasil
 * bentrok, dibangkitkan ulang secara rekursif dengan nomor tersebut ditambahkan ke daftar
 * pengecualian.
 */
public class StaknPalangkarayaNimGenerator implements NimGenerator {

	/** Membangkitkan NIM baru untuk {@code calonMahasiswa} tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan NIM berformat {@code YY+"02"+KODEPRODI+URUT}, menghindari nomor pada
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

			String maxNim = ((String) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.max("nim"))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult());

			Integer tahun = calonMahasiswa.getTahun();

			String digitKedua = "02";

			String digitPertama = tahun.toString().substring(2);

			String digitKetiga = calonMahasiswa.getProdiLulus().getKode();

			Integer n = Integer.parseInt(maxNim == null ? "0" : maxNim.trim().substring(maxNim.length() - 4));

			n += jumlahPengecualian.size();
			String digitKeempat = "000000000000" + (n + 1);
			digitKeempat = digitKeempat.substring(digitKeempat.length() - 4);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode 02) = " + digitKedua);
			System.out.println("digit ketiga (kode prodi) = " + digitKetiga);
			System.out.println("digit keempat (urutan) = " + digitKeempat);

			nim = digitPertama + digitKedua + digitKetiga + digitKeempat;

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
