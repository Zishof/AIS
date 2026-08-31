package ais.database.dao;

import ais.database.model.PaketRegistrasiMahasiswa;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PaketRegistrasiMahasiswa} (data
 * paket registrasi mahasiswa). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PaketRegistrasiMahasiswaDaoImpl extends GenericHibernateDao<PaketRegistrasiMahasiswa, Long, PaketRegistrasiMahasiswaDao> implements PaketRegistrasiMahasiswaDao{

}
