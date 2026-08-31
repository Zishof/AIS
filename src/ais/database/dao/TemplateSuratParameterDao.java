package ais.database.dao;

import ais.database.model.TemplateSuratParameter;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.TemplateSuratParameter} (data parameter
 * template surat). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface TemplateSuratParameterDao extends GenericDao<TemplateSuratParameter, Long>{

}
