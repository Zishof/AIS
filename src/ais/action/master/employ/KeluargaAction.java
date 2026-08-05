package ais.action.master.employ;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyWindow;

import ais.action.master.employ.helper.KeluargaPegawaiHelper;
import ais.common.Common;
import ais.database.model.Tbmuser;

public class KeluargaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6541231963020247727L;
	private MyWindow window;

	private KeluargaPegawaiHelper keluargaHelper;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Tbmuser tbmuser = Common.getCurrentUser();
		keluargaHelper = new KeluargaPegawaiHelper(tbmuser == null ? null : tbmuser.ambilPegawai());
		keluargaHelper.display().setParent(window);
	}

}
