package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.Rak;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.Rak} pada modul
 * perpustakaan — rak penyimpanan koleksi pustaka. Kelas ini sengaja kosong: seluruh perilaku CRUD
 * generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di
 * sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class RakDaoImpl extends GenericHibernateDao<Rak, Long, RakDao> implements RakDao{

}
