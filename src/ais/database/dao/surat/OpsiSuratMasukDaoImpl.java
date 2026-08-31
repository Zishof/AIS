package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.OpsiSuratMasuk;

/**
 * Implementasi Hibernate {@link OpsiSuratMasukDao} untuk entitas
 * {@link ais.database.model.surat.OpsiSuratMasuk}. Kelas ini sengaja kosong: seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class OpsiSuratMasukDaoImpl extends
		GenericHibernateDao<OpsiSuratMasuk, Long, OpsiSuratMasukDao>
		implements OpsiSuratMasukDao {

}
