<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.obe.CapaianPembelajaranLulusan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%!
	private String eh(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
%><%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	out.println("<div class='alert alert-danger'>" + Common.getBahasaConfig("Sesi Anda telah berakhir.") + "</div>");
	return;
}
String rnd = Common.getGeneratedBarCode(7);
String idPerk = request.getParameter("perkuliahan");
if (idPerk == null || idPerk.trim().isEmpty()) {
	out.println("<div class='alert alert-danger'>" + Common.getBahasaConfig("Parameter perkuliahan tidak valid.") + "</div>");
	return;
}

boolean bolehEdit = tbmuser.getDosen() != null || Common.getApakahAdminLain(tbmuser);

Perkuliahan p = null;
Matakuliah mk = null;
List<String[]> cpmkList = new ArrayList<String[]>(); // [kode, nama]
Map<String, JSONObject> cqiMap = new HashMap<String, JSONObject>();
String namaMk = "-", namaDosen = "-";

Session sess = HibernateUtil.openSession();
try {
	p = (Perkuliahan) sess.get(Perkuliahan.class, Long.parseLong(idPerk.trim()));
	if (p != null) {
		mk = p.getMatakuliah();
		if (mk != null) {
			namaMk = (mk.getKode() != null ? mk.getKode() + " - " : "") + (mk.getNama() != null ? mk.getNama() : "-");
			if (mk.getCapaianPembelajaranLulusan() != null && !mk.getCapaianPembelajaranLulusan().trim().isEmpty()) {
				for (String id : mk.getCapaianPembelajaranLulusan().split(",")) {
					if (id == null || id.trim().isEmpty()) continue;
					CapaianPembelajaranLulusan cp = (CapaianPembelajaranLulusan) GeneralValueObject.ambilData(CapaianPembelajaranLulusan.class, id.trim(), true);
					if (cp == null) continue;
					String kode = cp.getKode() != null ? cp.getKode() : ("CPMK" + cp.getId());
					cpmkList.add(new String[]{ kode, cp.getNama() != null ? cp.getNama() : "" });
				}
			}
		}
		try { if (p.getDosen1() != null && p.getDosen1().getNama() != null) namaDosen = p.getDosen1().getNama(); } catch (Exception e) {}
		// CQI existing
		try {
			if (p.getCqiData() != null && p.getCqiData().trim().startsWith("[")) {
				JSONArray arr = new JSONArray(p.getCqiData());
				for (int i = 0; i < arr.length(); i++) {
					JSONObject o = arr.optJSONObject(i);
					if (o != null) cqiMap.put(o.optString("cpmk", ""), o);
				}
			}
		} catch (Exception e) {}
	}
} finally {
	try { if (sess.isConnected()) sess.disconnect(); } catch (Exception e) {}
	try { if (sess.isOpen()) sess.close(); } catch (Exception e) {}
}

if (p == null || mk == null) {
	out.println("<div class='alert alert-warning'>" + Common.getBahasaConfig("Data perkuliahan tidak ditemukan.") + "</div>");
	return;
}
String[] statusOpsi = { "Planned", "On Progress", "Done" };
%>

