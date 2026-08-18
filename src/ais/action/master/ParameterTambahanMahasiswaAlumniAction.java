package ais.action.master;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.common.Common;

public class ParameterTambahanMahasiswaAlumniAction extends ParameterTambahanMahasiswaAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7190378986913670113L;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onAlumni(arg0);
			}
		});

	}

}
