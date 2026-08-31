package ais.database.dao.employ;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.employ.RiwayatPendidikanPegawai;

/**
 * Implementasi Hibernate untuk {@link RiwayatPendidikanPegawaiDao}, mengelola entitas
 * {@link ais.database.model.employ.RiwayatPendidikanPegawai}. Kosong sesuai desain — seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}.
 */
public class RiwayatPendidikanPegawaiDaoImpl
		extends
		GenericHibernateDao<RiwayatPendidikanPegawai, Long, RiwayatPendidikanPegawaiDao>
		implements RiwayatPendidikanPegawaiDao {

}