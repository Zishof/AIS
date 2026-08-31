package ais.database.dao;

import ais.database.model.TugasKelompok;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.TugasKelompok} (data tugas
 * kelompok). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class TugasKelompokDaoImpl extends
		GenericHibernateDao<TugasKelompok, Long, TugasKelompokDao> implements
		TugasKelompokDao {

}
