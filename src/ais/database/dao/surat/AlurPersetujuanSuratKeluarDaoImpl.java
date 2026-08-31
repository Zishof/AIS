package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;

/**
 * Implementasi Hibernate {@link AlurPersetujuanSuratKeluarDao} untuk entitas
 * {@link ais.database.model.surat.AlurPersetujuanSuratKeluar}. Kelas ini sengaja kosong: seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc
 * di sana untuk detail perilaku method.
 */
public class AlurPersetujuanSuratKeluarDaoImpl extends
		GenericHibernateDao<AlurPersetujuanSuratKeluar, Long, AlurPersetujuanSuratKeluarDao> implements
		AlurPersetujuanSuratKeluarDao {
	
}
