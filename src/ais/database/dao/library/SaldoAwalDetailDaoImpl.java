package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.SaldoAwalDetail;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.SaldoAwalDetail} pada
 * modul perpustakaan — baris detail saldo awal item pustaka (rincian per item dari
 * {@link ais.database.model.library.SaldoAwal}). Kelas ini sengaja kosong: seluruh perilaku CRUD
 * generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di
 * sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class SaldoAwalDetailDaoImpl extends
		GenericHibernateDao<SaldoAwalDetail, Long, SaldoAwalDetailDao> implements
		SaldoAwalDetailDao {

}
