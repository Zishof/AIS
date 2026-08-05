package ais.action.servlet;

import static nl.captcha.Captcha.NAME;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ais.common.Common;
import ais.common.ConstantValues;
import nl.captcha.Captcha;
import nl.captcha.Captcha.Builder;
import nl.captcha.backgrounds.BackgroundProducer;
import nl.captcha.backgrounds.GradiatedBackgroundProducer;
import nl.captcha.gimpy.FishEyeGimpyRenderer;
import nl.captcha.gimpy.GimpyRenderer;
import nl.captcha.noise.NoiseProducer;
import nl.captcha.noise.StraightLineNoiseProducer;
import nl.captcha.servlet.CaptchaServletUtil;
import nl.captcha.text.producer.TextProducer;

public class CaptchaServlet extends HttpServlet {

	private static final long serialVersionUID = 40913456229L;

	/**
	 * Write out the CAPTCHA image stored in the session. If not present,
	 * generate a new <code>Captcha</code> and write out its image.
	 * 
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpSession session = req.getSession();

		Captcha captcha;
		try {
			Builder builder = new Captcha.Builder(Integer.parseInt(ConstantValues.aktifkanCaptchaLokalLebar),
					Integer.parseInt(ConstantValues.aktifkanCaptchaLokalTinggi));

			if (!ConstantValues.aktifkanCaptchaLokalNoice.trim().isEmpty()) {
				builder.addNoise((NoiseProducer) Class.forName(ConstantValues.aktifkanCaptchaLokalNoice).newInstance());
			}

			if (!ConstantValues.aktifkanCaptchaLokalRender.trim().isEmpty()) {
				builder.gimp((GimpyRenderer) Class.forName(ConstantValues.aktifkanCaptchaLokalRender).newInstance());
			}

			if (!ConstantValues.aktifkanCaptchaLokalBackground.trim().isEmpty()) {
				builder.addBackground((BackgroundProducer) Class.forName(ConstantValues.aktifkanCaptchaLokalBackground)
						.newInstance());
			}

			if (!ConstantValues.aktifkanCaptchaLokalText.trim().isEmpty()) {
				builder.addText((TextProducer) Class.forName(ConstantValues.aktifkanCaptchaLokalText).newInstance());
			}

			builder.addBorder();

			captcha = builder.build();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			captcha = new Captcha.Builder(300, 70).addText().addBackground(new GradiatedBackgroundProducer())
					.addNoise(new StraightLineNoiseProducer()).gimp(new FishEyeGimpyRenderer()).addBorder().build();
			Common.tampilErrorJikaAdmin(e); 
		} // Required.
		session.setAttribute(NAME, captcha);
		CaptchaServletUtil.writeImage(resp, captcha.getImage());
	}
}
