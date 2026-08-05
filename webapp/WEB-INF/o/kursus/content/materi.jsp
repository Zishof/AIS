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
<script src="<%=request.getContextPath() %>/js/pesan-formal.js"></script>
<%@page import="org.json.JSONArray"%>
<%@page import="ais.database.model.kursus.ProdukKursus"%>
<%
try {

	ProdukKursus produkKursus = (ProdukKursus) request.getAttribute("produkKursus");
	Boolean peserta = (Boolean) request.getAttribute("peserta");
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

<div id="accordion_lessons" role="tablist" class="add_bottom_45">

	<%
	int indexdata = 0;
	for (Long key : treeMap.keySet()) {
		indexdata++;
		KomponenProdukKursus komponenProdukKursusBiaya = (KomponenProdukKursus) ConstantValues
		.ambil(KomponenProdukKursus.class.getName(), key);

		String komponenProdukKursus = komponenProdukKursusBiaya.getNama();

		if (komponenProdukKursus.equals(KomponenProdukKursus.VIDEO)
		|| komponenProdukKursus.equals(KomponenProdukKursus.BUKU)
		|| komponenProdukKursus.equals(KomponenProdukKursus.EBOOK)
		|| komponenProdukKursus.equals(KomponenProdukKursus.LATIHAN_SOAL)
		|| komponenProdukKursus.equals(KomponenProdukKursus.UJIAN)) {

			List<JSONObject> jsonObjects = treeMap.get(komponenProdukKursusBiaya.getId());
	%>

	<div class="card">
		<div class="card-header" role="tab" id="headingOne<%=key%>">
			<h5 class="mb-0">
				<a <%=(indexdata == 1 ? "class=\"collapsed\"" : "")%>
					data-toggle="collapse" href="#collapseOne<%=key%>"
					aria-expanded="<%=(indexdata == 1 ? "true" : "false")%>"
					aria-controls="collapseOne<%=key%>"><i
					class="indicator <%=(indexdata == 1 ? " ti-plus" : " ti-minus")%>"></i>
					<%=komponenProdukKursusBiaya.getNama()%></a>
			</h5>
		</div>

		<div id="collapseOne<%=key%>"
			class="collapse<%=(indexdata == 1 ? " show" : "")%>" role="tabpanel"
			aria-labelledby="headingOne<%=key%>" data-parent="#accordion_lessons">
			<div class="card-body">
				<div class="list_lessons">
					<ul>

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

								if (komponenProdukKursus.equals(KomponenProdukKursus.VIDEO)) {

									LampiranLain lainMahasiswa = LampiranLain.ambil(komponenDataProdukKursus.getId(),
											KomponenDataProdukKursus.class.getName());
									if (lainMahasiswa != null) {
						%>

						<li><a 
						<%if(peserta){ %>
							href="<%=lainMahasiswa.createLinkUri()%>"
							<%} else { %> onclick="return tampilkanPesanGagalFormal('akses ke materi ini', '<%= Common.getBahasaConfig("Anda belum terdaftar sebagai peserta pada kursus ini sehingga tidak diizinkan mengakses materi.") %>', ['Lakukan pendaftaran sebagai peserta pada kursus ini terlebih dahulu.', 'Setelah terdaftar sebagai peserta, silakan muat ulang halaman ini.']);" <%} %>
							class="video"><%=komponenDataProdukKursus.getNama()%></a><span><%=Common.timeFormat.get().format(komponenDataProdukKursus.getDurasi())%></span></li>
						<%
						}
						} else if (komponenProdukKursus.equals(KomponenProdukKursus.EBOOK)) {

						LampiranLain lainMahasiswa = LampiranLain.ambil(komponenDataProdukKursus.getId(),
								KomponenDataProdukKursus.class.getName());
						if (lainMahasiswa != null) {
						%>
						<li><a 
						<%if(peserta){ %>
							href="<%=lainMahasiswa.createLinkUri()%>"
							<%} else { %> onclick="return tampilkanPesanGagalFormal('akses ke materi ini', '<%= Common.getBahasaConfig("Anda belum terdaftar sebagai peserta pada kursus ini sehingga tidak diizinkan mengakses materi.") %>', ['Lakukan pendaftaran sebagai peserta pada kursus ini terlebih dahulu.', 'Setelah terdaftar sebagai peserta, silakan muat ulang halaman ini.']);" <%} %>
							target="_blank" class="txt_doc"><%=komponenDataProdukKursus.getNama()%></a><span><%=komponenDataProdukKursus.getKeterangan()%></span></li>
						<%
						}
						}

						}
						}
						}
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
	} catch (Exception e) {
	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/kursus/content/materi.jsp:154");
	}
	%>


</div>
<!-- /accordion -->