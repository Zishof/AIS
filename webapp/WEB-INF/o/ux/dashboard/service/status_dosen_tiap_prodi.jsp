<%@page import="java.util.Map"%>
<%@page
	import="ais.action.master.dashboard.admin.DashboardStatistikStatusDosenPerJurusan"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.model.IkatanKerjaDosen"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
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
PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);

Session mySession = HibernateUtil.currentNativeSession();

List<IkatanKerjaDosen> ikatanKerjaDosens = ConstantValues.simpleList(
		mySession.createCriteria(IkatanKerjaDosen.class)
		.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
		.add(Restrictions.isNotNull("nama")).add(Restrictions.ne("nama", "")).addOrder(Order.desc("nama")),
		ConstantValues.class);
// mySession.disconnect();
if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
HibernateUtil.closeSession();

List<Object[]> jurusans = DashboardStatistikStatusDosenPerJurusan.ambilData(ikatanKerjaDosens, perguruanTinggi, null,
		null);

List<String> categories = new ArrayList<String>();
TreeMap<String, List<Double>> seriesData = new TreeMap<String, List<Double>>();
String tr = "<thead class=\"bg-dark text-white\"><tr>";
tr += "<th>Prodi</th>";
for (IkatanKerjaDosen ikatanKerjaDosen : ikatanKerjaDosens) {
	tr += "<th>" + ikatanKerjaDosen.getNama() + "</th>";
}
tr += "<th>TOTAL</th>";
tr += "</tr></thead><tbody>";
for (Object[] objects : jurusans) {

	int tot = 0;
	int index = 0;
	for (IkatanKerjaDosen ikatanKerjaDosen : ikatanKerjaDosens) {
		Double status = ((Number) (objects[index] == null ? 0.0 : objects[index])).doubleValue();
		tot += status.intValue();
		index++;
	}

	if (tot > 0) {
		String jurusan = (objects[ikatanKerjaDosens.size() + 1] == null ? "" : objects[ikatanKerjaDosens.size() + 1])
		.toString();
		Long jurusanId = ((Number) (objects[ikatanKerjaDosens.size() + 2] == null ? -1L
		: objects[ikatanKerjaDosens.size() + 2])).longValue();
		Jurusan jurusan2 = (Jurusan) ConstantValues.ambil(Jurusan.class.getName(), jurusanId);
		tr += "<tr>" + "<td>" + (jurusan2 == null ? "" : jurusan2.getNama()) + "</td>";

		index = 0;
		int total = 0;
		for (IkatanKerjaDosen ikatanKerjaDosen : ikatanKerjaDosens) {
	Double status = ((Number) (objects[index] == null ? 0.0 : objects[index])).doubleValue();
	total += status.intValue();
	List<Double> datas = seriesData.get(ikatanKerjaDosen.getNama());
	if (datas == null) {
		datas = new ArrayList<Double>();
	}
	datas.add(status);
	seriesData.put(ikatanKerjaDosen.getNama(), datas);

	tr += "<td>" + status.intValue() + "</td>";
	index++;
		}

		tr += "<td>" + total + "</td>";

		tr += "</tr>";

		categories.add(jurusan2 == null ? "" : jurusan2.getNama());
	}

}
tr += "</tbody>";

JSONObject jsonObject = new JSONObject();

JSONArray series = new JSONArray();

for (String key : seriesData.keySet()) {
	JSONObject a = new JSONObject();
	a.put("name", key);
	a.put("data", seriesData.get(key));
	series.put(a);
}

jsonObject.put("series", series);
jsonObject.put("categories", categories);
jsonObject.put("tr", tr);

out.println(jsonObject.toString());
%>