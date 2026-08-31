package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.PeminjamanPengadaanItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.PeminjamanPengadaanItem}
 * pada modul perpustakaan — transaksi (header) peminjaman item pustaka oleh anggota. Kelas ini
 * sengaja kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PeminjamanPengadaanItemDaoImpl extends
		GenericHibernateDao<PeminjamanPengadaanItem, Long, PeminjamanPengadaanItemDao> implements
		PeminjamanPengadaanItemDao {

}
