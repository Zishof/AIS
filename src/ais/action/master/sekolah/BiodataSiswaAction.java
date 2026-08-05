package ais.action.master.sekolah;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyWindow;

public class BiodataSiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6197765299898455855L;

	private MyWindow window;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Tbmuser tbmuser = Common.getCurrentUser();
		Siswa siswa = tbmuser.getSiswa();
		HibernateUtil.currentSession().refresh(siswa);

		SiswaAction.onAddExternal(window, null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

			}
		}, siswa);
	}

}
