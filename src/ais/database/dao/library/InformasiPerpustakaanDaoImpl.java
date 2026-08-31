package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.InformasiPerpustakaan;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.InformasiPerpustakaan}
 * pada modul perpustakaan — informasi/pengumuman yang diterbitkan perpustakaan. Kelas ini sengaja
 * kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class InformasiPerpustakaanDaoImpl
		extends
		GenericHibernateDao<InformasiPerpustakaan, Long, InformasiPerpustakaanDao>
		implements InformasiPerpustakaanDao {

}
