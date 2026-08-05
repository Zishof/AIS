<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.common.Common"%>
<%
    // Ambil parameter
    String itemsParam = request.getParameter("item");
    String tabParam = request.getParameter("tab");
    
    // Tentukan mode tampilan (Tab atau List)
    boolean isTabMode = "true".equalsIgnoreCase(tabParam);
    
    // Siapkan list untuk menampung ID yang valid agar tidak perlu split berulang kali
    java.util.List<String> validItemIds = new java.util.ArrayList<String>();
    boolean adaData = false;

    // Parsing data item
    if (itemsParam != null && !itemsParam.trim().isEmpty()) {
        for (String itemId : itemsParam.split(",")) {
            if (itemId != null && !itemId.trim().isEmpty()) {
                validItemIds.add(itemId.trim());
            }
        }
    }
    
    // Set status adaData
    if (!validItemIds.isEmpty()) {
        adaData = true;
    }
%>

<% 
    // JIKA ADA DATA
    if (adaData) { 
%>

    <%-- LOGIKA 1: TAMPILAN TAB (Jika tab="true") --%>
    <% if (isTabMode) { %>
        <div class="fade-in-up">
            <ul class="nav nav-tabs mb-3" id="pertemuanTabs" role="tablist">
                <% 
                for (int i = 0; i < validItemIds.size(); i++) { 
                    String itemId = validItemIds.get(i);
                    Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, itemId.trim(), true);
                    if (pertemuan != null && pertemuan.getId() != null && pertemuan.getAktif()) {
                    	pertemuan.setPertemuanManual(i+1);
                    // Tab pertama aktif secara default
	                    String activeClass = (i == 0) ? "active" : "";
	                %>
	                <li class="nav-item" role="presentation">
	                    <button class="nav-link <%=activeClass%>" 
	                            id="tab-<%=itemId%>-btn" 
	                            data-bs-toggle="tab" 
	                            data-bs-target="#content-<%=itemId%>" 
	                            type="button" 
	                            role="tab" 
	                            aria-selected="<%= (i==0) %>">
	                        <%=Common.getBahasaConfig("Ke") %> <%= (i+1) %>
	                    </button>
	                </li>
                <% 	}
                    }%>
            </ul>

            <div class="tab-content" id="pertemuanTabsContent">
                <% 
                for (int i = 0; i < validItemIds.size(); i++) { 
                    String itemId = validItemIds.get(i);
                    Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, itemId.trim(), true);
                    if (pertemuan != null && pertemuan.getId() != null && pertemuan.getAktif()) {
	                    // Konten pertama aktif secara default
	                    String showActive = (i == 0) ? "show active" : "";
	                %>
	                <div class="tab-pane fade <%=showActive%>" 
	                     id="content-<%=itemId%>" 
	                     role="tabpanel" 
	                     aria-labelledby="tab-<%=itemId%>-btn" id="tampilan_pertemuan_<%=pertemuan.getId()%>">
	                    
	                    <jsp:include page="/WEB-INF/baru/modul/elearning/pertemuan.jsp">
	                        <jsp:param name="pertemuan" value="<%=itemId%>" />
	                    </jsp:include>
	                    
	                </div>
                <% 		}
                    }%>
            </div>
        </div>

    <%-- LOGIKA 2: TAMPILAN LIST/ORIGINAL (Jika tab kosong atau "false") --%>
    <% } else { %>
    
        <div class="d-flex flex-column gap-3">
            <%
            for (String itemId : validItemIds) {
            	Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, itemId.trim(), true);
                if (pertemuan != null && pertemuan.getId() != null && pertemuan.getAktif()) {
		            %>
		                <div class="fade-in-up" id="tampilan_pertemuan_<%=pertemuan.getId()%>"> 
		                    <jsp:include page="/WEB-INF/baru/modul/elearning/pertemuan.jsp">
		                        <jsp:param name="pertemuan" value="<%=itemId%>" />
		                    </jsp:include>
		                </div>
		            <%
                }
            }
            %>
        </div>
        
    <% } %>

<% 
    } // End if (adaData)
%>

<% 
    // TAMPILAN JIKA KOSONG / ERROR
    if (!adaData) { 
%>
    <jsp:include page="/WEB-INF/baru/componen/tidak_ketemu.jsp">
        <jsp:param name="pesan" value="Mohon maaf, data pertemuan tidak tersedia di database atau parameter pencarian tidak valid." />
     </jsp:include>
<% 
    } 
%>