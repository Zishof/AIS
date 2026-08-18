package ais.action.master;

import org.zkoss.zk.ui.Component;

public class PerkuliahanJadwalAction extends PerkuliahanAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2589792589519814884L;

	public PerkuliahanJadwalAction() {
		super();
		
		today = true;
	}

	
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp); 
		if (add != null) { add.setVisible(false); }
		if (addJadwalKurikulum != null) { addJadwalKurikulum.setVisible(false); }
		delete = false;
		edit = false;
	}
}
