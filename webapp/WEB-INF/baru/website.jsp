<%@page import="java.lang.reflect.Method"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Bantuan"%>
<%@page import="ais.database.model.Beasiswa"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.PenumumanWebsite"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
private String cfg(String key, String def) {
	try {
		Konfigurasi k = Common.getKonfigurasi(key, def);
		if (k != null && k.getNilai() != null && k.getNilai().trim().length() > 0) {
			return k.getNilai();
		}
	} catch (Exception e) {
		ais.common.ErrorAuditUtil.record(e, "website.jsp cfg " + key);
	}
	return def == null ? "" : def;
}

private String safe(String value) {
	if (value == null) {
		return "";
	}
	return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
			.replace("'", "&#39;");
}

private String htmlText(String value) {
	String text = safe(value);
	return text.replace("\r\n", "\n").replace("\r", "\n").replace("\n\n", "</p><p>").replace("\n", "<br/>");
}

private String formalLong(String tema, String nama, int minWords) {
	String[] paragraf = new String[] {
			tema + " " + nama + " disusun sebagai pernyataan resmi yang menggambarkan kesungguhan institusi dalam menyelenggarakan pendidikan tinggi secara bermutu, akuntabel, inklusif, dan berkelanjutan. Setiap layanan akademik diarahkan untuk mendukung mahasiswa, dosen, tenaga kependidikan, mitra, alumni, dan masyarakat agar memperoleh informasi yang jelas, tertib, serta dapat dipercaya.",
			"Dalam menjalankan mandat pendidikan, institusi menempatkan integritas akademik, tata kelola yang baik, pelayanan publik yang santun, pemanfaatan teknologi, dan pengembangan sumber daya manusia sebagai dasar utama. Seluruh proses dirancang agar pembelajaran, penelitian, pengabdian kepada masyarakat, kemahasiswaan, kerja sama, dan administrasi saling terhubung dalam satu budaya mutu.",
			"Kehadiran laman ini menjadi ruang informasi formal bagi sivitas akademika dan masyarakat. Informasi yang disajikan diharapkan membantu pengunjung memahami karakter kampus, arah pengembangan, fasilitas, layanan, prestasi, program studi, peluang beasiswa, dokumen panduan, serta kanal komunikasi resmi yang dapat digunakan untuk memperoleh layanan secara tepat.",
			"Institusi percaya bahwa perguruan tinggi tidak hanya berfungsi sebagai tempat belajar, tetapi juga sebagai lingkungan pembentukan karakter, etika profesi, kepekaan sosial, kepemimpinan, kreativitas, dan kemampuan beradaptasi. Karena itu, setiap kebijakan diarahkan untuk menghasilkan lulusan yang kompeten, bertanggung jawab, dan mampu memberi kontribusi nyata.",
			"Pengembangan kampus dilakukan melalui evaluasi berkelanjutan, penguatan tata kelola, peningkatan kapasitas dosen, pembaruan kurikulum, penguatan jejaring, serta pemanfaatan sistem informasi akademik. Dengan pendekatan tersebut, pelayanan dapat berlangsung lebih terbuka, terdokumentasi, dan responsif terhadap kebutuhan zaman." };
	StringBuilder sb = new StringBuilder();
	int words = 0;
	for (int i = 0; words < minWords; i++) {
		if (sb.length() > 0) {
			sb.append("\n\n");
		}
		String p = paragraf[i % paragraf.length];
		sb.append(p);
		words += p.split("\\s+").length;
	}
	return sb.toString();
}

private String invokeText(Object obj, String... methods) {
	if (obj == null) {
		return "";
	}
	for (int i = 0; i < methods.length; i++) {
		try {
			Method m = obj.getClass().getMethod(methods[i]);
			Object v = m.invoke(obj);
			if (v != null && v.toString().trim().length() > 0) {
				return v.toString();
			}
		} catch (Exception e) {
		}
	}
	return obj.toString();
}

private boolean approved(Object obj) {
	String status = invokeText(obj, "getStatus", "getStatusPersetujuan", "getPersetujuan");
	return status != null && status.trim().equalsIgnoreCase("Disetujui");
}

@SuppressWarnings("unchecked")
private List listApproved(Session hs, String className, int max) {
	List result = new ArrayList();
	try {
		List rows = hs.createCriteria(Class.forName(className)).setMaxResults(max * 3).list();
		for (int i = 0; i < rows.size() && result.size() < max; i++) {
			Object row = rows.get(i);
			if (approved(row)) {
				result.add(row);
			}
		}
	} catch (Exception e) {
	}
	return result;
}

