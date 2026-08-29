<%@page import="java.net.URLEncoder"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.action.master.sekolah.util.SekolahUtil"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%
Sekolah sekolah = SekolahUtil.getSekolah(request);
Yayasan yayasan = SekolahUtil.getYayasan(request);
String telp = "";
String notelp = Common.getKonfigurasi("no_whatsapp_operator", "").getNilai();
String email = "";
PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
String nama = sekolah != null && sekolah.getId() != null ? sekolah.getNama()
		: yayasan != null && yayasan.getId() != null ? yayasan.getNama()
		: selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
				? selectedPerguruanTinggi.getNama()
				: "";
String motto = sekolah != null && sekolah.getId() != null && !sekolah.getMotto().isEmpty() ? (sekolah.getMotto())
		: selectedPerguruanTinggi == null ? "" : (selectedPerguruanTinggi.getMotto());
String link = sekolah != null && sekolah.getId() != null && Boolean.TRUE.equals(sekolah.getAktif())
		? request.getContextPath() + "/sekolah/" + sekolah.getId()
		: selectedPerguruanTinggi == null ? "" : (selectedPerguruanTinggi.getWebsite());
String alamat = sekolah != null && sekolah.getId() != null && !sekolah.getAlamat().isEmpty()
		? ("Alamat: " + sekolah.getAlamat())
		: yayasan != null && yayasan.getId() != null && !yayasan.getAlamat().isEmpty()
		? ("Alamat: " + yayasan.getAlamat())
		: selectedPerguruanTinggi == null ? "" : ("Alamat: " + selectedPerguruanTinggi.getAlamat1());
if (sekolah != null && sekolah.getId() != null && !sekolah.getWa().isEmpty()) {
	notelp = sekolah.getWa();
} else if (yayasan != null && yayasan.getId() != null && !yayasan.getWa().isEmpty()) {
	notelp = yayasan.getWa();
} else if (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
		&& !selectedPerguruanTinggi.getWa().isEmpty()) {
	notelp = selectedPerguruanTinggi.getWa();
}

if (sekolah != null && sekolah.getId() != null && !sekolah.getTelp().isEmpty()) {
	telp = sekolah.getTelp();
} else if (yayasan != null && yayasan.getId() != null && !yayasan.getTelp().isEmpty()) {
	telp = yayasan.getTelp();
} else if (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
		&& !selectedPerguruanTinggi.getTelepon().isEmpty()) {
	telp = selectedPerguruanTinggi.getTelepon();
}

if (sekolah != null && sekolah.getId() != null && !sekolah.getEmail().isEmpty()) {
	email = sekolah.getEmail();
} else if (yayasan != null && yayasan.getId() != null && !yayasan.getEmail().isEmpty()) {
	email = yayasan.getEmail();
} else if (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
		&& !selectedPerguruanTinggi.getEmail().isEmpty()) {
	email = selectedPerguruanTinggi.getEmail();
}
String hp = telp;

if (hp != null && !hp.trim().isEmpty() && !(hp == null || hp.toString().trim().isEmpty()
		|| hp.toString().trim().equals("00000000000000000000") || hp.toString().trim().equals("000000000"))) {
	hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
	hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
	hp = !hp.startsWith("+") ? "+62" + hp : hp;
}

String linktelp = "tel:" + notelp;
String text = "Kami ingin mendapatkan informasi tentang \"" + nama + "\"";
String linkWa = "https://api.whatsapp.com/send?phone=" + hp + "&text="
		+ URLEncoder.encode(text.replaceAll("<br>", "\n"), "UTF-8");

String linkEmail = "mailto:" + email + "?&subject="
		+ URLEncoder.encode(text.replaceAll("<br>", "\n"), "UTF-8") + "&body="
		+ URLEncoder.encode(text.replaceAll("<br>", "\n"), "UTF-8");
%>

	

<footer class="app-footer fw-light fs-9">
	<!--begin::To the end-->
	<div class="float-end d-none d-sm-inline "><%=motto%></div>
	<!--end::To the end-->
	<!--begin::Copyright-->
	<strong><%=alamat%> Telp: <a
		href="<%=linktelp%>" class="text-decoration-none"><%=notelp%></a> WA: <a
		href="<%=linkWa%>" class="text-decoration-none"><%=telp%></a> Email: <a
		href="<%=linkEmail%>" class="text-decoration-none"><%=email%></a> | Copyright &copy; <%=java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) %>&nbsp; <a
		href="<%=link%>" class="text-decoration-none"><%=nama%></a>. </strong> All
	rights reserved.
	<!--end::Copyright-->
</footer>
<!--end::Footer-->
