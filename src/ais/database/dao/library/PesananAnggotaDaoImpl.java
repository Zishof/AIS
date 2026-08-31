package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.PesananAnggota;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.PesananAnggota} pada
 * modul perpustakaan — pesanan/reservasi item pustaka oleh anggota. Kelas ini sengaja kosong:
 * seluruh perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class PesananAnggotaDaoImpl extends
		GenericHibernateDao<PesananAnggota, Long, PesananAnggotaDao> implements
		PesananAnggotaDao {

}
