<%@page import="java.net.URLEncoder"%>
<%@page import="ais.common.Common"%>
<html>
<head>
<title>Qrcode</title>
<meta name="viewport"
	content="width=device-width, initial-scale=1, shrink-to-fit=no" />
<meta name="description" content="Sistem Informasi Akademik Ecampus" />
<meta name="author" content="Mohammad Fauzi Murtadho" />
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">

<style>

        #camBox{
            position:fixed;
            border:0;
            top:0;
            right:0;
            left:0;
            overflow-x:auto;
            overflow-y:hidden;
            z-index:9999;
            background-color:rgba(239,239,239,.9);
            width:100%;
            height:100%;
            padding-top:10px;
            text-align:center;
            cursor:pointer;
            -webkit-box-align:center;-webkit-box-orient:vertical;
            -webkit-box-pack:center;-webkit-transition:.2s opacity;
            -webkit-perspective:1000
        }

        .revdivshowimg{
            width:300px;
            top:0;
            padding:0;
            position:relative;
            margin:0 auto;
            display:block;
            background-color:#fff;
            webkit-box-shadow:6px 0 10px rgba(0,0,0,.2),-6px 0 10px rgba(0,0,0,.2);
            -moz-box-shadow:6px 0 10px rgba(0,0,0,.2),-6px 0 10px rgba(0,0,0,.2);
            box-shadow:6px 0 10px rgba(0,0,0,.2),-6px 0 10px rgba(0,0,0,.2);
            overflow:hidden;
            border-radius:3px;
            color:#17293c
        }        
        .fa-2x {
		  vertical-align: middle;
		}
    </style>

<%
String host = request.getParameter("q") == null ? "" : URLEncoder.encode(request.getParameter("q"), "UTF-8");
String deviceNumber = request.getParameter("deviceNumber") == null ? "0" : request.getParameter("deviceNumber");
String deviceNumberUpdate = request.getParameter("deviceNumber") == null || request.getParameter("deviceNumber").equalsIgnoreCase("0") ? "1" : "0";
String dev = deviceNumber.equalsIgnoreCase("0") ? "devices.length-1" : "0";
%>
<body> 
	<div id="camBox" style="width: 100%; height: 100%;">
		<div class="revdivshowimg"
			style="top: 1%; text-align: center; margin: 0 auto;">
			<h4>Arahkan kamera ke posisi QR-Code</h4>
			<div id="reader"></div>
			<br>
			
			<button id="gantiKamera" class="btn" onClick="window.location.replace('read_qr_codejsp.jsp?deviceNumber=<%=deviceNumberUpdate%>&q=<%=host%>');"><i class="fa fa-refresh fa-2x"></i> Ganti Kamera</button>
				
			<br>
			<p id="location"></p>
			<br>
			<div style="color: blue;font-size: 10px">*) Pastikan browser yang Anda gunakan sudah diizinkan akses kamera atau mendukung akses kamera, jika kamera tidak tampil juga, coba gunakan Google Chrome.</div>
		</div>
	</div>
