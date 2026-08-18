<!--begin::Col-->
<%@page import="java.util.Date"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="java.util.Calendar"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="java.util.Map"%>
<%@page import="ais.common.Common"%>

<%
Date kemaren = null;
if (request.getParameter("kemaren") == null) {
	Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
	calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 7);
	kemaren = calendar.getTime();
}
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
<!--start::Col-->
<div class="col-xl-6" id="kunjungan_pengguna">
	<!--begin::Chart widget 18-->
	<div class="card card-flush h-xl-100">
		<!--begin::Header-->
		<div class="card-header pt-7">
			<!--begin::Title-->
			<h3 class="card-title align-items-start flex-column">
				<span class="card-label fw-bold text-gray-800">Akses Pengguna</span>
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
					<div class="col-lg-6">
						<div class="form-group mb-2">
							<label for="fakultas"><%= Common.getBahasaConfig("Fakultas") %></label> <select
								onchange="reloadKunjungan();" id="fakultas"
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
							<label for="prodi"><%= Common.getBahasaConfig("Prodi") %></label> <select name="" id="prodi"
								onchange="reloadKunjungan();"
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
					<div class="col-lg-12">
						<div class="form-group">
							<label for="tahun"><%= Common.getBahasaConfig("Tanggal") %></label>
							<div class="d-flex align-items-center">
								<div class="flex-grow-1">
									<input type="date" name="tanggal_awal"
										onchange="reloadKunjungan();"
										value="<%=Common.databaseDateFormat.get().format(kemaren)%>"
										class="form-control form-control-sm" id="tanggal_awal">
								</div>
								<div class="px-2">s.d</div>
								<div class="flex-grow-1">
									<input type="date" name="tanggal_akhir"
										onchange="reloadKunjungan();"
										value="<%=Common.databaseDateFormat.get().format(new Date())%>"
										class="form-control form-control-sm" id="tanggal_akhir">
								</div>
							</div>
						</div>
					</div>
				</div>
			</form>
		</div>
		<div class="card-body d-flex align-items-end px-0 pt-3 pb-5">
			<!--begin::Chart-->
			<div id="akses-pengguna" class="h-325px w-100 min-h-auto ps-4 pe-6"></div>
			<!--end::Chart-->
		</div>
		<!--end: Card Body-->
	</div>
	<!--end::Chart widget 18-->
</div>
<!--end::Col-->

<script type="text/javascript">




<%String thispage = request.getContextPath() + "/ux/dashboard/service/kunjungan_pengguna.jsp";%>

var dtMhs = [];
var dtDsn = [];
var dtAdm = [];
var dtDate = [];

function reloadKunjungan(){
	
	document.getElementById("akses-pengguna").innerHTML = "";
	
	var fakultas = document.getElementById("fakultas").value;
	var prodi = document.getElementById("prodi").value;
	
	var tanggal_awal = document.getElementById("tanggal_awal").value;
	var tanggal_akhir = document.getElementById("tanggal_akhir").value;
	
	var xhttp = new XMLHttpRequest();
	  xhttp.onreadystatechange = function() {
	    if (this.readyState == 4 && this.status == 200) {
	     var data = this.responseText.trim();
	     const obj = JSON.parse(data);
	      dtDate = obj.dtDate;
	      dtMhs = obj.dtMhs;
	      dtDsn = obj.dtDsn;
	      dtAdm = obj.dtAdm;
	      AksesPengguna.init();
	    }
	  };
	  xhttp.open("GET", "<%=thispage%>?prodi="+prodi+"&fakultas="+fakultas+"&tanggal_awal="+tanggal_awal+"&tanggal_akhir="+tanggal_akhir, true);
	  xhttp.send();
	
}



var AksesPengguna = function () {
	  var e = {
	    self: null,
	    rendered: !1
	  }

	  const t = function (e) {
	    var t = document.getElementById("akses-pengguna");
	    if (t == null) {
	      return
	    }

	    var a = parseInt(KTUtil.css(t, "height")),
	      l = KTUtil.getCssVariableValue("--kt-gray-900"),
	      r = KTUtil.getCssVariableValue("--kt-border-dashed-color")

	    var o = {
	      series: [
	        {
	          name: "Mahasiswa",
	          data: dtMhs
	        },
	        {
	          name: "Dosen",
	          data: dtDsn
	        },
	        {
	          name: "Admin",
	          data: dtAdm
	        },
	      ],
	      chart: {
	        fontFamily: "inherit",
	        type: "line",
	        height: a,
	        // stacked: true,
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
	      stroke: {
	        curve: 'smooth'
	      },
	      xaxis: {
	        categories: dtDate,
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
	        title: {
	          text: "Jumlah Akses",
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

	"undefined" != typeof module && (module.exports = AksesPengguna)

	KTUtil.onDOMContentLoaded((function () {
		reloadKunjungan();
	}));


</script>