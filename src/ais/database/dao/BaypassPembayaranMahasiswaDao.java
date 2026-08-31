package ais.database.dao;

import ais.database.model.BaypassPembayaranMahasiswa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.BaypassPembayaranMahasiswa} (data bypass
 * / pengecualian kewajiban pembayaran seorang mahasiswa). Pasangan Dao/DaoImpl ini murni memakai
 * perilaku generik {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di
 * sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface BaypassPembayaranMahasiswaDao extends GenericDao<BaypassPembayaranMahasiswa, Long>{

}
