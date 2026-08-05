package ais.action.master.employ;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import ais.action.report.format1.employ.LaporanDaftarPegawai;
import ais.common.Common;

public class LihatDaftarPegawaiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2199792139180848345L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		// Tema silvertail (zkplus.theme, ZK5-only) dihapus: tema kini diatur css_utama.css
		LaporanDaftarPegawai laporan = new LaporanDaftarPegawai();
		if (laporan != null) { laporan.setHeight("100%"); }
		if (laporan != null) { laporan.setWidth("100%"); }
		if (laporan != null) { laporan.setParent(this.page.getFirstRoot()); }
	}

}
