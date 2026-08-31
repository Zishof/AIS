package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.JenisAnggota;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.JenisAnggota} pada modul
 * perpustakaan — jenis/klasifikasi anggota perpustakaan. Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao},
 * lihat javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class JenisAnggotaDaoImpl extends GenericHibernateDao<JenisAnggota, Long, JenisAnggotaDao> implements JenisAnggotaDao{

}
