<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // Inisialisasi Parameter dengan Default Value yang Aman
    String rnd = (request.getParameter("rnd") == null) ? "" : request.getParameter("rnd");
    
    // Logika Fungsi JavaScript
    String prevPage = (request.getParameter("prevPage") == null) ? "prevPage" + rnd + "();" : request.getParameter("prevPage");
    String resetTimeline = (request.getParameter("resetTimeline") == null) ? "resetTimeline" + rnd + "();" : request.getParameter("resetTimeline");
    String btnNext = (request.getParameter("btnNext") == null) ? "btnNext" + rnd + "();" : request.getParameter("btnNext");
    
    // ID Elemen
    String btnPrevId = (request.getParameter("btnPrevId") == null) ? "btnPrev" + rnd : request.getParameter("btnPrevId"); 
    String btnNextId = (request.getParameter("btnNextId") == null) ? "btnNext" + rnd : request.getParameter("btnNextId"); 


    try {
        // Blok logis aplikasi diletakkan di sini jika ada pengambilan data langsung
%>

<div class="card-header bg-white py-3 shadow-sm border-bottom"> 
    <div class="row align-items-center g-3">
        
        
        <div class="col-lg-10 col-md-12">
            
        </div>
        
        <div class="col-lg-2 col-md-12 text-end">
            <div class="btn-group btn-group-sm shadow-sm" role="group">
                <button type="button" class="btn btn-white border text-secondary" 
                        onclick="<%= prevPage %>" id="<%= btnPrevId %>_foot" disabled 
                        title="<%= Common.getBahasaConfig("Halaman Sebelumnya") %>">
                    <i class="fas fa-chevron-left"></i>
                </button>
                
                <button type="button" class="btn btn-primary" 
                        onclick="<%= resetTimeline %>" 
                        title="<%= Common.getBahasaConfig("Segarkan Data") %>">
                    <i class="fas fa-sync-alt"></i>
                </button>
                
                <button type="button" class="btn btn-white border text-secondary" 
                        onclick="<%= btnNext %>" id="<%= btnNextId %>_foot" disabled 
                        title="<%= Common.getBahasaConfig("Halaman Selanjutnya") %>">
                    <i class="fas fa-chevron-right"></i>
                </button>
            </div>
        </div>
    </div>
</div>

<%
    } catch (Exception e) {
        out.println("");
    } finally {
        // Menutup session atau resource di sini
        // Contoh: if(sessionDb != null) { sessionDb.close(); }
    }
%>