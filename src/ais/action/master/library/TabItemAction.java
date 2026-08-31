package ais.action.master.library;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import ais.common.Common;
import ais.common.CommonPrivilages;

/**
 * Controller/action ZK untuk tab item. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code doAfterCompose}(). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
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
