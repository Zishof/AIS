package ais.database.dao;

import ais.database.model.TemplateQuery;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.TemplateQuery} (data template
 * query). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class TemplateQueryDaoImpl extends
		GenericHibernateDao<TemplateQuery, Long, TemplateQueryDao> implements
		TemplateQueryDao {

}