package ais.ui.util;

import org.zkoss.zul.Comboitem;
import ais.database.model.CommonVO;


/**
 * Renderer {@link Comboitem} generik untuk combobox yang daftar isinya berupa objek
 * {@link CommonVO} (pasangan id+nama umum yang dipakai di berbagai lookup sederhana AIS) —
 * label yang ditampilkan diambil dari {@link CommonVO#getName()} dan nilai combobox diisi
 * {@link CommonVO#getId()}.
 */
public class ComboCommonRenderer extends MyComboitemRenderer {

	/**
	 * Mengisi satu {@link Comboitem} dari data {@link CommonVO}: label diisi nama, nilai
	 * comboitem diisi id.
	 *
	 * @param arg0 comboitem yang akan diisi
	 * @param arg1 data baris, harus berupa {@link CommonVO}
	 * @throws Exception diteruskan bila terjadi kegagalan (mis. {@code ClassCastException}
	 *                    tersembunyi di balik cast implisit)
	 */
	@Override
	public void render(Comboitem arg0, Object arg1) throws Exception {

		CommonVO dept = (CommonVO) arg1;
		arg0.setLabel(dept.getName());
		arg0.setValue(dept.getId());

	}

}
