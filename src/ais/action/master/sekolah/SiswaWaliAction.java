package ais.action.master.sekolah;

import org.zkoss.zk.ui.Component;

import ais.common.Common;
import ais.action.master.helper.FilterLanjutHelper;

public class SiswaWaliAction extends SiswaAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2232351752130172775L;

	public SiswaWaliAction() {
		super();

	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		

	        FilterLanjutHelper.setup(comp);
}
}
