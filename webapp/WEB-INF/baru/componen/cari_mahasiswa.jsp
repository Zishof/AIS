<%@page import="ais.common.Common"%>
<%
    // Parameter Wajib
    String rand = request.getParameter("rand") == null ? Common.getGeneratedBarCode() : request.getParameter("rand");
    String var_name = request.getParameter("var_name"); // Nama input field
    
    // Parameter Opsional
    String var_value = request.getParameter(var_name) == null ? "" : request.getParameter(var_name).trim();
    
    // Parameter Baru: Nama fungsi JS yang akan dipanggil saat item dipilih
    // Contoh penggunaan di JSP induk: <jsp:param name="on_select" value="myCustomFunction" />
    String onSelectCallback = request.getParameter("on_select"); 
%>

<div class="input-group">
    <span class="input-group-text bg-white text-muted border-end-0">
        <i class="fas fa-search"></i>
    </span>
    
    <input type="text" 
           class="form-control border-start-0" 
           id="input_<%=rand%>"
           name="<%=var_name%>" 
           value="<%=var_value%>" 
           list="datalist_<%=rand%>"
           placeholder="<%=Common.getBahasaConfig("Cari Mahasiswa")%>..."
           autocomplete="off"
           oninput="handleInput<%=rand%>(this)">

    <datalist id="datalist_<%=rand%>"></datalist>
</div>

<script>
    // Variabel untuk debounce (penunda request)
    let timeout<%=rand%> = null;

    /**
     * Fungsi utama yang menangani input user
     */
    function handleInput<%=rand%>(ele) {
        const val = ele.value;
        const listId = 'datalist_<%=rand%>';

        // 1. Cek apakah input cocok dengan salah satu opsi di datalist (User Memilih)
        const opts = document.getElementById(listId).childNodes;
        for (var i = 0; i < opts.length; i++) {
            if (opts[i].value === val) {
                // ITEM DIPILIH!
                console.log("Item selected: " + val);
                
                // Jalankan fungsi callback kustom jika ada
                <% if (onSelectCallback != null && !onSelectCallback.isEmpty()) { %>
                    // Panggil fungsi dengan parameter value lengkap (NIM - Nama)
                    if (typeof <%=onSelectCallback%> === "function") {
                        <%=onSelectCallback%>(val); 
                    }
                <% } %>
                return; // Stop searching
            }
        }

        // 2. Jika input diketik manual, jalankan pencarian ke server (Search)
        // Gunakan timeout agar tidak menembak server setiap ketikan huruf
        clearTimeout(timeout<%=rand%>);
        timeout<%=rand%> = setTimeout(() => {
            fetchData<%=rand%>(val);
        }, 500); // Delay 500ms
    }

    /**
     * Fungsi fetch data dari Servlet
     */
    const fetchData<%=rand%> = async (searchTerm) => {
        if (!searchTerm || searchTerm.length < 2) return; // Minimal 2 huruf

        try {
            // Bersihkan keyword pencarian untuk query (ambil bagian depan jika ada dash)
            const cleanTerm = searchTerm.split("-")[0].trim();

            // Konstruksi JSON Query (Sesuai format asli Anda)
            const reqObj = {
                "action": "daftar",
                "class": "ais.database.model.Mahasiswa",
                "projection": "id;nama;nim",
                "deep": "1",
                "max": "10",
                "halaman": "0",
                "where1": "nama ilike '%" + cleanTerm + "%' or nim ilike '%" + cleanTerm + "%'",
                "order1": "desc",
                "sort1": "tahunangkatan",
                "order2": "asc",
                "sort2": "nim"
            };

            const dataReq = encodeURIComponent(JSON.stringify(reqObj));
            const servletUrl = '<%=request.getContextPath()%>/Data?datasearch=' + dataReq;

            const response = await fetch(servletUrl);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            const result = await response.json();
            const dataList = document.getElementById('datalist_<%=rand%>');

            // Reset opsi
            dataList.innerHTML = '';

            // Populate opsi baru
            if (result.data && Array.isArray(result.data)) {
                result.data.forEach(item => {
                    const option = document.createElement('option');
                    // Format: "NIM - Nama"
                    option.value = item.nim + " - " + item.nama; 
                    dataList.appendChild(option);
                });
            }

        } catch (error) {
            console.error('Error fetching data:', error);
        }
    };

    // Inisialisasi awal (jika ada value tersimpan)
    window.addEventListener('load', function() {
        const initialVal = document.getElementById('input_<%=rand%>').value;
        if(initialVal) {
             fetchData<%=rand%>(initialVal);
        }
    });
</script>