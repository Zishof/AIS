package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.ReturPengadaanItemDetail;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.ReturPengadaanItemDetail}
 * pada modul perpustakaan — baris detail retur pengadaan item pustaka (rincian per item dari
 * {@link ais.database.model.library.ReturPengadaanItem}). Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class ReturPengadaanItemDetailDaoImpl
		extends
		GenericHibernateDao<ReturPengadaanItemDetail, Long, ReturPengadaanItemDetailDao>
		implements ReturPengadaanItemDetailDao {

}
