package ais.database.model;

import java.io.Serializable;

/**
 * Model data untuk common sorter. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini mendeklarasikan kontrak {@link Serializable}, {@link Comparable}.
 * Implementasi konkret bertanggung jawab atas transaksi, resource, error handling, dan efek samping; pemanggil
 * sebaiknya bergantung pada kontrak ini agar tidak menggandakan integrasi.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code serializable}, {@code serializable1}, {@code
 * value}; operasi lokal: {@code compareTo()}, {@code getValue()}, {@code setValue()}, {@code getSerializable()},
 * {@code setSerializable()}, {@code getSerializable1()}, {@code setSerializable1}(). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 */
public class CommonSorter implements Serializable, Comparable<CommonSorter> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5763578573109731308L;

	private Serializable serializable;
	private Serializable serializable1;
	private Double value = 0.0;

	@Override
	public int compareTo(CommonSorter arg0) {
		return arg0.value.compareTo(value);
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	public Serializable getSerializable() {
		return serializable;
	}

	public void setSerializable(Serializable serializable) {
		this.serializable = serializable;
	}

	public Serializable getSerializable1() {
		return serializable1;
	}

	public void setSerializable1(Serializable serializable1) {
		this.serializable1 = serializable1;
	}

}
