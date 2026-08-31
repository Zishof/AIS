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
 * Algoritma NIM khusus Politeknik Kesehatan (Poltekkes) Palu. Format NIM:
 * {@code "PO" + [kode program studi kelulusan] + [2 digit terakhir tahun angkatan] + [3 digit
 * nomor urut]}, mis. {@code PODIII26007}. Nomor urut dihitung dari jumlah {@link Mahasiswa} aktif
 * pada tahun angkatan dan jurusan (prodi kelulusan) yang sama, ditambah jumlah kandidat yang sudah
 * dicoba tapi bentrok pada pemanggilan rekursif, lalu ditambah 1 dan dipad nol ke kiri menjadi 3
 * digit. Bila {@code calonMahasiswa.getProdiLulus()} belum diisi, NIM dikembalikan sebagai
 * {@code "-"} (belum dapat dibangkitkan). Bila hasil gabungan ternyata sudah dipakai mahasiswa
 * lain, nomor tersebut dicatat sebagai pengecualian dan method memanggil dirinya sendiri untuk
 * mencoba nomor berikutnya.
 */
public class PoltekkesPaluNimGenerator implements NimGenerator {

	/** Menghasilkan NIM baru tanpa daftar pengecualian awal — lihat {@link #generateNim(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM berformat {@code "PO"+kode prodi lulus+2 digit tahun+3 digit urut},
	 * menghindari nilai yang ada di {@code jumlahPengecualian} maupun yang sudah dipakai mahasiswa
	 * lain di database; mencoba ulang secara rekursif bila terjadi bentrok.
	 *
	 * @param calonMahasiswa     calon mahasiswa target; {@code prodiLulus} dan {@code tahun}-nya
	 *                           menentukan bagian awal NIM
	 * @param jumlahPengecualian NIM-NIM yang sudah dicoba dan diketahui bentrok, dihindari pada
	 *                           percobaan berikutnya (diperbarui di tempat)
	 * @return NIM baru yang belum pernah dipakai, atau {@code "-"} bila {@code prodiLulus} belum diisi
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitKedua = tahun.toString().substring(2);

			String pertamaBanget = "PO";
			String digitPertama = calonMahasiswa.getProdiLulus().getKode();

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();

			jumlah += jumlahPengecualian.size();
			String digitKetiga = "000000000000" + (jumlah + 1);
			digitKetiga = digitKetiga.substring(digitKetiga.length() - 3);

			System.out.println("digit pertama (kode prodi) = " + digitPertama);
			System.out.println("digit kedua (kode tahun) = " + digitKedua);
			System.out.println("digit ketiga (urutan) = " + digitKetiga);

			nim = pertamaBanget + digitPertama + digitKedua + digitKetiga;

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
