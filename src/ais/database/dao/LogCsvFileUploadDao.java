package ais.database.dao;

import ais.database.model.file.LogCsvFileUpload;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.file.LogCsvFileUpload} (log riwayat
 * unggah berkas CSV). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface LogCsvFileUploadDao extends GenericDao<LogCsvFileUpload, Long>{

}
