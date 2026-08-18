<%@page import="ais.common.Common"%>
<%
String rand = Common.getGeneratedBarCode();
%>

<select id="<%=rand%>">
	<!-- Opsi akan dimuat di sini oleh JavaScript -->
	<option value="">Memuat data...</option>
</select>


<script>
        document.addEventListener('DOMContentLoaded', () => {
            const searchSelect = document.getElementById('<%=rand%>');
            var searchTerm = "";
            var req = "{\"action\":\"daftar\",\"class\": \"ais.database.model.Mahasiswa\",\"projection\":\"id;nama;nim\",\"deep\":\"1\", \"max\":\"10\",\"halaman\":\"0\", \"where1\":\"nama ilike '%"+searchTerm+"%' or nim ilike '%"+searchTerm+"%' \",\"order1\":\"desc\", \"sort1\":\"tahunangkatan\",\"order2\":\"asc\", \"sort2\":\"nim\"}";
            var dataReq = encodeURIComponent(req);
            // URL servlet (ganti dengan URL servlet Java Anda)
            const servletUrl = '<%=request.getContextPath()%>/Data?datasearch='+dataReq;

            // Fungsi untuk memuat data dari servlet
            const fetchData = async () => {
              
                try {
                    // Gunakan Fetch API untuk mendapatkan data dari servlet
                    const response = await fetch(servletUrl);
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    // Asumsi servlet mengembalikan JSON
                    const data = await response.json();

                    // Bersihkan opsi yang ada
                    searchSelect.innerHTML = '<option value="">Pilih item...</option>';
                    
                    //alert(data.data);

                    // Tambahkan setiap item ke dalam select
                    data.data.forEach(item => {
                        const option = document.createElement('option');
                        option.value = item.id;
                        option.textContent = item.nama;
                        searchSelect.appendChild(option);
                    });

                } catch (error) {
                    console.error('Terjadi kesalahan saat memuat data:', error);
                    
                } finally {
                }
            };

            // Panggil fungsi fetchData saat halaman dimuat
            fetchData();

        });
    </script>