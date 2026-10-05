package com.ieum;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IeumApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(IeumApplication.class);
		// 공통 application.yaml 이 없으므로, 프로필 없이 실행하면 설정이 하나도 로드되지 않는다.
		// 우선순위가 가장 낮은 기본 속성으로 두어 SPRING_PROFILES_ACTIVE=prod 가 항상 이기게 한다.
		application.setDefaultProperties(Map.of("spring.profiles.default", "dev"));
		application.run(args);
	}

}
