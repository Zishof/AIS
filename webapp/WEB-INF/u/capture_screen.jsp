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
<title>Ambil Video</title>
<script src="<%=request.getContextPath() %>/js/pesan-formal.js"></script>
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
			
				
				<h2 style="color:red;display: none;" id="title_data">
				Harap Tunggu, video layar Anda sedang diproses, browser Anda jangan ditutup..
				<br><img alt="Gambar Loading" src="<%=Common.getRequestHostWithProtocol(request)%>/loading_icon.gif" width="65%"/>
				</h2>
				
				<div style="text-align: center;">
					<button id="start" onclick="recordScreen()" class="btn"><i class="fa fa-play fa-2x"></i> Rekam Layar</button>
					<button disabled id="stop" class="btn"><i class="fa fa-stop-circle fa-2x"></i> Selesaikan Rekam Layar</button>
					
				</div>
				 <br><br>
			    <video autoplay id="video" style="height: auto; width:340px; text-align: center; margin: 0 auto;" muted></video>
				
            
		</div>
	</div>
	
	
	<script>
	
	// Store a reference of the preview video element and a global reference to the recorder instance
    var snd = new Audio(
					"data:audio/wav;base64,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qJrx4KOQjahZxWbcZgztj2c49nKmkId44S71j0c8eV9yDK6uPRzx5X18eDvjvQ6yKo9ZSS6l//8elePK/Lf//IInrOF/FvDoADYAGBMGb7FtErm5MXMlmPAJQVgWta7Zx2go+8xJ0UiCb8LHHdftWyLJE0QIAIsI+UbXu67dZMjmgDGCGl1H+vpF4NSDckSIkk7Vd+sxEhBQMRU8j/12UIRhzSaUdQ+rQU5kGeFxm+hb1oh6pWWmv3uvmReDl0UnvtapVaIzo1jZbf/pD6ElLqSX+rUmOQNpJFa/r+sa4e/pBlAABoAAAAA3CUgShLdGIxsY7AUABPRrgCABdDuQ5GC7DqPQCgbbJUAoRSUj+NIEig0YfyWUho1VBBBA//uQZB4ABZx5zfMakeAAAAmwAAAAF5F3P0w9GtAAACfAAAAAwLhMDmAYWMgVEG1U0FIGCBgXBXAtfMH10000EEEEEECUBYln03TTTdNBDZopopYvrTTdNa325mImNg3TTPV9q3pmY0xoO6bv3r00y+IDGid/9aaaZTGMuj9mpu9Mpio1dXrr5HERTZSmqU36A3CumzN/9Robv/Xx4v9ijkSRSNLQhAWumap82WRSBUqXStV/YcS+XVLnSS+WLDroqArFkMEsAS+eWmrUzrO0oEmE40RlMZ5+ODIkAyKAGUwZ3mVKmcamcJnMW26MRPgUw6j+LkhyHGVGYjSUUKNpuJUQoOIAyDvEyG8S5yfK6dhZc0Tx1KI/gviKL6qvvFs1+bWtaz58uUNnryq6kt5RzOCkPWlVqVX2a/EEBUdU1KrXLf40GoiiFXK///qpoiDXrOgqDR38JB0bw7SoL+ZB9o1RCkQjQ2CBYZKd/+VJxZRRZlqSkKiws0WFxUyCwsKiMy7hUVFhIaCrNQsKkTIsLivwKKigsj8XYlwt/WKi2N4d//uQRCSAAjURNIHpMZBGYiaQPSYyAAABLAAAAAAAACWAAAAApUF/Mg+0aohSIRobBAsMlO//Kk4soosy1JSFRYWaLC4qZBYWFRGZdwqKiwkNBVmoWFSJkWFxX4FFRQWR+LsS4W/rFRb/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////VEFHAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAU291bmRib3kuZGUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMjAwNGh0dHA6Ly93d3cuc291bmRib3kuZGUAAAAAAAAAACU=");
			
    function beep() {
			snd.play();
		}
    
	var lat = 0.0;
	var lng = 0.0;
	
    let shouldStop = false;
    let stopped = false;
    const videoElement = document.getElementsByTagName("video")[0];
    const stopButton = document.getElementById('stop');
    function startRecord() {
       
    }
    
    const audioRecordConstraints = {
        echoCancellation: true
    }

    stopButton.addEventListener('click', function () {
        shouldStop = true;
    });

    const handleRecord = function ({stream, mimeType}) {
        startRecord()
        let recordedChunks = [];
        stopped = false;
        const mediaRecorder = new MediaRecorder(stream);

        mediaRecorder.ondataavailable = function (e) {
            if (e.data.size > 0) {
                recordedChunks.push(e.data);
            }

            if (shouldStop === true && stopped === false) {
                mediaRecorder.stop();
                stopped = true;
            }
        };

        mediaRecorder.onstop = function () {
        	
        	beep();
        	stopButton.style.display="none";
        	videoElement.style.display="none";
    		//document.getElementById("btnChangeCamera").style.display="none";
    	    document.getElementById("title_data").style.display="block";
        	
            const blob = new Blob(recordedChunks, {
                type: mimeType
            });
            recordedChunks = []
            
           
           
	   	    var fileName = "Recording_" + new Date().getTime() + ".webm";

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
	           
	            downloadLink = document.createElement('a');
		   	    downloadLink.href = URL.createObjectURL(blob);
		   	    downloadLink.download = `${filename}.webm`;
		   	
		   	    document.body.appendChild(downloadLink);
		   	    downloadLink.click();
		   	    
		   	    
		   	   
	            
	            var url = "<%=Common.getRequestHostWithProtocol(request)%>/common/scan_berhasil.zul"; 
				var form = document.createElement('form');
				form.action = url;
				form.method = "post";
				form.innerHTML = '<input type="text" name="image" value="' + fileName + '" /><input type="text" name="userid" value="<%=userid%>" /><input type="text" name="jenis" value="<%=jenis%>" /><input type="text" name="state" value="<%=state%>" /><input type="text" name="lokasi" value="<%=lokasi%>" /><input type="text" name="lat" value="' + lat + '" /><input type="text" name="lng" value="' + lng + '" /><input type="text" name="mahasiswa" value="<%=mahasiswa%>" /><input type="text" name="calon_mahasiswa" value="<%=calon_mahasiswa%>" /><input type="text" name="siswa" value="<%=siswa%>" /><input type="text" name="calon_siswa" value="<%=calon_siswa%>" /><input type="text" name="dosen" value="<%=dosen%>" /><input type="text" name="guru" value="<%=guru%>" /><input type="text" name="pegawai" value="<%=pegawai%>" /><input type="text" name="pert" value="<%=pert%>" /><input type="text" name="clazz" value="<%=clazz%>" /><input type="text" name="rand" value="<%=rand%>" />';
				document.body.appendChild(form);
				form.submit();
	        })
            
            
        };

        mediaRecorder.start(200);
    };

    async function recordAudio() {
        const mimeType = 'audio/webm';
        shouldStop = false;
        const stream = await navigator.mediaDevices.getUserMedia({audio: audioRecordConstraints});
        handleRecord({stream, mimeType})
    }

    async function recordVideo() {
        const mimeType = 'video/webm';
        shouldStop = false;
        const constraints = {
            audio: {
                "echoCancellation": true
            },
            video: {
                "width": {
                    "min": 640,
                    "max": 1024
                },
                "height": {
                    "min": 480,
                    "max": 768
                }
            }
        };
        const stream = await navigator.mediaDevices.getUserMedia(constraints);
        videoElement.srcObject = stream;
        handleRecord({stream, mimeType})
    }

    async function recordScreen() {
    	beep();
    	
    	document.getElementById("stop").disabled = false;
        document.getElementById("start").style.display="none";
    	
        const mimeType = 'video/webm';
        shouldStop = false;
        const constraints = {
            video: {
                cursor: 'motion'
            }
        };
        if(!(navigator.mediaDevices && navigator.mediaDevices.getDisplayMedia)) {
            return tampilkanPesanGagalFormal(
                "perekaman layar (screen recording)",
                '<%= Common.getBahasaConfig("Perekaman layar tidak didukung pada peramban Anda.") %>',
                ["Gunakan Google Chrome atau Microsoft Edge versi terbaru untuk mengakses fitur perekaman layar ini.", "Pastikan Bapak/Ibu tidak mengakses halaman ini melalui aplikasi WebView (mis. dari dalam aplikasi lain)."]
            );
        }
        let stream = null;
        const displayStream = await navigator.mediaDevices.getDisplayMedia({video: {cursor: "motion"}, audio: {'echoCancellation': true}});
       
            const audioContext = new AudioContext();

            const voiceStream = await navigator.mediaDevices.getUserMedia({ audio: {'echoCancellation': true}, video: false });
            const userAudio = audioContext.createMediaStreamSource(voiceStream);
            
            const audioDestination = audioContext.createMediaStreamDestination();
            userAudio.connect(audioDestination);

            if(displayStream.getAudioTracks().length > 0) {
                const displayAudio = audioContext.createMediaStreamSource(displayStream);
                displayAudio.connect(audioDestination);
            }

            const tracks = [...displayStream.getVideoTracks(), ...audioDestination.stream.getTracks()]
            stream = new MediaStream(tracks);
            handleRecord({stream, mimeType})
        
        videoElement.srcObject = stream;
    }
</script>

	

</body>
</html>
