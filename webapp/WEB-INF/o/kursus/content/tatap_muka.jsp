<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.library.Item"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.database.model.kursus.KomponenDataProdukKursus"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.TreeMap"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.kursus.KomponenProdukKursus"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="ais.database.model.kursus.ProdukKursus"%>
<%
try {

	ProdukKursus produkKursus = (ProdukKursus) request.getAttribute("produkKursus");
	Boolean peserta = (Boolean) request.getAttribute("peserta");
	request.setAttribute("peserta", peserta);
	JSONArray array = new JSONArray(produkKursus.getHargaKomponens());

	TreeMap<Long, List<JSONObject>> treeMap = new TreeMap<Long, List<JSONObject>>();

	for (int i = 0; i < array.length(); i++) {

		JSONObject jsonObject = array.getJSONObject(i);

		Long key = null;
		if (!jsonObject.isNull("key")) {
	key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
		}

		if (key != null) {

	KomponenProdukKursus komponenProdukKursusBiaya = (KomponenProdukKursus) (jsonObject
			.isNull("komponenProdukKursus") ? null
					: ConstantValues.ambil(KomponenProdukKursus.class.getName(),
							ais.common.CommonJSONUtil.ambilLong(jsonObject,"komponenProdukKursus")));
	if (komponenProdukKursusBiaya != null) {
		List<JSONObject> jsonObjects = treeMap.get(komponenProdukKursusBiaya.getId());
		if (jsonObjects == null) {
			jsonObjects = new ArrayList<JSONObject>();
			treeMap.put(komponenProdukKursusBiaya.getId(), jsonObjects);
		}
		jsonObjects.add(jsonObject);
	}
		}
	}
%>

<div id="accordion_lessons_tatap_muka" role="tablist"
	class="add_bottom_45">

	<%
	int indexdata = 1;
	for (Long key1 : treeMap.keySet()) {

		KomponenProdukKursus komponenProdukKursusBiaya = (KomponenProdukKursus) ConstantValues
		.ambil(KomponenProdukKursus.class.getName(), key1);

		List<JSONObject> jsonObjects = treeMap.get(komponenProdukKursusBiaya.getId());
	%>



	<%
	for (JSONObject jsonObject : jsonObjects) {

		JSONArray arrayDetail = jsonObject.isNull("arrayDetail") ? new JSONArray() : jsonObject.getJSONArray("arrayDetail");

		for (int i = 0; i < arrayDetail.length(); i++) {
			final int index = i;
			final JSONObject jsonObjectDetail = arrayDetail.getJSONObject(i);
			Long keyDetail = null;
			if (!jsonObjectDetail.isNull("key")) {
		keyDetail = ais.common.CommonJSONUtil.ambilLong(jsonObjectDetail,"key");
			}

			if (keyDetail != null) {
		KomponenDataProdukKursus komponenDataProdukKursus = (KomponenDataProdukKursus) (jsonObjectDetail
				.isNull("komponenDataProdukKursus") ? null
						: ConstantValues.ambil(KomponenDataProdukKursus.class.getName(),
								ais.common.CommonJSONUtil.ambilLong(jsonObjectDetail,"komponenDataProdukKursus")));

		if (komponenDataProdukKursus != null) {

			String komponenProdukKursus = komponenDataProdukKursus.getKomponenProdukKursus();

			if (komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)) {

				indexdata++;

				Long key = komponenDataProdukKursus.getId();
	%>

	<div class="card">
		<div class="card-header" role="tab" id="headingTwo<%=key%>">
			<h5 class="mb-0">
				<a <%=(indexdata == 1 ? "class=\"collapsed\"" : "")%>
					data-toggle="collapse" href="#collapseTwo<%=key%>"
					aria-expanded="<%=(indexdata == 1 ? "true" : "false")%>"
					aria-controls="collapseTwo<%=key%>"><i
					class="indicator <%=(indexdata == 1 ? " ti-plus" : " ti-minus")%>"></i>
					<%=komponenDataProdukKursus.getNama()%></a>
			</h5>
		</div>

		<div id="collapseTwo<%=key%>"
			class="collapse<%=(indexdata == 1 ? " show" : "")%>" role="tabpanel"
			aria-labelledby="headingTwo<%=key%>"
			data-parent="#accordion_lessons_tatap_muka">
			<div class="card-body">
				<div class="list_lessons">

					<ul>
						<%
						TreeMap<String, Long> pertemuans = komponenDataProdukKursus.ambilPertemuan();

						for (Long pertemuanid : pertemuans.values()) {
						%>
						<jsp:include page="/WEB-INF/o/kursus/content/pertemuan.jsp">
							<jsp:param value="<%=pertemuanid%>" name="pertemuanid" />
							<jsp:param value="${peserta}" name="peserta" />
						</jsp:include>
						<%
						}
						%>
					</ul>
				</div>
			</div>
		</div>
	</div>

	<%
	}

	}
	}
	}
	}
	%>



	<%
	}
	} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/kursus/content/tatap_muka.jsp:149");
	}
	%>


</div>
<!-- /accordion -->