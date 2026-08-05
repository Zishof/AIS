<!--begin::Col-->
<%@page import="ais.database.model.Jurusan"%>
<%@page import="java.util.Map"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.common.ConstantValues"%>
<%@ page import="ais.common.Common" %>
<%
String fak = request.getParameter("fakultas") == null ? "-1" : request.getParameter("fakultas");
String jur = request.getParameter("prodi") == null ? "-1" : request.getParameter("prodi");
Fakultas fakultasData = fak.equals("-1")
		? null
		: (Fakultas) ConstantValues.ambil(Fakultas.class.getName(), Long.parseLong(fak));
Jurusan jurusanData = jur.equals("-1")
		? null
		: (Jurusan) ConstantValues.ambil(Jurusan.class.getName(), Long.parseLong(jur));

if (jurusanData != null && jurusanData.getFakultas() != null) {
	fak = jurusanData.getFakultas().getId().toString();
	fakultasData = jurusanData.getFakultas();
}
%>
<div class="col-xl-6">
	<!--begin::Chart widget 18-->
	<div class="card card-flush h-xl-100">
		<!--begin::Header-->
		<div class="card-header pt-7">
			<!--begin::Title-->
			<h3 class="card-title align-items-start flex-column">
				<span class="card-label fw-bold text-gray-800">Status Dosen</span>
			</h3>
			<!--end::Title-->
			<div>
				<a href="javascript:;" class="btn btn-primary btn-sm"> <i
					class="fas fa-list"></i> Lihat Rinci
				</a>
			</div>
		</div>
		<!--end::Header-->
		<!--begin::Body-->
		<div class="px-8 py-2">
			<form action="" class="form">
				<div class="row">
					<div class="col-lg-6">
						<div class="form-group mb-2">
							<label for="fakultas"><%= Common.getBahasaConfig("Fakultas") %></label> <select
								onchange="reloadStatusDosen();" id="fakultas_status-dosen-all"
								class="form-control form-control-sm">
								<%
								Map<Long, Fakultas> map = ConstantValues.ambilBerdasarClass(Fakultas.class);
								for (Fakultas fakultas : map.values()) {
									if (fakultas.getAktif()) {
								%>
								<option
									<%=fak.equalsIgnoreCase(fakultas.getId().toString()) ? "selected" : ""%>
									value="<%=fakultas.getId()%>"><%=fakultas.getNama()%></option>
								<%
								}
								}
								%>
								<option <%=fak.equalsIgnoreCase("-1") ? "selected" : ""%>
									value="-1">Semua</option>
							</select>
						</div>
					</div>
					<div class="col-lg-6">
						<div class="form-group mb-2">
							<label for="prodi"><%= Common.getBahasaConfig("Prodi") %></label> <select name="" id="prodi_status-dosen-all"
								onchange="reloadStatusDosen();"
								class="form-control form-control-sm">
								<%
								Map<Long, Jurusan> map1 = ConstantValues.ambilBerdasarClass(Jurusan.class);
								for (Jurusan jurusan : map1.values()) {
									if (jurusan.getAktif()) {
								%>
								<option
									<%=jur.equalsIgnoreCase(jurusan.getId().toString()) ? "selected" : ""%>
									value="<%=jurusan.getId()%>"><%=jurusan.getNama()%></option>
								<%
								}
								}
								%>
								<option <%=jur.equalsIgnoreCase("-1") ? "selected" : ""%>
									value="-1">Semua</option>
							</select>
						</div>
					</div>
				</div>
			</form>
		</div>
		<div class="card-body d-flex align-items-end px-0 pt-3 pb-5">
			<!--begin::Chart-->
			<div id="status-dosen-all" class="h-400px w-100 min-h-auto ps-4 pe-6"></div>
			<!--end::Chart-->
		</div>
		<!--end: Card Body-->
	</div>
	<!--end::Chart widget 18-->
</div>
<!--end::Col-->


<script type="text/javascript">


var categories = [];
var series = [];

function reloadStatusDosen(){
	
	document.getElementById("status-dosen-all").innerHTML = "";
	var fakultas = document.getElementById("fakultas_status-dosen-all").value;
	var prodi = document.getElementById("prodi_status-dosen-all").value;
	
	var xhttp = new XMLHttpRequest();
	  xhttp.onreadystatechange = function() {
	    if (this.readyState == 4 && this.status == 200) {
	     var data = this.responseText.trim();
	     const obj = JSON.parse(data);
	     categories = obj.categories;
	     series = obj.series;
	     
	     StatusDosenAll.init();
	    }
	  };
	  xhttp.open("GET", "<%=(request.getContextPath() + "/ux/dashboard/service/status_dosen.jsp")%>?fakultas="+fakultas+"&prodi="+prodi, true);
	  xhttp.send();
}

var StatusDosenAll = function () {
	  var e = {
	    self: null,
	    rendered: !1
	  }

	  const t = function (e) {
	    var t = document.getElementById("status-dosen-all");
	    if (t == null) {
	      return
	    }

	    var a = parseInt(KTUtil.css(t, "height")),
	      l = KTUtil.getCssVariableValue("--kt-gray-900"),
	      r = KTUtil.getCssVariableValue("--kt-border-dashed-color")

	    var o = {
	      series: series,
	      chart: {
	        fontFamily: "inherit",
	        type: "pie",
	        height: a,
	        toolbar: {
	          show: !1
	        }
	      },
	      plotOptions: {
	        bar: {
	          horizontal: !1,
	          columnWidth: ["28%"],
	          borderRadius: 5,
	          dataLabels: {
	            position: "top"
	          },
	          startingShape: "flat"
	        }
	      },
	      legend: {
	        show: true,
	      },

	      // stroke: {
	      //   show: !0,
	      //   width: 2,
	      //   colors: ["transparent"]
	      // },
	      labels: categories,
	      // tooltip: {
	      //   style: {
	      //     fontSize: "12px"
	      //   },
	      //   y: {
	      //     formatter: function (e) {
	      //       return e
	      //     }
	      //   }
	      // },
	      // colors: [KTUtil.getCssVariableValue("--kt-primary"), KTUtil.getCssVariableValue("--kt-primary-light")],
	      grid: {
	        borderColor: r,
	        strokeDashArray: 4,
	        yaxis: {
	          lines: {
	            show: !0
	          }
	        }
	      }
	    };

	    e.self = new ApexCharts(t, o)

	    setTimeout((function () {
	      e.self.render(), e.rendered = !0
	    }), 200)
	  };

	  return {
	    init: function () {
	      t(e)

	      KTThemeMode.on("kt.thememode.change", (function () {
	        e.rendered && e.self.destroy(), t(e)
	      }))
	    }
	  }
	}();

	"undefined" != typeof module && (module.exports = StatusDosenAll)

	KTUtil.onDOMContentLoaded((function () {
		reloadStatusDosen();
	}));
</script>