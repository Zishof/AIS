<%@page session="false"%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%
response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
try {
    Tbmuser actor = Common.getCurrentUser(request);
    if (actor == null || actor.getUserId() == null) { result.put("status","91"); result.put("description","Sesi login tidak valid."); out.print(result.toString()); return; }
    String memberId = request.getParameter("memberId");
    String modality = request.getParameter("modality");
    String probe = request.getParameter("probe_base64");
    String format = request.getParameter("template_format");
    String referenceId = request.getParameter("referenceId");
    if (memberId == null || modality == null || probe == null || probe.trim().length() == 0) {
        result.put("status","91"); result.put("description","Data capture biometrik belum lengkap."); out.print(result.toString()); return;
    }
    Session session = HibernateUtil.currentNativeSession();
    AnggotaKoperasi member = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, Long.valueOf(memberId.trim()));
    if (member == null) { result.put("status","91"); result.put("description","Member tidak ditemukan."); out.print(result.toString()); return; }
    String subject = ais.action.servlet.api.BiometricApi.linkedUserIdForMember(session, member);
    if (subject == null) { result.put("status","91"); result.put("description","Member belum terhubung ke akun biometrik."); out.print(result.toString()); return; }

    JSONObject payload = new JSONObject();
    payload.put("modality", modality.trim().toUpperCase());
    payload.put("probe_base64", probe.trim());
    payload.put("template_format", format == null ? "" : format.trim());
    payload.put("liveness_score", request.getParameter("liveness_score") == null ? 0D : Double.parseDouble(request.getParameter("liveness_score")));
    payload.put("captured_at_epoch", request.getParameter("captured_at_epoch") == null ? System.currentTimeMillis() : Long.parseLong(request.getParameter("captured_at_epoch")));
    payload.put("reference_type", "POS_PURCHASE");
    payload.put("reference_id", referenceId == null ? "" : referenceId.trim());
    payload.put("clientMutationId", "pos-biometric-web-" + modality.trim().toLowerCase() + "-" + (referenceId == null ? "" : referenceId.trim()));
    result = ais.action.servlet.api.BiometricApi.verifyLinkedSubject(actor, subject, payload, "POS_PURCHASE");
} catch (Exception e) {
    ais.common.ErrorAuditUtil.record(e, "verifikasi_biometric_service.jsp");
    result = new JSONObject(); result.put("status","99"); result.put("description","Verifikasi biometrik belum dapat diproses.");
}
out.print(result.toString());
%>
