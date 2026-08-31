package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.DataUdcItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.DataUdcItem} pada modul
 * perpustakaan — data klasifikasi Universal Decimal Classification (UDC) yang melekat pada item
 * pustaka tertentu. Kelas ini sengaja kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class DataUdcItemDaoImpl extends
		GenericHibernateDao<DataUdcItem, Long, DataUdcItemDao> implements
		DataUdcItemDao {

}
