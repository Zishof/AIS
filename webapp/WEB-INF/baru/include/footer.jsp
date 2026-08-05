<%@page import="java.util.Calendar"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. Mengambil data perguruan tinggi dengan aman
    PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
    
    // 2. Null safety & Default value yang sangat efisien
    String nama = (pt != null && pt.getNama() != null) ? pt.getNama() : "Enterprise Education";
    String motto = (pt != null && pt.getMotto() != null) ? pt.getMotto() : "";
    String website = (pt != null && pt.getWebsite() != null) ? pt.getWebsite() : "#";
    String email = (pt != null && pt.getEmail() != null) ? pt.getEmail() : "";
    String alamat = (pt != null && pt.getAlamat1() != null) ? pt.getAlamat1() : "";
    
    // 3. Merapikan logika penggabungan kontak (Telp & HP) di ranah Java agar HTML bersih
    String telp = (pt != null && pt.getTelepon() != null) ? pt.getTelepon() : "";
    String hp = (pt != null && pt.getWa() != null) ? pt.getWa() : "";
    String kontakGabungan = telp;
    if (!hp.isEmpty()) {
        kontakGabungan += kontakGabungan.isEmpty() ? hp : (" / " + hp);
    }
    
    int year = Calendar.getInstance().get(Calendar.YEAR);
%>

<div class="clearfix" style="padding-bottom: 2rem;"></div>

<footer class="footer mt-auto py-3 theme-footer border-top transition-base">
    <div class="container-fluid px-4"> 
        <div class="row align-items-center justify-content-between flex-column flex-md-row">
            
            <div class="col-auto text-center text-md-start mb-3 mb-md-0">
                <p class="mb-0 fw-semi-bold text-600 fs--1">
                    &copy; <%=year%> 
                    <a href="https://<%=website%>" target="_blank" rel="noopener noreferrer" class="footer-link text-decoration-none transition-base">
                        <%=nama%>
                    </a>
                </p>
                <% if(!motto.isEmpty()) { %>
                    <p class="mb-0 fs--2 text-500 fst-italic mt-1">
                        <i class="fas fa-quote-left me-1 opacity-50"></i> <%=motto%>
                    </p>
                <% } %>
            </div>

            <div class="col-auto text-center text-md-end">
                <ul class="list-inline mb-0 fs--2 text-500">
                    
                    <% if(!alamat.isEmpty()) { %>
                        <li class="list-inline-item d-block d-xl-inline-block mb-2 mb-xl-0 me-xl-3">
                            <i class="fas fa-map-marker-alt footer-icon me-1"></i> <%=alamat%>
                        </li>
                    <% } %>
                    
                    <% if(!kontakGabungan.isEmpty()) { %>
                        <li class="list-inline-item d-inline-block me-3">
                            <i class="fas fa-phone-alt footer-icon me-1"></i> <%=kontakGabungan%>
                        </li>
                    <% } %>

                    <% if(!email.isEmpty()) { %>
                        <li class="list-inline-item d-inline-block">
                            <i class="fas fa-envelope footer-icon me-1"></i> 
                            <a href="mailto:<%=email%>" class="text-500 text-decoration-none footer-link-hover transition-base"><%=email%></a>
                        </li>
                    <% } %>
                    
                </ul>
            </div>
            
        </div>
    </div>
</footer>