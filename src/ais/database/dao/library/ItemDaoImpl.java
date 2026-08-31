package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.Item;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.Item} pada modul
 * perpustakaan — item koleksi pustaka (buku/bahan pustaka). Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class ItemDaoImpl extends GenericHibernateDao<Item, Long, ItemDao> implements ItemDao{

}
