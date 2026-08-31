package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.PemesananPengadaanItemDetail;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.PemesananPengadaanItemDetail}
 * pada modul perpustakaan — baris detail pemesanan pengadaan item pustaka (rincian per item dari
 * {@link ais.database.model.library.PemesananPengadaanItem}). Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PemesananPengadaanItemDetailDaoImpl
		extends
		GenericHibernateDao<PemesananPengadaanItemDetail, Long, PemesananPengadaanItemDetailDao>
		implements PemesananPengadaanItemDetailDao {

}
