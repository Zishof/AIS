<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
String[] _c = { "ais.database.model.sekolah.Penghargaan", "ais.database.model.Penghargaan" };
Class _cls = null;
for (String _cn : _c) { try { _cls = Class.forName(_cn); break; } catch (ClassNotFoundException _e) { ais.common.ErrorAuditUtil.record(_e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersekolahpenghargaanzul/index.jsp:5");} }
if (_cls != null) {
    out.println(ais.common.DynamicJspCrudGenerator.generate(_cls));
} else {
    out.println(
        "<div style='padding:2rem;text-align:center;color:var(--color-text-secondary)'>" +
        "<i class='fas fa-tools' style='font-size:2rem;margin-bottom:.75rem;display:block;opacity:.35' aria-hidden='true'></i>" +
        "<h5 style='margin:0 0 .4rem;font-weight:500;color:var(--color-text-primary)'>Penghargaan</h5>" +
        "<p style='margin:0;font-size:.875rem'>Halaman ini belum sepenuhnya dikonversi ke tampilan baru.</p>" +
        "</div>"
    );
}
%>