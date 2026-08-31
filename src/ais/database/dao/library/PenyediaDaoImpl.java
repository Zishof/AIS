package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.Penyedia;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.Penyedia} pada modul
 * perpustakaan — penyedia/pemasok pengadaan item pustaka. Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PenyediaDaoImpl extends
		GenericHibernateDao<Penyedia, Long, PenyediaDao> implements PenyediaDao {

}
