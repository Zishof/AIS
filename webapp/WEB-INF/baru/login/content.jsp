<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.common.ConstantValues"%>
<%@ page import="ais.common.Common" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    if(session.getAttribute("mytbmuser") != null){
        response.sendRedirect(request.getContextPath() +"/main");
        return;
    }
    
    // Optimasi pengambilan data agar lebih rapi
    PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
    String judul = pt.getNama();
    String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_login_perguruanTinggi_");
    String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
%>

<div class="container-fluid">
    <div class="row min-vh-100">
        <div class="col-lg-6 d-none d-lg-block p-0">
            <div class="h-100 w-100" style="background-image: url('<%=background_PerguruanTinggi %>'); background-size: cover; background-position: center;">
                <div class="h-100 w-100" style="background: rgba(0,0,0,0.2);"></div>
            </div>
        </div>

        <div class="col-lg-6 d-flex align-items-center justify-content-center bg-light">
            <div class="col-md-8 col-lg-7 col-xl-6 py-5">
                
                <div class="text-center mb-4">
                    <img src="<%=logo_PerguruanTinggi %>" alt="Logo" class="img-fluid mb-3" style="max-height: 80px;">
                    <h4 class="fw-bold text-primary"><%=judul %></h4>
                </div>

                <div class="card shadow-sm border-0">
                    <div class="card-body p-4 p-sm-5">
                        <div class="mb-4">
                            <h3 class="fw-bold">Login</h3>
                            <p class="text-muted small">Silakan masuk menggunakan akun Anda.</p>
                        </div>

                        <% if (request.getParameter("login_error") != null) { %>
                            <div class="alert alert-danger d-flex align-items-center p-2 mb-3" role="alert">
                                <small>
                                    <% try {
                                        out.println(org.jsoup.Jsoup.parse(request.getParameter("login_error")).text());
                                    } catch(Exception e) { 
                                        out.print("Gagal login, periksa kembali akun Anda.");
                                    } %>
                                </small>
                            </div>
                        <% } %>

                        <form name="f" action="j_spring_security_check" method="POST">
                            <div class="form-floating mb-3">
                                <input type="text" class="form-control" id="j_username" name="j_username" placeholder="Username" required>
                                <label for="j_username"><%= Common.getBahasaConfig("Username") %></label>
                            </div>
                            
                            <div class="form-floating mb-3">
                                <input type="password" class="form-control" id="j_password" name="j_password" placeholder="Password" required>
                                <label for="j_password"><%= Common.getBahasaConfig("Password") %></label>
                            </div>

                            <% if (ConstantValues.aktifkanRememeberMe) { %>
                            <div class="form-check mb-3">
                                <input class="form-check-input" type="checkbox" id="rememberMe" name="rememberMe" value="true">
                                <label class="form-check-label text-muted small" for="rememberMe" style="cursor: pointer;">
                                    <%= Common.getBahasaConfig("Ingat akun saya di browser ini") %>
                                </label>
                            </div>
                            <% } %>

                            <button class="btn btn-primary w-100 py-2 fw-bold" type="submit">
                                <%= Common.getBahasaConfig("Masuk Ke Sistem") %>
                            </button>
                        </form>

                        <div class="text-center my-4">
                            <div class="position-relative">
                                <hr class="text-300">
                                <div class="position-absolute top-50 start-50 translate-middle px-3 bg-white text-muted small">
                                    atau masuk via
                                </div>
                            </div>
                        </div>

                        <div class="d-grid">
                            <a class="btn btn-outline-danger d-flex align-items-center justify-content-center py-2" href="<%=request.getContextPath()%>/google.zul">
                                <span class="fab fa-google me-2"></span> Google Account
                            </a>
                        </div>
                    </div>
                </div>
                
                <div class="mt-4 text-center">
                    <p class="text-muted small">&copy; 2024 <%=judul %>. All Rights Reserved.</p>
                </div>
            </div>
        </div>
    </div>
</div>