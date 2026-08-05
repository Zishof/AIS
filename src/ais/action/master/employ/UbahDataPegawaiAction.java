package ais.action.master.employ;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyMessageboxConfig;

import ais.action.master.BiodataPegawaiAction;
import ais.action.master.employ.util.EmployUtil;
import ais.common.Common;
import ais.common.CommonOnSearchdefault;
import ais.database.model.Pegawai;

public class UbahDataPegawaiAction extends GenericAutowireComposer implements
		CommonOnSearchdefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2849401207117097393L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		// Tema silvertail (zkplus.theme, ZK5-only) dihapus: tema kini diatur css_utama.css
		EmployUtil employUtil = new EmployUtil();
		Pegawai pegawai = employUtil.initLoginFromOutApp(execution, page,
				"daftar riwayat hidup pegawai");

		if (pegawai == null) {
			return;
		}

		BiodataPegawaiAction biodataPegawaiAction = new BiodataPegawaiAction(
				pegawai, pegawai.getSatuanKerja(), false, false);
		biodataPegawaiAction
				.setCommonOnSearchdefault(UbahDataPegawaiAction.this);
		if (biodataPegawaiAction != null) { biodataPegawaiAction.setHeight("100%"); }
		if (biodataPegawaiAction != null) { biodataPegawaiAction.setWidth("100%"); }

		page.getFirstRoot().appendChild(biodataPegawaiAction);
	}

	@Override
	public void onSearchDefault(Event event) {
		try {
			MyMessageboxConfig.show("Data pegawai berhasil disimpan.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

}
