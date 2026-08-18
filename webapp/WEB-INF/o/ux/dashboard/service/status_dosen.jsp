<%@page import="ais.database.model.Fakultas"%>
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
String fak = request.getParameter("fakultas") == null ? "-1" : request.getParameter("fakultas");
String jur = request.getParameter("prodi") == null ? "-1" : request.getParameter("prodi");
Fakultas fakultasData = fak.equals("-1") ? null
		: (Fakultas) ConstantValues.ambil(Fakultas.class.getName(), Long.parseLong(fak));
Jurusan jurusanData = jur.equals("-1") ? null
		: (Jurusan) ConstantValues.ambil(Jurusan.class.getName(), Long.parseLong(jur));
Session mySession = HibernateUtil.currentNativeSession();

List<IkatanKerjaDosen> ikatanKerjaDosens = ConstantValues.simpleList(
		mySession.createCriteria(IkatanKerjaDosen.class)
		.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
		.add(Restrictions.isNotNull("nama")).add(Restrictions.ne("nama", "")).addOrder(Order.desc("nama")),
		ConstantValues.class);
// mySession.disconnect();
if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
HibernateUtil.closeSession();

List<Object[]> jurusans = DashboardStatistikStatusDosenPerJurusan.ambilData(ikatanKerjaDosens, perguruanTinggi,
		fakultasData, jurusanData);

TreeSet<String> categories = new TreeSet<String>();
TreeMap<String, Double> series = new TreeMap<String, Double>();

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

		index = 0;
		int total = 0;
		for (IkatanKerjaDosen ikatanKerjaDosen : ikatanKerjaDosens) {
	Double status = ((Number) (objects[index] == null ? 0.0 : objects[index])).doubleValue();
	total += status.intValue();
	Double nilai = series.get(ikatanKerjaDosen.getNama());
	if (nilai == null) {
		nilai = 0.0;
	}
	nilai = nilai + status;
	series.put(ikatanKerjaDosen.getNama(), nilai);
	categories.add(ikatanKerjaDosen == null ? "" : ikatanKerjaDosen.getNama());
	index++;
		}

		
	}

}

JSONObject jsonObject = new JSONObject();

jsonObject.put("series", series.values());
jsonObject.put("categories", categories);

out.println(jsonObject.toString());
%>