<%-- Role switcher topbar New UI. Tampil hanya bila user punya >1 role.
     Data dari NewUiRoleSwitcherService; POST+CSRF ke _shared/role_switch. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="ais.common.newui.NewUiRoleSwitcherService" %>
<%@ page import="ais.common.newui.NewUiCsrfUtil" %>
<script src="<%=request.getContextPath()%>/js/pesan-formal.js?v=20260906-2"></script>
<%!
    private String h(Object v) {
        if (v == null) return "";
        return String.valueOf(v).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String js(Object v) {
        if (v == null) return "";
        return String.valueOf(v).replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"")
                .replace("<", "\\u003c").replace(">", "\\u003e");
    }
%>
<%
    List<String[]> roles = NewUiRoleSwitcherService.getRoleOptions(request);
    if (roles != null && roles.size() > 1) {
        String csrf = NewUiCsrfUtil.getToken(session);
        String ctx = request.getContextPath();
        String activeName = "";
        for (int i = 0; i < roles.size(); i++) {
            if ("1".equals(roles.get(i)[3])) { activeName = roles.get(i)[1]; break; }
        }
%>
<div class="nui-role-switcher" style="position:relative;margin-right:8px;">
    <button type="button" class="nui-icon-btn" id="nuiRoleBtn" onclick="nuiToggleRoleMenu(event)"
            style="display:flex;align-items:center;gap:6px;max-width:220px;">
        <span>&#8644;</span><span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;"><%=h(activeName)%></span>
    </button>
    <div id="nuiRoleMenu" role="menu"
         style="display:none;position:absolute;right:0;top:120%;background:#0b1220;border:1px solid #1e293b;border-radius:10px;min-width:240px;box-shadow:0 12px 30px rgba(0,0,0,.4);z-index:60;overflow:hidden;">
        <div style="padding:8px 12px;font-size:11px;letter-spacing:.5px;color:#64748b;text-transform:uppercase;">Ganti Peran Aktif</div>
<%
        for (int i = 0; i < roles.size(); i++) {
            String[] r = roles.get(i);
            boolean active = "1".equals(r[3]);
            boolean enabled = "1".equals(r[2]);
            boolean clickable = enabled && !active;
%>
        <a href="#" role="menuitem" onclick="return nuiSwitchRole('<%=js(r[0])%>',<%=clickable%>)"
           style="display:flex;justify-content:space-between;align-items:center;gap:12px;padding:9px 12px;text-decoration:none;color:<%=enabled ? "#e2e8f0" : "#64748b"%>;<%=clickable ? "cursor:pointer;" : "cursor:default;"%>">
            <span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;"><%=h(r[1])%><%= enabled ? "" : " (nonaktif)"%></span>
            <span style="color:#22c55e;"><%= active ? "&#10003;" : ""%></span>
        </a>
<%
        }
%>
    </div>
</div>
<script>
(function(){
    var NUI_CSRF='<%=js(csrf)%>';
    var NUI_URL='<%=js(ctx)%>'+'/new?service=1&module=_shared&page=role_switch';
    window.nuiToggleRoleMenu=function(e){ if(e){e.stopPropagation();} var m=document.getElementById('nuiRoleMenu'); if(m){ m.style.display=(m.style.display==='none'?'block':'none'); } };
    window.nuiSwitchRole=function(roleId,clickable){ if(!clickable){return false;} var x=new XMLHttpRequest(); x.open('POST',NUI_URL,true); x.setRequestHeader('Content-Type','application/x-www-form-urlencoded'); x.setRequestHeader('X-NUI-CSRF',NUI_CSRF); x.onreadystatechange=function(){ if(x.readyState===4){ if(x.status===200){ window.location.reload(); } else { alert('Gagal mengganti peran aktif.'); } } }; x.send('roleId='+encodeURIComponent(roleId)+'&nui_csrf='+encodeURIComponent(NUI_CSRF)); return false; };
    document.addEventListener('click',function(){ var m=document.getElementById('nuiRoleMenu'); if(m){ m.style.display='none'; } });
})();
</script>
<% } %>
