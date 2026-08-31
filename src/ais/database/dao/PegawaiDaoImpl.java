package ais.database.dao;

import ais.database.model.Pegawai;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Pegawai} (data pegawai). Kelas ini
 * murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class PegawaiDaoImpl extends
		GenericHibernateDao<Pegawai, Long, PegawaiDao> implements PegawaiDao {

}
