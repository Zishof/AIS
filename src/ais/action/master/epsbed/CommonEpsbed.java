package ais.action.master.epsbed;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;

/**
 * Kumpulan utilitas statis bersama untuk paket pelaporan EPSBED (Evaluasi Program Studi Berbasis
 * Evaluasi Diri — pelaporan PDDikti versi lama): format tanggal baku EPSBED (thread-safe via
 * {@link ThreadLocal}), penyusunan kode tahun-semester pelaporan, dan penghitungan jumlah calon
 * mahasiswa per program studi/tahun akademik dalam empat kategori (peminat, lulus seleksi, daftar
 * ulang/registrasi menjadi mahasiswa aktif dengan NIM, dan mengundurkan diri/tidak registrasi ulang)
 * yang menjadi dasar berbagai laporan EPSBED. Memperluas {@link Common} untuk mewarisi utilitas
 * umum aplikasi.
 */
public class CommonEpsbed extends Common {

	/** Format tanggal baku EPSBED ({@code yyyyMMdd}, locale Indonesia), disimpan per-thread agar {@link SimpleDateFormat} yang tidak thread-safe aman dipakai bersama. */
	public static final ThreadLocal<SimpleDateFormat> dateFormatEpsbed = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd", new Locale("in", "ID"));
		}
	};

	/**
	 * Menyusun kode tahun-semester pelaporan EPSBED dari tahun akademik (mis. {@code "2024/2025"})
	 * dan status ganjil/genap: tahun awal digabung dengan {@code "1"} untuk semester ganjil atau
	 * {@code "2"} untuk genap.
	 *
	 * @param tahunAkademik string tahun akademik berformat {@code "tahunAwal/tahunAkhir"}
	 * @param ganjilgenap   nilai semester, dibandingkan terhadap {@link Perkuliahan#GANJIL}
	 * @return kode tahun-semester (mis. {@code "20241"})
	 */
	public static String getTahunSemesterPelaporan(String tahunAkademik, String ganjilgenap) {
		String tahunSemesterPelaporan = "";
		String tahun = tahunAkademik.split("/")[0];
		String semesterPelaporan = ganjilgenap.equals(Perkuliahan.GANJIL) ? "1" : "2";
		tahunSemesterPelaporan = tahun.toString() + semesterPelaporan;
		return tahunSemesterPelaporan;
	}

	/**
	 * Menghitung jumlah calon mahasiswa aktif yang berminat (memilih sebagai program studi pilihan
	 * pertama atau kedua) pada program studi dan tahun akademik tertentu.
	 *
	 * @param jurusan       program studi yang dihitung peminatnya
	 * @param tahunAkademik string tahun akademik, hanya tahun awal yang dipakai untuk filter
	 * @return jumlah calon mahasiswa peminat
	 */
	public static Integer hitungJumlahPeminat(Jurusan jurusan, String tahunAkademik) {
		Integer jumlah = 0;
		String tahun = tahunAkademik.split("/")[0];
		jumlah = ((Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", Integer.parseInt(tahun)))
				.add(Restrictions.or(Restrictions.eq("prodi1", jurusan), Restrictions.eq("prodi2", jurusan)))
				.uniqueResult()).intValue();
		return jumlah;
	}

	/**
	 * Menghitung jumlah calon mahasiswa aktif yang dinyatakan lulus seleksi (memiliki
	 * {@code prodiLulus} sesuai) pada program studi dan tahun akademik tertentu — tidak
	 * mensyaratkan sudah registrasi ulang/memiliki NIM.
	 *
	 * @param jurusan       program studi kelulusan yang dihitung
	 * @param tahunAkademik string tahun akademik, hanya tahun awal yang dipakai untuk filter
	 * @return jumlah calon mahasiswa lulus seleksi
	 */
	public static Integer hitungJumlahLulus(Jurusan jurusan, String tahunAkademik) {
		Integer jumlah = 0;
		String tahun = tahunAkademik.split("/")[0];
		jumlah = ((Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", Integer.parseInt(tahun)))
				.add(Restrictions.eq("prodiLulus", jurusan)).uniqueResult()).intValue();
		return jumlah;
	}

	/**
	 * Menghitung jumlah calon mahasiswa lulus seleksi yang sudah registrasi ulang menjadi mahasiswa
	 * aktif (memiliki NIM terisi) pada program studi dan tahun akademik tertentu.
	 *
	 * @param jurusan       program studi kelulusan yang dihitung
	 * @param tahunAkademik string tahun akademik, hanya tahun awal yang dipakai untuk filter
	 * @return jumlah calon mahasiswa yang registrasi ulang
	 */
	public static Integer hitungJumlahDaftarUlang(Jurusan jurusan, String tahunAkademik) {
		Integer jumlah = 0;
		String tahun = tahunAkademik.split("/")[0];
		jumlah = ((Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", Integer.parseInt(tahun)))
				.add(Restrictions.eq("prodiLulus", jurusan)).add(Restrictions.isNotNull("nim")).uniqueResult())
						.intValue();
		return jumlah;
	}

	/**
	 * Menghitung jumlah calon mahasiswa lulus seleksi yang TIDAK registrasi ulang (mengundurkan diri,
	 * NIM masih kosong) pada program studi dan tahun akademik tertentu — komplemen dari
	 * {@link #hitungJumlahDaftarUlang}.
	 *
	 * @param jurusan       program studi kelulusan yang dihitung
	 * @param tahunAkademik string tahun akademik, hanya tahun awal yang dipakai untuk filter
	 * @return jumlah calon mahasiswa yang mengundurkan diri
	 */
	public static Integer hitungJumlahMundur(Jurusan jurusan, String tahunAkademik) {
		Integer jumlah = 0;
		String tahun = tahunAkademik.split("/")[0];
		jumlah = ((Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", Integer.parseInt(tahun)))
				.add(Restrictions.eq("prodiLulus", jurusan)).add(Restrictions.isNull("nim")).uniqueResult()).intValue();
		return jumlah;
	}

}