</body>
<script src="<%=request.getContextPath()%>/js/html5-qrcode.min.js"></script>
<script src="<%=request.getContextPath()%>/js/pesan-formal.js"></script>
<script>

	

	function beep() {
		var snd = new Audio("data:audio/wav;base64,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qJrx4KOQjahZxWbcZgztj2c49nKmkId44S71j0c8eV9yDK6uPRzx5X18eDvjvQ6yKo9ZSS6l//8elePK/Lf//IInrOF/FvDoADYAGBMGb7FtErm5MXMlmPAJQVgWta7Zx2go+8xJ0UiCb8LHHdftWyLJE0QIAIsI+UbXu67dZMjmgDGCGl1H+vpF4NSDckSIkk7Vd+sxEhBQMRU8j/12UIRhzSaUdQ+rQU5kGeFxm+hb1oh6pWWmv3uvmReDl0UnvtapVaIzo1jZbf/pD6ElLqSX+rUmOQNpJFa/r+sa4e/pBlAABoAAAAA3CUgShLdGIxsY7AUABPRrgCABdDuQ5GC7DqPQCgbbJUAoRSUj+NIEig0YfyWUho1VBBBA//uQZB4ABZx5zfMakeAAAAmwAAAAF5F3P0w9GtAAACfAAAAAwLhMDmAYWMgVEG1U0FIGCBgXBXAtfMH10000EEEEEECUBYln03TTTdNBDZopopYvrTTdNa325mImNg3TTPV9q3pmY0xoO6bv3r00y+IDGid/9aaaZTGMuj9mpu9Mpio1dXrr5HERTZSmqU36A3CumzN/9Robv/Xx4v9ijkSRSNLQhAWumap82WRSBUqXStV/YcS+XVLnSS+WLDroqArFkMEsAS+eWmrUzrO0oEmE40RlMZ5+ODIkAyKAGUwZ3mVKmcamcJnMW26MRPgUw6j+LkhyHGVGYjSUUKNpuJUQoOIAyDvEyG8S5yfK6dhZc0Tx1KI/gviKL6qvvFs1+bWtaz58uUNnryq6kt5RzOCkPWlVqVX2a/EEBUdU1KrXLf40GoiiFXK///qpoiDXrOgqDR38JB0bw7SoL+ZB9o1RCkQjQ2CBYZKd/+VJxZRRZlqSkKiws0WFxUyCwsKiMy7hUVFhIaCrNQsKkTIsLivwKKigsj8XYlwt/WKi2N4d//uQRCSAAjURNIHpMZBGYiaQPSYyAAABLAAAAAAAACWAAAAApUF/Mg+0aohSIRobBAsMlO//Kk4soosy1JSFRYWaLC4qZBYWFRGZdwqKiwkNBVmoWFSJkWFxX4FFRQWR+LsS4W/rFRb/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////VEFHAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAU291bmRib3kuZGUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMjAwNGh0dHA6Ly93d3cuc291bmRib3kuZGUAAAAAAAAAACU=");  
		snd.play();
	}
	
	var lat = 0.0;
	var lng = 0.0;
	function showPosition(position) {
		lat = position.coords.latitude;
		lng = position.coords.longitude;
		document.getElementById("location").innerHTML = "<div style=\"color: blue;font-size: 10px\">Lokasi Anda :</div><br><iframe style=\"width:100%;height:350px\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\"  marginwidth=\"0\" src=\"https://maps.google.com/maps?q="+lat+","+lng+"&hl=id&z=14&amp;output=embed\"></iframe>";
	}
	
	function getLocation() {
		if (navigator.geolocation) {
		      navigator.geolocation.getCurrentPosition(showPosition);
		      return true;
		} else { 
			  document.getElementById("location").innerHTML = "<div style=\"color: blue;font-size: 10px\">Pastikan browser Anda mendukung akses lokasi GPS. Jika lokasi tidak diaktifkan, maka Anda tidak bisa melakukan absensi via QR-Code</div>";
			  return false;
		}
	}
	
	function docReady(fn) {
		// see if DOM is already available
		if (document.readyState === "complete"
				|| document.readyState === "interactive") {
			// call on next available tick
			setTimeout(fn, 1);
		} else {
			document.addEventListener("DOMContentLoaded", fn);
		}
	}

	docReady(function() {
		
		if(!getLocation()){
			return;
		}
		
		// This method will trigger user permissions
		Html5Qrcode.getCameras().then(devices => {
		  /**
		   * devices would be an array of objects of type:
		   * { id: "id", label: "label" }
		   */
		  if (devices && devices.length) {
			//alert("Jumlah camera -> "+devices.length)
			if(devices.length <= 1){
				document.getElementById("gantiKamera").style.display="none";
			}
			
			var cameraId = devices[<%=dev%>].id;
			// .. use this to start scanning.
			
			// Create instance of the object. The only argument is the "id" of HTML element created above.
			const html5QrCode = new Html5Qrcode("reader");

			html5QrCode.start(
			  cameraId,     // retreived in the previous step.
			  {
				fps: 10,    // sets the framerate to 10 frame per second
				qrbox: 250  // sets only 250 X 250 region of viewfinder to
							// scannable, rest shaded.
			  },
			  qrCodeMessage => {
				// do something when code is read. For example:
				console.log(`QR Code detected: ${qrCodeMessage}`);
				beep();
				html5QrCode.stop().then(ignore => {
				  // QR Code scanning is stopped.
				  console.log("QR Code scanning stopped.");
				  getLocation();
				  
				  if(qrCodeMessage == null || qrCodeMessage == ''){
					  tampilkanPesanGagalFormal(
						  "pembacaan kode QR",
						  '<%= Common.getBahasaConfig("Kode QR yang dipindai tidak dapat terbaca dengan benar (hasil pemindaian kosong).") %>',
						  ["Pastikan kode QR tidak rusak, buram, atau tertutup pantulan cahaya.", "Dekatkan/jauhkan kamera ke kode QR hingga posisinya jelas terlihat, lalu ulangi pemindaian."]
					  );
					  return; 
				  }
				  
				  window.location.replace("<%=request.getParameter("q")%>&pert="+encodeURIComponent(qrCodeMessage)+"&lat="+lat+"&lng="+lng+"&lokasi=true");
				}).catch(err => {
				  // Stop failed, handle it.
				  console.log("Unable to stop scanning.");
				});
				
			  },
			  errorMessage => {
				// parse error, ideally ignore it. For example:
				console.log(`QR Code no longer in front of camera.`);
			  })
			.catch(err => {
			  // Start failed, handle it. For example,
			  console.log(`Unable to start scanning, error: ${err}`);
			});
			
			
		  } else {
			  tampilkanPesanGagalFormal(
				  "pemindaian kode QR",
				  '<%= Common.getBahasaConfig("Kamera tidak ditemukan, akses kamera tidak diizinkan, atau peramban Anda tidak mendukung akses ke kamera.") %>',
				  ["Pastikan perangkat memiliki kamera yang berfungsi dan tidak sedang dipakai aplikasi lain.", "Berikan izin akses kamera untuk halaman ini pada pengaturan peramban.", "Gunakan Google Chrome versi terbaru, lalu muat ulang halaman ini."]
			  );
		  }
		}).catch(err => {
		  // handle err
		});
	});
</script>
</head>
</html>
