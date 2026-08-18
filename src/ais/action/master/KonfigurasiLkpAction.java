package ais.action.master;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.CommonPrivilages;

public class KonfigurasiLkpAction extends KonfigurasiNewAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterComposeOri(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		onTampil();

	}

	public void onTampil() {

		Rows rows = (createSpan("Pengaturan Sasaran Kerja Pegawai"));

		rows.appendChild(createRowNilai("Prosentasi nilai Sasaran Kerja Pegawai pada bidang kuantitas",
				"prosentasi_nilai_skp_kuantitas", "70"));
		rows.appendChild(createRowNilai("Prosentasi nilai Sasaran Kerja Pegawai pada bidang kualitas",
				"prosentasi_nilai_skp_kualitas", "10"));
		rows.appendChild(createRowNilai("Prosentasi nilai Sasaran Kerja Pegawai pada bidang waktu",
				"prosentasi_nilai_skp_waktu", "20"));
	}
}
