package ais.database.dao;

import ais.database.model.KurikulumPunyaMatakuliah;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.KurikulumPunyaMatakuliah} (relasi
 * kurikulum dengan mata kuliah yang menjadi anggotanya). Diimplementasikan oleh
 * {@link ais.database.dao.KurikulumPunyaMataKuliahDaoImpl} (perhatikan perbedaan kapitalisasi
 * nama berkas implementasinya). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface KurikulumPunyaMatakuliahDao extends GenericDao<KurikulumPunyaMatakuliah, Long>{

}
