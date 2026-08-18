package ais.action.servlet.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.kpi.ItemKpi;
import ais.database.model.kpi.NilaiKpi;
import ais.database.model.kpi.PenilaianKpi;

/** Tampilan native realisasi KPI untuk akun mahasiswa yang juga terhubung ke pegawai. */
public final class KpiMahasiswaApi {
    private KpiMahasiswaApi() { }

    @SuppressWarnings("unchecked")
    public static JSONObject daftar(HttpServletRequest req, JSONObject request) {
        Tbmuser caller = ApiUtil.currentUser(request, req);
        if (caller == null || caller.getMahasiswa() == null || caller.getMahasiswa().getId() == null) {
            return ApiHelperSupport.status("97", "Token mahasiswa tidak valid atau sudah berakhir.");
        }
        Pegawai tokenEmployee = caller.getPegawai();
        JSONObject result = ApiHelperSupport.status("00", "Data KPI berhasil dimuat.");
        if (tokenEmployee == null || tokenEmployee.getId() == null) {
            try {
                return result.put("tersedia", false)
                        .put("pesan", "Akun mahasiswa ini tidak terhubung dengan data pegawai.")
                        .put("data", new JSONArray());
            } catch (Exception ignored) { return result; }
        }

        Session session = null;
        try {
            session = HibernateUtil.openSession();
            Pegawai employee = (Pegawai) session.get(Pegawai.class, tokenEmployee.getId());
            Criteria assessmentCriteria = session.createCriteria(PenilaianKpi.class)
                    .add(Restrictions.eq("pegawai", employee));
            String year = ApiHelperSupport.optString(request, "tahun").trim();
            if (ApiHelperSupport.hasText(year)) assessmentCriteria.add(Restrictions.eq("ta", year));
            PenilaianKpi assessment = (PenilaianKpi) assessmentCriteria
                    .addOrder(Order.desc("ta")).addOrder(Order.desc("id"))
                    .setMaxResults(1).uniqueResult();
            if (assessment == null) {
                return result.put("tersedia", false)
                        .put("pesan", "Belum ada penilaian KPI untuk periode yang dipilih.")
                        .put("data", new JSONArray());
            }
            List<NilaiKpi> values = session.createCriteria(NilaiKpi.class)
                    .add(Restrictions.eq("penilaianKpi", assessment))
                    .createAlias("itemKpi", "item")
                    .addOrder(Order.asc("item.nomorUrut")).addOrder(Order.asc("item.id")).list();
            JSONArray data = new JSONArray();
            for (NilaiKpi value : values) {
                ItemKpi item = value.getItemKpi();
                data.put(new JSONObject().put("id", value.getId())
                        .put("kode", item == null ? "" : item.getKode())
                        .put("nama", item == null ? "" : item.getNama())
                        .put("target", item == null ? 0.0 : item.getTarget())
                        .put("nilaiInput", value.getValtampil())
                        .put("realisasi", value.getRealisasi())
                        .put("persen", value.getPersen())
                        .put("keterangan", value.getKeterangan()));
            }
            return result.put("tersedia", true).put("tahun", assessment.getTa())
                    .put("target", assessment.getTarget()).put("realisasi", assessment.getNilai())
                    .put("persen", assessment.getPersen()).put("kode", assessment.getKode())
                    .put("terkunci", assessment.getKunci() != null).put("data", data);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return ApiHelperSupport.errorResponse("Data KPI gagal dimuat.");
        } finally { ApiHelperSupport.closeOpenedSession(session); }
    }
}
