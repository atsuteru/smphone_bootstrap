package jp.kk_brain.smphone.glassfish;

import java.io.File;
import java.io.IOException;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.archive.ScatteredArchive;

public class Bootstrap {
	public static void main(String... args) throws GlassFishException {
		
        // Set JSP to use Standard JavaC always
        System.setProperty("org.apache.jasper.compiler.disablejsr199", "false");
		
		// HTTPポートの取得。8080 < 環境変数(HTTP_PORT) < Javaシステムプロパティ(portHttp)の優先順位
		int portHttp = 8080;
		if(null != System.getenv("PORT") && System.getenv("PORT").matches("^[0-9]{1,5}$")) {
			portHttp = Integer.parseInt(System.getenv("PORT"));
		}
		if(null != System.getProperty("portHttp") && System.getProperty("portHttp").matches("^[0-9]{1,5}$")) {
			portHttp = Integer.parseInt(System.getProperty("portHttp"));
		}
		// HTTPSポートの取得。8081 < 環境変数(HTTPS_PORT) < Javaシステムプロパティ(portHttps)の優先順位
		int portHttps = 8081;
		if(null != System.getenv("HTTPS_PORT") && System.getenv("HTTPS_PORT").matches("^[0-9]{1,5}$")) {
			portHttps = Integer.parseInt(System.getenv("HTTPS_PORT"));
		}
		if(null != System.getProperty("portHttps") && System.getProperty("portHttps").matches("^[0-9]{1,5}$")) {
			portHttps = Integer.parseInt(System.getProperty("portHttps"));
		}
		// アプリケーション名の取得。"myapp" < 環境変数(APP_NAME) < Javaシステムプロパティ(appName)の優先順位
		String appName = "works";
		if(null != System.getenv("APP_NAME") && System.getenv("APP_NAME").matches("^[a-zA-Z].+$")) {
			appName = System.getenv("APP_NAME");
		}
		if(null != System.getProperty("appName") && System.getProperty("appName").matches("^[a-zA-Z].+$")) {
			appName = System.getProperty("appName");
		}
		
		GlassFishProperties glassfishProperties = new GlassFishProperties();
		glassfishProperties.setPort("http-listener", portHttp);
		glassfishProperties.setPort("https-listener", portHttps);

		// 埋め込みサーバーの初期化
		// シャットダウン・フックから呼び出すためfinal宣言します
		final GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish(glassfishProperties);

		// シャットダウン・フック登録
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					glassfish.stop();
					System.out.println("GlassFish Stoped.");
					glassfish.dispose();
					System.out.println("GlassFish Disposed.");
				} catch (GlassFishException e) {
					throw new RuntimeException("faild in shutdownHook task.", e);
				}
			}
		}));

		// GlassFish起動
		glassfish.start();
		System.out.println("GlassFish Started.");

		// 資源をGlassFishに配備
		try {
			ScatteredArchive war = new ScatteredArchive(appName, ScatteredArchive.Type.WAR, new File("src/main/webapp"));
			war.addClassPath(new File("target/classes"));
			glassfish.getDeployer().deploy(war.toURI(), "--name=" + appName, "--contextroot=" + appName, "--force=true");
		} catch (IOException e) {
			throw new GlassFishException("faild to deploy.", e);
		}
	}
}
