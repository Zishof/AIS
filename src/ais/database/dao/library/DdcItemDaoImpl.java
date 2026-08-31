package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.DdcItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.DdcItem} pada modul
 * perpustakaan — master kode/kategori klasifikasi Dewey Decimal Classification (DDC). Kelas ini
 * sengaja kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class DdcItemDaoImpl extends
		GenericHibernateDao<DdcItem, Long, DdcItemDao> implements
		DdcItemDao {

}
