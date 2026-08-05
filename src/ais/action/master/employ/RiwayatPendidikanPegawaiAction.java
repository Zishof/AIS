package ais.action.master.employ;

import ais.common.Common;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyWindow;

import ais.action.master.employ.helper.RiwayatPendidikanPegawaiHelper;

public class RiwayatPendidikanPegawaiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6541231963020247727L;
	private MyWindow window;

	private RiwayatPendidikanPegawaiHelper riwayatPendidikanPegawaiHelper;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {Common.doCheckSecurity();return super.doBeforeCompose(page, parent, compInfo);}public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		riwayatPendidikanPegawaiHelper = new RiwayatPendidikanPegawaiHelper(
                Common.getCurrentUser().ambilPegawai(), true);
		riwayatPendidikanPegawaiHelper.display().setParent(window);
	}

}
