package ais.common.newui.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.DepositoAroScheduler;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.DepositoRolloverKoperasi;
import ais.database.model.koperasi.TransaksiKoperasi;

/** Headless parity untuk {@code DepositoAroKoperasiAction}. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiDepositoAroService {
    private NewUiDepositoAroService() { }

    public static Dashboard load() {
        Session session = HibernateUtil.currentSession();
        List rollovers = session.createCriteria(DepositoRolloverKoperasi.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .addOrder(Order.asc("tanggalJatuhTempo")).list();
        List ids = new ArrayList();
        for (int i = 0; i < rollovers.size(); i++) {
            Long id = ((DepositoRolloverKoperasi) rollovers.get(i)).getTransaksiKoperasiId();
            if (id != null && !ids.contains(id)) ids.add(id);
        }
        Map transactions = new HashMap();
        if (!ids.isEmpty()) {
            List values = session.createQuery("select t from TransaksiKoperasi t "
                    + "left join fetch t.anggotaKoperasi a left join fetch t.produkKoperasi p "
                    + "where t.id in (:ids)").setParameterList("ids", ids).list();
            for (int i = 0; i < values.size(); i++) {
                TransaksiKoperasi value = (TransaksiKoperasi) values.get(i);
                if (value != null && value.getId() != null) transactions.put(value.getId(), value);
            }
        }
        Dashboard result = new Dashboard();
        for (int i = 0; i < rollovers.size(); i++) {
            DepositoRolloverKoperasi rollover = (DepositoRolloverKoperasi) rollovers.get(i);
            TransaksiKoperasi transaction = (TransaksiKoperasi) transactions.get(rollover.getTransaksiKoperasiId());
            Row row = new Row(); row.id = rollover.getId(); row.transactionId = rollover.getTransaksiKoperasiId();
            row.member = transaction == null || transaction.getAnggotaKoperasi() == null
                    ? "-" : safe(transaction.getAnggotaKoperasi().getNama());
            row.product = transaction == null || transaction.getProdukKoperasi() == null
                    ? "-" : safe(transaction.getProdukKoperasi().getNama());
            row.nominal = transaction == null || transaction.getNilai() == null ? 0D
                    : transaction.getNilai().doubleValue();
            row.dueDate = rollover.getTanggalJatuhTempo();
            row.extensionCount = rollover.getJumlahPerpanjangan() == null ? 0
                    : rollover.getJumlahPerpanjangan().intValue();
            row.automatic = Boolean.TRUE.equals(rollover.getAroOtomatis());
            row.status = rollover.getStatus(); row.statusLabel = statusLabel(rollover.getStatus());
            result.rows.add(row);
            if (DepositoRolloverKoperasi.STATUS_JATUH_TEMPO.equals(row.status)) {
                result.dueCount++; result.dueValue += row.nominal;
            } else if (row.automatic) { result.automaticCount++; result.automaticValue += row.nominal; }
        }
        result.rows = Collections.unmodifiableList(result.rows);
        return result;
    }

    public static void toggle(Long id, boolean enabled) throws Exception {
        if (id == null) throw new IllegalArgumentException("ID deposito wajib diisi.");
        Session session = HibernateUtil.currentSession(); Transaction transaction = session.getTransaction();
        boolean own = transaction == null || !transaction.isActive(); if (own) transaction = session.beginTransaction();
        try {
            DepositoRolloverKoperasi value = (DepositoRolloverKoperasi) session.get(DepositoRolloverKoperasi.class, id);
            if (value == null) throw new IllegalArgumentException("Data ARO tidak ditemukan.");
            value.setAroOtomatis(Boolean.valueOf(enabled));
            if (enabled && DepositoRolloverKoperasi.STATUS_JATUH_TEMPO.equals(value.getStatus())) {
                value.setStatus(DepositoRolloverKoperasi.STATUS_BERJALAN);
            }
            session.saveOrUpdate(value); if (own) transaction.commit();
        } catch (Exception error) {
            if (own && transaction != null && transaction.isActive()) transaction.rollback(); throw error;
        }
    }

    public static int[] processNow() throws Exception { return DepositoAroScheduler.jalankanSekali(); }

    private static String safe(String value) { return value == null || value.trim().length() == 0 ? "-" : value; }
    private static String statusLabel(String value) {
        if (DepositoRolloverKoperasi.STATUS_JATUH_TEMPO.equals(value)) return "Jatuh Tempo (cairkan)";
        if (DepositoRolloverKoperasi.STATUS_DICAIRKAN.equals(value)) return "Dicairkan";
        return "Berjalan";
    }

    public static final class Dashboard implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<Row> rows = new ArrayList<Row>();
        public int automaticCount, dueCount; public double automaticValue, dueValue;
        public List<Row> getRows() { return rows; }
    }
    public static final class Row implements Serializable {
        private static final long serialVersionUID = 1L;
        public Long id, transactionId; public String member, product, status, statusLabel;
        public double nominal; public java.util.Date dueDate; public int extensionCount; public boolean automatic;
    }
}
