package ais.ui.util;

import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;

/**
 * Basis renderer comboitem dual-kompatibel ZK 5 dan ZK CE 9/10 (pola sama
 * dengan MyRowRenderer): ZK 5 memanggil render 2 argumen, ZK 7+ memanggil
 * render 3 argumen yang didelegasikan ke versi 2 argumen.
 */
@SuppressWarnings("rawtypes")
public abstract class MyComboitemRenderer implements ComboitemRenderer {

	public void render(Comboitem item, Object data, int index) throws Exception {
		render(item, data);
	}

	public abstract void render(Comboitem item, Object data) throws Exception;
}
