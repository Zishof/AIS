package ais.common;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;

/**
 * Kumpulan method utilitas statis untuk mengambil (atau membuat bila belum ada) baris
 * konfigurasi {@link Konfigurasi} yang mengatur aktif/tidaknya dua fitur akademik terkait
 * penilaian: penilaian itu sendiri (KHS/nilai per mata kuliah) dan persetujuan KRS oleh dosen
 * (wali/pembimbing akademik). Kelas ini mengikuti pola "get-or-create" yang umum dipakai di AIS
 * untuk konfigurasi bertingkat tahun akademik + jenis semester (+ opsional semester pendek):
 * setiap kombinasi tersebut memiliki baris {@link Konfigurasi} tersendiri, dan bila baris untuk
 * kombinasi yang diminta belum pernah dibuat, kelas ini akan membuatnya secara otomatis dengan
 * nilai default {@link Konfigurasi#AKTIF} (menyalakan fitur secara default) sebelum
 * mengembalikannya kepada pemanggil.
 *
 * <p>
 * Kedua method publik pada kelas ini memiliki struktur yang identik dan hanya berbeda pada nama
 * konfigurasi ({@code nama}) yang dicari/dibuat:
 * </p>
 * <ol>
 * <li>Bila {@code jenisSemester} tidak diberikan (null/kosong), method menentukan semester
 * berjalan secara otomatis lewat {@link Common#isNowSemensterGanjil()} — bila saat ini semester
 * ganjil, dipakai {@link Perkuliahan#GANJIL}, selain itu {@link Perkuliahan#GENAP}. Dengan
 * demikian pemanggil yang tidak peduli pada semester spesifik cukup melewatkan {@code null} dan
 * akan otomatis mendapat konfigurasi semester yang sedang berjalan.</li>
 * <li>Baris {@link Konfigurasi} dicari lewat kriteria Hibernate yang menyaring berdasarkan
 * {@code info1} (jenis semester), {@code nama} (nama konfigurasi, dibedakan lagi antara varian
 * semester reguler dan semester pendek berdasarkan parameter {@code semesterPendek}), dan
 * {@code tahunAkademik}, diurutkan menurun berdasarkan {@code id} dan dibatasi satu hasil — pola
 * ini memastikan bila (secara tidak normal) ada lebih dari satu baris yang cocok, baris paling
 * baru (id terbesar) yang dipakai.</li>
 * <li>Bila tidak ditemukan baris yang cocok, sebuah {@link Konfigurasi} baru dibuat dan langsung
 * disimpan ke database (tanpa membuka transaksi eksplisit — mengandalkan sesi Hibernate yang
 * sedang berjalan di pemanggil) dengan nilai default {@link Konfigurasi#AKTIF}, lalu
 * dikembalikan. Ini berarti pemanggilan pertama kali untuk kombinasi tahun akademik+semester
 * tertentu akan otomatis "menyalakan" fitur terkait, bukan meninggalkannya dalam keadaan tidak
 * terdefinisi.</li>
 * </ol>
 *
 * <p>
 * Parameter {@code semesterPendek} pada kedua method berfungsi sebagai penentu varian nama
 * konfigurasi: bila {@code null}, dipakai nama konfigurasi reguler ({@link Konfigurasi#PENILAIAN}
 * atau {@code "aktivasi_persetujuan_KRS_oleh_dosen"}); bila diisi (menandakan konteks semester
 * pendek/SP), dipakai nama varian semester pendek ({@link Konfigurasi#PENILAIAN_SP} atau
 * {@code "aktivasi_persetujuan_KRS_sp_oleh_dosen"}). Nilai integer {@code semesterPendek} itu
 * sendiri tidak dipakai lebih lanjut selain sebagai penanda null/tidak-null pada implementasi
 * saat ini.
 * </p>
 *
 * <p>
 * Kelas ini murni statis (tidak ada state instance) dan bergantung pada sesi Hibernate yang
 * sedang aktif melalui {@link HibernateUtil#currentSession()} — pemanggil bertanggung jawab
 * memastikan sesi/transaksi Hibernate sudah tersedia sebelum memanggil method di kelas ini.
 * </p>
 */
public class CommonPenilaian {

	/**
	 * Mengambil baris konfigurasi aktif/tidak-aktifnya fitur <b>penilaian</b> (KHS/nilai) untuk
	 * kombinasi tahun akademik dan jenis semester tertentu, membuatnya secara otomatis dengan
	 * status {@link Konfigurasi#AKTIF} bila belum ada.
	 *
	 * @param tahunAkademik   tahun akademik yang dicari/dibuatkan konfigurasinya
	 * @param jenisSemester   {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}; bila
	 *                        {@code null}/kosong, ditentukan otomatis dari semester berjalan lewat
	 *                        {@link Common#isNowSemensterGanjil()}
	 * @param semesterPendek  bila {@code null}, dipakai nama konfigurasi penilaian reguler
	 *                        ({@link Konfigurasi#PENILAIAN}); bila diisi, dipakai nama konfigurasi
	 *                        varian semester pendek ({@link Konfigurasi#PENILAIAN_SP})
	 * @return baris {@link Konfigurasi} yang ditemukan atau baru dibuat (tidak pernah {@code null})
	 */
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

	/**
	 * Mengambil baris konfigurasi aktif/tidak-aktifnya fitur <b>persetujuan KRS oleh dosen</b>
	 * (wali/pembimbing akademik) untuk kombinasi tahun akademik dan jenis semester tertentu,
	 * membuatnya secara otomatis dengan status {@link Konfigurasi#AKTIF} bila belum ada. Struktur
	 * dan perilaku method ini identik dengan {@link #getKonfigurasi(String, String, Integer)},
	 * hanya berbeda pada nama konfigurasi yang dicari/dibuat.
	 *
	 * @param tahunAkademik   tahun akademik yang dicari/dibuatkan konfigurasinya
	 * @param jenisSemester   {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}; bila
	 *                        {@code null}/kosong, ditentukan otomatis dari semester berjalan lewat
	 *                        {@link Common#isNowSemensterGanjil()}
	 * @param semesterPendek  bila {@code null}, dipakai nama konfigurasi
	 *                        {@code "aktivasi_persetujuan_KRS_oleh_dosen"}; bila diisi, dipakai
	 *                        nama varian semester pendek
	 *                        {@code "aktivasi_persetujuan_KRS_sp_oleh_dosen"}
	 * @return baris {@link Konfigurasi} yang ditemukan atau baru dibuat (tidak pernah {@code null})
	 */
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
