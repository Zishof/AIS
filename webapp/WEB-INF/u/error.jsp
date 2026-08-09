
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
private String errorH(Object value){if(value==null)return "";return String.valueOf(value).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>
<%
Object status=request.getAttribute("javax.servlet.error.status_code");
Object uri=request.getAttribute("javax.servlet.error.request_uri");
Object message=request.getAttribute("javax.servlet.error.message");
Throwable throwable=(Throwable)request.getAttribute("ais.error.throwable");
String trace=(String)request.getAttribute("ais.error.trace");
Object logId=request.getAttribute("ais.error.log_id");
String content=(String)request.getAttribute("ais.error.content");
boolean showDetail=Boolean.TRUE.equals(request.getAttribute("ais.error.show_detail"));
%>
<!doctype html><html lang="id"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Error aplikasi</title>
<style>body{font-family:Arial,sans-serif;background:#f8fafc;color:#334155;margin:0;padding:24px}.error-box{max-width:980px;margin:auto;background:#fff;border:1px solid #fecaca;border-radius:16px;padding:24px;box-shadow:0 16px 40px rgba(15,23,42,.08)}h1{color:#b91c1c;margin-top:0}.meta{display:grid;grid-template-columns:150px 1fr;gap:8px 14px}.meta b{color:#475569}.trace{font-family:monospace;background:#f1f5f9;padding:3px 7px;border-radius:6px;word-break:break-all}details{margin-top:18px}pre{white-space:pre-wrap;overflow:auto;background:#0f172a;color:#e2e8f0;padding:16px;border-radius:10px;font-size:12px}.actions{margin-top:20px}.actions a{display:inline-block;padding:10px 14px;border-radius:8px;background:#2563eb;color:#fff;text-decoration:none}@media(max-width:600px){.meta{grid-template-columns:1fr}.error-box{padding:18px}}</style></head><body><section class="error-box">
<h1>Terjadi error saat memuat halaman</h1><p>Error sudah dicatat. Gunakan ID pelacakan berikut untuk mencocokkan halaman dengan log server.</p>
<div class="meta"><b>Trace ID</b><span class="trace"><%=errorH(trace)%></span><b>HTTP status</b><span><%=errorH(status)%></span><b>Request URI</b><span><%=errorH(uri)%></span><b>Jenis error</b><span><%=errorH(throwable==null?"Tidak diketahui":throwable.getClass().getName())%></span><b>Pesan</b><span><%=errorH(throwable!=null&&throwable.getMessage()!=null?throwable.getMessage():message)%></span><b>Error Log ID</b><span><%=errorH(logId==null?"Tidak tersimpan / duplikat":logId)%></span></div>
<%if(showDetail&&content!=null){%><details><summary>Stack trace lengkap</summary><pre><%=errorH(content)%></pre></details><%}else{%><p><small>Stack trace lengkap tersimpan di log server. Aktifkan konfigurasi <code>global_error_tampilkan_stacktrace_ui</code> hanya saat diagnosis terkontrol.</small></p><%}%>
<div class="actions"><a href="<%=request.getContextPath()%>/main">Kembali ke halaman utama</a></div></section></body></html>
