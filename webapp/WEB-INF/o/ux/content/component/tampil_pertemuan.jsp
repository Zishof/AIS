<%@page import="ais.common.YouTubeHelper"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.database.model.Pertemuan"%>
<%
Pertemuan pertemuan = (Pertemuan) request.getAttribute("pertemuan");
request.setAttribute("pertemuan", pertemuan);
String waktu = (pertemuan.getTanggal() == null ? "-"
		: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
		+ Common.dateFormat6.get().format(pertemuan.getTanggal())) + " "
		+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
				: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai()));
%>

<div class="card mb-5 mb-xl-10">
	<!--begin::details-->
	<div class="card shadow-sm">
		<div class="card-header collapsible cursor-pointer rotate"
			data-bs-toggle="collapse" data-bs-target="#kt_docs_card_collapsible2">
			<h3 class="card-title">
				Pertemuan ke
				<%=pertemuan.getPertemuanKe()%>,
				<%=pertemuan.info()%></h3>
			<div class="card-toolbar">
				<!--begin::Date-->
				<span class="text-gray-400 fw-semibold d-block"><%=waktu%></span>
				<!--end::Date-->
			</div>
		</div>
		<div id="kt_docs_card_collapsible2" class="collapse show">

			<div class="d-flex flex-stack mb-3">

				<div class="card-body"><%=pertemuan.getTopik() == null || pertemuan.getTopik().isEmpty()
		? "Belum ada informasi topik pembahasan pada pertemuan ini.."
		: pertemuan.getTopik()%>

				</div>

				<jsp:include
					page="/WEB-INF/o/ux/content/elearning_detail/quick_action.jsp">
					<jsp:param value="${pertemuan}" name="pertemuan" />
				</jsp:include>

			</div>

			<div class="card-footer">

				<%=pertemuan.getCatatan() == null || pertemuan.getCatatan().isEmpty() ? ""
		: "Catatan:<br>" + pertemuan.getCatatan()%>



				<%
				LampiranLain lain = LampiranLain.ambil(pertemuan.getId(), LampiranLain.CATATAN_PERKULIAHAN);
				if (lain != null && lain.getId() != null) {

					String n = lain.getNama() != null && lain.getNama().trim().equalsIgnoreCase("Berupa link file") ? lain.getLink()
					: lain.getNama();

					if (n == null) {
						n = lain.getNama();
					}
					n = n.length() > 50 ? n.substring(0, 50) + "..." : n;

					String iconAwesome = "fa " + LampiranLain.iconAwesome(n);
					if (lain.getLink().contains("yout")) {
						iconAwesome = "fab " + LampiranLain.iconAwesome(n);
					}

					if (lain.getNama().toLowerCase().endsWith("png") || lain.getNama().toLowerCase().endsWith("jpg")
					|| lain.getNama().toLowerCase().endsWith("jpeg") || lain.getNama().toLowerCase().endsWith("gif")
					|| lain.getNama().toLowerCase().endsWith("svg") || lain.getNama().toLowerCase().endsWith("tiff")) {
				%>
				<div class="image">
					<img alt="image" class="img-responsive" width="200px"
						src="<%=lain.createLinkUri()%>" />
				</div>
				<%
				} else if (lain.getLink().contains("yout")) {
				String contentVideo = YouTubeHelper.convertoToEmbed(lain.getLink());
				%>
				<div class="image">
					<%=contentVideo%>
				</div>
				<%
				}
				%>

				<a href="<%=lain.createLinkUri()%>" target="_blank"><i
					class="<%=iconAwesome%> fa-fw"></i> <%=n%></a>

				<%
				}
				%>

			</div>
		</div>


		<div class="card-footer"></div>

	</div>
	<!--end::details-->
</div>