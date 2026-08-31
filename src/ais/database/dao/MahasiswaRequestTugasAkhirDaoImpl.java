package ais.database.dao;

import ais.database.model.MahasiswaRequestTugasAkhir;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.MahasiswaRequestTugasAkhir} (data
 * permintaan tugas akhir oleh mahasiswa). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class MahasiswaRequestTugasAkhirDaoImpl
		extends
		GenericHibernateDao<MahasiswaRequestTugasAkhir, Long, MahasiswaRequestTugasAkhirDao>
		implements MahasiswaRequestTugasAkhirDao {

}
