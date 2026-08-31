package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.epsbed.KapasitasMahasiswaBaru;

/**
 * Implementasi {@link NimGenerator} khusus institusi ITKP. Digit pertama adalah "kode angkatan" 3
 * digit yang diresolusi dari {@link KapasitasMahasiswaBaru}: dicari kapasitas mahasiswa baru yang
 * cocok persis (jurusan, tahun akademik, semester); bila tidak ada, dicari kapasitas terbaru untuk
 * jurusan yang sama lalu {@code angkatanKe} diekstrapolasi maju sebesar selisih tahun berjalan
 * terhadap tahun akademik kapasitas tersebut. Semester genap menggeser tahun masuk +1 sebelum
 * dipakai. Untuk jenjang S1 ({@link ConstantValues#s1}), NIM = angkatan(3) + 1 digit akhir tahun +
 * {@code "2"} (penanda S1) + 3 digit urut; untuk jenjang lain, NIM = angkatan(3) + tahun penuh +
 * 3 digit urut. Nomor urut dihitung dari 3 digit akhir NIM tertinggi ({@code MAX(nim)}) pada jurusan
 * yang sama. Keunikan diverifikasi ulang terhadap {@link Mahasiswa} dan tabrakan ditangani rekursif
 * via daftar pengecualian.
 */
public class ItkpNimGenerator implements NimGenerator {

	/** Menghasilkan NIM tanpa daftar pengecualian; mendelegasikan ke varian dengan daftar kosong. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM khusus ITKP (lihat format berbeda untuk S1 vs jenjang lain di javadoc kelas)
	 * untuk calon mahasiswa yang sudah memiliki program studi kelulusan; mengembalikan {@code "-"}
	 * bila belum.
	 *
	 * @param calonMahasiswa      data calon mahasiswa, sumber jenjang, tahun, semester, dan prodi kelulusan
	 * @param jumlahPengecualian  daftar NIM yang harus dihindari, dimodifikasi di tempat saat rekursi
	 * @return NIM yang belum terpakai, atau {@code "-"} bila {@code prodiLulus} belum diisi
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();
			KapasitasMahasiswaBaru kapasitasMahasiswaBaru = (KapasitasMahasiswaBaru) session
					.createCriteria(KapasitasMahasiswaBaru.class)
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus()))
					.add(Restrictions.eq("tahunAkademik", calonMahasiswa.getTahunAkademik()))
					.add(Restrictions.eq("ganjilGenap", calonMahasiswa.getSemesterMulai())).addOrder(Order.desc("id"))
					.setMaxResults(1).uniqueResult();

			String tahunAngkatan = "000";
			if (kapasitasMahasiswaBaru != null) {
				tahunAngkatan = kapasitasMahasiswaBaru == null ? "0000000"
						: ("0000000" + kapasitasMahasiswaBaru.getAngkatanKe());
				tahunAngkatan = tahunAngkatan.substring(tahunAngkatan.length() - 3);
			} else {
				kapasitasMahasiswaBaru = (KapasitasMahasiswaBaru) session.createCriteria(KapasitasMahasiswaBaru.class)
						.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus()))
						.addOrder(Order.desc("tahunAkademik")).setMaxResults(1).uniqueResult();
				if (kapasitasMahasiswaBaru != null) {
					Integer tahun = Integer.parseInt(kapasitasMahasiswaBaru.getTahunAkademik().split("/")[0]);
					Integer currtahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
					int selisih = currtahun - tahun;
					int angkatanKe = kapasitasMahasiswaBaru.getAngkatanKe() + selisih;
					tahunAngkatan = ("0000000" + angkatanKe);
					tahunAngkatan = tahunAngkatan.substring(tahunAngkatan.length() - 3);
				}
			}

			String maxNim = ((String) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.max("nim"))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult());

			Integer tahun = calonMahasiswa.getTahun();
			if (calonMahasiswa.getJenisSemester().equals(Perkuliahan.GENAP)) {
				tahun++;
			}

			if (calonMahasiswa.getJenjang().getId().equals(ConstantValues.s1.getId())) {

				String digitKedua = tahun.toString().substring(1);

				String digitPertama = tahunAngkatan;

				String digitKetiga = "2";

				Integer n = Integer.parseInt(maxNim == null ? "0" : maxNim.trim().substring(maxNim.length() - 3));

				n += jumlahPengecualian.size();
				String digitKeempat = "000000000000" + (n + 1);
				digitKeempat = digitKeempat.substring(digitKeempat.length() - 3);

				System.out.println("jenjang = " + calonMahasiswa.getJenjang().getNama());
				System.out.println("digit pertama (kode angkatan) = " + digitPertama);
				System.out.println("digit kedua (kode tahun masuk) = " + digitKedua);
				System.out.println("digit ketiga (identitas S1) = " + digitKetiga);
				System.out.println("digit keempat (urutan) = " + digitKeempat);

				nim = digitPertama + digitKedua + digitKetiga + digitKeempat;

				Integer count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
						.setProjection(Projections.count("nim")).uniqueResult()).intValue();

				HibernateUtil.closeSessionQuietly(session);

				if (!count.equals(0)) {
					jumlahPengecualian.add(nim);
					return generateNim(calonMahasiswa, jumlahPengecualian);
				}

			} else {

				String digitKedua = tahun.toString();

				String digitPertama = tahunAngkatan;
				Integer n = Integer.parseInt(maxNim == null ? "0" : maxNim.trim().substring(maxNim.length() - 3));

				n += jumlahPengecualian.size();
				String digitKetiga = "000000000000" + (n + 1);
				digitKetiga = digitKetiga.substring(digitKetiga.length() - 3);

				System.out.println("jenjang = " + calonMahasiswa.getJenjang().getNama());
				System.out.println("digit pertama (kode angkatan) = " + digitPertama);
				System.out.println("digit kedua (kode tahun masuk) = " + digitKedua);
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

		}

		return nim;
	}

}
