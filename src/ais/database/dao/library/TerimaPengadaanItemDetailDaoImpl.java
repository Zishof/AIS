package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.TerimaPengadaanItemDetail;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.TerimaPengadaanItemDetail}
 * pada modul perpustakaan — baris detail terima pengadaan item pustaka (rincian per item dari
 * {@link ais.database.model.library.TerimaPengadaanItem}). Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class TerimaPengadaanItemDetailDaoImpl
		extends
		GenericHibernateDao<TerimaPengadaanItemDetail, Long, TerimaPengadaanItemDetailDao>
		implements TerimaPengadaanItemDetailDao {

}
