package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.SuratKeluar;

/**
 * Implementasi Hibernate {@link SuratKeluarDao} untuk entitas {@link ais.database.model.surat.SuratKeluar}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class SuratKeluarDaoImpl extends
		GenericHibernateDao<SuratKeluar, Long, SuratKeluarDao> implements
		SuratKeluarDao {

}
