<!--begin::Col-->
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Calendar"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="java.util.Map"%>
<%@ page import="ais.common.Common" %>
<%
String fak = request.getParameter("sekolah") == null ? "-1" : request.getParameter("sekolah");

Sekolah sekolahData = fak.equals("-1")
		? null
		: (Sekolah) ConstantValues.ambil(Sekolah.class.getName(), Long.parseLong(fak));


%>
<div class="col-xl-12">
	<!--begin::Chart widget 18-->
	<div class="card card-flush h-xl-100">
		<!--begin::Body-->
		<div class="card-body pb-2">
			<form action="" class="form">
				<div class="row">
					<div class="col-lg-3">
						<div class="form-group mb-2">
							<label for="sekolah"><%= Common.getBahasaConfig("Sekolah") %></label> <select
								onchange="reloadSiswa();" id="sekolah_siswa"
								class="form-control form-control-sm">
								<%
								Map<Long, Sekolah> map = ConstantValues.ambilBerdasarClass(Sekolah.class);
								for (Sekolah sekolah : map.values()) {
									if (sekolah.getAktif()) {
								%>
								<option
									<%=fak.equalsIgnoreCase(sekolah.getId().toString()) ? "selected" : ""%>
									value="<%=sekolah.getId()%>"><%=sekolah.getNama()%></option>
								<%
								}
								}
								%>
								<option <%=fak.equalsIgnoreCase("-1") ? "selected" : ""%>
									value="-1">Semua</option>
							</select>
						</div>
					</div>
					
					<div class="col-lg-4">
						<div class="form-group">
							<label for="tahun"><%= Common.getBahasaConfig("Tahun Masuk") %></label>
							<div class="d-flex align-items-center">
								<div class="flex-grow-1">
									<select name="" onchange="reloadSiswa();"
										id="angkatan_siswa"
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
									<select name="" onchange="reloadSiswa();"
										id="angkatansd_siswa"
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
			<div id="siswa_data"
				class="h-500px w-100 min-h-auto ps-4 pe-6"></div>
			<!--end::Chart-->
		</div>
		<div class="card-body pt-3">
			<table id="siswa_data_table"
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


function reloadSiswa(){
	
	document.getElementById("siswa_data").innerHTML = "";
	document.getElementById("siswa_data_table").innerHTML = "";
	
	
	var sekolah = document.getElementById("sekolah_siswa").value;
	var angkatan = document.getElementById("angkatan_siswa").value;
	var angkatansd = document.getElementById("angkatansd_siswa").value;
	
	var xhttp = new XMLHttpRequest();
	  xhttp.onreadystatechange = function() {
	    if (this.readyState == 4 && this.status == 200) {
	     var data = this.responseText.trim();
	     const obj = JSON.parse(data);
	     categories = obj.categories;
	     series = obj.series;
	     document.getElementById("siswa_data_table").innerHTML = obj.tr;
	     Siswa.init();
	    }
	  };
	  xhttp.open("GET", "<%=(request.getContextPath() + "/ux/dashboard/service/siswa.jsp")%>?sekolah="+sekolah+"&angkatan="+angkatan+"&angkatansd="+angkatansd, true);
	  xhttp.send();
}


var Siswa = function () {
	  var e = {
	    self: null,
	    rendered: !1
	  }

	  const t = function (e) {
	    var t = document.getElementById("siswa_data");
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

	"undefined" != typeof module && (module.exports = Siswa)

	KTUtil.onDOMContentLoaded((function () {
		reloadSiswa();
	}));

</script>