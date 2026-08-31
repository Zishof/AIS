package ais.database.dao;

import ais.database.model.MahasiswaRequestTugasAkhirMintaPembimbing;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.MahasiswaRequestTugasAkhirMintaPembimbing}
 * (data permintaan dosen pembimbing tugas akhir oleh mahasiswa). Pasangan Dao/DaoImpl ini murni
 * memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat
 * javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface MahasiswaRequestTugasAkhirMintaPembimbingDao extends
		GenericDao<MahasiswaRequestTugasAkhirMintaPembimbing, Long> {

}
