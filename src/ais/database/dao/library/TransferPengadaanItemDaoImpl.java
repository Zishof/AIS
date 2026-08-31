package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.TransferPengadaanItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.TransferPengadaanItem}
 * pada modul perpustakaan — transaksi (header) transfer item pustaka antar perpustakaan/cabang.
 * Kelas ini sengaja kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class TransferPengadaanItemDaoImpl extends
		GenericHibernateDao<TransferPengadaanItem, Long, TransferPengadaanItemDao> implements
		TransferPengadaanItemDao {

}
