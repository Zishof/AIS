
<!--begin::Col-->
<div class="col-xl-12">
	<!--begin::Chart widget 18-->
	<div class="card card-flush h-xl-100">
		<!--begin::Header-->
		<div class="card-header pt-7">
			<!--begin::Title-->
			<h3 class="card-title align-items-start flex-column">
				<span class="card-label fw-bold text-gray-800">Status Dosen</span> <span
					class="text-gray-400 mt-1 fw-semibold fs-6">Tiap Prodi</span>
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
		<div class="card-body d-flex align-items-end px-0 pt-3 pb-5">
			<!--begin::Chart-->
			<div id="status_dosen" class="h-325px w-100 min-h-auto ps-4 pe-6"></div>
			<!--end::Chart-->
		</div>
		<!--end: Card Body-->
		<div class="card-body pt-3">
			<table class="table table-striped table-sm mb-0 table-hover"
				id="status_dosen_tiap_prodi_table">

			</table>
			
		</div>
	</div>
	<!--end::Chart widget 18-->
</div>
<!--end::Col-->


<script type="text/javascript">


var categories = [];
var series = [];

function reloadStatusDosenTiapProdi(){
	
	document.getElementById("status_dosen").innerHTML = "";
	document.getElementById("status_dosen_tiap_prodi_table").innerHTML = "";
	
	
	var xhttp = new XMLHttpRequest();
	  xhttp.onreadystatechange = function() {
	    if (this.readyState == 4 && this.status == 200) {
	     var data = this.responseText.trim();
	     const obj = JSON.parse(data);
	     categories = obj.categories;
	     series = obj.series;
	     
	   //var d =  JSON.stringify(series, undefined, 4);
	  // document.getElementById("log_status").innerHTML = d;
	     
	     document.getElementById("status_dosen_tiap_prodi_table").innerHTML = obj.tr;
	     StatDosen.init();
	    }
	  };
	  xhttp.open("GET", "<%=(request.getContextPath() + "/ux/dashboard/service/status_dosen_tiap_prodi.jsp")%>", true);
	  xhttp.send();
}

var StatDosen = function () {
	  var e = {
	    self: null,
	    rendered: !1
	  }

	  const t = function (e) {
	    var t = document.getElementById("status_dosen");
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
	          fontSize: "12px",
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
	        title: {
	          text: "Jumlah Dosen",
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
	        e.rendered && e.self.destroy(), t(e)
	      }))
	    }
	  }
	}();

	"undefined" != typeof module && (module.exports = StatDosen)

	KTUtil.onDOMContentLoaded((function () {
		reloadStatusDosenTiapProdi();
	}));

</script>