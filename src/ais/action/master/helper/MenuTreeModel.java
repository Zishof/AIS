package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;

/**
 * Tipe khusus untuk menu tree model. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractTreeModel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code getChildren()}, {@code getChild()}, {@code
 * getChildCount()}, {@code isLeaf()}, {@code getIndexOfChild}(). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 *
 * @see AbstractTreeModel
 */
public class MenuTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public MenuTreeModel() {
		super(new Menu(-1L, 0L));
	}

	@SuppressWarnings("unchecked")
	public List<Menu> getChildren(Menu parentMenu) {
		Session session = HibernateUtil.currentSession();
		List<Menu> menus = session.createCriteria(Menu.class).addOrder(Order.asc("nomorUrut"))
				.add(Restrictions.eq("root", parentMenu.getChild())).addOrder(Order.asc("root"))
				.addOrder(Order.asc("child")).addOrder(Order.asc("label")).list();
		return menus;
	}

	public Object getChild(Object parent, int index) {
		Menu parentMenu = (Menu) parent;

		List<Menu> menus = getChildren(parentMenu);

		Menu menu = null;

		if (menus.size() < index) {
			menu = null;
		} else {
			menu = menus.get(index);
		}

		return menu;
	}

	public int getChildCount(Object parent) {
		Menu parentMenu = (Menu) parent;

		Session session = HibernateUtil.currentSession();

		Integer count = ((Number) session.createCriteria(Menu.class).add(Restrictions.eq("root", parentMenu.getChild()))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return count;
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	/**
	 * @since 5.0.6
	 * @see org.zkoss.zul.TreeModel#getIndexOfChild(java.lang.Object,
	 *      java.lang.Object)
	 */
	@Override
	public int getIndexOfChild(Object arg0, Object arg1) {
		return 0;
	}

}