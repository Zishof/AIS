package ais.database.dao;

import ais.database.model.JenisKegiatanDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.JenisKegiatanDetail} (detail jenis
 * kegiatan) -- catatan: entitas yang sama juga punya DAO kedua bernama
 * {@link ais.database.dao.DetailJenisKegiatanDao}; keduanya sama-sama kosong dan hanya
 * mewarisi perilaku generik. Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface JenisKegiatanDetailDao extends GenericDao<JenisKegiatanDetail, Long>{

}
