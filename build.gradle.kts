plugins {
	java
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.jhh"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot Web
	implementation("org.springframework.boot:spring-boot-starter-web:3.0.2")
	implementation("org.springframework:spring-test")

	// 기존 spring-boot-starter-test는 테스트 코드에서 사용하도록 testImplementation으로 유지
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	// Lombok (컴파일 타임에만 사용)
	implementation("org.projectlombok:lombok:1.18.26")
	annotationProcessor("org.projectlombok:lombok:1.18.26")

	// Kotlin 표준 라이브러리
	implementation(kotlin("stdlib"))
	implementation(kotlin("reflect"))


}

tasks.withType<Test> {
	useJUnitPlatform()
}