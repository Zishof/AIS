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
 * Algoritma penomoran NIM khas STKIP Muhammadiyah (STKIPM). Format: {@code "0142" + namaJenjang +
 * kodeProdi + potonganTahun + 3 digit urutan} — prefix institusi {@code "0142"} tetap, diikuti
 * <b>nama</b> jenjang (bukan kode) dan kode program studi. Catatan: variabel bernama
 * {@code digitPertama}/{@code digitKedua} pada implementasi sengaja tertukar posisi terhadap
 * urutan pada NIM akhir (lihat susunan {@code nim = "0142"+digitPertama+digitKedua+...}) — nama
 * variabel yang membingungkan ini dipertahankan apa adanya sesuai kode asli.
 */
public class StkipmNimGenerator implements NimGenerator {

	/** Seperti {@link #generateNim(BiodataCalonMahasiswa, List)} tanpa daftar NIM yang harus dihindari. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM baru untuk {@code calonMahasiswa} sesuai format khas STKIPM (lihat javadoc
	 * kelas), menghindari nomor pada {@code jumlahPengecualian}. Rekursif pada bentrokan nomor.
	 *
	 * @param calonMahasiswa      calon mahasiswa yang akan diberi NIM; harus memiliki program studi
	 *                            lulus agar NIM dapat dihitung (selain itu mengembalikan {@code "-"})
	 * @param jumlahPengecualian  daftar nomor yang harus dihindari, dimutasi langsung saat bentrokan
	 * @return NIM unik berprefix {@code "0142"} sesuai format STKIPM, atau {@code "-"} bila program studi lulus belum diisi
	 */
	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitKedua = calonMahasiswa.getProdiLulus().getKode();

			String digitPertama = calonMahasiswa.getProdiLulus().getJenjang().getNama();

			String digitKetiga = tahun.toString().substring(1);

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();

			jumlah += jumlahPengecualian.size();
			String digitKeempat = "000000000000" + (jumlah + 1);
			digitKeempat = digitKeempat.substring(digitKeempat.length() - 3);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode prodi) = " + digitKedua);
			System.out.println("digit ketiga (kosong) = " + digitKetiga);
			System.out.println("digit keempat (urutan) = " + digitKeempat);

			nim = "0142" + digitPertama + digitKedua + digitKetiga + digitKeempat;

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
