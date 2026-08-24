<%@ page import="java.util.*,ais.action.master.sosial.helper.*,ais.database.model.sosial.JenisDanaSosial" %>
<% String socialTitle="Tunaikan Donasi"; %>
<%@ include file="_header.jspf" %>
<%
List<SocialProgramView> programs=(List<SocialProgramView>)request.getAttribute("programs");
List<JenisDanaSosial> funds=(List<JenisDanaSosial>)request.getAttribute("funds");
%>
<section class="social-page-head"><div class="social-wrap narrow"><span class="social-eyebrow">Checkout aman</span><h1>Tunaikan ZIS atau Donasi</h1><p>Nominal dana, biaya administrasi gateway, kontribusi operasional, dan total pembayaran akan ditampilkan terpisah.</p></div></section>
<section class="social-section"><div class="social-wrap narrow">
<% if(socialUser==null){ %>
<div class="social-note"><strong>Login AIS diperlukan.</strong> Transaksi tamu belum diaktifkan karena token kepemilikan transaksi belum tersedia pada AIS V1.</div>
<% }else{ %>
<form id="social-checkout" class="social-form" action="<%=request.getContextPath()%>/sosial-api" method="post">
<input type="hidden" name="action" value="donation"><input type="hidden" name="idempotencyKey" value="">
<fieldset><legend>1. Dana dan nominal</legend>
<label>Program (opsional)<select name="programId"><option value="">Dana sosial umum</option><% if(programs!=null)for(SocialProgramView p:programs){ %><option value="<%=p.id%>" data-fund="<%=p.fundTypeId%>"><%=SocialHtml.e(p.name)%></option><% } %></select></label>
<label>Jenis dana<select name="fundTypeId" required><option value="">Pilih jenis dana</option><% if(funds!=null)for(JenisDanaSosial f:funds){ %><option value="<%=f.getId()%>"><%=SocialHtml.e(f.getNama())%></option><% } %></select></label>
<label>Nominal dana<input name="amount" inputmode="decimal" required placeholder="100000"></label>
<label>Kontribusi operasional sukarela <span class="muted">(opsional)</span><input name="contribution" inputmode="decimal" placeholder="0"></label>
</fieldset>
<fieldset><legend>2. Identitas dan niat</legend>
<label class="social-check"><input type="checkbox" name="anonymous" value="true"> Tampilkan sebagai Hamba Allah di area publik</label>
<label>Doa atau pesan <span class="muted">(opsional, dimoderasi)</span><textarea name="prayer" maxlength="1000" rows="4"></textarea></label>
<label class="social-check"><input type="checkbox" name="publicPrayer" value="true"> Izinkan pesan ditampilkan setelah moderasi</label>
</fieldset>
<fieldset><legend>3. Pembayaran</legend><label>Saluran<select name="gatewayId"><option value="smartlink">Smartlink</option></select></label><p class="social-note">Status berhasil hanya diberikan setelah callback atau inquiry server terverifikasi.</p></fieldset>
<button class="social-button" type="submit">Buat Transaksi &amp; Lanjut Bayar</button><div id="checkout-result" class="social-result" aria-live="polite" hidden></div>
</form>
<% } %>
</div></section>
<%@ include file="_footer.jspf" %>
