package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.DataDdcItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.DataDdcItem} pada modul
 * perpustakaan — data klasifikasi Dewey Decimal Classification (DDC) yang melekat pada item
 * pustaka tertentu. Kelas ini sengaja kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class DataDdcItemDaoImpl extends
		GenericHibernateDao<DataDdcItem, Long, DataDdcItemDao> implements
		DataDdcItemDao {

}
