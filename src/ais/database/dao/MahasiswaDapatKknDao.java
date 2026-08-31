package ais.database.dao;

import ais.database.model.MahasiswaDapatKkn;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.MahasiswaDapatKkn} (relasi mahasiswa
 * peserta suatu program KKN). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface MahasiswaDapatKknDao extends GenericDao<MahasiswaDapatKkn, Long> {
    

}
