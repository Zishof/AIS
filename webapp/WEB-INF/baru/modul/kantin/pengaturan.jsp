<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
//2. SECURITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	out.print("{\"status\":\"error\", \"message\":\""
	+ Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
	return;
}
Pedagang pedagang = tbmuser.getPedagang();
Toko toko = pedagang == null ? null : pedagang.getToko();
String rnd = Common.getGeneratedBarCode(7);

// Menyiapkan path foto (Ganti 'foto_path' dengan getter yang sesuai dari model Anda jika ada)
String pathFotoPedagang = CommonMedia.getUrlFotoPengguna(tbmuser);
%>
<div class="card border-0 shadow-sm rounded-4">
	<div class="card-body p-4">
		<div class="row">
			<div class="col-md-3 border-end pe-4">
				<h6 class="fw-bold text-muted mb-3 text-uppercase small"><%=Common.getBahasaConfig("Menu Master Data")%></h6>
				<div class="nav flex-column nav-pills gap-1"
					id="v-pills-tab<%=rnd%>" role="tablist" aria-orientation="vertical">

					<button class="nav-link active text-start rounded-3 fw-medium py-2"
						id="v-pills-member-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-member<%=rnd%>" type="button" role="tab">
						<i class="fas fa-users me-2 text-primary text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Manajemen Member")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-barang-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-barang<%=rnd%>" type="button" role="tab">
						<i class="fas fa-box-open me-2 text-primary text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Barang & Jasa")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-hargabeli-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-hargabeli<%=rnd%>" type="button"
						role="tab">
						<i
							class="fas fa-file-invoice-dollar me-2 text-success text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Harga Beli (Kulakan)")%>
					</button>


					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-diskon-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-diskon<%=rnd%>" type="button" role="tab">
						<i class="fas fa-percentage me-2 text-danger text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Pengaturan Diskon")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-bayar-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-bayar<%=rnd%>" type="button" role="tab">
						<i class="fas fa-wallet me-2 text-info text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Metode Pembayaran")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-pedagang-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-pedagang<%=rnd%>" type="button"
						role="tab">
						<i class="fas fa-store me-2 text-warning text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Manajemen Pedagang")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-stok-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-stok<%=rnd%>" type="button" role="tab">
						<i class="fas fa-cubes me-2 text-secondary text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Manajemen Stok")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-hadiah-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-hadiah<%=rnd%>" type="button" role="tab">
						<i class="fas fa-gift me-2 text-danger text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Promo & Hadiah")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-meja-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-meja<%=rnd%>" type="button" role="tab">
						<i class="fas fa-chair me-2 text-secondary text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Manajemen Meja")%>
					</button>

					<hr class="my-2">
					<h6 class="fw-bold text-muted mb-2 mt-2 text-uppercase small"><%=Common.getBahasaConfig("Pergudangan")%></h6>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-lokasi-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-lokasi<%=rnd%>" type="button" role="tab">
						<i class="fas fa-warehouse me-2 text-primary text-center" style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Lokasi (Gudang)")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-jenislokasi-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-jenislokasi<%=rnd%>" type="button" role="tab">
						<i class="fas fa-tags me-2 text-primary text-center" style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Jenis Lokasi")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-mutasigudang-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-mutasigudang<%=rnd%>" type="button" role="tab">
						<i class="fas fa-dolly me-2 text-primary text-center" style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Mutasi Gudang")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-pengirimangudang-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-pengirimangudang<%=rnd%>" type="button" role="tab">
						<i class="fas fa-truck-fast me-2 text-primary text-center" style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Pengiriman Antar Gudang")%>
					</button>

					<hr class="my-2">
					<h6 class="fw-bold text-muted mb-2 mt-2 text-uppercase small"><%=Common.getBahasaConfig("Sistem & Kasir")%></h6>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-pajak-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-pajak<%=rnd%>" type="button" role="tab">
						<i class="fas fa-receipt me-2 text-dark text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Pajak & Layanan")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-shift-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-shift<%=rnd%>" type="button" role="tab">
						<i class="fas fa-user-clock me-2 text-dark text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Shift Kasir")%>
					</button>

					<button class="nav-link text-start rounded-3 fw-medium py-2"
						id="v-pills-struk-tab<%=rnd%>" data-bs-toggle="pill"
						data-bs-target="#v-pills-struk<%=rnd%>" type="button" role="tab">
						<i class="fas fa-print me-2 text-dark text-center"
							style="width: 20px;"></i>
						<%=Common.getBahasaConfig("Pengaturan Struk")%>
					</button>
				</div>
			</div>

			<div class="col-md-9 ps-md-4">
				<div class="tab-content" id="v-pills-tabContent<%=rnd%>">

					<div class="tab-pane fade" id="v-pills-lokasi<%=rnd%>" role="tabpanel">
						<jsp:include page="/WEB-INF/baru/modul/kantin/lokasi/index.jsp" />
					</div>
					<div class="tab-pane fade" id="v-pills-jenislokasi<%=rnd%>" role="tabpanel">
						<jsp:include page="/WEB-INF/baru/modul/kantin/jenis_lokasi/index.jsp" />
					</div>
					<div class="tab-pane fade" id="v-pills-mutasigudang<%=rnd%>" role="tabpanel">
						<jsp:include page="/WEB-INF/baru/modul/kantin/mutasi_gudang/index.jsp" />
					</div>
					<div class="tab-pane fade" id="v-pills-pengirimangudang<%=rnd%>" role="tabpanel">
						<jsp:include page="/WEB-INF/baru/modul/kantin/pengiriman_gudang/index.jsp" />
					</div>

					<div class="tab-pane fade show active" id="v-pills-member<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Member")%></h5>
						<ul class="nav nav-tabs mb-3" id="subTabMember<%=rnd%>"
							role="tablist">
							<li class="nav-item" role="presentation">
								<button class="nav-link active" data-bs-toggle="tab"
									data-bs-target="#sub-data-member<%=rnd%>" type="button"
									role="tab"><%=Common.getBahasaConfig("Data Member Baru")%></button>
							</li>
							<li class="nav-item" role="presentation">
								<button class="nav-link" data-bs-toggle="tab"
									data-bs-target="#sub-jenis-member<%=rnd%>" type="button"
									role="tab"><%=Common.getBahasaConfig("Jenis Member")%></button>
							</li>
							<li class="nav-item" role="presentation">
								<button class="nav-link" data-bs-toggle="tab"
									data-bs-target="#sub-tipe-member<%=rnd%>" type="button"
									role="tab"><%=Common.getBahasaConfig("Tipe Member")%></button>
							</li>
						</ul>
						<div class="tab-content">
							<div class="tab-pane fade show active"
								id="sub-data-member<%=rnd%>" role="tabpanel">
								<jsp:include page="/WEB-INF/baru/modul/kantin/member/index.jsp" />
							</div>
							<div class="tab-pane fade" id="sub-jenis-member<%=rnd%>"
								role="tabpanel">
								<jsp:include
									page="/WEB-INF/baru/modul/kantin/member/jenis_anggota_koperasi.jsp" />
							</div>
							<div class="tab-pane fade" id="sub-tipe-member<%=rnd%>"
								role="tabpanel">
								<jsp:include
									page="/WEB-INF/baru/modul/kantin/member/tipe_anggota_koperasi.jsp" />
							</div>
						</div>
					</div>

					<div class="tab-pane fade" id="v-pills-barang<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Barang / Jasa")%></h5>
						<ul class="nav nav-tabs mb-3" id="subTabBarang<%=rnd%>"
							role="tablist">
							<li class="nav-item" role="presentation">
								<button class="nav-link active" data-bs-toggle="tab"
									data-bs-target="#sub-data-barang<%=rnd%>" type="button"
									role="tab"><%=Common.getBahasaConfig("Katalog Barang")%></button>
							</li>
							<li class="nav-item" role="presentation">
								<button class="nav-link" data-bs-toggle="tab"
									data-bs-target="#sub-jenis-barang<%=rnd%>" type="button"
									role="tab"><%=Common.getBahasaConfig("Jenis / Kategori Barang")%></button>
							</li>
						</ul>
						<div class="tab-content">
							<div class="tab-pane fade show active"
								id="sub-data-barang<%=rnd%>" role="tabpanel">
								<jsp:include page="/WEB-INF/baru/modul/kantin/barang/index.jsp" />
							</div>
							<div class="tab-pane fade" id="sub-jenis-barang<%=rnd%>"
								role="tabpanel">
								<jsp:include
									page="/WEB-INF/baru/modul/kantin/barang/jenis_produk.jsp" />
							</div>
						</div>
					</div>

					<div class="tab-pane fade" id="v-pills-hargabeli<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Harga Beli")%></h5>
						<jsp:include page="/WEB-INF/baru/modul/kantin/pengadaan/index.jsp" />
					</div>

					<div class="tab-pane fade" id="v-pills-diskon<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Pengaturan Diskon")%></h5>
						<ul class="nav nav-tabs mb-3" id="subTabDiskon<%=rnd%>"
							role="tablist">
							<li class="nav-item" role="presentation">
								<button class="nav-link active" data-bs-toggle="tab"
									data-bs-target="#sub-aturan-diskon<%=rnd%>" type="button"
									role="tab"><%=Common.getBahasaConfig("Aturan Diskon")%></button>
							</li>
							<li class="nav-item" role="presentation">
								<button class="nav-link" data-bs-toggle="tab"
									data-bs-target="#sub-pencairan-diskon<%=rnd%>" type="button"
									role="tab"><%=Common.getBahasaConfig("Pencairan Diskon")%></button>
							</li>
						</ul>
						<div class="tab-content">
							<div class="tab-pane fade show active"
								id="sub-aturan-diskon<%=rnd%>" role="tabpanel">
								<jsp:include page="/WEB-INF/baru/modul/kantin/diskon/index.jsp" />
							</div>
							<div class="tab-pane fade" id="sub-pencairan-diskon<%=rnd%>"
								role="tabpanel">
								<jsp:include page="/WEB-INF/baru/modul/kantin/diskon/pencairan_diskon.jsp" />
							</div>
						</div>
					</div>

					<div class="tab-pane fade" id="v-pills-bayar<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Metode Pembayaran")%></h5>
						<jsp:include page="/WEB-INF/baru/modul/kantin/cara_bayar/index.jsp" />
					</div>

					<div class="tab-pane fade" id="v-pills-pedagang<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Pedagang")%></h5>
						
						<ul class="nav nav-tabs mb-3" id="subTabPedagang<%=rnd%>"
							role="tablist">
							<li class="nav-item" role="presentation">
								<button class="nav-link active" data-bs-toggle="tab"
									data-bs-target="#sub-toko<%=rnd%>" type="button"
									role="tab"><%=Common.getBahasaConfig("Data Toko (Kios)")%></button>
							</li>
							<li class="nav-item" role="presentation">
								<button class="nav-link" data-bs-toggle="tab"
									data-bs-target="#sub-akun-pedagang<%=rnd%>" type="button"
									role="tab"><%=Common.getBahasaConfig("Akun Pedagang")%></button>
							</li>
						</ul>
						
						<div class="tab-content">
							<div class="tab-pane fade show active"
								id="sub-toko<%=rnd%>" role="tabpanel">
									<jsp:include page="/WEB-INF/baru/modul/kantin/toko_dan_pedagang/toko.jsp" />
								</div>
							
							<div class="tab-pane fade" id="sub-akun-pedagang<%=rnd%>"
								role="tabpanel">
								<jsp:include page="/WEB-INF/baru/modul/kantin/toko_dan_pedagang/pedagang.jsp" />
							</div>
						</div>
					</div>

					<div class="tab-pane fade" id="v-pills-stok<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Stok Barang")%></h5>
						<ul class="nav nav-tabs mb-3" id="subTabStok<%=rnd%>"
							role="tablist">
							<li class="nav-item" role="presentation">
								<button class="nav-link active" data-bs-toggle="tab"
									data-bs-target="#sub-kartu-stok<%=rnd%>" type="button"
									role="tab"><%=Common.getBahasaConfig("Kartu Mutasi Stok")%></button>
							</li>
							<li class="nav-item" role="presentation">
								<button class="nav-link" data-bs-toggle="tab"
									data-bs-target="#sub-stok-opname<%=rnd%>" type="button"
									role="tab"><%=Common.getBahasaConfig("Stok Opname")%></button>
							</li>
						</ul>
						<div class="tab-content">
							<div class="tab-pane fade show active"
								id="sub-kartu-stok<%=rnd%>" role="tabpanel">
								<jsp:include page="/WEB-INF/baru/modul/kantin/stok/mutasi_stok.jsp" />
							</div>
							<div class="tab-pane fade" id="sub-stok-opname<%=rnd%>"
								role="tabpanel">
								<jsp:include page="/WEB-INF/baru/modul/kantin/stok/index.jsp" />
							</div>
						</div>
					</div>

					<div class="tab-pane fade" id="v-pills-hadiah<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Hadiah & Promo")%></h5>
						<p class="text-muted"><%=Common.getBahasaConfig("Aturan promo seperti Beli 3 Gratis 1 atau hadiah produk tertentu.")%></p>
					</div>

					<div class="tab-pane fade" id="v-pills-meja<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Meja")%></h5>
						<p class="text-muted"><%=Common.getBahasaConfig("Pendataan nomor meja untuk status ketersediaan.")%></p>
					</div>

					<div class="tab-pane fade" id="v-pills-pajak<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Pajak & Biaya Layanan")%></h5>
						<p class="text-muted"><%=Common.getBahasaConfig("Konfigurasi PPN atau biaya administrasi koperasi.")%></p>
					</div>

					<div class="tab-pane fade" id="v-pills-shift<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Shift Kasir")%></h5>
						<p class="text-muted"><%=Common.getBahasaConfig("Fitur pembukaan dan penutupan shift kasir harian.")%></p>
					</div>

					<div class="tab-pane fade" id="v-pills-struk<%=rnd%>"
						role="tabpanel">
						<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Pengaturan Struk")%></h5>
						<p class="text-muted"><%=Common.getBahasaConfig("Edit teks pada bagian kepala dan kaki struk belanja.")%></p>
					</div>

				</div>
			</div>
		</div>
	</div>
</div>