package ais.action.master.resources.helper;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.model.library.Item;

/**
 * Helper navigasi hierarki {@link Item} (item pustaka) pada modul sumber daya perpustakaan:
 * mengambil id anak langsung dari suatu item induk, menghitung jumlah anak, dan mengumpulkan
 * seluruh keturunan (rekursif) sebuah item ke dalam satu himpunan datar.
 */
public class PerpustakaanResourcesHelper {

	/**
	 * Mengambil id item aktif yang merupakan anak langsung dari {@code parentItem} (atau item
	 * akar bila {@code parentItem} {@code null}), diurutkan menurun berdasarkan id.
	 *
	 * @param session    sesi Hibernate aktif
	 * @param parentItem id item induk, atau {@code null} untuk item tingkat akar
	 * @param index      bila {@code null}, ambil hingga 10000 baris dari awal; bila diisi, ambil
	 *                   satu baris pada offset tersebut (dipakai untuk pengecekan keberadaan)
	 * @return daftar id item anak
	 */
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

	/** @return id item anak langsung dari {@code parentItem} (hingga 10000 baris), lihat {@link #getChildrenByIds(Session, Long, Integer)}. */
	public List<Long> getChildrenByIds(Session session, Long parentItem) {
		return getChildrenByIds(session, parentItem, null);
	}

	/**
	 * @param session sesi Hibernate aktif
	 * @param parent  id item induk, atau {@code null} untuk item tingkat akar
	 * @return jumlah item anak aktif langsung dari {@code parent}
	 */
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

	/**
	 * Mengumpulkan seluruh keturunan (anak, cucu, dst.) dari item {@code parent} secara rekursif
	 * ke dalam {@code childs}. Bila {@code parent} tidak memiliki anak, {@code parent} sendiri yang
	 * ditambahkan ke {@code childs} (dipakai pemanggil untuk kasus item daun/leaf).
	 *
	 * @param session sesi Hibernate aktif
	 * @param parent  id item awal penelusuran
	 * @param childs  himpunan hasil, diisi/diperbarui di tempat
	 */
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
