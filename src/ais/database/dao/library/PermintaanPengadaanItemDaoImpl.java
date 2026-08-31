package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.PermintaanPengadaanItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.PermintaanPengadaanItem}
 * pada modul perpustakaan — transaksi (header) permintaan pengadaan item pustaka. Kelas ini
 * sengaja kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PermintaanPengadaanItemDaoImpl extends
		GenericHibernateDao<PermintaanPengadaanItem, Long, PermintaanPengadaanItemDao> implements
		PermintaanPengadaanItemDao {

}
