/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ais.common;

import java.util.concurrent.Callable;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 *
 * @author Ridho
 */
public class Sender implements Callable<Void> {

	IMqttClient client;
	public String TOPIC = "pengumuman";
	private String payload;

	public Sender(IMqttClient client, String TOPIC, String payload) {
		this.client = client;
		this.TOPIC = TOPIC;
		this.payload = payload;
	}

	@Override
	public Void call() throws Exception {

		boolean connect = client.isConnected();
		System.out.println("connect => " + connect + ", TOPIC => " + TOPIC + ", payload => " + payload);
		if (!connect) {
			return null;
		}
		MqttMessage msg = readEngineTemp();
		msg.setQos(0);
		msg.setRetained(false);
		client.publish(TOPIC, msg);
		return null;
	}

	private MqttMessage readEngineTemp() {
		// Random rnd = new Random();
		// double temp = 80 + rnd.nextDouble() * 20.0;
		// byte[] payload = "{\"id\": 5, \"message\":\"Lorem ipsum dolor sit
		// amet\"}".getBytes();
		return new MqttMessage(payload.getBytes());
	}
}
