package ais.common.newui.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.koperasi.PembagianShu;
import ais.database.model.koperasi.ShuAnggota;
import ais.database.model.koperasi.TransaksiKoperasi;

/** Implementasi headless dari perhitungan PembagianShuAction, tanpa komponen ZUL. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiPembagianShuService {

    public Result load(int year, Koperasi koperasi) {
        Session session = HibernateUtil.openSession();
        try { return load(session, year, koperasi); }
        finally { session.close(); }
    }

    public Result calculate(int year, double total, double[] percentages, Koperasi koperasi) {
        validate(year, total, percentages, koperasi);
        Session session = HibernateUtil.openSession(); Transaction tx = null;
        try {
            tx = session.beginTransaction();
            PembagianShu head = find(session, year, koperasi);
            if (head == null) { head = new PembagianShu(); head.setTahun(Integer.valueOf(year)); head.setKoperasi(koperasi); }
            head.setTotalShu(Double.valueOf(total));
            head.setPersenCadangan(Double.valueOf(percentages[0]));
            head.setPersenJasaModal(Double.valueOf(percentages[1]));
            head.setPersenJasaUsaha(Double.valueOf(percentages[2]));
            head.setPersenPendidikan(Double.valueOf(percentages[3]));
            head.setPersenPengurus(Double.valueOf(percentages[4]));
            head.setPersenSosial(Double.valueOf(percentages[5]));
            head.setPersenLain(Double.valueOf(percentages[6]));
            head.setStatus(PembagianShu.STATUS_DIBAGIKAN);
            Common.refreshSaveOrUpdate(session, head); session.flush();
            session.createQuery("delete from ShuAnggota s where s.pembagianShu.id=:id")
                    .setParameter("id", head.getId()).executeUpdate();

            Map<Long, Double> savings = new HashMap<Long, Double>();
            Map<Long, Double> participation = new HashMap<Long, Double>();
            Map<Long, AnggotaKoperasi> members = new HashMap<Long, AnggotaKoperasi>();
            Long savingType = ConstantValues.SIMPANAN == null ? null : ConstantValues.SIMPANAN.getId();
            Long loanType = ConstantValues.PINJAMAN == null ? null : ConstantValues.PINJAMAN.getId();
            if (savingType != null) accumulate(session, savingType, false, koperasi, savings, members);
            if (loanType != null) accumulate(session, loanType, true, koperasi, participation, members);
            double allSavings = sum(savings), allParticipation = sum(participation);
            for (Map.Entry<Long, AnggotaKoperasi> entry : members.entrySet()) {
                Long id = entry.getKey(); double memberSavings = value(savings, id);
                double memberParticipation = value(participation, id);
                double capital = allSavings <= 0 ? 0 : memberSavings / allSavings * head.getNominalJasaModal();
                double business = allParticipation <= 0 ? 0 : memberParticipation / allParticipation * head.getNominalJasaUsaha();
                ShuAnggota detail = new ShuAnggota(); detail.setPembagianShu(head); detail.setAnggota(entry.getValue());
                detail.setTotalSimpanan(Double.valueOf(memberSavings)); detail.setTotalTransaksi(Double.valueOf(memberParticipation));
                detail.setJasaModal(Double.valueOf(capital)); detail.setJasaUsaha(Double.valueOf(business));
                detail.setTotalShu(Double.valueOf(capital + business)); detail.setSudahDibayar(Boolean.FALSE); session.save(detail);
            }
            session.flush(); tx.commit(); return load(session, year, koperasi);
        } catch (RuntimeException error) {
            if (tx != null) try { tx.rollback(); } catch (Exception ignored) { }
            throw error;
        } finally { session.close(); }
    }

    private void accumulate(Session session, Long type, boolean activeOnly, Koperasi koperasi,
            Map<Long, Double> values, Map<Long, AnggotaKoperasi> members) {
        String hql = "select distinct t from TransaksiKoperasi t left join fetch t.anggotaKoperasi a left join fetch t.produkKoperasi p where p.tipeProdukKoperasi.id=:type";
        if (koperasi != null && koperasi.getId() != null) hql += " and p.koperasi.id=:koperasi";
        Query query = session.createQuery(hql).setParameter("type", type);
        if (koperasi != null && koperasi.getId() != null) query.setParameter("koperasi", koperasi.getId());
        List rows = query.list();
        for (int i = 0; i < rows.size(); i++) {
            TransaksiKoperasi transaction = (TransaksiKoperasi) rows.get(i);
            if (activeOnly && !transaction.getAktif()) continue;
            AnggotaKoperasi member = transaction.getAnggotaKoperasi();
            if (member == null || member.getId() == null) continue;
            double amount = activeOnly ? transaction.getMargin() : transaction.getNilai();
            Long id = member.getId(); values.put(id, Double.valueOf(value(values, id) + amount)); members.put(id, member);
        }
    }

    private Result load(Session session, int year, Koperasi koperasi) {
        PembagianShu head = find(session, year, koperasi); List<Row> rows = new ArrayList<Row>(); double distributed = 0;
        if (head != null) {
            List details = session.createQuery("select distinct s from ShuAnggota s left join fetch s.anggota a where s.pembagianShu.id=:id order by s.totalShu desc")
                    .setParameter("id", head.getId()).list();
            for (int i = 0; i < details.size(); i++) { ShuAnggota item = (ShuAnggota) details.get(i); Row row = new Row(item); rows.add(row); distributed += row.total; }
        }
        return new Result(year, head, rows, distributed);
    }

    private PembagianShu find(Session session, int year, Koperasi koperasi) {
        String hql = "from PembagianShu p where p.tahun=:year";
        if (koperasi != null && koperasi.getId() != null) hql += " and p.koperasi.id=:koperasi";
        Query query = session.createQuery(hql + " order by p.id desc").setParameter("year", Integer.valueOf(year)).setMaxResults(1);
        if (koperasi != null && koperasi.getId() != null) query.setParameter("koperasi", koperasi.getId());
        return (PembagianShu) query.uniqueResult();
    }

    private void validate(int year, double total, double[] values, Koperasi koperasi) {
        if (koperasi == null || koperasi.getId() == null) throw new IllegalArgumentException("Koperasi aktif tidak tersedia.");
        if (year < 1900 || year > 2200) throw new IllegalArgumentException("Tahun buku tidak valid.");
        if (total <= 0) throw new IllegalArgumentException("Total SHU harus lebih dari nol.");
        if (values == null || values.length != 7) throw new IllegalArgumentException("Tujuh persentase alokasi wajib diisi.");
        double sum = 0; for (int i = 0; i < values.length; i++) { if (values[i] < 0 || values[i] > 100) throw new IllegalArgumentException("Persentase harus 0 sampai 100."); sum += values[i]; }
        if (Math.abs(sum - 100.0) > 0.0001) throw new IllegalArgumentException("Total persentase alokasi harus tepat 100%.");
    }

    private static double sum(Map<Long, Double> values) { double result = 0; for (Double value : values.values()) result += value == null ? 0 : value.doubleValue(); return result; }
    private static double value(Map<Long, Double> values, Long key) { Double value = values.get(key); return value == null ? 0 : value.doubleValue(); }

    public static final class Result {
        public final int year; public final PembagianShu head; public final List<Row> rows; public final double distributed;
        Result(int year, PembagianShu head, List<Row> rows, double distributed) { this.year=year; this.head=head; this.rows=rows; this.distributed=distributed; }
    }
    public static final class Row {
        public final Long id; public final String member; public final double savings, participation, capital, business, total; public final boolean paid;
        Row(ShuAnggota value) { id=value.getId(); AnggotaKoperasi a=value.getAnggota(); member=a==null||a.getNama()==null?"-":a.getNama(); savings=value.getTotalSimpanan(); participation=value.getTotalTransaksi(); capital=value.getJasaModal(); business=value.getJasaUsaha(); total=value.getTotalShu(); paid=value.getSudahDibayar(); }
    }
}
