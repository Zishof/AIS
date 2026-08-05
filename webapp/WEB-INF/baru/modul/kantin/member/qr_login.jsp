<%@page import="java.net.URLEncoder"%>
<%@page import="ais.action.master.sekolah.util.SekolahUtil"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%

AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) GeneralValueObject.ambilData(AnggotaKoperasi.class, request.getParameter("anggotaKoperasi"), true);

if(anggotaKoperasi== null || anggotaKoperasi.getUserid() == null){
	%>
	
	<jsp:include page="/WEB-INF/baru/componen/tidak_ketemu.jsp">
                            <jsp:param name="judul" value='<%= Common.getBahasaConfigJS("Anggota / Member Bermasalah") %>' />
                            <jsp:param name="jenis" value="alert" />
                            <jsp:param name="pesan" value='<%= Common.getBahasaConfigJS("Anggota / Member belum memiliki akun login.") %>' />
                        </jsp:include>
	
	<%
	return;
}

Mahasiswa mahasiswa = anggotaKoperasi.getMahasiswa();

PerguruanTinggi perguruanTinggi = mahasiswa != null && mahasiswa.getJurusan() != null
		&& mahasiswa.getJurusan().getFakultas() != null
		&& mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null
				? mahasiswa.getJurusan().getFakultas().getPerguruanTinggi()
				: PerguruanTinggiUtil.getPerguruanTinggi(request);

String strURL = Common.getKonfigurasi("ambil_kode_url", "https://dev.ecampus.id/ecampus/Api")
		.getNilai();

String username = anggotaKoperasi.getUserid() + ";" + Common.getRequestHostWithProtocol();

if(mahasiswa != null && mahasiswa.getNim() != null){
	username = mahasiswa.getNim() + ";" + Common.getRequestHostWithProtocol();
}

String link = Common.getRequestHostWithProtocol() + "/Api";
if (Common.getKonfigurasi("dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF).getNilai()
		.equals(Konfigurasi.AKTIF)) {
	link = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai()
			+ "/Api";
	username = anggotaKoperasi.getUserid() + ";"
			+ Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai();
	
	if(mahasiswa != null && mahasiswa.getNim() != null){
		username = mahasiswa.getNim() + ";" + Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai();
	}
}


String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
		.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
		.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
String banner_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
		.getPerguruanTinggiMedia(request, "banner_perguruanTinggi_");
String background_login_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
		.getPerguruanTinggiMedia(request, "background_login_perguruanTinggi_");

String judul = perguruanTinggi == null ? "" : perguruanTinggi.getNama();
String Motto = perguruanTinggi == null || perguruanTinggi.getMotto() == null
		|| perguruanTinggi.getMotto().trim().isEmpty() ? "eCampus Information System"
				: perguruanTinggi.getMotto();
String Alamat1 = perguruanTinggi == null ? "" : perguruanTinggi.getAlamat1();
String Telepon = perguruanTinggi.getTelepon();
String Email = perguruanTinggi.getEmail();

Siswa siswa = anggotaKoperasi.getSiswa();

Sekolah sekolah = siswa != null && siswa.getSekolah() != null ? siswa.getSekolah()
		: SekolahUtil.getSekolah(request);
Yayasan yayasan = sekolah != null ? sekolah.getYayasan()
		: SekolahUtil.getYayasan(request);
if (sekolah != null && sekolah.getId() != null) {
	judul = sekolah.getNama();

	if (sekolah.getMotto() != null && !sekolah.getMotto().trim().isEmpty()) {
		Motto = sekolah.getMotto();
	}

	if (sekolah.getAlamat() != null && !sekolah.getAlamat().trim().isEmpty()) {
		Alamat1 = sekolah.getAlamat();
	}

	if (sekolah.getTelp() != null && !sekolah.getTelp().trim().isEmpty()) {
		Telepon = sekolah.getTelp();
	}

	if (sekolah.getEmail() != null && !sekolah.getEmail().trim().isEmpty()) {
		Email = sekolah.getEmail();
	}

	logo_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(request,
			"logo_sekolah_");

} else if (yayasan != null && yayasan.getId() != null) {
	judul = yayasan.getNama();

	if (yayasan.getMotto() != null && !yayasan.getMotto().trim().isEmpty()) {
		Motto = yayasan.getMotto();
	}

	if (yayasan.getAlamat() != null && !yayasan.getAlamat().trim().isEmpty()) {
		Alamat1 = yayasan.getAlamat();
	}

	if (yayasan.getTelp() != null && !yayasan.getTelp().trim().isEmpty()) {
		Telepon = yayasan.getTelp();
	}

	if (yayasan.getEmail() != null && !yayasan.getEmail().trim().isEmpty()) {
		Email = yayasan.getEmail();
	}

	logo_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia("logo_yayasan_");
}

String hasil = "";
try {

	JSONObject postData = new JSONObject();
	postData.put("username", username);
	postData.put("link", link);
	postData.put("nama_pt", judul);
	postData.put("login_bg_pt", background_login_PerguruanTinggi);
	postData.put("bg_pt", background_PerguruanTinggi);
	postData.put("logo_pt", logo_PerguruanTinggi);
	postData.put("banner_pt", banner_PerguruanTinggi);

	postData.put("motto_pt", Motto);
	postData.put("alamat_pt", Alamat1);
	postData.put("telp_pt", Telepon);
	postData.put("email_pt", Email);
	postData.put("action", "code");

	System.out.println("linkPost -> " + strURL);
	System.out.println("postData -> " + postData);

	String[] command = { "curl", "-d", postData.toString(), "-H", "Content-Type: application/json",
			strURL };

	ProcessBuilder process = new ProcessBuilder(command);
	Process p;
	p = process.start();
	BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	StringBuilder builder = new StringBuilder();
	String line = null;
	while ((line = reader.readLine()) != null) {
		builder.append(line);
		builder.append(System.getProperty("line.separator"));
	}
	hasil = builder.toString();

} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/member/qr_login.jsp:170");
}

