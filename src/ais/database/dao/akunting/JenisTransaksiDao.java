package ais.database.dao.akunting;

import ais.database.dao.GenericDao;
import ais.database.model.akunting.JenisTransaksi;

/**
 * DAO untuk entitas {@link ais.database.model.akunting.JenisTransaksi} — jenis transaksi akunting
 * (mis. kas masuk, kas keluar, jurnal umum). Interface ini sengaja kosong: seluruh kontrak CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail
 * perilaku method.
 */
public interface JenisTransaksiDao extends GenericDao<JenisTransaksi, Long> {

}
