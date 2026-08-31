package ais.database.dao;

import ais.database.model.PengajuanBeasiswa;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PengajuanBeasiswa} (data pengajuan
 * beasiswa). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PengajuanBeasiswaDaoImpl extends
		GenericHibernateDao<PengajuanBeasiswa, Long, PengajuanBeasiswaDao>
		implements PengajuanBeasiswaDao {

}
