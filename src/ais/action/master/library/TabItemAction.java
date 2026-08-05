package ais.action.master.library;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import ais.common.Common;
import ais.common.CommonPrivilages;

public class TabItemAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7023140518217601998L;

//	protected Tabs tabs;
//	protected Tabpanels tabpanels;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		// Session session = HibernateUtil.currentSession();
		// List<Perpustakaan> perpustakaans =
		// session.createCriteria(Perpustakaan.class).addOrder(Order.asc("nama"))
		// .list();
		// for (final Perpustakaan perpustakaan : perpustakaans) {
		// MyTabConfig tab = new MyTabConfig("Di \"" + perpustakaan.getNama() +
		// "\"");
		// tabs.appendChild(tab);
		//
		// final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		// tabpanels.appendChild(tabpanel);
		// tab.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// if (tabpanel.getChildren().isEmpty()) {
		// Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		// tabpanel.appendChild(borderlayout);
		//
		// Center center = new Center();
		// center.setParent(borderlayout);
		// ais.ui.util.ZkCompat.setFlex(center, true);
		//
		// MyIframe include = new MyIframe(
		// "/pages/master/library/monitor_stok_item.zul?perpustakaan=" +
		// perpustakaan.getId());
		// include.setHeight("1000px");
		// include.setWidth("100%");
		// center.appendChild(include);
		// }
		// }
		// });
		// }
	}
}
