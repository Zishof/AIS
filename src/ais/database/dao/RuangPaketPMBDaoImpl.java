package ais.database.dao;

import ais.database.model.RuangPaketPMB;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.RuangPaketPMB} (data relasi ruang
 * dengan paket PMB). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class RuangPaketPMBDaoImpl extends GenericHibernateDao<RuangPaketPMB, Long, RuangPaketPMBDao> implements RuangPaketPMBDao{

}