private String link(String value) {
	String root = Common.ROOT == null ? "" : Common.ROOT;
	String v = value == null ? "" : value.trim();
	if (v.length() == 0) {
		return "#";
	}
	String l = v.toLowerCase();
	if (l.startsWith("http://") || l.startsWith("https://") || l.startsWith("mailto:") || l.startsWith("tel:")
			|| l.startsWith("#")) {
		return v;
	}
	return v.startsWith("/") ? root + v : root + "/" + v;
}
%>
<%
PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
String namaPt = pt != null && pt.getNama() != null && pt.getNama().trim().length() > 0 ? pt.getNama() : "Perguruan Tinggi";
String logo = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
String bg = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
if (bg == null || bg.trim().length() == 0) {
	bg = Common.ROOT + "/img/pmb_bg.jpg";
}
String alamat = pt == null ? "" : ((pt.getAlamat1() == null ? "" : pt.getAlamat1()) + " " + (pt.getAlamat2() == null ? "" : pt.getAlamat2())).trim();
String telepon = pt != null && pt.getTelepon() != null ? pt.getTelepon() : "-";
String email = pt != null && pt.getEmail() != null ? pt.getEmail() : "-";
String website = pt != null && pt.getWebsite() != null ? pt.getWebsite() : "";

String selamatDatang = cfg("website_selamat_datang", formalLong("Selamat datang di website resmi", namaPt, 1000));
String profilKampus = cfg("website_profil_kampus", formalLong("Profil kampus", namaPt, 1000));
String sejarahKampus = cfg("website_sejarah_kampus", formalLong("Sejarah kampus", namaPt, 1000));
String strukturOrganisasi = cfg("website_struktur_organisasi", formalLong("Struktur organisasi", namaPt, 1000));
String fasilitas = cfg("website_fasilitas_layanan",
		formalLong("Fasilitas dan layanan kampus", namaPt, 1500));
String akademik = cfg("website_akademik_kemahasiswaan",
		formalLong("Akademik dan kemahasiswaan", namaPt, 1500));
String kerjaSama = cfg("website_kerja_sama_alumni",
		formalLong("Kerja sama, alumni, dan pengembangan karir", namaPt, 1500));

