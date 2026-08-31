package ais.database.dao;


import ais.database.model.FormatNilaiSkripsi;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.FormatNilaiSkripsi} (format/skala
 * penilaian skripsi). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface FormatNilaiSkripsiDao extends GenericDao<FormatNilaiSkripsi, Long>{

}
