package ais.action.master.helper.util;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Berkas;

public class BerkasTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;

	public BerkasTreeModel() {
		super(null);

	}

	public BerkasTreeModel(Berkas parentBerkas) {
		super(parentBerkas);

	}

	@SuppressWarnings("unchecked")
	public void getChildDeepSet(Long parent, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		List<Long> values = session.createCriteria(Berkas.class).add(Restrictions.eq("parent.id", parent))
				.setProjection(Projections.property("id")).list();
		if (values.size() > 0) {
			longs.add(values.get(0));
			getChildDeepSet(values.get(0), longs);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Berkas> getChildren(Berkas parentBerkas) {
		Session session = HibernateUtil.currentSession();
		List<Berkas> berkass = session.createCriteria(Berkas.class)
				.add(parentBerkas == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parentBerkas))
				.addOrder(Order.asc("nama")).list();
		return berkass;
	}

	public void generateAllChildren(Berkas parentBerkas, Set<Berkas> berkass) {
		if (!isLeaf(parentBerkas)) {
			List<Berkas> kerjas = getChildren(parentBerkas);
			for (Berkas berkas : kerjas) {
				berkass.add(berkas);
				generateAllChildren(berkas, berkass);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		Berkas parentBerkas = (Berkas) parent;

		List<Berkas> berkass = getChildren(parentBerkas);

		Berkas berkas = null;

		if (berkass.size() < index) {
			berkas = null;
		} else {
			berkas = berkass.get(index);
		}

		return berkas;
	}

	public int getChildCount(Object parent) {
		Berkas parentBerkas = (Berkas) parent;
		Session session = HibernateUtil.currentSession();

		Integer count = ((Number) session.createCriteria(Berkas.class)
				.add(parentBerkas == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parentBerkas))
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