package ais.action.servlet.api;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.LogMobile;
import ais.database.model.Tbmuser;

/**
 * Logger terpusat untuk request/response API mobile.
 * Dipisahkan dari servlet agar bisa direuse dan agar penanganan transaksi konsisten.
 */
public final class ApiMobileLogger {

    private ApiMobileLogger() {
    }

    public static void save(HttpServletRequest request, JSONObject jsonObject, String responseBody) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            Tbmuser users = ApiUtil.currentUser(jsonObject, request);
            LogMobile log = buildLog(request, jsonObject, responseBody, users);

            transaction = session.beginTransaction();
            session.save(log);
            transaction.commit();
        } catch (Exception e) {
            ApiHelperSupport.rollbackQuietly(transaction);
        } finally {
            ApiHelperSupport.closeOpenedSession(session);
        }
    }

    private static LogMobile buildLog(HttpServletRequest request, JSONObject jsonObject, String responseBody, Tbmuser users) {
        LogMobile log = new LogMobile();
        log.setSessionid(request == null ? null : request.getRequestedSessionId());
        log.setDosen(users == null ? null : users.getDosen());
        log.setPegawai(users == null ? null : users.getPegawai());
        log.setFakultas(users != null && users.getDosen() != null ? users.getDosen().getFakultas()
                : users == null ? null : users.ambilFakultas());
        log.setIp(ApiHelperSupport.getClientIp(request));
        log.setJurusan(users != null && users.getDosen() != null ? users.getDosen().getJurusan()
                : users == null ? null : users.getJurusan());
        log.setKeterangan("");
        log.setLogin(ais.ui.util.WaktuUtil.getDate());
        log.setMahasiswa(users == null ? null : users.getMahasiswa());
        log.setSiswa(users == null ? null : users.getSiswa());
        log.setNama(ApiHelperSupport.optString(jsonObject, "action"));
        log.setSekolah(users == null ? null : users.getSekolah());
        log.setYayasan(users == null ? null : users.getYayasan());
        log.setTbmuser(users);
        log.setLinkProfile(ApiHelperSupport.truncate(redactSensitive(jsonObject).toString()));
        log.setHostname(request == null ? "" : request.getServerName());
        log.setSuccess_status(Boolean.TRUE);
        log.setHeader(ApiHelperSupport.truncate(responseBody));
        return log;
    }

    /** Template/probe biometrik tidak boleh masuk LogMobile walau mode debug. */
    private static JSONObject redactSensitive(JSONObject source) {
        if (source == null) return new JSONObject();
        try {
            JSONObject safe = new JSONObject(source.toString());
            String[] keys = new String[] { "template_base64", "probe_base64", "biometric_template" };
            for (int i = 0; i < keys.length; i++) {
                if (safe.has(keys[i])) safe.put(keys[i], "[REDACTED]");
            }
            return safe;
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
