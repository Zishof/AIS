<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.action.master.sekolah.util.SekolahUtil"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.action.master.TampilanELearningAction"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. Inisialisasi Parameter & Proteksi Logika
    String rnd = (request.getParameter("rnd") == null) ? "" : request.getParameter("rnd").toString();
    String judulRaw = (request.getParameter("judul") == null) ? "Daftar Data" : request.getParameter("judul").toString();
    String faIcon = (request.getParameter("faIcon") == null) ? "fas fa-layer-group" : request.getParameter("faIcon").toString();
    
    // 2. Definisi Fungsi JavaScript
    String prevPage = (request.getParameter("prevPage") == null) ? "prevPage" + rnd + "();" : request.getParameter("prevPage");
    String resetTimeline = (request.getParameter("resetTimeline") == null) ? "resetTimeline" + rnd + "();" : request.getParameter("resetTimeline");
    String btnNext = (request.getParameter("btnNext") == null) ? "btnNext" + rnd + "();" : request.getParameter("btnNext");

    // 3. ID Elemen & State
    String btnPrevId = (request.getParameter("btnPrevId") == null) ? "btnPrev" + rnd : request.getParameter("btnPrevId");
    String btnNextId = (request.getParameter("btnNextId") == null) ? "btnNext" + rnd : request.getParameter("btnNextId");
    String searchKeyId = (request.getParameter("searchKey") == null) ? "searchKey" + rnd : request.getParameter("searchKey");
    boolean tampilsmtTa = request.getParameter("tampilsmtTa") == null ? true : Boolean.parseBoolean(request.getParameter("tampilsmtTa"));
    boolean isSemuaAda = request.getParameter("semuaAda") == null ? true : Boolean.parseBoolean(request.getParameter("semuaAda"));
    Sekolah sekolah = SekolahUtil.getSekolah(request);
    boolean isSekolah = (sekolah != null && sekolah.getId() != null);
    
    String taCUrrent = Common.getCurrentTahunAkademik();
    Boolean sekarangGajil = Common.isNowSemensterGanjil();

    try {
        // Blok pengambilan data atau inisialisasi session database diletakkan di sini
%>

<div class="card-header bg-white border-0 shadow-sm rounded-4 p-3 mb-3">
    <div class="row align-items-center g-3">
        
        <div class="col-xl-3 col-lg-4 col-md-12">
            <div class="d-flex align-items-center">
                <div class="p-2 bg-primary bg-gradient text-white rounded-3 shadow-sm me-3">
                    <i class="<%= faIcon %> fa-fw"></i>
                </div>
                <div>
                    <h6 class="text-muted small mb-0"><%= Common.getBahasaConfig("Modul") %></h6>
                    <h5 class="fw-bold text-dark mb-0"><%= Common.getBahasaConfig(judulRaw) %></h5>
                </div>
            </div>
        </div>

        <div class="col-xl-7 col-lg-6 col-md-12">
            <div class="row g-2">
                <div class="col-md-<%=tampilsmtTa ? "4" : "12"%>">
                    <div class="input-group input-group-sm">
                        <span class="input-group-text bg-white border-primary text-primary border-end-0 rounded-start-pill">
                            <i class="fas fa-search"></i>
                        </span>
                        <input type="text" id="<%= searchKeyId %>" name="<%= searchKeyId %>"
                            class="form-control border-primary border-start-0 rounded-end-pill shadow-none"
                            placeholder="<%= Common.getBahasaConfig("Pencarian...") %>"
                            onkeypress="if(event.keyCode == 13) { <%= resetTimeline %> }">
                    </div>
                </div>

				<%
				if(tampilsmtTa){
				%>
                <div class="col-md-3">
                    <select class="form-select form-select-sm border-info rounded-pill shadow-none" 
                            onchange="<%= resetTimeline %>" id="filterTahun<%= rnd %>">
                        <option value=""><%= Common.getBahasaConfig("Tahun Akademik") %></option>
                        <% if (Common.tahunAngkatans != null) { 
                            for (String ta : Common.tahunAngkatans) { %>
                                <option value="<%= ta %>"><%= ta %></option>
                        <%  } 
                           } %>
                    </select>
                </div>

                <div class="col-md-2">
                    <select class="form-select form-select-sm border-info rounded-pill shadow-none" 
                            onchange="<%= resetTimeline %>" id="filterSemester<%= rnd %>">
                        <option value=""><%= Common.getBahasaConfig("Semester") %></option>
                        <option value="<%= Perkuliahan.GANJIL %>"><%= Common.getBahasaConfig("Ganjil") %></option>
                        <option value="<%= Perkuliahan.GENAP %>"><%= Common.getBahasaConfig("Genap") %></option>
                        <option value="<%= Perkuliahan.SP %>"><%= Common.getBahasaConfig("Pendek") %></option>
                    </select>
                </div>
                
                

                <div class="col-md-3">
                    <select class="form-select form-select-sm border-info rounded-pill shadow-none" 
                            id="filterJenis<%= rnd %>" onchange="<%= resetTimeline %>">
                        <% if (isSemuaAda) { %>
                            <option value="" selected><%= Common.getBahasaConfig("Semua Kategori") %></option>
                        <% } %>
                        <% if (isSekolah) { %>
                            <option <%= (!isSemuaAda ? "selected" : "") %> value="<%= TampilanELearningAction.PELAJARAN %>"><%= Common.getBahasaConfig("Mata Pelajaran") %></option>
                            <option value="<%= TampilanELearningAction.KEGIATAN %>"><%= Common.getBahasaConfig("Kegiatan Ekstrakurikuler") %></option>
                        <% } else { %>
                            <option <%= (!isSemuaAda ? "selected" : "") %> value="<%= TampilanELearningAction.PERKULIAHAN %>"><%= Common.getBahasaConfig("Perkuliahan") %></option>
                            <option value="<%= TampilanELearningAction.SKRIPSI %>"><%= Common.getBahasaConfig("Ujian Akhir") %></option>
                            <option value="<%= TampilanELearningAction.BIMBINGAN %>"><%= Common.getBahasaConfig("Bimbingan") %></option>
                            <option value="<%= TampilanELearningAction.KKN %>"><%= Common.getBahasaConfig("KKN") %></option>
                            <option value="<%= TampilanELearningAction.PKL %>"><%= Common.getBahasaConfig("Praktik Lapangan") %></option>
                            <option value="<%= TampilanELearningAction.KRS %>"><%= Common.getBahasaConfig("Rencana Studi") %></option>
                            <option value="<%= TampilanELearningAction.KEGIATAN %>"><%= Common.getBahasaConfig("Kegiatan Akademik") %></option>
                            <option value="<%= TampilanELearningAction.WISUDA %>"><%= Common.getBahasaConfig("Kelulusan") %></option>
                        <% } %>
                    </select>
                </div>
                <%
				} else {
                %>
                	<input type="hidden" id="filterJenis<%= rnd %>" value="">
                	<input type="hidden" id="filterTahun<%= rnd %>" value="">
                	<input type="hidden" id="filterSemester<%= rnd %>" value="">
                <%} %>
            </div>
        </div>

        <div class="col-xl-2 col-lg-2 col-md-12 text-end">
            <div class="btn-group shadow-sm rounded-pill overflow-hidden">
                <button type="button" class="btn btn-light border-0 px-3"
                    onclick="<%= prevPage %>" id="<%= btnPrevId %>" disabled
                    title="<%= Common.getBahasaConfig("Halaman Sebelumnya") %>">
                    <i class="fas fa-chevron-left text-primary"></i>
                </button>

                <button type="button" class="btn btn-primary bg-gradient px-3 border-0"
                    onclick="<%= resetTimeline %>"
                    title="<%= Common.getBahasaConfig("Sinkronisasi Data") %>">
                    <i class="fas fa-sync-alt"></i>
                </button>

                <button type="button" class="btn btn-light border-0 px-3"
                    onclick="<%= btnNext %>" id="<%= btnNextId %>" disabled
                    title="<%= Common.getBahasaConfig("Halaman Selanjutnya") %>">
                    <i class="fas fa-chevron-right text-primary"></i>
                </button>
            </div>
        </div>
    </div>
</div>

<%
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/_pencarian.jsp:148"); 
%>
    <div class="alert alert-custom bg-danger text-white border-0 shadow-sm rounded-3">
        <i class="fas fa-exclamation-circle me-2"></i>
        <%= Common.getBahasaConfig("Sistem gagal memproses data navigasi.") %>
    </div>
<%
    } finally {
        // Blok Penutupan Session Database (Contoh)
        // if(dbConnection != null) { dbConnection.close(); }
    }
%>