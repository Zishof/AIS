<!--begin::Col-->
<%@page import="ais.common.Common"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Calendar"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="java.util.Map"%>
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
<div class="col-xl-12">
	<!--begin::Chart widget 18-->
	<div class="card card-flush h-xl-100">
		<!--begin::Header-->
		<div class="card-header pt-7">
			<!--begin::Title-->
			<h3 class="card-title align-items-start flex-column">
				<span class="card-label fw-bold text-gray-800">Maksimal IPK Mahasiswa</span>
			</h3>
			<!--end::Title-->
			<div>
				<a href="javascript:popupCenter({url: '<%=request.getContextPath() %>/win.zul?win=ais.action.master.dashboard.admin.DashboardIpkMahasiswa', title: 'Book', w: 1200, h: 600});" class="btn btn-primary btn-sm"> <i
					class="fas fa-list"></i> Lihat Rinci
				</a>
			</div>
		</div>
		<!--end::Header-->
		<!--begin::Body-->
		<div class="card-body pb-2">
			<form action="" class="form">
				<div class="row">

					<div class="col-lg-3">
						<div class="form-group">
							<label for="tahun-ajaran"><%= Common.getBahasaConfig("Tahun Ajaran") %></label> <select name=""
								onchange="reloadMaxIpkMahasiswa();" id="idsmt_max_ipk_mahasiswa"
								class="form-control form-control-sm">

								<%
								String ta = Common.getCurrentTahunAkademik();
								boolean ganjil = Common.isNowSemensterGanjil();

								String idsmt = request.getParameter("idsmt");

								for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + 5; i >= ais.ui.util.WaktuUtil.getCalendar()
										.get(Calendar.YEAR) - 15; i--) {
									String v = (i - 1) + "/" + (i);

									String selectGanjil = "";
									String selectGenap = "";
									String selectSP = "";

									if (idsmt != null && idsmt.equals(i + "1")) {
										selectGanjil = "selected";
									} else if (idsmt != null && idsmt.equals(i + "2")) {
										selectGenap = "selected";
									} else if (idsmt != null && idsmt.equals(i + "3")) {
										selectSP = "selected";
									} else if ((ta.equalsIgnoreCase(v) && ganjil)) {
										selectGanjil = "selected";
									} else if ((ta.equalsIgnoreCase(v) && !ganjil)) {
										selectGenap = "selected";
									}

									out.println("<option value=\"" + ((i - 1) + "2") + "\" " + selectGenap + ">" + v + "/Genap</option>");

									out.println("<option value=\"" + ((i - 1) + "1") + "\" " + selectGanjil + ">" + v + "/Ganjil</option>");
								}
								%>

							</select>
						</div>
					</div>


					<div class="col-lg-3">
						<div class="form-group mb-2">
							<label for="fakultas"><%= Common.getBahasaConfig("Fakultas") %></label> <select
								onchange="reloadMaxIpkMahasiswa();" id="fakultas_max_ipk_mahasiswa"
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
					<div class="col-lg-3">
						<div class="form-group mb-2">
							<label for="prodi"><%= Common.getBahasaConfig("Prodi") %></label> <select name=""
								id="prodi_max_ipk_mahasiswa" onchange="reloadMaxIpkMahasiswa();"
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
					<div class="col-lg-4">
						<div class="form-group">
							<label for="tahun"><%= Common.getBahasaConfig("Tahun Masuk") %></label>
							<div class="d-flex align-items-center">
								<div class="flex-grow-1">
									<select name="" onchange="reloadMaxIpkMahasiswa();"
										id="angkatan_max_ipk_mahasiswa"
										class="form-control form-control-sm">

										<%
										Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
										int year = calendar.get(Calendar.YEAR) - 6;
										for (int i = calendar.get(Calendar.YEAR) - 20; i < calendar.get(Calendar.YEAR) + 2; i++) {
											String select = year == i ? "selected" : "";
											out.println("<option value=\"" + i + "\" " + select + ">" + i + "</option>");
										}
										%>
									</select>
								</div>
								<div class="px-2">s.d</div>
								<div class="flex-grow-1">
									<select name="" onchange="reloadMaxIpkMahasiswa();"
										id="angkatansd_max_ipk_mahasiswa"
										class="form-control form-control-sm">
										<%
										calendar = ais.ui.util.WaktuUtil.getCalendar();
										year = calendar.get(Calendar.YEAR);
										for (int i = calendar.get(Calendar.YEAR) - 20; i < calendar.get(Calendar.YEAR) + 2; i++) {
											String select = year == i ? "selected" : "";
											out.println("<option value=\"" + i + "\" " + select + ">" + i + "</option>");
										}
										%>
									</select>
								</div>
							</div>
						</div>
					</div>



				</div>
			</form>
		</div>
		<div class="card-body d-flex align-items-end px-0 pt-3 pb-5">
			<!--begin::Chart-->
			<div id="max_ipk_mahasiswa_data"
				class="h-500px w-100 min-h-auto ps-4 pe-6"></div>
			<!--end::Chart-->
		</div>
		<div class="card-body pt-3">
			<table id="max_ipk_mahasiswa_data_table"
				class="table table-striped mb-0 table-hover table-sm">
				<thead class="bg-dark text-white">
					<tr>
						<th><%= Common.getBahasaConfig("Prodi") %></th>
						<th>2017</th>
						<th>2018</th>
						<th>2019</th>
						<th>2020</th>
						<th>2021</th>
						<th>2022</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td>Informatika</td>
						<td>12</td>
						<td>200</td>
						<td>2</td>
						<td>7</td>
						<td>321</td>
						<td>321</td>
					</tr>

				</tbody>
			</table>
		</div>
		<!--end: Card Body-->
	</div>
	<!--end::Chart widget 18-->
