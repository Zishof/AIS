package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.JenisIdentitasAnggota;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.JenisIdentitasAnggota}
 * pada modul perpustakaan — jenis dokumen identitas anggota (mis. KTP, SIM, Paspor). Kelas ini
 * sengaja kosong: seluruh perilaku CRUD generik diwarisi langsung dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class JenisIdentitasAnggotaDaoImpl
		extends
		GenericHibernateDao<JenisIdentitasAnggota, Long, JenisIdentitasAnggotaDao>
		implements JenisIdentitasAnggotaDao {

}
