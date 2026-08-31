package ais.database.dao;

import ais.database.model.PendaftaranCutiMahasiswa;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PendaftaranCutiMahasiswa} (data
 * pendaftaran cuti mahasiswa). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PendaftaranCutiMahasiswaDaoImpl extends GenericHibernateDao<PendaftaranCutiMahasiswa, Long, PendaftaranCutiMahasiswaDao> implements PendaftaranCutiMahasiswaDao{

}
