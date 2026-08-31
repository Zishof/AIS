package ais.database.dao;

import ais.database.model.TemplatePerkuliahan;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.TemplatePerkuliahan} (data
 * template perkuliahan). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class TemplatePerkuliahanDaoImpl extends
		GenericHibernateDao<TemplatePerkuliahan, Long, TemplatePerkuliahanDao>
		implements TemplatePerkuliahanDao {

}
