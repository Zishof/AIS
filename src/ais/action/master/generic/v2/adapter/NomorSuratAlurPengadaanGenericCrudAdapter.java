package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.NomorSuratAlurPengadaan;

/** Parity editor inline NomorSuratAlurPengadaanAction tanpa runtime ZK. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class NomorSuratAlurPengadaanGenericCrudAdapter
        extends AbstractGenericCrudEntityAdapter<NomorSuratAlurPengadaan>
        implements GenericCrudScopeAdapter, GenericCrudQueryInitializer {
    public NomorSuratAlurPengadaan createNew(GenericCrudRequestContext context) { return new NomorSuratAlurPengadaan(); }
    public void applyUpdateValues(NomorSuratAlurPengadaan target, Map values,
            GenericCrudRequestContext context) throws Exception {
        Map safe = new java.util.LinkedHashMap();
        if (values.containsKey("nomorSurat")) safe.put("nomorSurat", values.get("nomorSurat"));
        super.applyUpdateValues(target, safe, context);
    }
    public void afterSave(Session session, NomorSuratAlurPengadaan target,
            GenericCrudRequestContext context) { updateCache(target); }
    public boolean canDelete(NomorSuratAlurPengadaan target, GenericCrudRequestContext context, List reasons) {
        reasons.add("Jenis alur pengadaan adalah konfigurasi tetap dan tidak dapat dihapus."); return false;
    }
    public List getNaturalKeyProperties() { List result = new ArrayList(); result.add("kode"); return result; }
    public synchronized void prepareRead(Session session, GenericCrudRequestContext context) throws Exception {
        Number count = (Number) session.createCriteria(NomorSuratAlurPengadaan.class)
                .setProjection(Projections.rowCount()).uniqueResult();
        if (count == null || count.longValue() == 0L) {
            Transaction transaction = session.beginTransaction();
            try {
                for (int i = 0; i < NomorSuratAlurPengadaan.S.length; i++) {
                    String[] parts = NomorSuratAlurPengadaan.S[i].split(";");
                    NomorSuratAlurPengadaan value = new NomorSuratAlurPengadaan();
                    value.setKode(parts[0]); value.setNama(parts[1]); value.setKeterangan(parts[2]); session.save(value);
                }
                transaction.commit();
            } catch (Exception failure) {
                try { transaction.rollback(); } catch (Exception ignored) { } throw failure;
            }
        }
        List values = session.createCriteria(NomorSuratAlurPengadaan.class).list();
        for (int i = 0; i < values.size(); i++) updateCache((NomorSuratAlurPengadaan) values.get(i));
    }
    private void updateCache(NomorSuratAlurPengadaan value) {
        String n = value.getNama();
        if (NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN.equals(n)) NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA = value;
        else if (NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN.equals(n)) NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN_DATA = value;
        else if (NomorSuratAlurPengadaan.PERJANJIAN_KERJASAMA.equals(n)) NomorSuratAlurPengadaan.PERJANJIAN_KERJASAMA_DATA = value;
        else if (NomorSuratAlurPengadaan.PENERIMAAN_PEMBELIAN.equals(n)) NomorSuratAlurPengadaan.PENERIMAAN_PEMBELIAN_DATA = value;
        else if (NomorSuratAlurPengadaan.PENERIMAAN_TAGIHAN.equals(n)) NomorSuratAlurPengadaan.PENERIMAAN_TAGIHAN_DATA = value;
        else if (NomorSuratAlurPengadaan.PEMBAYARAN_PEMBELIAN.equals(n)) NomorSuratAlurPengadaan.PEMBAYARAN_PEMBELIAN_DATA = value;
        else if (NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN.equals(n)) NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN_DATA = value;
        else if (NomorSuratAlurPengadaan.PEMBAYARAN_TERMIN_PEKERJAAN.equals(n)) NomorSuratAlurPengadaan.PEMBAYARAN_TERMIN_PEKERJAAN_DATA = value;
        else if (NomorSuratAlurPengadaan.PEMINJAMAN_BARANG.equals(n)) NomorSuratAlurPengadaan.PEMINJAMAN_BARANG_DATA = value;
        else if (NomorSuratAlurPengadaan.PENGEMBALIAN_BARANG.equals(n)) NomorSuratAlurPengadaan.PENGEMBALIAN_BARANG_DATA = value;
        else if (NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG.equals(n)) NomorSuratAlurPengadaan.PENGHAPUSAN_BARANG_DATA = value;
        else if (NomorSuratAlurPengadaan.PEMAKAIAN_BARANG.equals(n)) NomorSuratAlurPengadaan.PEMAKAIAN_BARANG_DATA = value;
        else if (NomorSuratAlurPengadaan.PINJAMAN_PEGAWAI.equals(n)) NomorSuratAlurPengadaan.PEMINJAMAN_PEGAWAI = value;
        else if (NomorSuratAlurPengadaan.GAJI_PEGAWAI.equals(n)) NomorSuratAlurPengadaan.PEMBAYARAN_GAJI_PEGAWAI = value;
        else if (NomorSuratAlurPengadaan.PENGAJUAN_KPI.equals(n)) NomorSuratAlurPengadaan.PENGAJUAN_KPI_PEGAWAI = value;
        else if (NomorSuratAlurPengadaan.PENYEDIA.equals(n)) NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA = value;
    }
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) { }
}
