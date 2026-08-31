package ais.database.dao;

import ais.database.model.file.LogCsvFileUpload;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.file.LogCsvFileUpload} (log
 * riwayat unggah berkas CSV), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada
 * method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class LogCsvFileUploadDaoImpl extends GenericHibernateDao<LogCsvFileUpload, Long, LogCsvFileUploadDao> implements LogCsvFileUploadDao{

}
