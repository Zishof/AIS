package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.DendaKeterlambatanItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.DendaKeterlambatanItem}
 * pada modul perpustakaan — denda keterlambatan pengembalian item pustaka. Kelas ini sengaja
 * kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class DendaKeterlambatanItemDaoImpl
		extends
		GenericHibernateDao<DendaKeterlambatanItem, Long, DendaKeterlambatanItemDao>
		implements DendaKeterlambatanItemDao {

}
