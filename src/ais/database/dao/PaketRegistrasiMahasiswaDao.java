package ais.database.dao;

import ais.database.model.PaketRegistrasiMahasiswa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.PaketRegistrasiMahasiswa} (data paket
 * registrasi mahasiswa). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PaketRegistrasiMahasiswaDao extends GenericDao<PaketRegistrasiMahasiswa, Long>{

}
