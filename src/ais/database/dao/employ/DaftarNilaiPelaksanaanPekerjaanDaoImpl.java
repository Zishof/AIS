package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.DaftarNilaiPelaksanaanPekerjaan;

/**
 * Implementasi Hibernate untuk {@link DaftarNilaiPelaksanaanPekerjaanDao}, mengelola entitas
 * {@link ais.database.model.employ.DaftarNilaiPelaksanaanPekerjaan}. Kosong sesuai desain —
 * seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class DaftarNilaiPelaksanaanPekerjaanDaoImpl extends GenericHibernateDao<DaftarNilaiPelaksanaanPekerjaan, Long, DaftarNilaiPelaksanaanPekerjaanDao>
		implements DaftarNilaiPelaksanaanPekerjaanDao {

}
