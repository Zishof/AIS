package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.PenerimaanPengadaanItemDetail;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.PenerimaanPengadaanItemDetail}
 * pada modul perpustakaan — baris detail penerimaan pengadaan item pustaka (rincian per item dari
 * {@link ais.database.model.library.PenerimaanPengadaanItem}). Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PenerimaanPengadaanItemDetailDaoImpl
		extends
		GenericHibernateDao<PenerimaanPengadaanItemDetail, Long, PenerimaanPengadaanItemDetailDao>
		implements PenerimaanPengadaanItemDetailDao {

}
