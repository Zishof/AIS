<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!doctype html>
<html>
<head>
<meta name="viewport"
	content="width=device-width, initial-scale=1, shrink-to-fit=no" />
<meta name="description" content="Sistem Informasi Akademik Ecampus" />
<meta name="author" content="Mohammad Fauzi Murtadho" />
<%

String mahasiswa = request.getParameter("mahasiswa") == null ? "" : request.getParameter("mahasiswa");
String calon_mahasiswa = request.getParameter("calon_mahasiswa") == null ? "" : request.getParameter("calon_mahasiswa");
String siswa = request.getParameter("siswa") == null ? "" : request.getParameter("siswa");
String calon_siswa = request.getParameter("calon_siswa") == null ? "" : request.getParameter("calon_siswa");
String dosen = request.getParameter("dosen") == null ? "" : request.getParameter("dosen");
String guru = request.getParameter("guru") == null ? "" : request.getParameter("guru");
String pegawai = request.getParameter("pegawai") == null ? "" : request.getParameter("pegawai");
String pert = request.getParameter("pert") == null ? "" : request.getParameter("pert");
String mobile = request.getParameter("mobile") == null ? "false" : request.getParameter("mobile");
String lokasi = request.getParameter("lokasi") == null ? "true" : request.getParameter("lokasi");
String rand = request.getParameter("rand") == null ? "" : request.getParameter("rand");
String clazz = request.getParameter("clazz") == null ? "" : request.getParameter("clazz");
String judul = request.getParameter("judul") == null ? "" : request.getParameter("judul");
String userid = request.getParameter("userid") == null ? "" : request.getParameter("userid");
String jenis = request.getParameter("jenis") == null ? "" : request.getParameter("jenis");
String state = request.getParameter("state") == null ? "" : request.getParameter("state");
%>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">

<style>

@import
	url('https://fonts.googleapis.com/css2?family=Open+Sans:ital,wght@0,300;1,300&display=swap')
	;
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
            width:360px;
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
        
        h3 {
        	font: normal normal normal 12px/1 Helvetica, arial, sans-serif;
			padding: 30px 0 8px;
			position: relative;
			font-family: 'Open Sans', sans-serif;
			border-bottom: 2px solid #000;
		}
		
		h3:first-line {
			background: #000;
			color: #fff;
		}
		
		.fa-2x {
		  vertical-align: middle;
		}
    </style>
<!-- Add icon library -->
<title>Ambil Audio</title>
<script src="https://cdn.webrtc-experiment.com/RecordRTC.js"></script>
<script src="https://webrtc.github.io/adapter/adapter-latest.js"></script>
<script type="text/javascript" src="https://code.jquery.com/jquery-3.4.1.min.js"></script>
</head>
<body style="text-align: center;">


	<div id="camBox" style="width: 100%; height: 100%;">
		<div class="revdivshowimg"
			style="top: 1%; text-align: center; margin: 0 auto;">
			<%
		if(!judul.isEmpty()){
		%>
			<h3><%=judul %></h3>
			<%
		} 
		%>
			
				<br>
				<h2 style="color:red;display: none;" id="title_data">Harap Tunggu, rekaman audio Anda sedang diproses, browser Anda jangan ditutup..
				<br><img alt="Gambar Loading" src="<%=Common.getRequestHostWithProtocol(request)%>/loading_icon.gif" width="65%"/>
				</h2>
				
				<div style="text-align: center;">
					<button id="btn-start-recording" class="btn"><i class="fa fa-play fa-2x"></i> Rekam Audio</button>
					<button disabled id="btn-stop-recording" class="btn"><i class="fa fa-stop-circle fa-2x"></i> Stop dan Kirim</button>
					<!-- 
					<button id="btnChangeCamera" class="btn" onClick="gantiCamera()"><i class="fa fa-refresh fa-2x"></i> Ganti Kamera</button>
					 -->
				</div>
				
                <br><br>
			    <audio autoplay id="audio" style="height: auto; width:340px; text-align: center; margin: 0 auto;"></audio>
				
			<canvas style="display: none;" id="canvas"></canvas>
			<br>
			<p id="location"></p>
			<br>
			<div style="color: blue; font-size: 10px">*) Pastikan browser
				yang Anda gunakan sudah diizinkan akses audio.</div>
			
		</div>
	</div>

	<!-- 4. Initialize and prepare the audio recorder logic -->
