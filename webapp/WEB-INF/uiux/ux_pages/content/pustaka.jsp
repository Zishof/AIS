<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
ais.database.model.sekolah.Sekolah sekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();
String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
		"background_perguruanTinggi_");
String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
		"logo_perguruanTinggi_");
Tbmuser tbmuser = Common.getCurrentUser(request);
String namaPengguna = tbmuser == null ? "" : tbmuser.getUserNama();
String jenisPenguna = tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleName();
String urlFoto = tbmuser == null ? "" : CommonMedia.getUrlFotoPengguna(tbmuser);
String linkFoto = (urlFoto == null || urlFoto.isEmpty()
		? request.getContextPath() + "/component/adminlte/assets/img/user2-160x160.jpg"
		: urlFoto);
%>

		<main class="app-main"> <!--begin::App Content Header-->
            <div class="app-content-header"> <!--begin::Container-->
                <div class="container-fluid"> <!--begin::Row-->
                    <div class="row">
                        <div class="col-sm-6">
                            <h3 class="mb-0"><%=Common.getBahasaConfig("Pustaka") %></h3>
                        </div>
                        <div class="col-sm-6">
                            <ol class="breadcrumb float-sm-end">
                                <li class="breadcrumb-item"><a href="<%=request.getContextPath() %>/main2?page=beranda">Home</a></li>
                                <li class="breadcrumb-item active" aria-current="page">
                                    <%=Common.getBahasaConfig("Pustaka") %>
                                </li>
                            </ol>
                        </div>
                    </div> <!--end::Row-->
                </div> <!--end::Container-->
            </div> <!--end::App Content Header--> <!--begin::App Content-->
            <div class="app-content"> <!--begin::Container-->
                <div class="container-fluid"> <!--begin::Row-->
                    <div class="row"> <!--begin::Col-->
                        
                    </div> <!--end::Row--> <!--begin::Row-->
                    <div class="row"> <!-- Start col -->
                        
                        
                    </div> <!-- /.row (main row) -->
                </div> <!--end::Container-->
            </div> <!--end::App Content-->
        </main> <!--end::App Main--> 