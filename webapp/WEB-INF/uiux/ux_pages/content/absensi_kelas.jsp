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

<div class="row row-cols-1 row-cols-md-3 g-4">

<div class="card mb-2" style="max-width: 300px;">
  <div class="row g-0">
    <div class="col-md-5">
     <img src="<%=request.getContextPath()%>/img/buku-contoh.png"
									class="card-img-top" alt="...">
    </div>
    <div class="col-md-8">
      <div class="card-body">
        <h5 class="card-title">Santoso</h5>
      
        <p class="card-text"><small class="text-body-secondary">Kelas 5-A</small></p>
        <!--checklist-->    
<div class="form-check form-switch form-check-reverse me-1">
  <input class="form-check-input" type="checkbox" id="flexSwitchCheckReverse">
  <label class="form-check-label" for="flexSwitchCheckReverse"><%= Common.getBahasaConfig("Hadir") %></label>
</div> 

<div class="form-check form-switch form-check-reverse me-1">
  <input class="form-check-input" type="checkbox" id="flexSwitchCheckReverse">
  <label class="form-check-label" for="flexSwitchCheckReverse"><%= Common.getBahasaConfig("Izin") %></label>
</div> 

<div class="form-check form-switch form-check-reverse me-1">
  <input class="form-check-input" type="checkbox" id="flexSwitchCheckReverse">
  <label class="form-check-label" for="flexSwitchCheckReverse"><%= Common.getBahasaConfig("Sakit") %></label>
</div> 

<div class="form-check form-switch form-check-reverse me-1">
  <input class="form-check-input" type="checkbox" id="flexSwitchCheckReverse">
  <label class="form-check-label" for="flexSwitchCheckReverse"><%= Common.getBahasaConfig("Alpha") %></label>
</div> 

<!--checklist-->
      </div>
    </div>
  </div>
</div>


  <div class="col">
    <div class="card h-100">
      <img src="..." class="card-img-top" alt="...">
      <div class="card-body">
        <h5 class="card-title">Card title</h5>
        <p class="card-text">This is a longer card with supporting text below as a natural lead-in to additional content. This content is a little bit longer.</p>
      </div>
      
 <!--checklist-->    
<div class="form-check form-switch form-check-reverse me-3">
  <input class="form-check-input" type="checkbox" id="flexSwitchCheckReverse">
  <label class="form-check-label" for="flexSwitchCheckReverse"><%= Common.getBahasaConfig("Hadir") %></label>
</div> 

<div class="form-check form-switch form-check-reverse me-3">
  <input class="form-check-input" type="checkbox" id="flexSwitchCheckReverse">
  <label class="form-check-label" for="flexSwitchCheckReverse"><%= Common.getBahasaConfig("Izin") %></label>
</div> 

<div class="form-check form-switch form-check-reverse me-3">
  <input class="form-check-input" type="checkbox" id="flexSwitchCheckReverse">
  <label class="form-check-label" for="flexSwitchCheckReverse"><%= Common.getBahasaConfig("Sakit") %></label>
</div> 

<div class="form-check form-switch form-check-reverse me-3">
  <input class="form-check-input" type="checkbox" id="flexSwitchCheckReverse">
  <label class="form-check-label" for="flexSwitchCheckReverse"><%= Common.getBahasaConfig("Alpha") %></label>
</div> 

<!--checklist-->

    </div>
  </div>
  <div class="col">
    <div class="card h-100">
      <img src="..." class="card-img-top" alt="...">
      <div class="card-body">
        <h5 class="card-title">Card title</h5>
        <p class="card-text">This is a short card.</p>
      </div>
    </div>
  </div>
  <div class="col">
    <div class="card h-100">
      <img src="..." class="card-img-top" alt="...">
      <div class="card-body">
        <h5 class="card-title">Card title</h5>
        <p class="card-text">This is a longer card with supporting text below as a natural lead-in to additional content.</p>
      </div>
    </div>
  </div>
  <div class="col">
    <div class="card h-100">
      <img src="..." class="card-img-top" alt="...">
      <div class="card-body">
        <h5 class="card-title">Card title</h5>
        <p class="card-text">This is a longer card with supporting text below as a natural lead-in to additional content. This content is a little bit longer.</p>
      </div>
    </div>
  </div>
</div>

</body>
</html>