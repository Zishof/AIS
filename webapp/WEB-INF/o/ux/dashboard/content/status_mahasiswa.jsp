<!--begin::Col-->
<%@page import="ais.common.Common"%>
<%@page import="java.util.Calendar"%>
<div class="col-xl-12">
	<!--begin::Chart widget 18-->
	<div class="card card-flush h-xl-100">
		<!--begin::Header-->
		<div class="card-header pt-7">
			<!--begin::Title-->
			<h3 class="card-title align-items-start flex-column">
				<span class="card-label fw-bold text-gray-800">Status
					Mahasiswa</span> <span class="text-gray-400 mt-1 fw-semibold fs-6">Tiap
					Prodi</span>
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
		<div class="card-body py-2">
			<form action="" class="form">
				<div class="row">
					<div class="col-lg-4">
						<div class="form-group">
							<label for="tahun-ajaran"><%= Common.getBahasaConfig("Tahun Ajaran") %></label> <select name=""
								onchange="reloadStatusMahasiswa();" id="idsmt"
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

					<div class="col-lg-4">
						<div class="form-group">
							<label for="tahun"><%= Common.getBahasaConfig("Tahun") %></label>
							<div class="d-flex align-items-center">
								<div class="flex-grow-1">
									<select name="" onchange="reloadStatusMahasiswa();"
										id="angkatan" class="form-control form-control-sm">

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
									<select name="" onchange="reloadStatusMahasiswa();"
										id="angkatansd" class="form-control form-control-sm">
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
			<div id="status-mahasiswa" class="h-325px w-100 min-h-auto ps-4 pe-6"></div>
			<!--end::Chart-->
		</div>
		<!--end: Card Body-->
		<div class="card-body pt-3">
			<table class="table table-striped mb-0 table-hover table-sm">
				<thead class="bg-dark text-white">
					<tr>
						<th><%= Common.getBahasaConfig("Prodi") %></th>
						<th><%= Common.getBahasaConfig("Aktif") %></th>
						<th><%= Common.getBahasaConfig("Cuti") %></th>
						<th><%= Common.getBahasaConfig("Lulus") %></th>
						<th><%= Common.getBahasaConfig("Drop Out") %></th>
						<th><%= Common.getBahasaConfig("Tidak Aktif") %></th>
						<th><%= Common.getBahasaConfig("Total") %></th>
					</tr>
				</thead>
				<tbody id="status-mahasiswa-tbody">
					
				</tbody>
			</table>
		</div>
	</div>
	<!--end::Chart widget 18-->
</div>
<!--end::Col-->

<script type="text/javascript">

var categories = [];
var series = [];

function reloadStatusMahasiswa(){
	
	document.getElementById("status-mahasiswa").innerHTML = "";
	document.getElementById("status-mahasiswa-tbody").innerHTML = "";
	
	
	var idsmt = document.getElementById("idsmt").value;
	var angkatan = document.getElementById("angkatan").value;
	var angkatansd = document.getElementById("angkatansd").value;
	
	var xhttp = new XMLHttpRequest();
	  xhttp.onreadystatechange = function() {
	    if (this.readyState == 4 && this.status == 200) {
	     var data = this.responseText.trim();
	     const obj = JSON.parse(data);
	     categories = obj.categories;
	     series = obj.series;
	     document.getElementById("status-mahasiswa-tbody").innerHTML = obj.tr;
	      StatusMahasiswa.init();
	    }
	  };
	  xhttp.open("GET", "<%=(request.getContextPath() + "/ux/dashboard/service/status_mahasiswa.jsp")%>?idsmt="+idsmt+"&angkatan="+angkatan+"&angkatansd="+angkatansd, true);
	  xhttp.send();
}

var StatusMahasiswa = function () {
	  var e = {
	    self: null,
	    rendered: !1
	  }

	  const t = function (e) {
	    var t = document.getElementById("status-mahasiswa");
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
	        stacked: true,
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
	      // dataLabels: {
	      //   enabled: !0,
	      //   offsetY: -28,
	      //   style: {
	      //     fontSize: "13px",
	      //     colors: [l]
	      //   },
	      //   formatter: function (e) {
	      //     return e
	      //   }
	      // },
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
	            fontSize: "12px"
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
	        title: {
	          text: "Jumlah Mahasiswa",
	        },
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
	        e.rendered && e.self.destroy()
	        t(e)
	      }))
	    }
	  }
	}();

	"undefined" != typeof module && (module.exports = StatusMahasiswa)

	KTUtil.onDOMContentLoaded((function () {
		reloadStatusMahasiswa();
	}));
</script>