package ais.database.dao;

import ais.database.model.MetaReport;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.MetaReport} (data meta laporan).
 * Kelas ini murni mewarisi perilaku generik dari {@link ais.database.dao.GenericHibernateDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public class MetaReportDaoImpl extends
		GenericHibernateDao<MetaReport, Long, MetaReportDao> implements
		MetaReportDao {

}
