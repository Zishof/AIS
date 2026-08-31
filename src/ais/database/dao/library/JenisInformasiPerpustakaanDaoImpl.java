package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.JenisInformasiPerpustakaan;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.JenisInformasiPerpustakaan}
 * pada modul perpustakaan — jenis/kategori informasi perpustakaan. Kelas ini sengaja kosong:
 * seluruh perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class JenisInformasiPerpustakaanDaoImpl
		extends
		GenericHibernateDao<JenisInformasiPerpustakaan, Long, JenisInformasiPerpustakaanDao>
		implements JenisInformasiPerpustakaanDao {

}
