package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.KoreksiItemDetail;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.KoreksiItemDetail} pada
 * modul perpustakaan — baris detail koreksi data item pustaka (rincian per item dari
 * {@link ais.database.model.library.KoreksiItem}). Kelas ini sengaja kosong: seluruh perilaku
 * CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao}, lihat javadoc
 * di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class KoreksiItemDetailDaoImpl extends
		GenericHibernateDao<KoreksiItemDetail, Long, KoreksiItemDetailDao>
		implements KoreksiItemDetailDao {

}
