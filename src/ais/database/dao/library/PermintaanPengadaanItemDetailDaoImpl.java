package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.PermintaanPengadaanItemDetail;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.PermintaanPengadaanItemDetail}
 * pada modul perpustakaan — baris detail permintaan pengadaan item pustaka (rincian per item dari
 * {@link ais.database.model.library.PermintaanPengadaanItem}). Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PermintaanPengadaanItemDetailDaoImpl
		extends
		GenericHibernateDao<PermintaanPengadaanItemDetail, Long, PermintaanPengadaanItemDetailDao>
		implements PermintaanPengadaanItemDetailDao {

}
