package ais.ui.util;

import org.zkoss.zul.Comboitem;
/**
 * Renderer {@link Comboitem} sederhana untuk combobox yang daftar isinya berupa nilai
 * {@link Integer} polos (mis. daftar tahun, semester, atau angka pilihan lain) — label yang
 * ditampilkan ke pengguna adalah representasi string dari angka itu sendiri, dan nilai
 * ({@code value}) combobox disimpan sebagai objek {@link Integer} yang sama.
 */
public class ComboIntegerRenderer extends MyComboitemRenderer {

	/**
	 * Mengisi satu {@link Comboitem} dari data {@link Integer}: label diisi teks angka
	 * tersebut, dan nilai comboitem diisi objek {@link Integer} itu sendiri.
	 *
	 * @param arg0 comboitem yang akan diisi
	 * @param arg1 data baris, harus berupa {@link Integer}
	 * @throws Exception diteruskan bila terjadi kegagalan (mis. {@code ClassCastException}
	 *                    tersembunyi di balik cast implisit)
	 */
	@Override
	public void render(Comboitem arg0, Object arg1) throws Exception {

		Integer dept = (Integer) arg1;
		arg0.setLabel(dept+"");
		arg0.setValue(dept);

	}

}
