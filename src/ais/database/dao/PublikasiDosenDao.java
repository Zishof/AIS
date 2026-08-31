package ais.database.dao;

import ais.database.model.epsbed.EpsbedPublikasiDosen;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.epsbed.EpsbedPublikasiDosen} (data
 * publikasi dosen untuk pelaporan EPSBED). Nama interface tidak mengikuti nama entitas persis
 * (entitas ada di sub-paket {@code ais.database.model.epsbed}), namun pasangan Dao/DaoImpl ini
 * tetap murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa method
 * tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PublikasiDosenDao extends
		GenericDao<EpsbedPublikasiDosen, Long> {

}
