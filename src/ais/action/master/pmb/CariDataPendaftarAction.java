package ais.action.master.pmb;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

public class CariDataPendaftarAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1155733365712985677L;

	private Textbox noRegistrasi;
	private Textbox noUjian;
	private Textbox nama;
	// private MyWindow window;
	private MyGrid grid;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		Common.initLaguage();
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<BiodataCalonMahasiswa> biodataCalonMahasiswa = ConstantValues
				.simpleList(session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
						.setMaxResults(Common.MAX_RESULT)
						.add(Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.ANYWHERE))
						.add(Restrictions.ilike("noUjian", noUjian.getValue().trim(), MatchMode.ANYWHERE))
						.setMaxResults(Common.MAX_RESULT), BiodataCalonMahasiswa.class);
		ListModel strset = new SimpleListModel(biodataCalonMahasiswa);
		grid.setRowRenderer(new CalonRenderer());
		grid.setModelCheckMobile(strset, true);

	}

	class CalonRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) arg1;

			new Label(biodataCalonMahasiswa.getNoRegistrasi() + "").setParent(arg0);
			new Label(biodataCalonMahasiswa.getNama().toUpperCase() + "").setParent(arg0);
			new Label(biodataCalonMahasiswa.getNoUjian() == null ? "" : biodataCalonMahasiswa.getNoUjian() + "")
					.setParent(arg0);
			new Label(
					biodataCalonMahasiswa.getAsalSma() == null ? "" : biodataCalonMahasiswa.getAsalSma().toUpperCase())
							.setParent(arg0);

		}

	}

	public void onReset() {
		noRegistrasi.setValue("");

	}

}
