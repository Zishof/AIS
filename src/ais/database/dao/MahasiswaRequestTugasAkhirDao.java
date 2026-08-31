package ais.database.dao;

import ais.database.model.MahasiswaRequestTugasAkhir;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.MahasiswaRequestTugasAkhir} (data
 * permintaan tugas akhir oleh mahasiswa). Pasangan Dao/DaoImpl ini murni memakai perilaku
 * generik {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface MahasiswaRequestTugasAkhirDao extends
		GenericDao<MahasiswaRequestTugasAkhir, Long> {

}
