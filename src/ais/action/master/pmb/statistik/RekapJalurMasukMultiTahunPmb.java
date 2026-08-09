package ais.action.master.pmb.statistik;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.database.model.Paket;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyHtml;

/**
 * <h1>RekapJalurMasukMultiTahunPmb — Rekap Jumlah Mahasiswa Baru Lintas Tahun</h1>
 *
 * <p>Tabel multi-tahun (4 tahun terakhir) yang merekap data PMB per Jenis Seleksi.
 * Semua data diambil langsung dari {@code BiodataCalonMahasiswa} (BCM) tanpa
 * memerlukan input manual.</p>
 *
 * <p>Kolom per tahun:</p>
 * <ul>
 *   <li><b>PIL 1–4</b> — jumlah BCM yang mengisi prodi1/2/3/4 (pilihan program studi)</li>
 *   <li><b>JUMLAH</b> — total BCM unik untuk jalur ini (peminat mendaftar)</li>
 *   <li><b>JML LULUS</b> — BCM yang {@code prodiLulus} terisi (diterima)</li>
 *   <li><b>ISI FORMULIR DAFTAR ULANG</b> — BCM yang {@code pembayaranDaftarUlang} terisi</li>
 *   <li><b>PEMBAYARAN</b> — BCM yang {@code pembayaranRegistrasi} terisi</li>
 * </ul>
 *
 * <p>Filter tambahan di filter bar:</p>
 * <ul>
 *   <li><b>Paket</b> — mempersempit data ke gelombang/paket tertentu</li>
 *   <li><b>Prodi</b> — mempersempit data ke pilihan program studi (Jurusan) tertentu</li>
 * </ul>
 *
 * <p>Ketika filter Prodi dipilih: PIL 1 = pilih prodi ini sebagai pilihan 1,
 * PIL 2 = pilih prodi ini sebagai pilihan 2, dst.
 * JUMLAH = total BCM yang memilih prodi ini di pilihan mana pun.</p>
 *
 * @see DashboardPmbBase
 */
public class RekapJalurMasukMultiTahunPmb extends DashboardPmbBase {

    private static final int JUMLAH_TAHUN = 4;

    private Combobox cboPaket;
    private Combobox cboJurusan;

    // ─── Inner DTO ────────────────────────────────────────────────────────────

    private static final class RowData {
        /** PIL 1 = count prodi1 not null (atau = jurusan filter) */
        int pil1;
        /** PIL 2 = count prodi2 */
        int pil2;
        /** PIL 3 = count prodi3 */
        int pil3;
        /** PIL 4 = count prodi4 */
        int pil4;
        /** JUMLAH = total BCM unik untuk jalur ini (tanpa filter prodi = total peminat) */
        int jumlah;
        int lulus;
        int formulir;
        int bayar;
    }

    // ─── Konstruktor ─────────────────────────────────────────────────────────

    public RekapJalurMasukMultiTahunPmb(PerguruanTinggi pt) {
        super(pt);
    }

    // ─── Filter tambahan ─────────────────────────────────────────────────────

    @Override
    protected void buildExtraFilter(Div filterBar) {
        // Separator visual
        Label sep = new Label("  |");
        sep.setStyle("color:#d1d5db;margin-left:6px;margin-right:6px;");
        sep.setParent(filterBar);

        // Paket
        Label lblPaket = new Label("Paket:");
        lblPaket.setStyle("font-size:12px;color:#6b7280;white-space:nowrap;");
        lblPaket.setParent(filterBar);

        cboPaket = buildComboPaket();
        cboPaket.setParent(filterBar);

        // Prodi
        Label lblProdi = new Label("Prodi:");
        lblProdi.setStyle("font-size:12px;color:#6b7280;white-space:nowrap;margin-left:6px;");
        lblProdi.setParent(filterBar);

        cboJurusan = buildComboJurusan();
        cboJurusan.setParent(filterBar);
    }

    @SuppressWarnings("unchecked")
    private Combobox buildComboPaket() {
        Combobox cbo = new Combobox();
        cbo.setWidth("160px");
        cbo.setReadonly(true);

        Comboitem ciSemua = new MyComboitemConfig();
        ciSemua.setLabel("Semua Paket");
        ciSemua.setValue(null);
        cbo.appendChild(ciSemua);
        cbo.setSelectedItem(ciSemua);

        try {
            List<Paket> list = HibernateUtil.currentSession()
                    .createQuery("FROM Paket ORDER BY nama ASC").list();
            for (Paket p : list) {
                Comboitem ci = new MyComboitemConfig();
                ci.setLabel(p.getNama() != null ? p.getNama() : p.getId().toString());
                ci.setValue(p.getId());
                cbo.appendChild(ci);
            }
        } catch (Exception ex) {
            logErr("buildComboPaket", ex);
        }

        return cbo;
    }

