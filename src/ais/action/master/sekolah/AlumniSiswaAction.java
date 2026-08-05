package ais.action.master.sekolah;

import org.zkoss.zk.ui.Component;

import ais.common.Common;
import ais.action.master.helper.FilterLanjutHelper;

public class AlumniSiswaAction extends SiswaAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8793276768316272112L;

	public AlumniSiswaAction() {
		super.alumni = true;
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub

		super.doAfterCompose(comp);
		Common.initLaguage();

	        FilterLanjutHelper.setup(comp);
}

}
