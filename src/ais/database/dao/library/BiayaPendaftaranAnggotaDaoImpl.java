package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.BiayaPendaftaranAnggota;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.BiayaPendaftaranAnggota}
 * pada modul perpustakaan — biaya pendaftaran keanggotaan perpustakaan. Kelas ini sengaja kosong:
 * seluruh perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class BiayaPendaftaranAnggotaDaoImpl
		extends
		GenericHibernateDao<BiayaPendaftaranAnggota, Long, BiayaPendaftaranAnggotaDao>
		implements BiayaPendaftaranAnggotaDao {

}
