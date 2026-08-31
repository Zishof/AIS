package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.Perpustakaan;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.Perpustakaan} pada modul
 * perpustakaan — data perpustakaan/cabang perpustakaan. Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PerpustakaanDaoImpl extends
		GenericHibernateDao<Perpustakaan, Long, PerpustakaanDao> implements
		PerpustakaanDao {

}
