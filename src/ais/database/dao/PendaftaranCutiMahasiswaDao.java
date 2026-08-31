package ais.database.dao;

import ais.database.model.PendaftaranCutiMahasiswa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.PendaftaranCutiMahasiswa} (data
 * pendaftaran cuti mahasiswa). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PendaftaranCutiMahasiswaDao extends GenericDao<PendaftaranCutiMahasiswa, Long>{

}
