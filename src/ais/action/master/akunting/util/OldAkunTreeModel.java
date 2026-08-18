package ais.action.master.akunting.util;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;

public class OldAkunTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Integer debetCredit;

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public OldAkunTreeModel(Integer debetCredit) {
		super(new Akun("-1"));
		this.debetCredit = debetCredit;
	}

	public OldAkunTreeModel() {
		super(new Akun("-1"));
		this.debetCredit = null;
	}

	@SuppressWarnings("unchecked")
	public List<Akun> getChildren(Akun parentAkun) {
		String parentKode = parentAkun.getKode().toLowerCase().trim();
		Session session = HibernateUtil.currentSession();
		List<Akun> akuns = session
				.createCriteria(Akun.class)
				.add(debetCredit == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("debetCredit", debetCredit))
				.add(Restrictions.sqlRestriction("trim(kode) != '" + parentKode
						+ "' and ((trim(kode) ilike '" + parentKode
						+ "%' and char_length(trim(kode)) = "
						+ (parentKode.length() + 2) + ") or ('-1' = '"
						+ parentKode + "' and char_length(trim(kode)) = 2))"))
				.addOrder(Order.asc("kode")).list();
		return akuns;
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		Akun parentAkun = (Akun) parent;

		List<Akun> akuns = getChildren(parentAkun);

		Akun akun = null;

		if (akuns.size() < index) {
			akun = null;
		} else {
			akun = akuns.get(index);
		}

		return akun;
	}

	public int getChildCount(Object parent) {
		Akun parentAkun = (Akun) parent;
		String parentKode = parentAkun.getKode().toLowerCase().trim();

		Session session = HibernateUtil.currentSession();

		Integer count = ((Number) session
				.createCriteria(Akun.class)
				.add(debetCredit == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("debetCredit", debetCredit))
				.add(Restrictions.sqlRestriction("trim(kode) != '" + parentKode
						+ "' and ((trim(kode) ilike '" + parentKode
						+ "%' and char_length(trim(kode)) = "
						+ (parentKode.length() + 2) + ") or ('-1' = '"
						+ parentKode + "' and char_length(trim(kode)) = 2))"))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

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