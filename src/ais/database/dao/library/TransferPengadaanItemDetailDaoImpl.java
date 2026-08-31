package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.TransferPengadaanItemDetail;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.TransferPengadaanItemDetail}
 * pada modul perpustakaan — baris detail transfer item pustaka (rincian per item dari
 * {@link ais.database.model.library.TransferPengadaanItem}). Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class TransferPengadaanItemDetailDaoImpl
		extends
		GenericHibernateDao<TransferPengadaanItemDetail, Long, TransferPengadaanItemDetailDao>
		implements TransferPengadaanItemDetailDao {

}
