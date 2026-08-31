package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.KategoriItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.KategoriItem} pada modul
 * perpustakaan — kategori item pustaka. Kelas ini sengaja kosong: seluruh perilaku CRUD generik
 * diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana
 * untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class KategoriItemDaoImpl extends
		GenericHibernateDao<KategoriItem, Long, KategoriItemDao> implements
		KategoriItemDao {

}