    @SuppressWarnings("unchecked")
    private Combobox buildComboJurusan() {
        Combobox cbo = new Combobox();
        cbo.setWidth("200px");
        cbo.setReadonly(true);

        Comboitem ciSemua = new MyComboitemConfig();
        ciSemua.setLabel("Semua Prodi");
        ciSemua.setValue(null);
        cbo.appendChild(ciSemua);
        cbo.setSelectedItem(ciSemua);

        try {
            List<Jurusan> list = HibernateUtil.currentSession()
                    .createQuery("FROM Jurusan WHERE aktif = true ORDER BY nama ASC").list();
            for (Jurusan j : list) {
                Comboitem ci = new MyComboitemConfig();
                ci.setLabel(j.getNama() != null ? j.getNama() : j.getId().toString());
                ci.setValue(j.getId());
                cbo.appendChild(ci);
            }
        } catch (Exception ex) {
            logErr("buildComboJurusan", ex);
        }

        return cbo;
    }

    private Object getSelectedPaketId() {
        if (cboPaket != null && cboPaket.getSelectedItem() != null) {
            return cboPaket.getSelectedItem().getValue();
        }
        return null;
    }

    private Object getSelectedJurusanId() {
        if (cboJurusan != null && cboJurusan.getSelectedItem() != null) {
            return cboJurusan.getSelectedItem().getValue();
        }
        return null;
    }

    // ─── doRefresh ───────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    protected void doRefresh(Session session, String ta, String sem) {
        List<String> years   = computeYears(ta);
        Object paketId   = getSelectedPaketId();
        Object jurusanId = getSelectedJurusanId();

        // Semua jenis seleksi aktif
        List<JenisSeleksi> allJs = new ArrayList();
        try {
            allJs = session.createQuery(
                    "FROM JenisSeleksi WHERE aktif = true "
                    + "ORDER BY nomorUrut ASC NULLS LAST, nama ASC")
                    .list();
        } catch (Exception ex) {
            logErr("loadJenisSeleksi", ex);
        }

        if (allJs.isEmpty()) {
            new MyHtml(fullWidth(emptyCard("Rekap Jumlah Mahasiswa Baru",
                    "Belum ada data Jenis Seleksi aktif."))).setParent(contentHolder);
            return;
        }

        // Inisialisasi map: tahunAkademik → jsId → RowData
        Map<String, Map<Object, RowData>> data = new LinkedHashMap();
        for (String yr : years) {
            Map<Object, RowData> inner = new LinkedHashMap();
            for (JenisSeleksi js : allJs) {
                inner.put(js.getId(), new RowData());
            }
            data.put(yr, inner);
        }

        // Jalankan query paralel (sequential dalam satu request)
        loadPil(session, years, paketId, jurusanId, data, 1);
        loadPil(session, years, paketId, jurusanId, data, 2);
        loadPil(session, years, paketId, jurusanId, data, 3);
        loadPil(session, years, paketId, jurusanId, data, 4);
        loadJumlah(session, years, paketId, jurusanId, data);
        loadLulus(session, years, paketId, jurusanId, data);
        loadFormulir(session, years, paketId, data);
        loadBayar(session, years, paketId, data);

