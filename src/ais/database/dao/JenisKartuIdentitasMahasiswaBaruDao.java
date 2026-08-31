package ais.database.dao;

import ais.database.model.JenisKartuIdentitasMahasiswaBaru;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.JenisKartuIdentitasMahasiswaBaru} (data
 * referensi jenis kartu identitas yang didaftarkan mahasiswa baru, mis. KTP/KK/Paspor). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface JenisKartuIdentitasMahasiswaBaruDao extends GenericDao<JenisKartuIdentitasMahasiswaBaru, Long>{

}
