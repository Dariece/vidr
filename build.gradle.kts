plugins {
    java
    `java-gradle-plugin`
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
}

group = "de.daniel.marlinghaus"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

gradlePlugin {
    plugins {
        create("VIDR") {
            id = "de.daniel.marlinghaus.vidr"
            implementationClass = "de.daniel.marlinghaus.vidr.VIDRPlugin"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
