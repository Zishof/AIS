package ais.database.dao;

import ais.database.model.MetaReport;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.MetaReport} (data meta laporan). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface MetaReportDao extends GenericDao<MetaReport, Long> {

}
