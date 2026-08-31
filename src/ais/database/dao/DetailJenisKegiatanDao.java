package ais.database.dao;


import ais.database.model.JenisKegiatanDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.JenisKegiatanDetail} (detail jenis
 * kegiatan) -- perhatikan nama berkas DAO ini ({@code DetailJenisKegiatanDao}) tidak persis sama
 * dengan nama kelas entitasnya. Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface DetailJenisKegiatanDao extends GenericDao<JenisKegiatanDetail, Long>{

}
