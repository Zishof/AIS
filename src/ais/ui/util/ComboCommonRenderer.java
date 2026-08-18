package ais.ui.util;

import org.zkoss.zul.Comboitem;
import ais.database.model.CommonVO;


public class ComboCommonRenderer extends MyComboitemRenderer {

	@Override
	public void render(Comboitem arg0, Object arg1) throws Exception {

		CommonVO dept = (CommonVO) arg1;
		arg0.setLabel(dept.getName());
		arg0.setValue(dept.getId());
		
	}

}
