package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.TerimaPengadaanItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.TerimaPengadaanItem}
 * pada modul perpustakaan — transaksi (header) terima pengadaan item pustaka. Kelas ini sengaja
 * kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class TerimaPengadaanItemDaoImpl extends
		GenericHibernateDao<TerimaPengadaanItem, Long, TerimaPengadaanItemDao> implements
		TerimaPengadaanItemDao {

}
