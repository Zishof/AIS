package ais.common.newui.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.StokThresholdScheduler;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.AmbangStokGudang;
import ais.database.model.inventory.PengajuanPembelianGudang;
import ais.database.model.inventory.Produk;
import ais.database.model.sirs.Gudang;

/** Headless parity PengajuanPembelianGudangAction, tanpa komponen ZK/ZUL. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiPengajuanPembelianGudangService {
    public Snapshot load() {
        Session session = HibernateUtil.openSession();
        try {
            List thresholds = session.createCriteria(AmbangStokGudang.class)
                    .createAlias("produk", "produk")
                    .createAlias("gudang", "gudang")
                    .addOrder(Order.desc("id")).list();
            List requests = session.createCriteria(PengajuanPembelianGudang.class)
                    .createAlias("produk", "produk")
                    .createAlias("gudangAsal", "asal")
                    .addOrder(Order.desc("id")).setMaxResults(200).list();
            List products = session.createCriteria(Produk.class)
                    .add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.asc("nama")).list();
            List warehouses = session.createCriteria(Gudang.class)
                    .add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.asc("nama")).list();
            Snapshot result = new Snapshot();
            for (int i = 0; i < thresholds.size(); i++) result.thresholds.add(new ThresholdRow((AmbangStokGudang) thresholds.get(i)));
            for (int i = 0; i < requests.size(); i++) result.requests.add(new RequestRow((PengajuanPembelianGudang) requests.get(i)));
            for (int i = 0; i < products.size(); i++) result.products.add(new Option((Produk) products.get(i)));
            for (int i = 0; i < warehouses.size(); i++) result.warehouses.add(new Option((Gudang) warehouses.get(i)));
            return result;
        } finally { session.close(); }
    }

    public void saveThreshold(Long id, Long productId, Long warehouseId, double minimum,
            boolean active, String note, Tbmuser user) {
        if (productId == null || warehouseId == null) throw new IllegalArgumentException("Produk dan gudang wajib dipilih.");
        if (Double.isNaN(minimum) || Double.isInfinite(minimum) || minimum < 0)
            throw new IllegalArgumentException("Ambang minimum harus angka nol atau lebih.");
        Session session = HibernateUtil.openSession(); Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Produk product = (Produk) session.get(Produk.class, productId);
            Gudang warehouse = (Gudang) session.get(Gudang.class, warehouseId);
            if (product == null || !Boolean.TRUE.equals(product.getAktif())) throw new IllegalArgumentException("Produk aktif tidak ditemukan.");
            if (warehouse == null || !Boolean.TRUE.equals(warehouse.getAktif())) throw new IllegalArgumentException("Gudang aktif tidak ditemukan.");
            AmbangStokGudang value = id == null ? new AmbangStokGudang()
                    : (AmbangStokGudang) session.get(AmbangStokGudang.class, id);
            if (value == null) throw new IllegalArgumentException("Ambang stok tidak ditemukan.");
            value.setProduk(product); value.setGudang(warehouse); value.setAmbangMinimum(Double.valueOf(minimum));
            value.setAktif(Boolean.valueOf(active)); value.setKeterangan(clean(note)); stamp(value, user);
            Common.refreshSaveOrUpdate(session, value); session.flush(); tx.commit();
        } catch (RuntimeException error) {
            if (tx != null) try { tx.rollback(); } catch (Exception ignored) { }
            throw error;
        } finally { session.close(); }
    }

    public void deleteThreshold(Long id) {
        if (id == null) throw new IllegalArgumentException("ID ambang stok wajib diisi.");
        Session session = HibernateUtil.openSession(); Transaction tx = null;
        try {
            tx = session.beginTransaction(); AmbangStokGudang value = (AmbangStokGudang) session.get(AmbangStokGudang.class, id);
            if (value == null) throw new IllegalArgumentException("Ambang stok tidak ditemukan.");
            session.delete(value); session.flush(); tx.commit();
        } catch (RuntimeException error) {
            if (tx != null) try { tx.rollback(); } catch (Exception ignored) { }
            throw error;
        } finally { session.close(); }
    }

    public void updateStatus(Long id, String status, Tbmuser user) {
        if (id == null) throw new IllegalArgumentException("ID pengajuan wajib diisi.");
        status = canonicalStatus(status);
        Session session = HibernateUtil.openSession(); Transaction tx = null;
        try {
            tx = session.beginTransaction(); PengajuanPembelianGudang value = (PengajuanPembelianGudang) session.get(PengajuanPembelianGudang.class, id);
            if (value == null) throw new IllegalArgumentException("Pengajuan pembelian tidak ditemukan.");
            value.setStatus(status); stamp(value, user); session.update(value); session.flush(); tx.commit();
        } catch (RuntimeException error) {
            if (tx != null) try { tx.rollback(); } catch (Exception ignored) { }
            throw error;
        } finally { session.close(); }
    }

    public int runNow() { return StokThresholdScheduler.jalankanSekali(); }

    public static String canonicalStatus(String status) {
        String value = status == null ? "" : status.trim().toUpperCase();
        if (PengajuanPembelianGudang.STATUS_BARU.equals(value)
                || PengajuanPembelianGudang.STATUS_DIPROSES.equals(value)
                || PengajuanPembelianGudang.STATUS_SELESAI.equals(value)
                || PengajuanPembelianGudang.STATUS_DIBATALKAN.equals(value)) return value;
        throw new IllegalArgumentException("Status pengajuan tidak valid.");
    }

    private void stamp(AmbangStokGudang value, Tbmuser user) {
        if (user == null) return; value.setOleh(user.getUserNama()); value.setOlehId(user.getUserId());
    }
    private void stamp(PengajuanPembelianGudang value, Tbmuser user) {
        if (user == null) return; value.setOleh(user.getUserNama()); value.setOlehId(user.getUserId());
    }
    private String clean(String value) { return value == null || value.trim().length() == 0 ? null : value.trim(); }
    private static double number(Double value) { return value == null ? 0 : value.doubleValue(); }
    private static String label(Object value) { return value == null ? "-" : String.valueOf(value); }

    public static final class Snapshot {
        public final List<ThresholdRow> thresholds = new ArrayList<ThresholdRow>();
        public final List<RequestRow> requests = new ArrayList<RequestRow>();
        public final List<Option> products = new ArrayList<Option>();
        public final List<Option> warehouses = new ArrayList<Option>();
    }
    public static final class Option {
        public final Long id; public final String label;
        Option(Produk value) { id=value.getId(); label=value.getKode()+" - "+value.getNama(); }
        Option(Gudang value) { id=value.getId(); label=value.getKode()+" - "+value.getNama(); }
    }
    public static final class ThresholdRow {
        public final Long id, productId, warehouseId; public final String product, warehouse, note;
        public final double minimum; public final boolean active;
        ThresholdRow(AmbangStokGudang value) { id=value.getId(); productId=value.getProduk().getId(); warehouseId=value.getGudang().getId();
            product=label(value.getProduk().getNama()); warehouse=label(value.getGudang().getNama()); minimum=number(value.getAmbangMinimum()); active=Boolean.TRUE.equals(value.getAktif()); note=value.getKeterangan(); }
    }
    public static final class RequestRow {
        public final Long id; public final String product, source, destination, status, note; public final double quantity, stock;
        public final boolean automatic; public final Date created;
        RequestRow(PengajuanPembelianGudang value) { id=value.getId(); product=label(value.getProduk().getNama()); source=label(value.getGudangAsal().getNama());
            destination=value.getGudangTujuan()==null?"Vendor Eksternal":label(value.getGudangTujuan().getNama()); quantity=number(value.getQtyDiminta()); stock=number(value.getStokSaatDiajukan());
            status=value.getStatus(); automatic=Boolean.TRUE.equals(value.getOtomatis()); created=value.getWaktuDibuat(); note=value.getKeterangan(); }
    }
}
