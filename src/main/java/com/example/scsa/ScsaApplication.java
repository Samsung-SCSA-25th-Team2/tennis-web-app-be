package com.example.scsa;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing // JPA Auditing 활성화
public class ScsaApplication {

	public static void main(String[] args) {
		// 환경에 따라 .env 파일 선택 (.env.prod 또는 .env.local)
		String envFile = System.getenv("ENV_FILE") != null ? System.getenv("ENV_FILE") : ".env.local";

		Dotenv dotenv = Dotenv.configure()
				.filename(envFile)
				.ignoreIfMissing() // 파일이 없어도 실행 가능하도록 (Spring 설정으로 대체)
				.load();

		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		SpringApplication.run(ScsaApplication.class, args);
	}

}
