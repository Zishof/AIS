package ais.action.master.resources.helper;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.model.library.Item;

public class PerpustakaanResourcesHelper {

	@SuppressWarnings("unchecked")
	public List<Long> getChildrenByIds(Session session, Long parentItem,
			Integer index) {
		List<Long> parents = session
				.createCriteria(Item.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.property("id"))
				.setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(parentItem == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent.id", parentItem))
				.addOrder(Order.desc("id")).list();
		return parents;
	}

	public List<Long> getChildrenByIds(Session session, Long parentItem) {
		return getChildrenByIds(session, parentItem, null);
	}

	public int getChildCountByIds(Session session, Long parent) {
		Integer count = ((Number) session
				.createCriteria(Item.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(parent == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent.id", parent))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		return count;
	}

	public void generateChildsByIds(Session session, Long parent,
			Set<Long> childs) {
		List<Long> mychilds = getChildrenByIds(session, parent);
		System.out.println("mychilds = " + mychilds);
		if (mychilds.size() > 0) {
			for (Long myItem : mychilds) {
				int count = getChildCountByIds(session, myItem);
				childs.add(myItem);
				if (count != 0) {
					generateChildsByIds(session, myItem, childs);
				}
			}
		} else {
			childs.add(parent);
		}
	}

}
