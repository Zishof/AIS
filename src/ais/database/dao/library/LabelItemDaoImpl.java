package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.LabelItem;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.LabelItem} pada modul
 * perpustakaan — label/barcode fisik yang ditempel pada item pustaka. Kelas ini sengaja kosong:
 * seluruh perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class LabelItemDaoImpl extends
		GenericHibernateDao<LabelItem, Long, LabelItemDao> implements
		LabelItemDao {

}