<div class="container-fluid py-2">
	<div class="card border-0 shadow-sm rounded-4 mb-3 bg-primary bg-gradient text-white">
		<div class="card-body p-3 d-flex justify-content-between align-items-center flex-wrap gap-2">
			<div>
				<div class="text-uppercase fw-bold text-white-50" style="font-size:.72rem;letter-spacing:1px;"><%= Common.getBahasaConfig("Analisis CQI per CPMK") %></div>
				<h5 class="fw-bold mb-0"><%= eh(namaMk) %></h5>
				<small class="text-white-50"><i class="fas fa-chalkboard-teacher me-1"></i><%= eh(namaDosen) %></small>
			</div>
			<% if (bolehEdit) { %>
			<div class="d-flex gap-2">
				<button type="button" id="btnGenCqiAi<%=rnd%>" class="btn fw-bold rounded-pill px-3 shadow-sm text-white" style="background-color:#7c3aed;" onclick="generateCqiAi<%=rnd%>()">
					<i class="fas fa-magic me-1"></i><%= Common.getBahasaConfig("Generate CQI via AI") %>
				</button>
				<button type="button" id="btnSimpanCqi<%=rnd%>" class="btn btn-light fw-bold rounded-pill px-3 shadow-sm text-primary" onclick="simpanCqi<%=rnd%>()">
					<i class="fas fa-save me-1"></i><%= Common.getBahasaConfig("Simpan CQI") %>
				</button>
			</div>
			<% } %>
		</div>
	</div>

	<% if (cpmkList.isEmpty()) { %>
		<div class="alert alert-warning shadow-sm"><i class="fas fa-info-circle me-2"></i><%= Common.getBahasaConfig("Belum ada CPMK pada mata kuliah ini. Tetapkan CPMK terlebih dahulu.") %></div>
	<% } else { %>
	<div id="cqiWrap<%=rnd%>">
	<% for (int i = 0; i < cpmkList.size(); i++) {
		String kode = cpmkList.get(i)[0];
		String nama = cpmkList.get(i)[1];
		JSONObject ex = cqiMap.get(kode);
		String masalah = ex != null ? ex.optString("masalah", "") : "";
		String analisis = ex != null ? ex.optString("analisis", "") : "";
		String rencana = ex != null ? ex.optString("rencana", "") : "";
		String pj = ex != null ? ex.optString("pj", "") : "";
		String target = ex != null ? ex.optString("targetWaktu", "") : "";
		String status = ex != null ? ex.optString("status", "Planned") : "Planned";
		String adminKomentar = ex != null ? ex.optString("adminKomentar", "") : "";
	%>
		<div class="card border-0 shadow-sm rounded-4 mb-3 cqi-row<%=rnd%>" data-kode="<%= eh(kode) %>" data-admin="<%= eh(adminKomentar) %>">
			<div class="card-header bg-white border-bottom py-3 px-4">
				<span class="badge bg-primary me-2 px-3 py-2"><%= eh(kode) %></span><span class="fw-bold text-dark"><%= eh(nama) %></span>
			</div>
			<div class="card-body p-4">
				<div class="row g-3">
					<div class="col-md-4">
						<label class="form-label fw-bold small text-danger"><i class="fas fa-exclamation-triangle me-1"></i><%= Common.getBahasaConfig("Masalah / Gap") %></label>
						<textarea id="masalah_<%=i%>_<%=rnd%>" class="form-control" rows="3" <%= bolehEdit ? "" : "readonly" %>><%= eh(masalah) %></textarea>
					</div>
					<div class="col-md-4">
						<label class="form-label fw-bold small text-warning"><i class="fas fa-search me-1"></i><%= Common.getBahasaConfig("Analisis Penyebab") %></label>
						<textarea id="analisis_<%=i%>_<%=rnd%>" class="form-control" rows="3" <%= bolehEdit ? "" : "readonly" %>><%= eh(analisis) %></textarea>
					</div>
					<div class="col-md-4">
						<label class="form-label fw-bold small text-success"><i class="fas fa-lightbulb me-1"></i><%= Common.getBahasaConfig("Rencana Tindak Lanjut") %></label>
						<textarea id="rencana_<%=i%>_<%=rnd%>" class="form-control" rows="3" <%= bolehEdit ? "" : "readonly" %>><%= eh(rencana) %></textarea>
					</div>
					<div class="col-md-4">
						<label class="form-label fw-bold small text-muted"><%= Common.getBahasaConfig("Penanggung Jawab") %></label>
						<input type="text" id="pj_<%=i%>_<%=rnd%>" class="form-control" value="<%= eh(pj) %>" <%= bolehEdit ? "" : "readonly" %>>
					</div>
					<div class="col-md-4">
						<label class="form-label fw-bold small text-muted"><%= Common.getBahasaConfig("Target Waktu") %></label>
						<input type="text" id="target_<%=i%>_<%=rnd%>" class="form-control" value="<%= eh(target) %>" placeholder="<%= Common.getBahasaConfig("mis. Semester berikutnya") %>" <%= bolehEdit ? "" : "readonly" %>>
					</div>
					<div class="col-md-4">
						<label class="form-label fw-bold small text-muted"><%= Common.getBahasaConfig("Status") %></label>
						<select id="status_<%=i%>_<%=rnd%>" class="form-select" <%= bolehEdit ? "" : "disabled" %>>
							<% for (String so : statusOpsi) { %>
								<option value="<%= so %>" <%= so.equals(status) ? "selected" : "" %>><%= so %></option>
							<% } %>
						</select>
					</div>
				</div>
			</div>
		</div>
	<% } %>
	</div>
	<% } %>
</div>

