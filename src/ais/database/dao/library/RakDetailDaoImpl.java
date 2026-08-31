package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.RakDetail;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.RakDetail} pada modul
 * perpustakaan — baris detail rak (rincian penempatan item pada
 * {@link ais.database.model.library.Rak}). Kelas ini sengaja kosong: seluruh perilaku CRUD
 * generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di
 * sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class RakDetailDaoImpl extends
		GenericHibernateDao<RakDetail, Long, RakDetailDao> implements
		RakDetailDao {

}
