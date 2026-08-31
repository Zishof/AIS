package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.TipeAnggota;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.TipeAnggota} pada modul
 * perpustakaan — tipe/kelas anggota perpustakaan. Kelas ini sengaja kosong: seluruh perilaku CRUD
 * generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di
 * sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class TipeAnggotaDaoImpl extends
		GenericHibernateDao<TipeAnggota, Long, TipeAnggotaDao> implements
		TipeAnggotaDao {

}
