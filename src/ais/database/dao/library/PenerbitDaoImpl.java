package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.Penerbit;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.Penerbit} pada modul
 * perpustakaan — penerbit item pustaka. Kelas ini sengaja kosong: seluruh perilaku CRUD generik
 * diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana
 * untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PenerbitDaoImpl extends
		GenericHibernateDao<Penerbit, Long, PenerbitDao> implements PenerbitDao {

}