</div>
<!--end::Col-->

<script type="text/javascript">

var categories = [];
var series = [];


function reloadMaxIpkMahasiswa(){
	
	document.getElementById("max_ipk_mahasiswa_data").innerHTML = "";
	document.getElementById("max_ipk_mahasiswa_data_table").innerHTML = "";
	
	
	var fakultas = document.getElementById("fakultas_max_ipk_mahasiswa").value;
	var prodi = document.getElementById("prodi_max_ipk_mahasiswa").value;
	var angkatan = document.getElementById("angkatan_max_ipk_mahasiswa").value;
	var angkatansd = document.getElementById("angkatansd_max_ipk_mahasiswa").value;
	var idsmt = document.getElementById("idsmt_max_ipk_mahasiswa").value;
	
	
	var xhttp = new XMLHttpRequest();
	  xhttp.onreadystatechange = function() {
	    if (this.readyState == 4 && this.status == 200) {
	     var data = this.responseText.trim();
	     const obj = JSON.parse(data);
	     categories = obj.categories;
	     series = obj.series;
	     document.getElementById("max_ipk_mahasiswa_data_table").innerHTML = obj.tr;
	     MaxIpkMahasiswa.init();
	    }
	  };
	  xhttp.open("GET", "<%=(request.getContextPath() + "/ux/dashboard/service/max_ipk_mahasiswa.jsp")%>?fakultas="+fakultas+"&prodi="+prodi+"&angkatan="+angkatan+"&angkatansd="+angkatansd+"&idsmt="+idsmt, true);
	  xhttp.send();
}


var MaxIpkMahasiswa = function () {
	  var e = {
	    self: null,
	    rendered: !1
	  }

	  const t = function (e) {
	    var t = document.getElementById("max_ipk_mahasiswa_data");
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
	        type: "bar",
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
	      dataLabels: {
	        enabled: !0,
	        offsetY: -28,
	        style: {
	          fontSize: "13px",
	          colors: [l]
	        },
	        formatter: function (e) {
	          return e
	        }
	      },
	      stroke: {
	        show: !0,
	        width: 2,
	        colors: ["transparent"]
	      },
	      xaxis: {
	        categories: categories,
	        axisBorder: {
	          show: !1
	        },
	        axisTicks: {
	          show: !1
	        },
	        labels: {
	          style: {
	            colors: KTUtil.getCssVariableValue("--kt-gray-500"),
	            fontSize: "13px"
	          }
	        },
	        crosshairs: {
	          fill: {
	            gradient: {
	              opacityFrom: 0,
	              opacityTo: 0
	            }
	          }
	        }
	      },
	      yaxis: {
	        // title: {
	        //   text: "Jumlah Dosen",
	        // },
	        labels: {
	          style: {
	            colors: KTUtil.getCssVariableValue("--kt-gray-500"),
	            fontSize: "13px"
	          },
	          formatter: function (e) {
	            return e
	          }
	        }
	      },
	      fill: {
	        opacity: 1
	      },
	      states: {
	        normal: {
	          filter: {
	            type: "none",
	            value: 0
	          }
	        },
	        hover: {
	          filter: {
	            type: "none",
	            value: 0
	          }
	        },
	        active: {
	          allowMultipleDataPointsSelection: !1,
	          filter: {
	            type: "none",
	            value: 0
	          }
	        }
	      },
	      tooltip: {
	        style: {
	          fontSize: "12px"
	        },
	        y: {
	          formatter: function (e) {
	            return e
	          }
	        }
	      },
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

	"undefined" != typeof module && (module.exports = MaxIpkMahasiswa)

	KTUtil.onDOMContentLoaded((function () {
		reloadMaxIpkMahasiswa();
	}));

</script>