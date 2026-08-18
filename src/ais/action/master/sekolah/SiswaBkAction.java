package ais.action.master.sekolah;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.common.Common;

public class SiswaBkAction extends SiswaAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2232351752130172775L;

	public SiswaBkAction() {
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

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchguruBk.setDisabled(true);
				searchguruBk.setValue("");
				searchguruBk.setAttribute("guru", null);
				onSearchDefault(arg0);
			}
		});

	}
}
