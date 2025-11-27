package com.example.scsa;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing // JPA Auditing 활성화
public class ScsaApplication {

	@PostConstruct
	public void started() {
		// timezone UTC 셋팅
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

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
