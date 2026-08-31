package ais.database.dao;

import ais.database.model.StatusPegawai;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.StatusPegawai} (data referensi
 * status pegawai). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class StatusPegawaiDaoImpl extends
		GenericHibernateDao<StatusPegawai, Long, StatusPegawaiDao> implements
		StatusPegawaiDao {

}
