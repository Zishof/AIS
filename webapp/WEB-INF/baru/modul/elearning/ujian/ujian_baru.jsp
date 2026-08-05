<%@page import="ais.common.GenCodeHelper"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        return;
    }

    String itemId = request.getParameter("pertemuan");
    if (itemId == null || itemId.trim().isEmpty()) {
        out.print("<div class='alert alert-warning'>" + Common.getBahasaConfig("Parameter pertemuan tidak ditemukan") + "</div>");
        return;
    }

    Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, itemId.trim(), true);
    if (pertemuan == null || pertemuan.getId() == null || !pertemuan.getAktif()) {
        out.print("<div class='alert alert-warning'>" + Common.getBahasaConfig("Data pertemuan tidak valid") + "</div>");
        return;
    }

    String contentModal = request.getParameter("contentModal");
    String rnd = (contentModal == null || contentModal.trim().isEmpty()) ? Common.getGeneratedBarCode(7) : contentModal;
    String afterSave = request.getParameter("afterSave") == null ? "" : request.getParameter("afterSave").trim();

    String tglMulai = "";
    String tglSelesai = "";
    if (pertemuan.getTanggal() != null) {
        java.text.SimpleDateFormat dtFmt = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        tglMulai = dtFmt.format(pertemuan.getTanggal());
        tglSelesai = dtFmt.format(pertemuan.getTanggal());
    }
%>

<div class="col-lg-12" id="form_ujian_baru_<%=rnd%>">
    <input type="hidden" name="pertemuan" id="pertemuan" value="<%=pertemuan.getId()%>">

    <div class="card shadow border-0 rounded-3">
        <div class="card-header bg-white border-bottom p-4 d-flex justify-content-between align-items-center">
            <h4 class="mb-0 fw-bold text-danger">
                <i class="fas fa-file-signature me-2"></i><%=Common.getBahasaConfig("Sesi Ujian Baru")%>
            </h4>
        </div>

        <div class="card-body p-4">
            <form>
                <div class="mb-4">
                    <label class="form-label fw-semibold">
                        <%=Common.getBahasaConfig("Nama / Keterangan Ujian")%> <span class="text-danger">*</span>
                    </label>
                    <input type="text" class="form-control form-control-lg" id="keterangan" name="keterangan"
                           placeholder="<%=Common.getBahasaConfig("Contoh: Ujian Tengah Semester, Kuis Pertemuan 3")%>" value="">
                </div>

                <div class="row g-3 mb-4">
                    <div class="col-md-6">
                        <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Mulai Ujian")%></label>
                        <div class="input-group">
                            <span class="input-group-text bg-light"><i class="fas fa-calendar-plus text-success"></i></span>
                            <input type="datetime-local" class="form-control" id="mulaiUjian" name="mulaiUjian" value="<%=tglMulai%>">
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Selesai Ujian")%></label>
                        <div class="input-group">
                            <span class="input-group-text bg-light"><i class="fas fa-calendar-check text-danger"></i></span>
                            <input type="datetime-local" class="form-control" id="sampaiUjian" name="sampaiUjian" value="<%=tglSelesai%>">
                        </div>
                    </div>
                </div>

                <div class="row g-3 mb-4">
                    <div class="col-md-4">
                        <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Durasi (HH:mm)")%></label>
                        <div class="input-group">
                            <span class="input-group-text bg-light"><i class="fas fa-stopwatch text-warning"></i></span>
                            <input type="time" class="form-control" id="lama" name="lama" value="00:30">
                        </div>
                        <small class="text-muted"><%=Common.getBahasaConfig("Lama waktu pengerjaan")%></small>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label fw-semibold"><%=Common.getBahasaConfig("Jumlah Soal Ditampilkan")%></label>
                        <div class="input-group">
                            <span class="input-group-text bg-light"><i class="fas fa-list-ol text-primary"></i></span>
                            <input type="number" class="form-control" id="jmlDitampilkan" name="jmlDitampilkan" min="1" value="">
                        </div>
                        <small class="text-muted"><%=Common.getBahasaConfig("Kosongkan = tampilkan semua soal")%></small>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label fw-semibold">&nbsp;</label>
                        <div class="border rounded p-3 bg-light d-flex flex-column gap-2">
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" id="random" name="random" value="true">
                                <label class="form-check-label fw-semibold" for="random">
                                    <i class="fas fa-random me-1 text-info"></i><%=Common.getBahasaConfig("Acak Soal")%>
                                </label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" id="dibatasiWaktu" name="dibatasiWaktu" value="true" checked>
                                <label class="form-check-label fw-semibold" for="dibatasiWaktu">
                                    <i class="fas fa-clock me-1 text-warning"></i><%=Common.getBahasaConfig("Batasi Waktu")%>
                                </label>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="alert alert-info d-flex align-items-center gap-2 mb-0">
                    <i class="fas fa-info-circle fa-lg"></i>
                    <div><%=Common.getBahasaConfig("Setelah sesi ujian dibuat, tambahkan soal melalui menu Daftar Soal di halaman ujian.")%></div>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
<%
    String d = GenCodeHelper.simpanDataHelper(
        "form_ujian_baru_" + rnd,
        PertemuanPunyaUjian.class,
        "",
        new Class[]{},
        new String[]{},
        rnd,
        afterSave,
        new String[]{"keterangan"},
        new String[]{Common.getBahasaConfig("Nama / Keterangan Ujian")}
    );
    out.println(d);
%>
</script>
