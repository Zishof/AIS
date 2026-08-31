package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.DomainPenelitian;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.DomainPenelitian} pada
 * modul perpustakaan — domain/bidang penelitian yang menjadi topik koleksi pustaka. Kelas ini
 * sengaja kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class DomainPenelitianDaoImpl extends
		GenericHibernateDao<DomainPenelitian, Long, DomainPenelitianDao>
		implements DomainPenelitianDao {

}
