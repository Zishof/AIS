package ais.action.master.sekolah;

import org.zkoss.zk.ui.Component;
import ais.action.master.helper.FilterLanjutHelper;

public class PembayaranCalonSiswaAction extends PembayaranSiswaAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5794731159936759364L;

	public void doAfterCompose(Component comp) throws Exception {
		super.pembayaranCalonSiswa = true;
		super.doAfterCompose(comp);

	        FilterLanjutHelper.setup(comp);
}

}
