package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Combobox;


public class MyCombobox extends Combobox {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7795947945271873137L;

	public MyCombobox() {
		super();
		initDefaultSearchable();
		// TODO Auto-generated constructor stub
	}

	public MyCombobox(String value) throws WrongValueException {
		super(value);
		initDefaultSearchable();
		// TODO Auto-generated constructor stub
	}
	private void initDefaultSearchable() {
		setWidth("90%");
		setReadonly(false);
		setAutodrop(true);
		String sclass = getSclass();
		if (sclass == null || sclass.indexOf("ecampus-combobox-searchable") < 0) {
			setSclass((sclass == null || sclass.trim().length() == 0) ? "ecampus-combobox-searchable" : (sclass + " ecampus-combobox-searchable"));
		}
	}

	/**
	 * Guard "Out of bound: N while size=M": ZUL/model kadang menyetel selectedIndex sebelum
	 * comboitem tersedia (mis. selectedIndex="0" saat data masih kosong / Include.afterCompose).
	 * Abaikan bila indeks di luar batas atas; ZK akan menyetel ulang saat item sudah ada.
	 * selectedIndex = -1 (deselect) dan indeks valid tetap berperilaku normal.
	 */
	@Override
	public void setSelectedIndex(int jsel) {
		if (jsel >= getItemCount()) {
			return;
		}
		super.setSelectedIndex(jsel);
	}


}
