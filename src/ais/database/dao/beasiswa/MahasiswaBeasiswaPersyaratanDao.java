package ais.database.dao.beasiswa;

import ais.database.dao.GenericDao;
import ais.database.model.beasiswa.MahasiswaBeasiswaPersyaratan;

/**
 * DAO untuk entitas {@link ais.database.model.beasiswa.MahasiswaBeasiswaPersyaratan} — catatan
 * pemenuhan satu persyaratan beasiswa oleh seorang mahasiswa (modul beasiswa). Interface ini
 * sengaja kosong: seluruh kontrak CRUD generik diwariskan dari {@link ais.database.dao.GenericDao},
 * lihat Javadoc di sana untuk detail perilaku method.
 */
public interface MahasiswaBeasiswaPersyaratanDao extends
		GenericDao<MahasiswaBeasiswaPersyaratan, Long> {

}