        // Label filter aktif untuk judul tabel
        String filterInfo = buildFilterInfo(cboPaket, cboJurusan);
        new MyHtml(buildTableHtml(allJs, years, data, ta, filterInfo)).setParent(contentHolder);
    }

    // ─── Query helpers ────────────────────────────────────────────────────────

    /**
     * PIL slot (1–4) = COUNT BCM yang mengisi prodi{slot}.
     * Jika jurusanId diset: hitung yang memilih jurusan tsb sebagai pilihan ke-{slot}.
     */
    @SuppressWarnings("unchecked")
    private void loadPil(Session session, List<String> years,
            Object paketId, Object jurusanId,
            Map<String, Map<Object, RowData>> data, int slot) {
        String prodiField = "bcm.prodi" + slot;
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("SELECT bcm.jenisSeleksi.id, bcm.tahunAkademik, COUNT(bcm) ")
               .append("FROM BiodataCalonMahasiswa bcm ")
               .append("WHERE bcm.tahunAkademik IN (:years) AND bcm.jenisSeleksi IS NOT NULL ");
			hql.append(jenjangClause());
            if (jurusanId != null) {
                hql.append("AND ").append(prodiField).append(".id = :jid ");
            } else {
                hql.append("AND ").append(prodiField).append(" IS NOT NULL ");
            }
            if (paketId != null) {
                hql.append("AND bcm.paket.id = :pid ");
            }
            hql.append("GROUP BY bcm.jenisSeleksi.id, bcm.tahunAkademik");

            org.hibernate.Query q = session.createQuery(hql.toString())
                    .setParameterList("years", years);
            if (jurusanId != null) q.setParameter("jid", jurusanId);
            if (paketId != null)   q.setParameter("pid", paketId);
			applyJenjangParam(q);

            for (Object obj : q.list()) {
                Object[] r   = (Object[]) obj;
                Object   jsId = r[0];
                String   yr   = (String) r[1];
                int      cnt  = r[2] == null ? 0 : ((Number) r[2]).intValue();
                RowData rd = getRowData(data, yr, jsId);
                if (rd == null) continue;
                if (slot == 1) rd.pil1 = cnt;
                else if (slot == 2) rd.pil2 = cnt;
                else if (slot == 3) rd.pil3 = cnt;
                else rd.pil4 = cnt;
            }
        } catch (Exception ex) {
            logErr("loadPil" + slot, ex);
        }
    }

    /**
     * JUMLAH = total BCM unik per (jenisSeleksi, tahunAkademik).
     * Jika jurusanId diset: total BCM yang memilih prodi tsb di pilihan mana pun.
     */
    @SuppressWarnings("unchecked")
    private void loadJumlah(Session session, List<String> years,
            Object paketId, Object jurusanId,
            Map<String, Map<Object, RowData>> data) {
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("SELECT bcm.jenisSeleksi.id, bcm.tahunAkademik, COUNT(bcm) ")
               .append("FROM BiodataCalonMahasiswa bcm ")
               .append("WHERE bcm.tahunAkademik IN (:years) AND bcm.jenisSeleksi IS NOT NULL ");
			hql.append(jenjangClause());
            if (jurusanId != null) {
                hql.append("AND (bcm.prodi1.id = :jid OR bcm.prodi2.id = :jid ")
                   .append("OR bcm.prodi3.id = :jid OR bcm.prodi4.id = :jid) ");
            }
            if (paketId != null) {
                hql.append("AND bcm.paket.id = :pid ");
            }
            hql.append("GROUP BY bcm.jenisSeleksi.id, bcm.tahunAkademik");

            org.hibernate.Query q = session.createQuery(hql.toString())
                    .setParameterList("years", years);
            if (jurusanId != null) q.setParameter("jid", jurusanId);
            if (paketId != null)   q.setParameter("pid", paketId);
			applyJenjangParam(q);

            for (Object obj : q.list()) {
                Object[] r   = (Object[]) obj;
                RowData  rd  = getRowData(data, (String) r[1], r[0]);
                if (rd != null) rd.jumlah = r[2] == null ? 0 : ((Number) r[2]).intValue();
            }
        } catch (Exception ex) {
            logErr("loadJumlah", ex);
        }
    }

    /**
     * JML LULUS = BCM dengan prodiLulus terisi.
     * Jika jurusanId diset: prodiLulus = jurusan tsb.
     */
    @SuppressWarnings("unchecked")
    private void loadLulus(Session session, List<String> years,
            Object paketId, Object jurusanId,
            Map<String, Map<Object, RowData>> data) {
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("SELECT bcm.jenisSeleksi.id, bcm.tahunAkademik, COUNT(bcm) ")
               .append("FROM BiodataCalonMahasiswa bcm ")
               .append("WHERE bcm.tahunAkademik IN (:years) AND bcm.jenisSeleksi IS NOT NULL ");
			hql.append(jenjangClause());
            if (jurusanId != null) {
                hql.append("AND bcm.prodiLulus.id = :jid ");
            } else {
                hql.append("AND bcm.prodiLulus IS NOT NULL ");
            }
            if (paketId != null) {
                hql.append("AND bcm.paket.id = :pid ");
            }
            hql.append("GROUP BY bcm.jenisSeleksi.id, bcm.tahunAkademik");

            org.hibernate.Query q = session.createQuery(hql.toString())
                    .setParameterList("years", years);
            if (jurusanId != null) q.setParameter("jid", jurusanId);
            if (paketId != null)   q.setParameter("pid", paketId);
			applyJenjangParam(q);

            for (Object obj : q.list()) {
                Object[] r  = (Object[]) obj;
                RowData  rd = getRowData(data, (String) r[1], r[0]);
                if (rd != null) rd.lulus = r[2] == null ? 0 : ((Number) r[2]).intValue();
            }
        } catch (Exception ex) {
            logErr("loadLulus", ex);
        }
    }

    /** ISI FORMULIR DAFTAR ULANG = pembayaranDaftarUlang terisi. */
    @SuppressWarnings("unchecked")
    private void loadFormulir(Session session, List<String> years,
            Object paketId, Map<String, Map<Object, RowData>> data) {
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("SELECT bcm.jenisSeleksi.id, bcm.tahunAkademik, COUNT(bcm) ")
               .append("FROM BiodataCalonMahasiswa bcm ")
               .append("WHERE bcm.tahunAkademik IN (:years) AND bcm.jenisSeleksi IS NOT NULL ")
               .append("AND bcm.pembayaranDaftarUlang IS NOT NULL ");
			hql.append(jenjangClause());
            if (paketId != null) hql.append("AND bcm.paket.id = :pid ");
            hql.append("GROUP BY bcm.jenisSeleksi.id, bcm.tahunAkademik");

            org.hibernate.Query q = session.createQuery(hql.toString())
                    .setParameterList("years", years);
            if (paketId != null) q.setParameter("pid", paketId);
			applyJenjangParam(q);

            for (Object obj : q.list()) {
                Object[] r  = (Object[]) obj;
                RowData  rd = getRowData(data, (String) r[1], r[0]);
                if (rd != null) rd.formulir = r[2] == null ? 0 : ((Number) r[2]).intValue();
            }
        } catch (Exception ex) {
            logErr("loadFormulir", ex);
        }
    }

    /** PEMBAYARAN = pembayaranRegistrasi terisi. */
    @SuppressWarnings("unchecked")
    private void loadBayar(Session session, List<String> years,
            Object paketId, Map<String, Map<Object, RowData>> data) {
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("SELECT bcm.jenisSeleksi.id, bcm.tahunAkademik, COUNT(bcm) ")
               .append("FROM BiodataCalonMahasiswa bcm ")
               .append("WHERE bcm.tahunAkademik IN (:years) AND bcm.jenisSeleksi IS NOT NULL ")
               .append("AND bcm.pembayaranRegistrasi IS NOT NULL ");
			hql.append(jenjangClause());
            if (paketId != null) hql.append("AND bcm.paket.id = :pid ");
            hql.append("GROUP BY bcm.jenisSeleksi.id, bcm.tahunAkademik");

            org.hibernate.Query q = session.createQuery(hql.toString())
                    .setParameterList("years", years);
            if (paketId != null) q.setParameter("pid", paketId);
			applyJenjangParam(q);

            for (Object obj : q.list()) {
                Object[] r  = (Object[]) obj;
                RowData  rd = getRowData(data, (String) r[1], r[0]);
                if (rd != null) rd.bayar = r[2] == null ? 0 : ((Number) r[2]).intValue();
            }
        } catch (Exception ex) {
            logErr("loadBayar", ex);
        }
    }

    private RowData getRowData(Map<String, Map<Object, RowData>> data, String yr, Object jsId) {
        Map<Object, RowData> inner = data.get(yr);
        if (inner == null) return null;
        return inner.get(jsId);
    }

    // ─── HTML table builder ───────────────────────────────────────────────────

    private String buildTableHtml(List<JenisSeleksi> allJs, List<String> years,
            Map<String, Map<Object, RowData>> data, String ta, String filterInfo) {

        // Kolom per tahun: PIL1 + PIL2 + PIL3 + PIL4 + JUMLAH + LULUS + FORMULIR + BAYAR = 8
        final int CPY = 8;

        String thBase = "border:1px solid #dee2e6;padding:5px 7px;text-align:center;"
                + "font-size:11px;white-space:nowrap;color:#fff;";
        String th1 = thBase + "background:#1e3a5f;";
        String th2 = thBase + "background:#2e5a9f;";
        String th3 = thBase + "background:#3b6fc9;";
        String tdC = "border:1px solid #dee2e6;padding:4px 6px;text-align:center;font-size:11px;";
        String tdL = "border:1px solid #dee2e6;padding:4px 6px;text-align:left;font-size:11px;white-space:nowrap;";
        String tdFC = "border:1px solid #dee2e6;padding:4px 6px;text-align:center;font-size:11px;font-weight:700;background:#f0f4ff;";
        String tdFL = "border:1px solid #dee2e6;padding:4px 6px;text-align:left;font-size:11px;font-weight:700;background:#f0f4ff;";

        StringBuilder sb = new StringBuilder(8192);

        // ── Header area ──────────────────────────────────────────────────────
        sb.append("<div style='overflow-x:auto;'>")
          .append("<div style='font-size:16px;font-weight:700;color:#1e3a5f;margin-bottom:4px;'>")
          .append("Rekap Jumlah Mahasiswa Baru &#8212; ").append(escHtml(ta)).append("</div>");

        if (!filterInfo.isEmpty()) {
            sb.append("<div style='font-size:11px;color:#3b6fc9;margin-bottom:6px;'>")
              .append(escHtml(filterInfo)).append("</div>");
        }

        sb.append("<div style='font-size:11px;color:#9ca3af;margin-bottom:10px;'>")
          .append("PIL 1–4 = pilihan program studi (prodi1–prodi4 di BCM). ")
          .append("Tanpa filter Prodi: jumlah BCM yang mengisi slot pilihan tersebut. ")
          .append("Dengan filter Prodi: jumlah yang memilih prodi spesifik pada slot tersebut.")
          .append("</div>");

        // ── Tabel ────────────────────────────────────────────────────────────
        sb.append("<table style='border-collapse:collapse;min-width:max-content;'>")
          .append("<thead>");

        // Header baris 1: NO | JENIS SELEKSI | [Tahun colspan=CPY ...]
        sb.append("<tr>")
          .append("<th rowspan='3' style='").append(th1).append("min-width:35px;'>NO</th>")
          .append("<th rowspan='3' style='").append(th1).append("min-width:130px;'>JENIS SELEKSI</th>");
        for (String yr : years) {
            sb.append("<th colspan='").append(CPY).append("' style='").append(th1).append("'>")
              .append(escHtml(yearLabel(yr))).append("</th>");
        }
        sb.append("</tr>");

        // Header baris 2: PEMINAT(5) | LULUS | FORMULIR | BAYAR
        sb.append("<tr>");
        for (int i = 0; i < years.size(); i++) {
            sb.append("<th colspan='5' style='").append(th2).append("'>PEMINAT</th>")
              .append("<th rowspan='2' style='").append(th2).append("'>JML<br/>LULUS</th>")
              .append("<th rowspan='2' style='").append(th2).append("'>ISI FORMULIR<br/>DAFTAR ULANG</th>")
              .append("<th rowspan='2' style='").append(th2).append("'>PEMBAYARAN</th>");
        }
        sb.append("</tr>");

        // Header baris 3: PIL 1 | PIL 2 | PIL 3 | PIL 4 | JUMLAH
        sb.append("<tr>");
        for (int i = 0; i < years.size(); i++) {
            sb.append("<th style='").append(th3).append("'>PIL 1</th>")
              .append("<th style='").append(th3).append("'>PIL 2</th>")
              .append("<th style='").append(th3).append("'>PIL 3</th>")
              .append("<th style='").append(th3).append("'>PIL 4</th>")
              .append("<th style='").append(th3).append("'>JUMLAH</th>");
        }
        sb.append("</tr></thead><tbody>");

        // Total per tahun: [pil1, pil2, pil3, pil4, jumlah, lulus, formulir, bayar]
        Map<String, int[]> totals = new LinkedHashMap();
        for (String yr : years) totals.put(yr, new int[8]);

        int no = 0;
        for (int i = 0; i < allJs.size(); i++) {
            JenisSeleksi js = allJs.get(i);

            // Skip baris tanpa data sama sekali
            boolean adaData = false;
            outer:
            for (String yr : years) {
                RowData rd = getRowData(data, yr, js.getId());
                if (rd != null && (rd.jumlah > 0 || rd.lulus > 0 || rd.pil1 > 0)) {
                    adaData = true; break outer;
                }
            }
            if (!adaData) continue;

            no++;
            sb.append("<tr>")
              .append("<td style='").append(tdC).append("'>").append(no).append("</td>")
              .append("<td style='").append(tdL).append("'>").append(escHtml(js.getNama())).append("</td>");

            for (String yr : years) {
                RowData rd = getRowData(data, yr, js.getId());
                if (rd == null) rd = new RowData();

                int[] tot = totals.get(yr);
                tot[0] += rd.pil1; tot[1] += rd.pil2;
                tot[2] += rd.pil3; tot[3] += rd.pil4;
                tot[4] += rd.jumlah; tot[5] += rd.lulus;
                tot[6] += rd.formulir; tot[7] += rd.bayar;

                sb.append("<td style='").append(tdC).append("'>").append(dash(rd.pil1)).append("</td>")
                  .append("<td style='").append(tdC).append("'>").append(dash(rd.pil2)).append("</td>")
                  .append("<td style='").append(tdC).append("'>").append(dash(rd.pil3)).append("</td>")
                  .append("<td style='").append(tdC).append("'>").append(dash(rd.pil4)).append("</td>")
                  .append("<td style='").append(tdC).append("font-weight:600;'>")
                  .append(dash(rd.jumlah)).append("</td>")
                  .append("<td style='").append(tdC).append("'>").append(dash(rd.lulus)).append("</td>")
                  .append("<td style='").append(tdC).append("'>").append(dash(rd.formulir)).append("</td>")
                  .append("<td style='").append(tdC).append("'>").append(dash(rd.bayar)).append("</td>");
            }
            sb.append("</tr>");
        }

        // Baris total
        sb.append("<tr>")
          .append("<td colspan='2' style='").append(tdFL).append("'>JUMLAH</td>");
        for (String yr : years) {
            int[] t = totals.get(yr);
            sb.append("<td style='").append(tdFC).append("'>").append(fmtAngka(t[0])).append("</td>")
              .append("<td style='").append(tdFC).append("'>").append(fmtAngka(t[1])).append("</td>")
              .append("<td style='").append(tdFC).append("'>").append(fmtAngka(t[2])).append("</td>")
              .append("<td style='").append(tdFC).append("'>").append(fmtAngka(t[3])).append("</td>")
              .append("<td style='").append(tdFC).append("'>").append(fmtAngka(t[4])).append("</td>")
              .append("<td style='").append(tdFC).append("'>").append(fmtAngka(t[5])).append("</td>")
              .append("<td style='").append(tdFC).append("'>").append(fmtAngka(t[6])).append("</td>")
              .append("<td style='").append(tdFC).append("'>").append(fmtAngka(t[7])).append("</td>");
        }
        sb.append("</tr></tbody></table></div>");

        return sb.toString();
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    /** Hitung {@code JUMLAH_TAHUN} tahun akademik berurutan yang berakhir di {@code ta}. */
    private List<String> computeYears(String ta) {
        List<String> result = new ArrayList();
        if (ta == null || !ta.contains("/")) {
            result.add(ta != null ? ta : "");
            return result;
        }
        int idx = ta.indexOf('/');
        try {
            int y1 = Integer.parseInt(ta.substring(0, idx).trim());
            int y2 = Integer.parseInt(ta.substring(idx + 1).trim());
            for (int i = JUMLAH_TAHUN - 1; i >= 0; i--) {
                result.add((y1 - i) + "/" + (y2 - i));
            }
        } catch (NumberFormatException ex) {
            result.add(ta);
        }
        return result;
    }

    /** "2025/2026" → "2025" */
    private String yearLabel(String ta) {
        if (ta == null) return "-";
        int idx = ta.indexOf('/');
        return idx > 0 ? ta.substring(0, idx).trim() : ta;
    }

    /** 0 → "-", singganya angka biasa. */
    private static String dash(int n) {
        return n == 0 ? "-" : String.valueOf(n);
    }

    /** Deskripsi singkat filter aktif untuk subtitle tabel. */
    private static String buildFilterInfo(Combobox cboPaket, Combobox cboJurusan) {
        StringBuilder sb = new StringBuilder();
        if (cboPaket != null && cboPaket.getSelectedItem() != null
                && cboPaket.getSelectedItem().getValue() != null) {
            sb.append("Paket: ").append(cboPaket.getSelectedItem().getLabel());
        }
        if (cboJurusan != null && cboJurusan.getSelectedItem() != null
                && cboJurusan.getSelectedItem().getValue() != null) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append("Prodi: ").append(cboJurusan.getSelectedItem().getLabel());
        }
        return sb.toString();
    }
}
