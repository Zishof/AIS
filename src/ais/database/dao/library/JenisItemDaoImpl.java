package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.JenisItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.JenisItem} pada modul
 * perpustakaan — jenis item pustaka (mis. buku, majalah, CD). Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class JenisItemDaoImpl extends GenericHibernateDao<JenisItem, Long, JenisItemDao> implements JenisItemDao{

}
