package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.Pustakawan;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.Pustakawan} pada modul
 * perpustakaan — pustakawan/petugas perpustakaan. Kelas ini sengaja kosong: seluruh perilaku CRUD
 * generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di
 * sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PustakawanDaoImpl extends
		GenericHibernateDao<Pustakawan, Long, PustakawanDao> implements
		PustakawanDao {

}
