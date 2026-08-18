<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="ais.common.Common" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Insert title here</title>
</head>
<body>

<div class="row row-cols-1 row-cols-md-5 g-4 ms-3 me-3 mt-2 mb-5">

  <div class="col">
    <div class="card h-150">
      <img src="<%=request.getContextPath()%>/img/buku-contoh.png" class="card-img-top" alt="...">
      <div class="card-body">
        <h4 class="card-title">Judul Tugas</h4>
      </div>
      <div>
            <p class="card-text ms-3 mb-3">keterangan Tugas</p>
            <jsp:include
	page="/WEB-INF/uiux/ux_pages/content/modal/catatan.jsp"></jsp:include>
            
        <div class="d-grid gap-2 me-2 ms-2 mb-2">
  <button class="btn btn-primary" type="Buka Tugas"><%= Common.getBahasaConfig("Buka Tugas") %></button>
</div>

	
      </div>
  
    </div>
  </div>
  
  <div class="col">
    <div class="card h-150">
      <img src="<%=request.getContextPath()%>/img/buku-contoh.png" class="card-img-top me-3" alt="...">
      <div class="card-body">
        <h4 class="card-title">Judul Tugas</h4>
      
      </div>
      
       <div>
            <p class="card-text ms-3 mb-3">keterangan Tugas</p>
           
                   
        <div class="d-grid gap-2 me-2 ms-2 mb-2">
  <button class="btn btn-primary" type="Buka Tugas"><%= Common.getBahasaConfig("Buka Tugas") %></button>
</div>
         
      </div>
      
    </div>
    
  </div>
  
  <div class="col">
    <div class="card h-150">
      <img src="<%=request.getContextPath()%>/img/buku-contoh.png" class="card-img-top" alt="...">
      <div class="card-body">
        <h4 class="card-title">Judul Tugas</h4>
       
      </div>
      <div>
            <p class="card-text ms-3 mb-3">keterangan Tugas</p>
                    
        <div class="d-grid gap-2 me-2 ms-2 mb-2">
  <button class="btn btn-primary" type="Buka Tugas"><%= Common.getBahasaConfig("Buka Tugas") %></button>
</div>
      </div>
    </div>
  </div>
  <div class="col">
    <div class="card h-150">
      <img src="<%=request.getContextPath()%>/img/buku-contoh.png" class="card-img-top" alt="...">
      <div class="card-body">
        <h4 class="card-title">Judul Tugas</h4>
       
      </div>
      <div>
            <p class="card-text ms-3 mb-3">keterangan Tugas</p>
                    
        <div class="d-grid gap-2 me-2 ms-2 mb-2">
  <button class="btn btn-primary" type="Buka Tugas"><%= Common.getBahasaConfig("Buka Tugas") %></button>
</div>

            
            
      </div>
    </div>
  </div>
  
</div>
</body>
</html>