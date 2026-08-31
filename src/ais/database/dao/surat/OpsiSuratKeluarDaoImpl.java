package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.OpsiSuratKeluar;

/**
 * Implementasi Hibernate {@link OpsiSuratKeluarDao} untuk entitas
 * {@link ais.database.model.surat.OpsiSuratKeluar}. Kelas ini sengaja kosong: seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class OpsiSuratKeluarDaoImpl extends
		GenericHibernateDao<OpsiSuratKeluar, Long, OpsiSuratKeluarDao>
		implements OpsiSuratKeluarDao {

}