<script>
	var cqiJml<%=rnd%> = <%= cpmkList.size() %>;

	window.generateCqiAi<%=rnd%> = function() {
		var btn = document.getElementById('btnGenCqiAi<%=rnd%>');
		var asli = btn ? btn.innerHTML : '';
		if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span><%= Common.getBahasaConfig("AI menganalisis...") %>'; }
		var body = new URLSearchParams(); body.append('perkuliahan', '<%= idPerk %>');
		fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_generate_cqi_ai', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body.toString() })
		.then(function(r){ return r.json(); })
		.then(function(d){
			if (!d || d.status !== 'success' || !d.data) throw new Error(d && d.message ? d.message : '<%= Common.getBahasaConfigJS("Gagal generate CQI.") %>');
			var arr = d.data;
			var rows = document.querySelectorAll('.cqi-row<%=rnd%>');
			var byKode = {};
			for (var i = 0; i < arr.length; i++) { if (arr[i] && arr[i].cpmk) byKode[String(arr[i].cpmk).trim().toUpperCase()] = arr[i]; }
			var terisi = 0;
			for (var r = 0; r < rows.length; r++) {
				var kode = String(rows[r].getAttribute('data-kode')).trim().toUpperCase();
				var o = byKode[kode];
				if (!o && arr[r]) o = arr[r]; // fallback posisi
				if (!o) continue;
				var im = document.getElementById('masalah_' + r + '_<%=rnd%>');
				var ia = document.getElementById('analisis_' + r + '_<%=rnd%>');
				var ir = document.getElementById('rencana_' + r + '_<%=rnd%>');
				if (im && o.masalah != null) im.value = o.masalah;
				if (ia && o.analisis != null) ia.value = o.analisis;
				if (ir && o.rencana != null) ir.value = o.rencana;
				terisi++;
			}
			if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Analisis CQI terisi via AI. Tinjau lalu klik Simpan CQI.") %> (' + terisi + ')', 'bg-success text-white');
		})
		.catch(function(err){ console.error(err); if (typeof tampilkanToast === 'function') tampilkanToast('AI: ' + (err && err.message ? err.message : err), 'bg-danger text-white'); })
		.finally(function(){ if (btn) { btn.disabled = false; btn.innerHTML = asli; } });
	};

	window.simpanCqi<%=rnd%> = function() {
		var rows = document.querySelectorAll('.cqi-row<%=rnd%>');
		var arr = [];
		for (var r = 0; r < rows.length; r++) {
			var kode = rows[r].getAttribute('data-kode');
			var adminK = rows[r].getAttribute('data-admin') || '';
			var st = (document.getElementById('status_' + r + '_<%=rnd%>') || {}).value || 'Planned';
			arr.push({
				cpmk: kode,
				masalah: (document.getElementById('masalah_' + r + '_<%=rnd%>') || {}).value || '',
				analisis: (document.getElementById('analisis_' + r + '_<%=rnd%>') || {}).value || '',
				rencana: (document.getElementById('rencana_' + r + '_<%=rnd%>') || {}).value || '',
				pj: (document.getElementById('pj_' + r + '_<%=rnd%>') || {}).value || '',
				targetWaktu: (document.getElementById('target_' + r + '_<%=rnd%>') || {}).value || '',
				status: st || 'Planned',
				adminKomentar: adminK
			});
		}
		var btn = document.getElementById('btnSimpanCqi<%=rnd%>');
		var asli = btn ? btn.innerHTML : '';
		if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span><%= Common.getBahasaConfig("Menyimpan...") %>'; }
		var body = new URLSearchParams();
		body.append('perkuliahan', '<%= idPerk %>');
		body.append('data', JSON.stringify(arr));
		fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_simpan_cqi', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body.toString() })
		.then(function(r){ return r.json(); })
		.then(function(d){
			if (!d || d.status !== 'success') throw new Error(d && d.message ? d.message : '<%= Common.getBahasaConfigJS("Gagal menyimpan CQI.") %>');
			if (typeof tampilkanToast === 'function') tampilkanToast(d.message || '<%= Common.getBahasaConfigJS("Data CQI berhasil disimpan.") %>', 'bg-success text-white');
		})
		.catch(function(err){ console.error(err); if (typeof tampilkanToast === 'function') tampilkanToast('AI: ' + (err && err.message ? err.message : err), 'bg-danger text-white'); })
		.finally(function(){ if (btn) { btn.disabled = false; btn.innerHTML = asli; } });
	};
</script>
