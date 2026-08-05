package ais.action.maintenance;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.South;

import ais.action.master.recruitment.CalonPegawaiAction;
import ais.common.Common;
import ais.database.model.Tbmuser;

public class CalonPegAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2446397351568124278L;
	private Tbmuser tbmuser;

	private South mysouth;
	private Center centerUtama;

	@SuppressWarnings({})
	public void doAfterCompose(Component comp) throws Exception {
		tbmuser = Common.getCurrentFromSpringUser();
		System.out.println("tbmuser => " + tbmuser);
		if (tbmuser == null && tbmuser.getPenyediaAsset() == null) {
			Common.goLogoff();
			return;
		}
		super.doAfterCompose(comp);
		CalonPegawaiAction.onAddExternal(centerUtama, mysouth, tbmuser.getCalonPegawai());

	}

}
