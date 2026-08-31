package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Perkuliahan;

/**
 * Pembangkit NIM khusus institusi ISM dengan format 7-segmen ditambah nomor urut:
 * {@code "4"+YY+PINDAHAN+JENJANG+KODEPRODI+PROGRAM+GANJILGENAP+URUT}. Setiap digit dikodekan
 * dari kombinasi atribut calon mahasiswa: digit pertama selalu {@code "4"}; {@code PINDAHAN}
 * bernilai {@code "2"} bila mahasiswa pindahan atau {@code "1"} bila bukan; {@code JENJANG}
 * bernilai {@code "2"} untuk S1, {@code "1"} untuk S2, atau {@code "-"} untuk jenjang lain;
 * {@code PROGRAM} mengkode jenis program (Reguler/Pascasarjana={@code "1"},
 * Ekstensi={@code "2"}, Karyawan={@code "3"}, Internasional={@code "4"}, lainnya={@code "5"});
 * {@code GANJILGENAP} bernilai {@code "1"} untuk semester ganjil ({@link Perkuliahan#GANJIL})
 * atau {@code "2"} untuk genap. Nomor urut (3 digit) dihitung lewat
 * {@link NimGeneratorSupport#nomorUrutBerikutnya} berdasarkan prefiks gabungan seluruh segmen di
 * atas. Bila calon mahasiswa belum memiliki prodi lulus, dikembalikan {@code "-"}; bila nomor
 * hasil bentrok, dibangkitkan ulang secara rekursif.
 */
public class IsmNimGenerator implements NimGenerator {

	/** Membangkitkan NIM baru untuk {@code calonMahasiswa} tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan NIM 7-segmen khas ISM (lihat javadoc kelas untuk arti tiap digit),
	 * menghindari nomor pada {@code jumlahPengecualian} maupun yang sudah tersimpan; mengulang
	 * secara rekursif bila terjadi bentrok. Mengembalikan {@code "-"} bila calon mahasiswa belum
	 * memiliki prodi lulus.
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

			String digitPertama = tahun.toString().substring(2);

			String digitKedua = calonMahasiswa.getMerupakanPindahan() ? "2" : "1";

			String digitKetiga = calonMahasiswa.getProdiLulus().getJenjang().getNama().equalsIgnoreCase("S1") ? "2"
					: calonMahasiswa.getProdiLulus().getJenjang().getNama().equalsIgnoreCase("S2") ? "1" : "-";

			// String digitKetiga =
			// calonMahasiswa.getProdiLulus().getFakultas().getKode();

			String digitKeempat = calonMahasiswa.getProdiLulus().getKode();

			String digitKelima = calonMahasiswa.getProgram().equalsIgnoreCase("Reguler")
					|| calonMahasiswa.getProgram().equalsIgnoreCase("Pascasarjana")
							? "1"
							: calonMahasiswa.getProgram().equalsIgnoreCase("Ekstensi") ? "2"
									: calonMahasiswa.getProgram().equalsIgnoreCase("Karyawan") ? "3"
											: calonMahasiswa.getProgram().equalsIgnoreCase("Internasional") ? "4" : "5";

			String digitKeEnam = calonMahasiswa.getJenisSemester().equals(Perkuliahan.GANJIL) ? "1" : "2";

			String prefix = "4" + digitPertama + digitKedua + digitKetiga + digitKeempat + digitKelima + digitKeEnam;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, 3, calonMahasiswa,
					jumlahPengecualian);
			String digitTujuh = NimGeneratorSupport.leftPadNomor(nomorUrut, 3);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (pindahan atau bukan) = " + digitKedua);
			System.out.println("digit ketiga (kode jenjang) = " + digitKetiga);
			System.out.println("digit keempat (kode prodi) = " + digitKeempat);
			System.out.println("digit kelima (kode program) = " + digitKelima);
			System.out.println("digit keenam (ganjil genap) = " + digitKeEnam);
			System.out.println("digit keenam (nomor urut) = " + digitTujuh);

			nim = "4" + digitPertama + digitKedua + digitKetiga + digitKeempat + digitKelima + digitKeEnam + digitTujuh;

			boolean nimSudahDipakai = NimGeneratorSupport.nimSudahDipakai(session, nim, calonMahasiswa);
			HibernateUtil.closeSessionQuietly(session);

			if (nimSudahDipakai) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

		}

		return nim;
	}

}
