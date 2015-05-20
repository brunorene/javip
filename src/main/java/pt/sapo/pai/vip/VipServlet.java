/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.sapo.pai.vip;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import javaslang.Tuple;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brsantos
 */
@WebServlet(name = "VipServlet", urlPatterns = {"/*"})
public class VipServlet extends HttpServlet {

	private static final Logger log = LoggerFactory.getLogger(VipServlet.class);
	private static final Random rand = new Random();
	private static final long serialVersionUID = -2011159554763671377L;
	private final HttpClientBuilder builder = HttpClientBuilder.create();
	private String[] servers;

	@Override
	public String getServletInfo() {
		return "Server VIP";
	}

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			servers = ((String) new InitialContext().lookup("java:comp/env/servers")).split(",");
		} catch (NamingException ex) {
			throw new ServletException(ex);
		}
	}

	private void processRequest(HttpServletRequest request, HttpServletResponse response, Supplier<HttpRequestBase> supplier)
		throws ServletException, IOException {
		try (OutputStream out = response.getOutputStream();
			 CloseableHttpClient http = builder.build()) {
			Optional.ofNullable(servers[rand.nextInt(2)])
				.map(server -> server.split(":"))
				.map(parts -> Tuple.of(parts[0], Integer.valueOf(parts[1])))
				.ifPresent(server -> {
					try {
						HttpRequestBase method = supplier.get();
						method.setURI(new URI(request.getScheme(), null, server._1, server._2, request.getRequestURI(), request.getQueryString(), null));
						HttpResponse rsp = http.execute(method);
						Collections.list(request.getHeaderNames())
						.forEach(name -> Collections.list(request.getHeaders(name))
							.forEach(value -> method.setHeader(name, value)));
						response.setStatus(rsp.getStatusLine().getStatusCode());
						response.setContentType(rsp.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue());
						IOUtils.copy(rsp.getEntity().getContent(), out);
					} catch (IOException | URISyntaxException ex) {
						log.error(null, ex);
					}
				});
		}
	}

	private Supplier<HttpRequestBase> withEntity(HttpEntityEnclosingRequestBase method, HttpServletRequest request) {
		try {
			method.setEntity(new InputStreamEntity(request.getInputStream()));
		} catch (IOException ex) {
			log.warn(null, ex);
		}
		return () -> method;
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response, () -> new HttpDelete());
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response, () -> new HttpGet());
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response, () -> new HttpHead());
	}

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response, () -> new HttpOptions());
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response, withEntity(new HttpPost(), request));
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response, withEntity(new HttpPut(), request));
	}

	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response, () -> new HttpTrace());
	}

}
