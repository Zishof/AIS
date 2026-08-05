package ais.ui.util;

import org.zkoss.zul.Comboitem;
public class ComboIntegerRenderer extends MyComboitemRenderer {

	@Override
	public void render(Comboitem arg0, Object arg1) throws Exception {

		Integer dept = (Integer) arg1;
		arg0.setLabel(dept+"");
		arg0.setValue(dept);
		
	}

}
