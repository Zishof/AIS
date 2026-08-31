package ais.ui.util;

/**
 * Kontrak pembangun kriteria pencarian/query untuk layar-layar ZK di AIS. Implementasi
 * (biasanya berupa kelas aksi/helper yang membungkus pencarian Hibernate Criteria) membangun
 * dan mengembalikan objek kriteria siap pakai lewat {@link #initCriteria(boolean)}, dipanggil
 * saat layar melakukan pencarian/refresh data grid. Parameter {@code order} memungkinkan
 * pemanggil meminta agar urutan (order by) disertakan atau tidak — dipakai mis. saat kriteria
 * yang sama juga dipakai untuk menghitung total baris (count) tanpa perlu pengurutan.
 */
public interface DataCriteria {

	/**
	 * Membangun objek kriteria pencarian.
	 *
	 * @param order bila {@code true}, kriteria yang dikembalikan menyertakan pengurutan;
	 *              bila {@code false}, pengurutan dilewati (mis. untuk keperluan hitung total)
	 * @return objek kriteria (biasanya {@code org.hibernate.Criteria}) siap dieksekusi
	 */
	public Object initCriteria(boolean order);

}
