package ais.database.dao;

import ais.database.model.TemplateSurat;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.TemplateSurat} (data template
 * surat). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class TemplateSuratDaoImpl extends
		GenericHibernateDao<TemplateSurat, Long, TemplateSuratDao> implements
		TemplateSuratDao {

}
