<%@page import="ais.common.Common"%>
<!DOCTYPE html>
<html lang="id">

<head>

    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Merupakan Sistem Informasi Perguruan Tinggi Terintegrasi">
    <meta name="author" content="Mohammad Fauzi Murtadho">
	
	<%
	ais.database.model.sekolah.Sekolah sekolah =  ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);

	String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
	String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
	String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
	String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();
	String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
	if(Common.isMobile(request)){
		background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "banner_perguruanTinggi_");
	}
	String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
	%>

    <title>eCampus | <%=judul%></title>

    <!-- Bootstrap Core CSS -->
    <link href="css/bootstrap.min.css" rel="stylesheet">

    <!-- Custom CSS -->
    <link href="css/stylish-portfolio.css" rel="stylesheet">

    <!-- Custom Fonts -->
    <link href="font-awesome/css/font-awesome.min.css" rel="stylesheet" type="text/css">
    <link href="https://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,700,300italic,400italic,700italic" rel="stylesheet" type="text/css">

    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
        <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
        <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
    <![endif]-->
	
	<style type="text/css">
body {
    overflow:hidden;
}

.header {
    display: table;
    position: relative;
    width: 100%;
    height: 100%;
    background: url('<%=background_PerguruanTinggi%>') no-repeat center center scroll;
    -webkit-background-size: cover;
    -moz-background-size: cover;
    background-size: cover;
    -o-background-size: cover;
}

</style>

<link rel="shortcut icon" href="<%=logo_PerguruanTinggi%>" type="image/png"/>


</head>

<body>

   

    <!-- Header -->
    <header id="top" class="header">
        <div class="text-vertical-center" style="<%=Common.isMobile(request) ? "" : "padding-right: 50%;"%>">
			<img src="<%=logo_PerguruanTinggi%>" style="height:150px"/>
            <h2 style="color:#7098db;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;">Selamat Datang</h2>
			
            <br>
			
			<%
			if(sekolah != null && sekolah.getId() != null){
			%>
			<a href="ecampus.jsp" class="btn btn-primary btn-lg">eSchool</a>
			<a target="_blank" href="psb.zul" class="btn btn-primary btn-lg">Siswa Baru</a>
			<%} else { %>
			<a href="ecampus.jsp" class="btn btn-primary btn-lg">eCampus</a>
			<a target="_blank" href="pmb.zul" class="btn btn-primary btn-lg">Mahasiswa Baru</a>
			<a target="_blank" href="alumni.zul" class="btn btn-primary btn-lg">Tracer Study</a>
			<a target="_blank" href="https://repository.utn.ac.id/" class="btn btn-primary btn-lg">Repository</a>
			<a target="_blank" href="document.zul" class="btn btn-primary btn-lg">Dokumen</a>
			<a target="_blank" href="https://perpus.utn.ac.id/" class="btn btn-primary btn-lg">Pustaka</a>
			<% } %>
			
			
			<br>
			<div class="row">
                <div class="col-lg-10 col-lg-offset-1 text-center">
                    <h3><strong style="color:#7098db;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;"><%=ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getMotto()%></strong>
                    </h3>
                    <h5><strong style="color:#7098db;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;"><%=ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1()%></strong>
                    </h5>
                </div>
            </div>
        </div>
    </header>

<script src="<%=request.getContextPath() %>/js/pesan-formal.js"></script>
	<script>
		<%
		if(request.getParameter("login_error") != null){
			%>
			setTimeout(function(){
				tampilkanPesanGagalFormal(
					"proses masuk (login) ke sistem",
					"<%=request.getParameter("login_error")%>",
					["Periksa kembali username/NIM/NIP dan kata sandi (password) yang Bapak/Ibu masukkan.", "Pastikan tombol Caps Lock pada papan ketik (keyboard) tidak sedang aktif.", "Apabila lupa kata sandi, gunakan fitur reset/lupa kata sandi yang tersedia pada halaman ini."]
				);
			}, 500);
			<%
		}
		%>
	</script>
</body>

</html>
