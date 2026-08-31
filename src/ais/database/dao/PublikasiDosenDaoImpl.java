package ais.database.dao;

import ais.database.model.epsbed.EpsbedPublikasiDosen;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.epsbed.EpsbedPublikasiDosen} (data
 * publikasi dosen untuk pelaporan EPSBED). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PublikasiDosenDaoImpl extends
		GenericHibernateDao<EpsbedPublikasiDosen, Long, PublikasiDosenDao>
		implements PublikasiDosenDao {

}
