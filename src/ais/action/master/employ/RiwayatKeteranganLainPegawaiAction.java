package ais.action.master.employ;

import ais.common.Common;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyWindow;

import ais.action.master.employ.helper.RiwayatKeteranganLainPegawaiHelper;

public class RiwayatKeteranganLainPegawaiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6541231963020247727L;
	private MyWindow window;

	private RiwayatKeteranganLainPegawaiHelper riwayatKeteranganLainPegawaiHelper;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {Common.doCheckSecurity();return super.doBeforeCompose(page, parent, compInfo);}public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		riwayatKeteranganLainPegawaiHelper = new RiwayatKeteranganLainPegawaiHelper(Common.getCurrentUser().ambilPegawai());
		riwayatKeteranganLainPegawaiHelper.display().setParent(window);
	}

}
