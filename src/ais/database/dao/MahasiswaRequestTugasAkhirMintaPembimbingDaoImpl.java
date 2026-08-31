package ais.database.dao;

import ais.database.model.MahasiswaRequestTugasAkhirMintaPembimbing;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.MahasiswaRequestTugasAkhirMintaPembimbing}
 * (data permintaan dosen pembimbing tugas akhir oleh mahasiswa). Kelas ini murni mewarisi
 * perilaku generik dari {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan --
 * lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class MahasiswaRequestTugasAkhirMintaPembimbingDaoImpl
		extends
		GenericHibernateDao<MahasiswaRequestTugasAkhirMintaPembimbing, Long, MahasiswaRequestTugasAkhirMintaPembimbingDao>
		implements MahasiswaRequestTugasAkhirMintaPembimbingDao {

}
