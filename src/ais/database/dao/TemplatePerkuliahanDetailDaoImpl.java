package ais.database.dao;

import ais.database.model.TemplatePerkuliahanDetail;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.TemplatePerkuliahanDetail} (data
 * detail template perkuliahan). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class TemplatePerkuliahanDetailDaoImpl
		extends
		GenericHibernateDao<TemplatePerkuliahanDetail, Long, TemplatePerkuliahanDetailDao>
		implements TemplatePerkuliahanDetailDao {

}
