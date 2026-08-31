package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.PeminjamanPengadaanItemDetail;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.PeminjamanPengadaanItemDetail}
 * pada modul perpustakaan — baris detail peminjaman item pustaka (rincian per item dari
 * {@link ais.database.model.library.PeminjamanPengadaanItem}). Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PeminjamanPengadaanItemDetailDaoImpl
		extends
		GenericHibernateDao<PeminjamanPengadaanItemDetail, Long, PeminjamanPengadaanItemDetailDao>
		implements PeminjamanPengadaanItemDetailDao {

}
