<%@page import="java.util.TreeMap"%>
<%@page import="java.util.TreeSet"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.util.Calendar"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.common.Common"%>
<%@page
	import="ais.action.master.dashboard.admin.DashboardStatistikStatusMahasiswaPerJurusan"%>
<%@page import="java.util.List"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%
String ta = Common.getCurrentTahunAkademik();
boolean ganjil = Common.isNowSemensterGanjil();
String idsmt = request.getParameter("idsmt");
Integer angkatansd = Integer.parseInt(request.getParameter("angkatansd"));
Integer angkatan = Integer.parseInt(request.getParameter("angkatan"));

try {
	if (idsmt == null) {
		idsmt = ta.substring(0, 4) + (ganjil ? "1" : "2");
	}
} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/ux/dashboard/service/status_mahasiswa.jsp:29");
}

if (idsmt != null) {
	ta = (Integer.parseInt(idsmt.substring(0, 4))) + "/" + (Integer.parseInt(idsmt.substring(0, 4)) + 1);
	ganjil = Integer.parseInt(idsmt.substring(4, 5)) == 1;
}

PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);

List<Object[]> jurusans = DashboardStatistikStatusMahasiswaPerJurusan.ambilData(perguruanTinggi, ta,
		ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP, angkatan, angkatansd);

Double aktifTotal = 0.0;
Double cutiTotal = 0.0;
Double lulusTotal = 0.0;
Double drop_outTotal = 0.0;
Double tidak_aktifTotal = 0.0;
Double semuaTotal = 0.0;

JSONArray jsonArray = new JSONArray();

List<String> categories = new ArrayList<String>();
List<Double> categoriesAktif = new ArrayList<Double>();
List<Double> categoriesCuti = new ArrayList<Double>();
List<Double> categoriesLulus = new ArrayList<Double>();
List<Double> categoriesDo = new ArrayList<Double>();
List<Double> categoriesTa = new ArrayList<Double>();
String tr = "";
for (Object[] objects : jurusans) {

	Double aktif = ((Number) (objects[0] == null ? 0.0 : objects[0])).doubleValue();
	Double cuti = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();
	Double lulus = ((Number) (objects[2] == null ? 0.0 : objects[2])).doubleValue();
	Double drop_out = ((Number) (objects[3] == null ? 0.0 : objects[3])).doubleValue();

	Double tidak_aktif = ((Number) (objects[4] == null ? 0.0 : objects[4])).doubleValue();

	String jurusan = (objects[5] == null ? "" : objects[5]).toString();

	Long jurusanId = ((Number) (objects[6] == null ? -1L : objects[6])).longValue();
	Jurusan jurusan2 = (Jurusan) ConstantValues.ambil(Jurusan.class.getName(), jurusanId);
	Double total = aktif + cuti + lulus + drop_out + tidak_aktif;

	JSONObject jsonObject = new JSONObject();
	jsonObject.put("jurusan_id", jurusanId);
	jsonObject.put("jurusan", jurusan2 == null ? "" : jurusan2.getNama());

	jsonObject.put("aktif", aktif);
	jsonObject.put("cuti", cuti);
	jsonObject.put("lulus", lulus);
	jsonObject.put("drop_out", drop_out);
	jsonObject.put("tidak_aktif", tidak_aktif);
	jsonObject.put("total", total);

	jsonArray.put(jsonObject);

	tr += "<tr>" + "<td>" + (jurusan2 == null ? "" : jurusan2.getNama()) + "</td>" + "<td>" + aktif.intValue() + "</td>"
	+ "<td>" + cuti.intValue() + "</td>" + "<td>" + lulus.intValue() + "</td>" + "<td>" + drop_out.intValue()
	+ "</td>" + "<td>" + tidak_aktif.intValue() + "</td>" + "<td>" + total.intValue() + "</td>" + "</tr>";

	categories.add(jurusan2 == null ? "" : jurusan2.getNama());

	categoriesAktif.add(aktif);
	categoriesCuti.add(cuti);
	categoriesLulus.add(lulus);
	categoriesDo.add(drop_out);
	categoriesTa.add(tidak_aktif);

	aktifTotal += aktif;
	cutiTotal += cuti;
	lulusTotal += lulus;
	drop_outTotal += drop_out;
	tidak_aktifTotal += tidak_aktif;
	semuaTotal += total;
}

JSONObject jsonObject = new JSONObject();
/*
jsonObject.put("data", jsonArray);
jsonObject.put("aktifTotal", aktifTotal);
jsonObject.put("cutiTotal", cutiTotal);
jsonObject.put("lulusTotal", lulusTotal);
jsonObject.put("drop_outTotal", drop_outTotal);
jsonObject.put("tidak_aktifTotal", tidak_aktifTotal);
jsonObject.put("semuaTotal", semuaTotal);
*/

JSONArray series = new JSONArray();

JSONObject a = new JSONObject();
a.put("name", "Aktif");
a.put("data", categoriesAktif);
series.put(a);

a = new JSONObject();
a.put("name", "Cuti");
a.put("data", categoriesCuti);
series.put(a);

a = new JSONObject();
a.put("name", "Lulus");
a.put("data", categoriesLulus);
series.put(a);

a = new JSONObject();
a.put("name", "Drop Out");
a.put("data", categoriesDo);
series.put(a);

a = new JSONObject();
a.put("name", "Tidak Aktif");
a.put("data", categoriesTa);
series.put(a);

jsonObject.put("series", series);
jsonObject.put("categories", categories);
jsonObject.put("tr", tr);

out.println(jsonObject.toString());
%>