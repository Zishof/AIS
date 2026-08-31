package ais.database.dao;

import ais.database.model.Pegawai;


/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Pegawai} (data pegawai). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
//Pegawai  : nama entitynya sesuai dao
//long : tipe primary key
public interface PegawaiDao extends GenericDao<Pegawai, Long> {
    

}
