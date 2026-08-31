package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.PenerimaanPengadaanItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.PenerimaanPengadaanItem}
 * pada modul perpustakaan — transaksi (header) penerimaan pengadaan item pustaka dari penyedia.
 * Kelas ini sengaja kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PenerimaanPengadaanItemDaoImpl extends
		GenericHibernateDao<PenerimaanPengadaanItem, Long, PenerimaanPengadaanItemDao> implements
		PenerimaanPengadaanItemDao {

}