System.out.println(hasil);

JSONObject jsonObject = new JSONObject(hasil);

String linkAndroid = Common.getKonfigurasi("default_linkAndroid",
		"https://play.google.com/store/apps/details?id=com.ecampus.zishof").getNilai();

if (sekolah != null && sekolah.getId() != null) {
	linkAndroid = Common.getKonfigurasi("default_linkAndroid_sekolah",
			"https://play.google.com/store/apps/details?id=com.eschool.zishof").getNilai();
}

linkAndroid = Common.getKonfigurasi(
		"linkAndroid" + (sekolah != null && sekolah.getId() != null ? "_s_" + sekolah.getId() : ""),
		linkAndroid).getNilai();

if (linkAndroid.endsWith("id.zishof.ecampusweb")) {
	if (sekolah != null && sekolah.getId() != null) {
		linkAndroid = "https://play.google.com/store/apps/details?id=com.eschool.zishof";
	} else {
		linkAndroid = "https://play.google.com/store/apps/details?id=com.ecampus.zishof";
	}
}

String linkIphone = Common
		.getKonfigurasi("default_linkIphone", "https://apps.apple.com/id/app/ecampus/id6503487876?l=id")
		.getNilai();

if (sekolah != null && sekolah.getId() != null) {
	linkIphone = Common.getKonfigurasi("default_linkIphone_sekolah",
			"https://apps.apple.com/us/app/eschool/id6503661156?l=id").getNilai();
}

linkIphone = Common.getKonfigurasi(
		"linkIphone" + (sekolah != null && sekolah.getId() != null ? "_s_" + sekolah.getId() : ""),
		linkIphone).getNilai();


String code = jsonObject.getString("code") ;

%>
<div class="card rounded-0 shadow" style="max-width: 550px; border: 2px solid #888;">
        

        <div class="card-body text-center px-4 pt-3 pb-5">
            
            <p class="fw-bold mb-3 fs-6"><%= Common.getBahasaConfig("SCAN QRCODE BERIKUT UNTUK MULAI MENGGUNAKAN APLIKASI :") %></p>
            
            <p class="mb-2 text-dark"><%= Common.getBahasaConfig("* Jika aplikasi belum ter-install di smartphone Anda, untuk android dapat di-download pada link berikut:") %></p>
            <div class="mb-3">
                <i class="fas fa-download fs-6 me-1 text-dark"></i>
                <a href="<%=linkAndroid %>" target="_blank" class="text-primary fw-bold text-decoration-none"><%= Common.getBahasaConfig("download versi android di google play") %></a>
            </div>

            <p class="mb-2 text-dark"><%= Common.getBahasaConfig("* Untuk versi iphone / IOS dapat di-download pada link berikut:") %></p>
            <div class="mb-4">
                <i class="fas fa-download fs-6 me-1 text-dark"></i>
                <a href="<%=linkIphone %>" target="_blank" class="text-primary fw-bold text-decoration-none"><%= Common.getBahasaConfig("download versi iphone di apps store") %></a>
            </div>

            <h5 class="fw-bold mb-4 fs-5"><%= Common.getBahasaConfig("ATAU MASUKKAN KODE INSTALL : ") %><%=code %></h5>

            <img src="https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=<%=URLEncoder.encode(code, "UTF-8") %>" alt="<%= Common.getBahasaConfig("QR Code") %>" class="img-fluid w-50 mb-2">
            
        </div>

        
    </div>