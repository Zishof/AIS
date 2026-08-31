package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.SaldoAwal;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.SaldoAwal} pada modul
 * perpustakaan — transaksi (header) saldo awal stok/koleksi item pustaka. Kelas ini sengaja
 * kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class SaldoAwalDaoImpl extends
		GenericHibernateDao<SaldoAwal, Long, SaldoAwalDao> implements
		SaldoAwalDao {

}
