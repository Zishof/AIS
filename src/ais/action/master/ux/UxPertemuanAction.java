package ais.action.master.ux;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import ais.action.master.helper.PertemuanHelper;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyWindow;

public class UxPertemuanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1783885254736882086L;

	private int index = 0;

	private MyWindow window;

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		try {
			index = Integer.parseInt(execution.getParameter("index"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ux/UxPertemuanAction.java:30");
			// TODO: handle exception
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, execution.getParameter("id"));

		PertemuanHelper pertemuanHelper = new PertemuanHelper(tbmuser.getMahasiswa(),
				tbmuser.getBiodataCalonMahasiswa());
		pertemuanHelper.window = window;
		pertemuanHelper.tampilSelesai = false;
		pertemuanHelper.display(pertemuan, new DataLoader() {

			@Override
			public void loadData(Object value) {

			}
		}, index);

	}

}
