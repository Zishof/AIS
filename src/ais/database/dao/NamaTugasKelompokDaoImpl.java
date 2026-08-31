package ais.database.dao;

import ais.database.model.NamaTugasKelompok;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.NamaTugasKelompok} (data nama tugas
 * kelompok). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class NamaTugasKelompokDaoImpl
		extends
		GenericHibernateDao<NamaTugasKelompok, Long, NamaTugasKelompokDao>
		implements NamaTugasKelompokDao {

}
