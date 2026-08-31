package ais.database.dao;

import ais.database.model.DiskusiPengumumanAkademis;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.DiskusiPengumumanAkademis} (diskusi/
 * komentar warga kampus atas suatu pengumuman akademis). Pasangan Dao/DaoImpl ini murni memakai
 * perilaku generik {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di
 * sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface DiskusiPengumumanAkademisDao extends GenericDao<DiskusiPengumumanAkademis, Long>{

}
