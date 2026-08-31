package ais.database.dao.surat;

import ais.database.dao.GenericDao;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;

/**
 * DAO untuk entitas {@link ais.database.model.surat.AlurPersetujuanSuratMasukStatus} — status/
 * riwayat keputusan pada satu tahapan {@link ais.database.model.surat.AlurPersetujuanSuratMasuk}
 * (modul tata naskah dinas/persuratan). Interface ini sengaja kosong: seluruh kontrak CRUD generik
 * diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku
 * method.
 */
public interface AlurPersetujuanSuratMasukStatusDao extends GenericDao<AlurPersetujuanSuratMasukStatus, Long> {

}
