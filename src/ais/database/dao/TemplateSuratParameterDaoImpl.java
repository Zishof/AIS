package ais.database.dao;

import ais.database.model.TemplateSuratParameter;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.TemplateSuratParameter} (data
 * parameter template surat). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class TemplateSuratParameterDaoImpl
		extends
		GenericHibernateDao<TemplateSuratParameter, Long, TemplateSuratParameterDao>
		implements TemplateSuratParameterDao {

}
