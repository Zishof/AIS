package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.StatusItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.StatusItem} pada modul
 * perpustakaan — status item pustaka (mis. tersedia, dipinjam, rusak, hilang). Kelas ini sengaja
 * kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class StatusItemDaoImpl extends
		GenericHibernateDao<StatusItem, Long, StatusItemDao> implements
		StatusItemDao {

}
