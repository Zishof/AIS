package ais.database.dao;

import ais.database.model.TemplatePerkuliahanDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.TemplatePerkuliahanDetail} (data detail
 * template perkuliahan). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface TemplatePerkuliahanDetailDao extends
		GenericDao<TemplatePerkuliahanDetail, Long> {

}
