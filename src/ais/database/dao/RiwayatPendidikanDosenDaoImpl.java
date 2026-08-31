package ais.database.dao;

import ais.database.model.RiwayatPendidikanDosen;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.RiwayatPendidikanDosen} (data
 * riwayat pendidikan dosen). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class RiwayatPendidikanDosenDaoImpl
		extends
		GenericHibernateDao<RiwayatPendidikanDosen, Long, RiwayatPendidikanDosenDao>
		implements RiwayatPendidikanDosenDao {

}
