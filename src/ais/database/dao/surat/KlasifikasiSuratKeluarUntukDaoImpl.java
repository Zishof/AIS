package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.KlasifikasiSuratKeluarUntuk;

/**
 * Implementasi Hibernate {@link KlasifikasiSuratKeluarUntukDao} untuk entitas
 * {@link ais.database.model.surat.KlasifikasiSuratKeluarUntuk}. Kelas ini sengaja kosong: seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc
 * di sana untuk detail perilaku method.
 */
public class KlasifikasiSuratKeluarUntukDaoImpl
		extends
		GenericHibernateDao<KlasifikasiSuratKeluarUntuk, Long, KlasifikasiSuratKeluarUntukDao>
		implements KlasifikasiSuratKeluarUntukDao {

}