List<PenumumanWebsite> berita = new ArrayList<PenumumanWebsite>();
List<Dosen> dosen = new ArrayList<Dosen>();
List<Jurusan> jurusan = new ArrayList<Jurusan>();
List<Beasiswa> beasiswa = new ArrayList<Beasiswa>();
List<Bantuan> bantuan = new ArrayList<Bantuan>();
List prestasi = new ArrayList();
Session hs = HibernateUtil.currentNativeSession();
try {
	berita = hs.createCriteria(PenumumanWebsite.class).add(Restrictions.eq("aktif", Boolean.TRUE))
			.addOrder(Order.desc("tanggal")).setMaxResults(8).list();
	dosen = hs.createCriteria(Dosen.class).add(Restrictions.eq("aktif", Boolean.TRUE)).setMaxResults(60).list();
	jurusan = hs.createCriteria(Jurusan.class).add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.asc("nama")).list();
	beasiswa = hs.createCriteria(Beasiswa.class).addOrder(Order.desc("tanggalBuka")).setMaxResults(12).list();
	bantuan = hs.createCriteria(Bantuan.class).addOrder(Order.asc("kunci")).setMaxResults(60).list();
	prestasi.addAll(listApproved(hs, "ais.database.model.KegiatanKemahasiswaan", 6));
	prestasi.addAll(listApproved(hs, "ais.database.model.PrestasiMahasiswa", 6));
	prestasi.addAll(listApproved(hs, "ais.database.model.PenghargaanMahasiswa", 6));
	prestasi.addAll(listApproved(hs, "ais.database.model.KegiatanKedosenan", 6));
	prestasi.addAll(listApproved(hs, "ais.database.model.PrestasiDosen", 6));
	prestasi.addAll(listApproved(hs, "ais.database.model.OrganisasiDosen", 6));
	prestasi.addAll(listApproved(hs, "ais.database.model.PenghargaanDosen", 6));
	prestasi.addAll(listApproved(hs, "ais.database.model.FormulirKegiatanPeserta", 6));
} catch (Exception e) {
	Common.tampilErrorJikaAdmin(e);
} finally {
}
String mapQuery = alamat.length() == 0 ? namaPt : alamat;
%>
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><%=safe(namaPt)%></title>
<meta name="description" content="<%=safe(cfg("website_meta_description", "Website resmi kampus yang memuat profil, program studi, berita, prestasi, beasiswa, panduan, fasilitas, layanan, dan kontak institusi."))%>">
<link rel="icon" href="<%=logo%>" type="image/x-icon">
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
<style>
html{scroll-behavior:smooth}body{font-family:Segoe UI,system-ui,-apple-system,sans-serif;color:#172033;background:#f5f7fb}.navbar{background:#ffffffd9;backdrop-filter:blur(16px);box-shadow:0 8px 22px rgba(15,23,42,.08)}.hero{min-height:86vh;background:linear-gradient(90deg,rgba(6,28,61,.88),rgba(0,88,122,.70)),url('<%=bg%>') center/cover no-repeat;color:white;display:flex;align-items:center}.hero h1{font-size:clamp(34px,6vw,76px);letter-spacing:0;font-weight:800}.section{padding:78px 0}.section-alt{background:white}.eyebrow{font-size:12px;font-weight:800;letter-spacing:0;text-transform:uppercase;color:#0f766e}.content-text{font-size:16px;line-height:1.85;color:#334155}.cardx{background:white;border:1px solid #e2e8f0;border-radius:8px;padding:22px;height:100%;box-shadow:0 8px 18px rgba(15,23,42,.05)}.cardx h3{font-size:18px}.quick a{display:flex;align-items:center;gap:10px;padding:13px 14px;border:1px solid #dbe3ee;border-radius:8px;color:#172033;text-decoration:none;background:white}.quick a:hover{border-color:#0f766e;color:#0f766e}.nav-link{font-weight:650}.faculty-pill{border:1px solid #dbe3ee;border-radius:8px;padding:16px;background:#fff}.footer{background:#0f172a;color:#cbd5e1;padding:42px 0}.footer a{color:white}.map{border:0;width:100%;min-height:320px;border-radius:8px}.logo{width:48px;height:48px;object-fit:contain}
</style>
</head>
<body>
<nav class="navbar navbar-expand-lg fixed-top">
	<div class="container">
		<a class="navbar-brand d-flex align-items-center gap-2 fw-bold" href="#beranda"><img class="logo" src="<%=logo%>" alt="Logo"> <%=safe(namaPt)%></a>
		<button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#nav"><span class="navbar-toggler-icon"></span></button>
		<div id="nav" class="collapse navbar-collapse">
			<ul class="navbar-nav ms-auto">
				<li class="nav-item"><a class="nav-link" href="#profil">Profil</a></li><li class="nav-item"><a class="nav-link" href="#prodi">Program Studi</a></li><li class="nav-item"><a class="nav-link" href="#prestasi">Prestasi</a></li><li class="nav-item"><a class="nav-link" href="#layanan">Layanan</a></li><li class="nav-item"><a class="nav-link" href="#berita">Berita</a></li><li class="nav-item"><a class="nav-link" href="#kontak">Kontak</a></li>
			</ul>
		</div>
	</div>
</nav>
<header id="beranda" class="hero">
	<div class="container">
		<div class="col-lg-9">
			<div class="eyebrow text-white mb-3"><%=safe(cfg("website_eyebrow", "Website Resmi Kampus"))%></div>
			<h1><%=safe(namaPt)%></h1>
			<p class="lead mt-4"><%=safe(cfg("website_tagline", "Pusat informasi resmi kampus untuk akademik, kemahasiswaan, layanan digital, prestasi, program studi, beasiswa, dan komunikasi publik institusi."))%></p>
			<div class="d-flex flex-wrap gap-2 mt-4">
				<a class="btn btn-light" href="<%=link(cfg("link_modul_pmb", "/pmb"))%>"><i class="fa-solid fa-user-plus"></i> Pendaftaran</a>
				<a class="btn btn-outline-light" href="<%=link(cfg("link_modul_login_ecampus", "/login"))%>"><i class="fa-solid fa-right-to-bracket"></i> eCampus</a>
			</div>
		</div>
	</div>
</header>
<section class="section section-alt">
	<div class="container">
		<div class="eyebrow">Selamat Datang</div>
		<h2 class="mb-4"><%=safe(cfg("website_judul_selamat_datang", "Selamat Datang di Website Resmi Kampus"))%></h2>
		<div class="content-text"><p><%=htmlText(selamatDatang)%></p></div>
	</div>
</section>
<section id="profil" class="section">
	<div class="container">
		<div class="row g-4">
			<div class="col-lg-6"><div class="eyebrow">Profil Kampus</div><h2>Profil Institusi</h2><div class="content-text"><p><%=htmlText(profilKampus)%></p></div></div>
			<div class="col-lg-6"><div class="eyebrow">Sejarah Kampus</div><h2>Sejarah Institusi</h2><div class="content-text"><p><%=htmlText(sejarahKampus)%></p></div></div>
			<div class="col-lg-12"><div class="eyebrow">Struktur Organisasi</div><h2>Tata Kelola dan Organisasi</h2><div class="content-text"><p><%=htmlText(strukturOrganisasi)%></p></div></div>
		</div>
	</div>
</section>
<section id="prodi" class="section section-alt">
	<div class="container">
		<div class="eyebrow">Program Studi</div><h2 class="mb-4">Daftar Program Studi</h2>
		<div class="row g-3">
		<% for (Jurusan j : jurusan) { %>
			<div class="col-md-6 col-xl-4"><a class="faculty-pill d-block text-decoration-none text-dark" href="#prodi-<%=j.getId()%>"><strong><%=safe(j.getNama())%></strong><br><small><%=safe(j.getStatusAkreditasi())%></small></a></div>
		<% } if (jurusan.isEmpty()) { %><div class="col-12"><div class="cardx">Data program studi belum tersedia.</div></div><% } %>
		</div>
		<% for (Jurusan j : jurusan) { %>
			<div id="prodi-<%=j.getId()%>" class="mt-5">
				<h3><%=safe(j.getNama())%></h3>
				<div class="content-text"><p><%=htmlText(cfg("website_profil_prodi_" + j.getId(), j.getProfil() + "\n\n" + formalLong("Profil program studi " + j.getNama(), namaPt, 1500)))%></p></div>
			</div>
		<% } %>
	</div>
</section>
<section id="prestasi" class="section">
	<div class="container">
		<div class="eyebrow">Prestasi</div><h2 class="mb-4">Prestasi Kampus yang Telah Disetujui</h2>
		<div class="row g-3">
		<% for (Object p : prestasi) { %><div class="col-md-6 col-lg-4"><div class="cardx"><h3><%=safe(invokeText(p, "getNama", "getJudul"))%></h3><p class="mb-0 text-muted"><%=safe(p.getClass().getSimpleName())%></p></div></div><% } if (prestasi.isEmpty()) { %><div class="col-12"><div class="cardx">Data prestasi yang telah disetujui belum tersedia.</div></div><% } %>
		</div>
	</div>
</section>
<section class="section section-alt">
	<div class="container">
		<div class="row g-4">
			<div class="col-lg-6"><div class="eyebrow">Daftar Dosen</div><h2>Dosen Aktif</h2><div class="row g-2"><% for (Dosen d : dosen) { %><div class="col-sm-6"><div class="cardx"><strong><%=safe(d.getNama())%></strong><br><small><%=safe(invokeText(d, "getKode", "getNidn"))%></small></div></div><% } %></div></div>
			<div class="col-lg-6"><div class="eyebrow">Beasiswa</div><h2>Informasi Beasiswa</h2><div class="row g-2"><% for (Beasiswa b : beasiswa) { %><div class="col-12"><div class="cardx"><strong><%=safe(b.getNama())%></strong><br><small><%=safe(b.getInstansi())%></small><p class="mb-0"><%=safe(b.getKeterangan())%></p></div></div><% } %></div></div>
		</div>
	</div>
</section>
<section id="layanan" class="section">
	<div class="container">
		<div class="eyebrow">Fasilitas dan Layanan</div><h2 class="mb-4">Layanan eCampus dan Fasilitas Kampus</h2>
		<div class="content-text mb-4"><p><%=htmlText(fasilitas)%></p></div>
		<div class="quick row g-3">
			<div class="col-md-4"><a href="<%=link(cfg("link_modul_login_ecampus", "/login"))%>"><i class="fa-solid fa-right-to-bracket"></i> Login eCampus</a></div>
			<div class="col-md-4"><a href="<%=link(cfg("link_modul_pmb", "/pmb"))%>"><i class="fa-solid fa-user-plus"></i> PMB</a></div>
			<div class="col-md-4"><a href="<%=link(cfg("link_modul_alumni", "/alumni"))%>"><i class="fa-solid fa-graduation-cap"></i> Alumni</a></div>
			<div class="col-md-4"><a href="<%=link(cfg("link_modul_pustaka", "/pustaka"))%>"><i class="fa-solid fa-book"></i> Perpustakaan</a></div>
			<div class="col-md-4"><a href="<%=link(cfg("link_modul_repository", "/repository"))%>"><i class="fa-solid fa-database"></i> Repository</a></div>
			<div class="col-md-4"><a href="<%=link(cfg("link_modul_dokumen", "/document"))%>"><i class="fa-solid fa-folder-open"></i> Dokumen</a></div>
			<div class="col-md-4"><a href="<%=link(cfg("link_modul_dashboard_pimpinan", "/dsh"))%>"><i class="fa-solid fa-chart-line"></i> Dashboard</a></div>
			<div class="col-md-4"><a href="<%=link(cfg("link_modul_karir", "/karir"))%>"><i class="fa-solid fa-briefcase"></i> Karir</a></div>
			<div class="col-md-4"><a href="<%=link(cfg("link_modul_buku_tamu", "/tamu"))%>"><i class="fa-solid fa-address-book"></i> Buku Tamu</a></div>
		</div>
	</div>
</section>
<section class="section section-alt">
	<div class="container">
		<div class="row g-4">
			<div class="col-lg-6"><div class="eyebrow">Akademik</div><h2>Akademik dan Kemahasiswaan</h2><div class="content-text"><p><%=htmlText(akademik)%></p></div></div>
			<div class="col-lg-6"><div class="eyebrow">Kerja Sama</div><h2>Kerja Sama, Alumni, dan Karir</h2><div class="content-text"><p><%=htmlText(kerjaSama)%></p></div></div>
		</div>
	</div>
</section>
<section class="section">
	<div class="container">
		<div class="eyebrow">Buku Panduan</div><h2 class="mb-4">Panduan Mahasiswa, Dosen, dan Admin</h2>
		<div class="row g-3">
		<% for (Bantuan b : bantuan) { String k = b.getKunci() == null ? "" : b.getKunci(); if (k.indexOf("mahasiswa") >= 0 || k.indexOf("dosen") >= 0 || k.indexOf("admin") >= 0) { %>
			<div class="col-md-4"><div class="cardx"><h3><%=safe(k)%></h3><p class="mb-0"><%=safe(invokeText(b, "getOleh"))%></p></div></div>
		<% }} %>
		</div>
	</div>
</section>
<section id="berita" class="section section-alt">
	<div class="container">
		<div class="eyebrow">Berita Kampus</div><h2 class="mb-4">Berita dan Informasi Resmi</h2>
		<div class="row g-3">
		<% for (PenumumanWebsite bw : berita) { %><div class="col-md-6 col-lg-4"><div class="cardx"><small class="text-muted"><%=safe(Common.dateFormat.get().format(bw.getTanggal()))%> | <%=safe(bw.getKategori())%></small><h3><%=safe(bw.getJudul())%></h3><p><%=safe(bw.getRingkasan())%></p><div class="content-text"><p><%=htmlText(bw.getIsi())%></p></div></div></div><% } if (berita.isEmpty()) { %><div class="col-12"><div class="cardx">Berita kampus belum tersedia.</div></div><% } %>
		</div>
	</div>
</section>
<section id="kontak" class="section">
	<div class="container">
		<div class="eyebrow">Kontak</div><h2 class="mb-4">Kontak dan Lokasi Kampus</h2>
		<div class="row g-4">
			<div class="col-lg-5"><div class="cardx"><p><strong>Alamat:</strong><br><%=safe(alamat)%></p><p><strong>Telepon:</strong><br><%=safe(telepon)%></p><p><strong>Email:</strong><br><%=safe(email)%></p><p><strong>Website:</strong><br><%=safe(website)%></p><div class="content-text"><p><%=htmlText(cfg("website_kontak_person", "Kontak resmi kampus dilayani melalui nomor telepon, email, dan alamat yang tercantum pada halaman ini. Apabila institusi memiliki beberapa lokasi kampus, alamat dapat dicantumkan secara berurutan dalam konfigurasi kontak website."))%></p></div></div></div>
			<div class="col-lg-7"><iframe class="map" loading="lazy" src="https://maps.google.com/maps?q=<%=URLEncoder.encode(mapQuery, "UTF-8")%>&amp;output=embed"></iframe></div>
		</div>
	</div>
</section>
<footer class="footer"><div class="container d-flex flex-wrap justify-content-between gap-3"><div><strong><%=safe(namaPt)%></strong><br><%=safe(alamat)%></div><div>&copy; <%=new java.text.SimpleDateFormat("yyyy").format(new Date())%> <%=safe(namaPt)%>. Seluruh hak cipta dilindungi.</div></div></footer>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
