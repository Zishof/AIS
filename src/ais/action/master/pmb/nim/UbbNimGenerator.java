package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;

/**
 * Algoritma penomoran NIM khas Universitas Bangka Belitung (UBB). Format: {@code <kodeProdi><2
 * digit tahun><kode program><"1" baru atau "2" pindahan><3 digit urutan>}. Kode program diambil
 * dari peta {@code Common.programs} berdasarkan program studi calon mahasiswa (S1/D3/dsb, default
 * {@code "0"} bila pemetaan gagal); digit status mahasiswa membedakan pendaftar baru dari
 * mahasiswa pindahan ({@link BiodataCalonMahasiswa#getMerupakanPindahan()}).
 */
public class UbbNimGenerator implements NimGenerator {

	/** Seperti {@link #generateNim(BiodataCalonMahasiswa, List)} tanpa daftar NIM yang harus dihindari. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM baru untuk {@code calonMahasiswa} sesuai format khas UBB (lihat javadoc
	 * kelas), menghindari nomor pada {@code jumlahPengecualian}. Rekursif pada bentrokan nomor.
	 *
	 * @param calonMahasiswa      calon mahasiswa yang akan diberi NIM; harus memiliki program studi
	 *                            lulus agar NIM dapat dihitung (selain itu mengembalikan {@code "-"})
	 * @param jumlahPengecualian  daftar nomor yang harus dihindari, dimutasi langsung saat bentrokan
	 * @return NIM unik sesuai format UBB, atau {@code "-"} bila program studi lulus belum diisi
	 */
	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = calonMahasiswa.getProdiLulus().getKode();

			String digitKedua = tahun.toString().substring(2);

			String digitKetiga = "0";

			try {
				digitKetiga = Common.programs.get(calonMahasiswa.getProgram()).getNum() + "";
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			String digitKetigaLagi = calonMahasiswa.getMerupakanPindahan() ? "2" : "1";

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();

			jumlah += jumlahPengecualian.size();
			String digitKeempat = "000000000000" + (jumlah + 1);
			digitKeempat = digitKeempat.substring(digitKeempat.length() - 3);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode prodi) = " + digitKedua);
			System.out.println("digit ketiga (program) = " + digitKetiga);
			System.out.println("digit ketiga lagi (pindahan/baru) = " + digitKetigaLagi);
			System.out.println("digit keempat (urutan) = " + digitKeempat);

			nim = digitPertama + digitKedua + digitKetiga + digitKetigaLagi + digitKeempat;

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
