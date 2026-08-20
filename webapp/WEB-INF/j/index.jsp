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
html, body {
    min-height: 100%;
}

body {
    margin: 0;
    overflow: auto;
    font-family: "Source Sans Pro", Arial, sans-serif;
    color: #ffffff;
}

.header {
    position: relative;
    min-height: 100vh;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 42px 18px;
    background: linear-gradient(115deg, rgba(5, 18, 44, .90), rgba(12, 55, 111, .62) 47%, rgba(3, 16, 38, .76)),
            url('<%=background_PerguruanTinggi%>') no-repeat center center fixed;
    -webkit-background-size: cover;
    -moz-background-size: cover;
    background-size: cover;
    -o-background-size: cover;
}

.landing-shell {
    width: min(1080px, 100%);
    display: grid;
    grid-template-columns: 1.1fr .9fr;
    gap: 22px;
    align-items: stretch;
}

.hero-panel, .quick-panel {
    border: 1px solid rgba(255, 255, 255, .22);
    background: rgba(10, 23, 51, .58);
    box-shadow: 0 24px 70px rgba(0, 0, 0, .32);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border-radius: 24px;
}

.hero-panel {
    padding: 34px;
}

.quick-panel {
    padding: 24px;
}

.brand-row {
    display: flex;
    align-items: center;
    gap: 18px;
    margin-bottom: 28px;
}

.brand-logo {
    width: 104px;
    height: 104px;
    object-fit: contain;
    padding: 12px;
    border-radius: 26px;
    background: rgba(255, 255, 255, .93);
    box-shadow: 0 18px 45px rgba(0, 0, 0, .22);
}

.eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    padding: 7px 12px;
    border-radius: 999px;
    background: rgba(255, 255, 255, .16);
    border: 1px solid rgba(255, 255, 255, .20);
    font-size: 13px;
    font-weight: 700;
    letter-spacing: .04em;
    text-transform: uppercase;
}

.hero-title {
    margin: 0 0 14px;
    font-size: clamp(34px, 5vw, 58px);
    line-height: 1.02;
    font-weight: 800;
    text-shadow: 0 8px 28px rgba(0, 0, 0, .38);
}

.hero-subtitle {
    max-width: 720px;
    margin: 0 0 24px;
    color: rgba(255, 255, 255, .86);
    font-size: 18px;
    line-height: 1.55;
}

.portal-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.portal-btn {
    display: inline-flex;
    align-items: center;
    gap: 9px;
    min-height: 46px;
    padding: 10px 16px;
    border-radius: 14px;
    border: 1px solid rgba(255, 255, 255, .25);
    background: rgba(255, 255, 255, .95);
    color: #0f3f77;
    font-weight: 800;
    text-decoration: none;
    box-shadow: 0 12px 24px rgba(0, 0, 0, .20);
    transition: transform .18s ease, box-shadow .18s ease, background .18s ease;
}

.portal-btn:hover, .portal-btn:focus {
    color: #0b3470;
    text-decoration: none;
    background: #ffffff;
    transform: translateY(-2px);
    box-shadow: 0 16px 32px rgba(0, 0, 0, .28);
}

.quick-title {
    margin: 0 0 16px;
    font-size: 22px;
    font-weight: 800;
}

.info-stack {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.info-card {
    padding: 14px 16px;
    border-radius: 16px;
    background: rgba(255, 255, 255, .12);
    border: 1px solid rgba(255, 255, 255, .16);
}

.info-label {
    display: block;
    margin-bottom: 4px;
    color: rgba(255, 255, 255, .66);
    font-size: 12px;
    font-weight: 700;
    text-transform: uppercase;
}

.info-value {
    color: rgba(255, 255, 255, .95);
    font-size: 15px;
    line-height: 1.45;
    font-weight: 700;
}

@media (max-width: 900px) {
    .landing-shell {
        grid-template-columns: 1fr;
    }
    .hero-panel, .quick-panel {
        border-radius: 18px;
    }
    .hero-panel {
        padding: 24px;
    }
    .brand-row {
        flex-direction: column;
        align-items: flex-start;
    }
    .brand-logo {
        width: 88px;
        height: 88px;
    }
}

</style>

<link rel="shortcut icon" href="<%=logo_PerguruanTinggi%>" type="image/png"/>


</head>

<body>

   

    <!-- Header -->
    <header id="top" class="header">
        <div class="landing-shell">
            <section class="hero-panel">
                <div class="brand-row">
                    <img src="<%=logo_PerguruanTinggi%>" class="brand-logo"/>
                    <span class="eyebrow"><i class="fa fa-university"></i> Enterprise Education</span>
                </div>
                <h1 class="hero-title"><%=judul%></h1>
                <p class="hero-subtitle">
                    Selamat datang di portal layanan digital kampus. Akses eCampus, penerimaan mahasiswa,
                    tracer study, dokumen, repository, dan pustaka dari satu halaman yang ringkas.
                </p>
                <div class="portal-actions">
                    <%
                    if(sekolah != null && sekolah.getId() != null){
                    %>
                    <a href="ecampus.jsp" class="portal-btn"><i class="fa fa-desktop"></i> eSchool</a>
                    <a target="_blank" href="psb.zul" class="portal-btn"><i class="fa fa-graduation-cap"></i> Siswa Baru</a>
                    <%} else { %>
                    <a href="ecampus.jsp" class="portal-btn"><i class="fa fa-desktop"></i> eCampus</a>
                    <a target="_blank" href="pmb.zul" class="portal-btn"><i class="fa fa-graduation-cap"></i> Mahasiswa Baru</a>
                    <a target="_blank" href="alumni.zul" class="portal-btn"><i class="fa fa-line-chart"></i> Tracer Study</a>
                    <a target="_blank" href="repository" class="portal-btn"><i class="fa fa-folder-open"></i> Repository</a>
                    <a target="_blank" href="document.zul" class="portal-btn"><i class="fa fa-file-text"></i> Dokumen</a>
                    <a target="_blank" href="pustaka" class="portal-btn"><i class="fa fa-book"></i> Pustaka</a>
                    <% } %>
                </div>
            </section>
            <aside class="quick-panel">
                <h2 class="quick-title">Informasi Kampus</h2>
                <div class="info-stack">
                    <div class="info-card">
                        <span class="info-label">Motto</span>
                        <span class="info-value"><%=ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getMotto()%></span>
                    </div>
                    <div class="info-card">
                        <span class="info-label">Alamat</span>
                        <span class="info-value"><%=Alamat1%></span>
                    </div>
                    <div class="info-card">
                        <span class="info-label">Kontak</span>
                        <span class="info-value"><i class="fa fa-phone fa-fw"></i> <%=Telepon%><br><i class="fa fa-envelope fa-fw"></i> <%=Email%></span>
                    </div>
                </div>
            </aside>
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
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>

</html>
