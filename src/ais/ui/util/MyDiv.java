package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Div;

public class MyDiv extends Div {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7735707868353728408L;

	public MyDiv() {
		super();
		super.setWidth("100%");
		super.setStyle("min-height: 40px;  overflow: hidden;overflow-y:hidden;");
	}

	public void setStyle(String s) {

	}

	public void setMold(String m) {

	}

	public boolean appendChild(Component child) {
		if (child instanceof Caption) {
			return true;
		} else {
			return super.appendChild(child);
		}
	}

}
