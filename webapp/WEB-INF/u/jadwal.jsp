<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Jadwal Pengajaran Dosen</title>
    <link rel="shortcut icon" href="<%=request.getContextPath() %>/img/logo.png" type="image/png"/>
    <link rel="stylesheet" href="<%=request.getContextPath() %>/css/jadwal.css">
</head>
<body>
    <div class="container">
        <div class="schedule-container">
            <div class="schedule-daily">
                <h2>Jadwal Pengajaran Harian</h2>
                <div class="schedule-list">
                    </div>
            </div>
            <div class="schedule-weekly">
                <h2>Jadwal Pengajaran Mingguan</h2>
                <div class="schedule-list">
                </div>
            </div>
        </div>
    </div>
    <script type="text/javascript">
	
	const dailyScheduleData = <%= request.getAttribute("dailySchedulesData") %>
	const weeklyScheduleData = <%= request.getAttribute("weeklySchedulesData") %>
	
	</script>
    <script src="<%=request.getContextPath() %>/js/jadwal.js"></script>
</body>
</html>