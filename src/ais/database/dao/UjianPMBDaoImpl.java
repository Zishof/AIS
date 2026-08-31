package ais.database.dao;

import ais.database.model.UjianPMB;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.UjianPMB} (data ujian PMB/
 * penerimaan mahasiswa baru). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class UjianPMBDaoImpl extends GenericHibernateDao<UjianPMB, Long, UjianPMBDao> implements UjianPMBDao{

}