<script>
    // Store a reference of the preview audio element and a global reference to the recorder instance
    var snd = new Audio(
					"data:audio/wav;base64,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qJrx4KOQjahZxWbcZgztj2c49nKmkId44S71j0c8eV9yDK6uPRzx5X18eDvjvQ6yKo9ZSS6l//8elePK/Lf//IInrOF/FvDoADYAGBMGb7FtErm5MXMlmPAJQVgWta7Zx2go+8xJ0UiCb8LHHdftWyLJE0QIAIsI+UbXu67dZMjmgDGCGl1H+vpF4NSDckSIkk7Vd+sxEhBQMRU8j/12UIRhzSaUdQ+rQU5kGeFxm+hb1oh6pWWmv3uvmReDl0UnvtapVaIzo1jZbf/pD6ElLqSX+rUmOQNpJFa/r+sa4e/pBlAABoAAAAA3CUgShLdGIxsY7AUABPRrgCABdDuQ5GC7DqPQCgbbJUAoRSUj+NIEig0YfyWUho1VBBBA//uQZB4ABZx5zfMakeAAAAmwAAAAF5F3P0w9GtAAACfAAAAAwLhMDmAYWMgVEG1U0FIGCBgXBXAtfMH10000EEEEEECUBYln03TTTdNBDZopopYvrTTdNa325mImNg3TTPV9q3pmY0xoO6bv3r00y+IDGid/9aaaZTGMuj9mpu9Mpio1dXrr5HERTZSmqU36A3CumzN/9Robv/Xx4v9ijkSRSNLQhAWumap82WRSBUqXStV/YcS+XVLnSS+WLDroqArFkMEsAS+eWmrUzrO0oEmE40RlMZ5+ODIkAyKAGUwZ3mVKmcamcJnMW26MRPgUw6j+LkhyHGVGYjSUUKNpuJUQoOIAyDvEyG8S5yfK6dhZc0Tx1KI/gviKL6qvvFs1+bWtaz58uUNnryq6kt5RzOCkPWlVqVX2a/EEBUdU1KrXLf40GoiiFXK///qpoiDXrOgqDR38JB0bw7SoL+ZB9o1RCkQjQ2CBYZKd/+VJxZRRZlqSkKiws0WFxUyCwsKiMy7hUVFhIaCrNQsKkTIsLivwKKigsj8XYlwt/WKi2N4d//uQRCSAAjURNIHpMZBGYiaQPSYyAAABLAAAAAAAACWAAAAApUF/Mg+0aohSIRobBAsMlO//Kk4soosy1JSFRYWaLC4qZBYWFRGZdwqKiwkNBVmoWFSJkWFxX4FFRQWR+LsS4W/rFRb/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////VEFHAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAU291bmRib3kuZGUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMjAwNGh0dHA6Ly93d3cuc291bmRib3kuZGUAAAAAAAAAACU=");
			
    function beep() {
			snd.play();
		}
    
    const audio = document.querySelector("#audio");
    var recorder;

    // When the user clicks on start audio recording
    document.getElementById('btn-start-recording').addEventListener("click", function(){
        // Disable start recording button
        this.disabled = true;
        beep();
        
        document.getElementById("btn-stop-recording").disabled = false;
        document.getElementById("btn-start-recording").style.display="none";
		//document.getElementById("btnChangeCamera").style.display="none";
		
		
        navigator.mediaDevices.getUserMedia({
            audio: true, 
            video: false
        }).then(function(stream) {
            // Display a live preview on the audio element of the page
            setSrcObject(stream, audio);

            // Start to display the preview on the audio element
            // and mute the audio to disable the echo issue !
            audio.play();
            audio.muted = true;

            // Initialize the recorder
            recorder = new RecordRTCPromisesHandler(stream, {
                mimeType: 'audio/webm',
                bitsPerSecond: 128000
            });

            // Start recording the audio
            recorder.startRecording().then(function() {
                console.info('Recording audio ...');
            }).catch(function(error) {
                console.error('Cannot start audio recording: ', error);
            });

            // release stream on stopRecording
            recorder.stream = stream;

            // Enable stop recording button
            document.getElementById('btn-stop-recording').disabled = false;
        }).catch(function(error) {
            console.error("Cannot access media devices: ", error);
        });

       
    }, false);
    
    
    

    // When the user clicks on Stop audio recording
    document.getElementById('btn-stop-recording').addEventListener("click", function(){
        this.disabled = true;
        beep();
        
        document.getElementById("btn-stop-recording").style.display="none";
		//document.getElementById("btnChangeCamera").style.display="none";
		document.getElementById("title_data").style.display="block";
		audio.style.display="none";

        recorder.stopRecording().then(function() {
            console.info('stopRecording success');
            var fileName = "Recording_" + new Date().getTime() + ".wav";
            // Retrieve recorded audio as blob and display in the preview element
            recorder.getBlob().then (function(blob) {
		        const fd = new FormData();
		        fd.append('video-filename', fileName);
		        fd.append('video-blob', blob);
		        $.ajax({
		            type: 'POST',
		            url: '<%=Common.getRequestHostWithProtocol(request)%>/Recording',
		            data: fd,
		            processData: false,
		            contentType: false
		        })
		        .done(function(data) {
		           console.log(data);
		           console.log("File uploaded successfully. " + new Date());
		            
		            var url = "<%=Common.getRequestHostWithProtocol(request)%>/common/scan_berhasil.zul"; 
					var form = document.createElement('form');
					form.action = url;
					form.method = "post";
					form.innerHTML = '<input type="text" name="image" value="' + fileName + '" /><input type="text" name="userid" value="<%=userid%>" /><input type="text" name="jenis" value="<%=jenis%>" /><input type="text" name="state" value="<%=state%>" /><input type="text" name="lokasi" value="<%=lokasi%>" /><input type="text" name="lat" value="' + lat + '" /><input type="text" name="lng" value="' + lng + '" /><input type="text" name="mahasiswa" value="<%=mahasiswa%>" /><input type="text" name="calon_mahasiswa" value="<%=calon_mahasiswa%>" /><input type="text" name="siswa" value="<%=siswa%>" /><input type="text" name="calon_siswa" value="<%=calon_siswa%>" /><input type="text" name="dosen" value="<%=dosen%>" /><input type="text" name="guru" value="<%=guru%>" /><input type="text" name="pegawai" value="<%=pegawai%>" /><input type="text" name="pert" value="<%=pert%>" /><input type="text" name="clazz" value="<%=clazz%>" /><input type="text" name="rand" value="<%=rand%>" />';
					document.body.appendChild(form);
					form.submit();
		        })
		    }).catch(console.err);

            // Enable record button again !
            document.getElementById('btn-start-recording').disabled = false;
        }).catch(function(error) {
            console.error('stopRecording failure', error);
        });
    }, false);

		
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
				  document.getElementById("location").innerHTML = "<div style=\"color: blue;font-size: 10px\">Pastikan browser Anda mendukung akses lokasi GPS. Jika lokasi tidak diaktifkan, maka Anda tidak bisa melakukan absensi via Foto</div>";
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
			
			
			<%
			if(lokasi.equalsIgnoreCase("true")){
			%>
				if(!getLocation()){
					return;
				}
			<%
			} 
			%>

		
		
		});


	</script>

</body>
</html>
