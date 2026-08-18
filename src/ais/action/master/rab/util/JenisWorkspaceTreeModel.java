package ais.action.master.rab.util;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.JenisWorkspace;

public class JenisWorkspaceTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public JenisWorkspaceTreeModel(Boolean tampilkanSemua) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public JenisWorkspaceTreeModel(JenisWorkspace parentJenisWorkspace,
			Boolean tampilkanSemua) {
		super(parentJenisWorkspace);
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<JenisWorkspace> getChildren(JenisWorkspace parentJenisWorkspace) {
		return getChildren(parentJenisWorkspace, null);
	}

	@SuppressWarnings("unchecked")
	public List<JenisWorkspace> getChildren(
			JenisWorkspace parentJenisWorkspace, Integer index) {
		Session session = HibernateUtil.currentSession();
		List<JenisWorkspace> jenisWorkspaces = session
				.createCriteria(JenisWorkspace.class)
				.setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))
				.add(parentJenisWorkspace == null ? Restrictions
						.isNull("parent") : Restrictions.eq("parent",
						parentJenisWorkspace)).addOrder(Order.asc("kode"))
				.addOrder(Order.asc("nama")).addOrder(Order.desc("id")).list();
		return jenisWorkspaces;
	}

	public void generateAllChildren(JenisWorkspace parentJenisWorkspace,
			Set<JenisWorkspace> jenisWorkspaces) {
		if (!isLeaf(parentJenisWorkspace)) {
			List<JenisWorkspace> kerjas = getChildren(parentJenisWorkspace);
			for (JenisWorkspace jenisWorkspace : kerjas) {
				jenisWorkspaces.add(jenisWorkspace);
				generateAllChildren(jenisWorkspace, jenisWorkspaces);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		JenisWorkspace parentJenisWorkspace = (JenisWorkspace) parent;
		List<JenisWorkspace> jenisWorkspaces = getChildren(
				parentJenisWorkspace, index);
		JenisWorkspace jenisWorkspace = jenisWorkspaces.size() > 0 ? jenisWorkspaces
				.get(0) : null;
		// System.out.println("index = " + index + ", jenisWorkspace = "
		// + jenisWorkspace);
		return jenisWorkspace;
	}

	public int getChildCount(Object parent) {
		JenisWorkspace parentJenisWorkspace = (JenisWorkspace) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session
				.createCriteria(JenisWorkspace.class)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("defaultItem", true))

				.add(parentJenisWorkspace == null ? Restrictions
						.isNull("parent") : Restrictions.eq("parent",
						parentJenisWorkspace))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		JenisWorkspace parentJenisWorkspace = (JenisWorkspace) parent;
		Session session = HibernateUtil.currentSession();
		List<JenisWorkspace> jenisWorkspaces = getChildren(parentJenisWorkspace);
		for (JenisWorkspace jenisWorkspace : jenisWorkspaces) {
			if (getChildCount(jenisWorkspace) == 0) {
				session.delete(jenisWorkspace);
			} else {
				deleteChilds(jenisWorkspace);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(JenisWorkspace jenisWorkspace,
			JenisWorkspace obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (jenisWorkspace.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session,(obj));
		} else {
			JenisWorkspace parentJenisWorkspace = (JenisWorkspace) session
					.createCriteria(JenisWorkspace.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(jenisWorkspace.getParent().getId()))
					.uniqueResult();
			longs.add(parentJenisWorkspace.getId());
			getParentCount(parentJenisWorkspace, obj, longs);
		}

	}

	public void getParentSet(JenisWorkspace jenisWorkspace,
			List<JenisWorkspace> jenisWorkspaces) {
		Session session = HibernateUtil.currentSession();
		if (jenisWorkspace.getParent() != null) {
			JenisWorkspace parentJenisWorkspace = (JenisWorkspace) session
					.createCriteria(JenisWorkspace.class)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("defaultItem", true))
					.add(Restrictions.idEq(jenisWorkspace.getParent().getId()))
					.uniqueResult();
			if (parentJenisWorkspace != null) {
				jenisWorkspaces.add(parentJenisWorkspace);
				getParentSet(parentJenisWorkspace, jenisWorkspaces);
			}
		}

	}

	public void getChildsSet(JenisWorkspace jenisWorkspace,
			Set<JenisWorkspace> jenisWorkspaces) {
		List<JenisWorkspace> childs = getChildren(jenisWorkspace);
		for (JenisWorkspace myJenisWorkspace : childs) {
			jenisWorkspaces.add(myJenisWorkspace);
			if (!isLeaf(myJenisWorkspace)) {
				getChildsSet(myJenisWorkspace, jenisWorkspaces);
			}
		}
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