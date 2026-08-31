package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.VersiDdcItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.VersiDdcItem} pada modul
 * perpustakaan — versi/edisi skema klasifikasi Dewey Decimal Classification (DDC) yang dipakai.
 * Kelas ini sengaja kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class VersiDdcItemDaoImpl extends
		GenericHibernateDao<VersiDdcItem, Long, VersiDdcItemDao> implements
		VersiDdcItemDao {

}
