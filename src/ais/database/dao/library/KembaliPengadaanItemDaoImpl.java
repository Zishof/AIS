package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.KembaliPengadaanItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.KembaliPengadaanItem}
 * pada modul perpustakaan — transaksi (header) pengembalian pengadaan item pustaka ke penyedia.
 * Kelas ini sengaja kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class KembaliPengadaanItemDaoImpl extends
		GenericHibernateDao<KembaliPengadaanItem, Long, KembaliPengadaanItemDao> implements
		KembaliPengadaanItemDao {

}
